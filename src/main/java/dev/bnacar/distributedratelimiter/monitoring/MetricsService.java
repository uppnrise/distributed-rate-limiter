package dev.bnacar.distributedratelimiter.monitoring;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {
    
    private final Map<String, KeyMetricsData> keyMetrics = new ConcurrentHashMap<>();
    private final AtomicLong totalAllowedRequests = new AtomicLong(0);
    private final AtomicLong totalDeniedRequests = new AtomicLong(0);
    private volatile boolean redisConnected = false;
    private ScheduledExecutorService healthCheckExecutor;
    private RedisConnectionFactory redisConnectionFactory;

    private static class KeyMetricsData {
        final AtomicLong allowedRequests = new AtomicLong(0);
        final AtomicLong deniedRequests = new AtomicLong(0);
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
        if (redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().ping();
                setRedisConnected(true);
            } catch (Exception e) {
                setRedisConnected(false);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void recordAllowedRequest(String key) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.allowedRequests.incrementAndGet();
        data.updateAccessTime();
        totalAllowedRequests.incrementAndGet();
    }

    public void recordDeniedRequest(String key) {
        KeyMetricsData data = keyMetrics.computeIfAbsent(key, k -> new KeyMetricsData());
        data.deniedRequests.incrementAndGet();
        data.updateAccessTime();
        totalDeniedRequests.incrementAndGet();
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