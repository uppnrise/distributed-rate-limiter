package dev.bnacar.distributedratelimiter.adaptive;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.TimeUnit;

/**
 * Collects system metrics for adaptive rate limiting decisions
 */
@Component
public class SystemMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(SystemMetricsCollector.class);
    
    private final MeterRegistry meterRegistry;
    private final HealthContributorRegistry healthRegistry;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    
    public SystemMetricsCollector(MeterRegistry meterRegistry, HealthContributorRegistry healthRegistry) {
        this.meterRegistry = meterRegistry;
        this.healthRegistry = healthRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }
    
    /**
     * Get current system health metrics
     */
    public SystemHealth getCurrentHealth() {
        return SystemHealth.builder()
            .cpuUtilization(getCPUUtilization())
            .memoryUtilization(getMemoryUtilization())
            .responseTimeP95(getResponseTimeP95())
            .errorRate(getErrorRate())
            .redisHealthy(isRedisHealthy())
            .downstreamServicesHealthy(true) // Default to true for now
            .build();
    }
    
    /**
     * Evaluate system capacity and generate adaptation signal
     */
    public AdaptationSignal evaluateSystemCapacity() {
        SystemHealth health = getCurrentHealth();
        
        // Reduce limits if system is under stress
        if (health.getCpuUtilization() > 0.8 || health.getResponseTimeP95() > 2000) {
            logger.warn("System under stress - CPU: {}%, Response Time P95: {}ms", 
                       health.getCpuUtilization() * 100, health.getResponseTimeP95());
            return AdaptationSignal.REDUCE_LIMITS;
        } 
        
        // Increase limits if system has capacity
        if (health.getCpuUtilization() < 0.3 && health.getErrorRate() < 0.001) {
            logger.info("System has capacity - CPU: {}%, Error Rate: {}", 
                       health.getCpuUtilization() * 100, health.getErrorRate());
            return AdaptationSignal.INCREASE_LIMITS;
        }
        
        // Maintain current limits if system is stable
        return AdaptationSignal.MAINTAIN_LIMITS;
    }
    
    /**
     * Get CPU utilization (0.0 to 1.0)
     */
    private double getCPUUtilization() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            double cpuLoad = sunOsBean.getProcessCpuLoad();
            // If CPU load is negative or unavailable, return 0
            return cpuLoad >= 0 ? cpuLoad : 0.0;
        }
        // Fallback: use system load average
        double loadAverage = osBean.getSystemLoadAverage();
        int processors = osBean.getAvailableProcessors();
        if (loadAverage >= 0 && processors > 0) {
            return Math.min(loadAverage / processors, 1.0);
        }
        return 0.0;
    }
    
    /**
     * Get memory utilization (0.0 to 1.0)
     */
    private double getMemoryUtilization() {
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        if (maxMemory > 0) {
            return (double) usedMemory / maxMemory;
        }
        return 0.0;
    }
    
    /**
     * Get 95th percentile response time in milliseconds
     */
    private double getResponseTimeP95() {
        try {
            // Look for common HTTP request timer metrics
            Timer timer = meterRegistry.find("http.server.requests").timer();
            if (timer != null && timer.count() > 0) {
                double p95 = timer.percentile(0.95, TimeUnit.MILLISECONDS);
                if (!Double.isNaN(p95)) {
                    return p95;
                }
                // Fallback to max if percentiles are not configured
                return timer.max(TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve response time metrics", e);
        }
        return 0.0;
    }
    
    /**
     * Get error rate (0.0 to 1.0)
     */
    private double getErrorRate() {
        try {
            // Calculate error rate from metrics
            long totalRequests = getTotalRequests();
            long errorRequests = getErrorRequests();
            
            if (totalRequests > 0) {
                return (double) errorRequests / totalRequests;
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve error rate metrics", e);
        }
        return 0.0;
    }
    
    private long getTotalRequests() {
        try {
            return (long) meterRegistry.find("http.server.requests").counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private long getErrorRequests() {
        try {
            long errorRequests = 0;
            for (int status = 500; status < 600; status++) {
                errorRequests += (long) meterRegistry.find("http.server.requests")
                    .tag("status", String.valueOf(status))
                    .counters()
                    .stream()
                    .mapToDouble(counter -> counter.count())
                    .sum();
            }
            return errorRequests;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Check if Redis is healthy
     */
    private boolean isRedisHealthy() {
        try {
            Object contributor = healthRegistry.getContributor("redis");
            if (contributor instanceof HealthIndicator) {
                HealthIndicator redisHealth = (HealthIndicator) contributor;
                Health health = redisHealth.health();
                return health.getStatus().equals(org.springframework.boot.actuate.health.Status.UP);
            }
        } catch (Exception e) {
            logger.debug("Could not retrieve Redis health status", e);
        }
        // Default to true if we can't determine health
        return true;
    }
}
