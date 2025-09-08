package dev.bnacar.distributedratelimiter.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory rate limiter backend.
 * Uses local JVM memory for rate limiter state.
 * This is the fallback when Redis is not available.
 */
public class InMemoryRateLimiterBackend implements RateLimiterBackend {
    
    private final ConcurrentHashMap<String, BucketHolder> buckets;
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultCleanupIntervalMs;
    
    private static class BucketHolder {
        final RateLimiter rateLimiter;
        final RateLimitConfig config;
        volatile long lastAccessTime;

        BucketHolder(RateLimiter rateLimiter, RateLimitConfig config) {
            this.rateLimiter = rateLimiter;
            this.config = config;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    public InMemoryRateLimiterBackend() {
        this(60000); // Default 60 seconds cleanup
    }
    
    public InMemoryRateLimiterBackend(long cleanupIntervalMs) {
        this.buckets = new ConcurrentHashMap<>();
        this.defaultCleanupIntervalMs = cleanupIntervalMs;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "InMemoryRateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        startCleanupTask();
    }
    
    @Override
    public RateLimiter getRateLimiter(String key, RateLimitConfig config) {
        BucketHolder holder = buckets.computeIfAbsent(key, k -> {
            RateLimiter rateLimiter = createRateLimiter(config);
            return new BucketHolder(rateLimiter, config);
        });
        
        holder.updateAccessTime();
        return holder.rateLimiter;
    }
    
    @Override
    public boolean isAvailable() {
        return true; // In-memory is always available
    }
    
    @Override
    public void clear() {
        buckets.clear();
    }
    
    @Override
    public int getActiveCount() {
        return buckets.size();
    }
    
    /**
     * Factory method to create the appropriate rate limiter based on configuration.
     */
    private RateLimiter createRateLimiter(RateLimitConfig config) {
        switch (config.getAlgorithm()) {
            case TOKEN_BUCKET:
                return new TokenBucket(config.getCapacity(), config.getRefillRate());
            case SLIDING_WINDOW:
                return new SlidingWindow(config.getCapacity(), config.getRefillRate());
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + config.getAlgorithm());
        }
    }
    
    private void startCleanupTask() {
        cleanupExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredBuckets,
            defaultCleanupIntervalMs,
            defaultCleanupIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    private void cleanupExpiredBuckets() {
        long currentTime = System.currentTimeMillis();
        buckets.entrySet().removeIf(entry -> {
            BucketHolder holder = entry.getValue();
            // Use the cleanup interval from the bucket's configuration
            long bucketCleanupInterval = holder.config.getCleanupIntervalMs();
            return (currentTime - holder.lastAccessTime) > bucketCleanupInterval;
        });
    }
    
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