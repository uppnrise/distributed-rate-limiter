package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final int capacity;
    private final int refillRate;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, BucketHolder> buckets;
    private final ScheduledExecutorService cleanupExecutor;

    private static class BucketHolder {
        final TokenBucket bucket;
        volatile long lastAccessTime;

        BucketHolder(TokenBucket bucket) {
            this.bucket = bucket;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    @Autowired
    public RateLimiterService(RateLimiterConfiguration config) {
        this(config.getCapacity(), config.getRefillRate(), config.getCleanupIntervalMs());
    }

    public RateLimiterService() {
        this(10, 2, 60000); // Default: 10 capacity, 2 tokens/sec, 60s cleanup interval
    }

    public RateLimiterService(int capacity, int refillRate) {
        this(capacity, refillRate, 60000); // Default 60s cleanup interval
    }

    public RateLimiterService(int capacity, int refillRate, long cleanupIntervalMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.buckets = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        startCleanupTask();
    }

    public boolean isAllowed(String key, int tokens) {
        if (tokens <= 0) {
            return false;
        }

        BucketHolder holder = buckets.computeIfAbsent(key, k -> 
            new BucketHolder(new TokenBucket(capacity, refillRate))
        );
        
        holder.updateAccessTime();
        return holder.bucket.tryConsume(tokens);
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredBuckets,
            cleanupIntervalMs,
            cleanupIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    private void cleanupExpiredBuckets() {
        long currentTime = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> {
            BucketHolder holder = entry.getValue();
            return (currentTime - holder.lastAccessTime) > cleanupIntervalMs;
        });
    }

    // Package-private method for testing
    int getBucketCount() {
        return buckets.size();
    }

    // Shutdown method for cleanup
    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}