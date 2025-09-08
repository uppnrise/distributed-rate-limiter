package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiter backend.
 * Uses local JVM memory for rate limiter state.
 * This is the fallback when Redis is not available.
 */
@Component
public class InMemoryRateLimiterBackend implements RateLimiterBackend {
    
    private final ConcurrentHashMap<String, BucketHolder> buckets;
    private final ScheduledExecutorService cleanupExecutor;
    private final long defaultCleanupIntervalMs;
    private final AtomicLong cleanupCounter = new AtomicLong(0);
    private final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());
    
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
            // Lower priority for cleanup tasks
            t.setPriority(Thread.NORM_PRIORITY - 1);
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
     * Get cleanup statistics for monitoring.
     */
    public long getCleanupCount() {
        return cleanupCounter.get();
    }
    
    /**
     * Get the last cleanup time.
     */
    public long getLastCleanupTime() {
        return lastCleanupTime.get();
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
            this::cleanupExpiredBucketsAsync,
            defaultCleanupIntervalMs,
            defaultCleanupIntervalMs,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Async cleanup method that can be executed in background.
     */
    @Async("rateLimiterTaskExecutor")
    protected void cleanupExpiredBucketsAsync() {
        cleanupExpiredBuckets();
    }

    private void cleanupExpiredBuckets() {
        long currentTime = System.currentTimeMillis();
        int sizeBefore = buckets.size();
        
        buckets.entrySet().removeIf(entry -> {
            BucketHolder holder = entry.getValue();
            // Use the cleanup interval from the bucket's configuration
            long bucketCleanupInterval = holder.config.getCleanupIntervalMs();
            return (currentTime - holder.lastAccessTime) > bucketCleanupInterval;
        });
        
        int sizeAfter = buckets.size();
        if (sizeBefore != sizeAfter) {
            cleanupCounter.incrementAndGet();
            lastCleanupTime.set(currentTime);
        }
    }
    
    /**
     * Force immediate cleanup for testing or manual triggers.
     */
    public void forceCleanup() {
        cleanupExpiredBuckets();
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