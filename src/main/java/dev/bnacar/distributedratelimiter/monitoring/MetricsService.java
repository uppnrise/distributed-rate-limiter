package dev.bnacar.distributedratelimiter.monitoring;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService implements ApplicationListener<ContextClosedEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final Map<String, KeyMetricsData> keyMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalAllowedRequests = new AtomicLong(0);
    private final AtomicLong totalDeniedRequests = new AtomicLong(0);
    private final AtomicLong totalBucketCreations = new AtomicLong(0);
    private final AtomicLong totalBucketCleanups = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private volatile boolean redisConnected = false;
    // Set as soon as the application context begins closing, so the scheduled
    // health check stops running before Spring's Lifecycle beans (e.g. the
    // Redis connection factory) are stopped. This avoids logging expected
    // shutdown noise ("... has been STOPPED. Use start() ...") as an error.
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private ScheduledExecutorService healthCheckExecutor;
    private RedisConnectionFactory redisConnectionFactory;

    private static class KeyMetricsData {
        final AtomicLong allowedRequests = new AtomicLong(0);
        final AtomicLong deniedRequests = new AtomicLong(0);
        final AtomicLong bucketCreations = new AtomicLong(0);
        final AtomicLong totalProcessingTime = new AtomicLong(0);
        volatile long lastAccessTime = System.currentTimeMillis();

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    @Autowired(required = false)
    public void setRedisConnectionFactory(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @PostConstruct
    public void initialize() {
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Redis-Health-Check");
            t.setDaemon(true);
            return t;
        });
        
        // Check Redis health every 30 seconds
        healthCheckExecutor.scheduleWithFixedDelay(this::checkRedisHealth, 0, 30, TimeUnit.SECONDS);
    }

    private void checkRedisHealth() {
        // Skip entirely once shutdown has started: the Redis connection
        // factory may already be stopped even though this task was still
        // queued, and that is an expected condition, not an error.
        if (shuttingDown.get()) {
            return;
        }
        if (redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().ping();
                if (!redisConnected) {
                    logger.info("Redis connection restored");
                }
                setRedisConnected(true);
            } catch (Exception e) {
                if (shuttingDown.get() || isConnectionFactoryStopped(e)) {
                    // The connection factory has been explicitly stopped
                    // (application/context shutdown, or - in tests - the
                    // underlying container being torn down). This is a
                    // terminal, expected lifecycle state rather than an
                    // operational Redis outage: log it quietly and stop
                    // polling a factory that will not recover without an
                    // explicit start().
                    logger.info("Redis connection factory has been stopped; halting health checks: {}",
                            e.getMessage());
                    setRedisConnected(false);
                    stopHealthCheck();
                    return;
                }
                if (redisConnected) {
                    logger.error("Redis connection lost: {}", e.getMessage());
                }
                setRedisConnected(false);
            }
        } else {
            logger.debug("Redis connection factory not available");
        }
    }

    /**
     * Detects whether the given exception indicates that the underlying
     * {@code LettuceConnectionFactory} has reached its terminal STOPPED
     * state, as opposed to a transient network-level connectivity failure.
     * Spring/Lettuce raise a distinctive message for this ("... has been
     * STOPPED. Use start() to initialize it") rather than throwing a
     * generic connection exception.
     */
    private boolean isConnectionFactoryStopped(Exception e) {
        String message = e.getMessage();
        return message != null && message.contains("has been STOPPED");
    }

    /**
     * Reacts to the application context closing by immediately stopping the
     * health-check scheduler. {@link ContextClosedEvent} is published before
     * Spring stops {@code Lifecycle} beans (such as the Redis connection
     * factory) and before {@code @PreDestroy} callbacks run, so cancelling
     * here guarantees no further health checks execute against an
     * already-stopped connection factory.
     */
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        stopHealthCheck();
    }

    @PreDestroy
    public void shutdown() {
        stopHealthCheck();
    }

    private void stopHealthCheck() {
        if (!shuttingDown.compareAndSet(false, true)) {
            return;
        }
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdownNow();
            try {
                healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void recordAllowedRequest(String key) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.allowedRequests.incrementAndGet();
        data.updateAccessTime();
        totalAllowedRequests.incrementAndGet();
        
        logger.debug("Recorded allowed request for key={}, total_allowed={}", 
                key, totalAllowedRequests.get());
    }

    public void recordDeniedRequest(String key) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.deniedRequests.incrementAndGet();
        data.updateAccessTime();
        totalDeniedRequests.incrementAndGet();
        
        logger.info("Recorded denied request for key={}, total_denied={}, denied_ratio={}%", 
                key, totalDeniedRequests.get(), calculateDeniedRatio());
    }

    public void recordBucketCreation(String key) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.bucketCreations.incrementAndGet();
        data.updateAccessTime();
        totalBucketCreations.incrementAndGet();
        
        logger.info("New bucket created for key={}, total_buckets_created={}", 
                key, totalBucketCreations.get());
    }

    public void recordBucketCleanup(int cleanedCount) {
        totalBucketCleanups.addAndGet(cleanedCount);
        
        logger.info("Bucket cleanup completed: cleaned={}, total_cleanups={}", 
                cleanedCount, totalBucketCleanups.get());
    }

    public void recordProcessingTime(String key, long processingTimeMs) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.totalProcessingTime.addAndGet(processingTimeMs);
        data.updateAccessTime();
        totalProcessingTimeMs.addAndGet(processingTimeMs);
        
        if (processingTimeMs > 10) { // Log slow processing
            logger.warn("Slow rate limit processing detected: key={}, processing_time_ms={}", 
                    key, processingTimeMs);
        }
    }

    private double calculateDeniedRatio() {
        long total = totalAllowedRequests.get() + totalDeniedRequests.get();
        return total > 0 ? (double) totalDeniedRequests.get() / total * 100 : 0.0;
    }

    public void setRedisConnected(boolean connected) {
        this.redisConnected = connected;
    }

    public boolean isRedisConnected() {
        return redisConnected;
    }

    public MetricsResponse getMetrics() {
        Map<String, MetricsResponse.KeyMetrics> metrics = new ConcurrentHashMap<>();
        
        keyMetrics.forEach((key, data) -> {
            metrics.put(key, new MetricsResponse.KeyMetrics(
                data.allowedRequests.get(),
                data.deniedRequests.get(),
                data.lastAccessTime
            ));
        });

        return new MetricsResponse(
            metrics,
            redisConnected,
            totalAllowedRequests.get(),
            totalDeniedRequests.get()
        );
    }

    public void clearMetrics() {
        keyMetrics.clear();
        totalAllowedRequests.set(0);
        totalDeniedRequests.set(0);
    }
}