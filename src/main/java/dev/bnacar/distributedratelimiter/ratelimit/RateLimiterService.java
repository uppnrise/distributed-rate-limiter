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

    private final ConfigurationResolver configurationResolver;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, BucketHolder> buckets;
    private final ScheduledExecutorService cleanupExecutor;

    private static class BucketHolder {
        final TokenBucket bucket;
        final RateLimitConfig config;
        volatile long lastAccessTime;

        BucketHolder(TokenBucket bucket, RateLimitConfig config) {
            this.bucket = bucket;
            this.config = config;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    @Autowired
    public RateLimiterService(ConfigurationResolver configurationResolver, RateLimiterConfiguration config) {
        this.configurationResolver = configurationResolver;
        this.cleanupIntervalMs = config.getCleanupIntervalMs();
        this.buckets = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        startCleanupTask();
    }

    // Constructors for backward compatibility and testing
    public RateLimiterService() {
        this(DefaultConfiguration.RESOLVER, DefaultConfiguration.INSTANCE);
    }

    public RateLimiterService(int capacity, int refillRate) {
        this(capacity, refillRate, 60000); // Default 60s cleanup interval
    }

    public RateLimiterService(int capacity, int refillRate, long cleanupIntervalMs) {
        RateLimiterConfiguration config = createDefaultConfiguration();
        config.setCapacity(capacity);
        config.setRefillRate(refillRate);
        config.setCleanupIntervalMs(cleanupIntervalMs);
        
        this.configurationResolver = new ConfigurationResolver(config);
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

    private static RateLimiterConfiguration createDefaultConfiguration() {
        return new RateLimiterConfiguration();
    }

    // Static holder for default configuration to avoid duplicate creation
    private static final class DefaultConfiguration {
        static final RateLimiterConfiguration INSTANCE = createDefaultConfiguration();
        static final ConfigurationResolver RESOLVER = new ConfigurationResolver(INSTANCE);
    }

    public boolean isAllowed(String key, int tokens) {
        if (tokens <= 0) {
            return false;
        }

        BucketHolder holder = buckets.computeIfAbsent(key, k -> {
            RateLimitConfig config = configurationResolver.resolveConfig(k);
            TokenBucket bucket = new TokenBucket(config.getCapacity(), config.getRefillRate());
            return new BucketHolder(bucket, config);
        });
        
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
            // Use the cleanup interval from the bucket's configuration
            long bucketCleanupInterval = holder.config.getCleanupIntervalMs();
            return (currentTime - holder.lastAccessTime) > bucketCleanupInterval;
        });
    }

    // Public method for monitoring bucket count
    public int getBucketCount() {
        return buckets.size();
    }

    /**
     * Clear all buckets and configuration cache. Useful for configuration reloading.
     */
    public void clearBuckets() {
        buckets.clear();
        configurationResolver.clearCache();
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