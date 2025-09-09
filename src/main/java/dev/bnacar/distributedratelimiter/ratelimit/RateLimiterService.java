package dev.bnacar.distributedratelimiter.ratelimit;

import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    private final ConfigurationResolver configurationResolver;
    private final long cleanupIntervalMs;
    private final ConcurrentHashMap<String, BucketHolder> buckets;
    private final ScheduledExecutorService cleanupExecutor;
    private final MetricsService metricsService;

    @Autowired
    public RateLimiterService(ConfigurationResolver configurationResolver, 
                             RateLimiterConfiguration config, 
                             MetricsService metricsService) {
        this.configurationResolver = configurationResolver;
        this.cleanupIntervalMs = config.getCleanupIntervalMs();
        this.buckets = new ConcurrentHashMap<>();
        this.metricsService = metricsService;
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
        this(DefaultConfiguration.RESOLVER, DefaultConfiguration.INSTANCE, null);
    }

    public RateLimiterService(ConfigurationResolver configurationResolver, RateLimiterConfiguration config) {
        this.configurationResolver = configurationResolver;
        this.cleanupIntervalMs = config.getCleanupIntervalMs();
        this.buckets = new ConcurrentHashMap<>();
        this.metricsService = null; // No metrics for testing constructors
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimiter-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Start cleanup task
        startCleanupTask();
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
        this.metricsService = null; // No metrics for testing constructors
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
            logger.warn("Invalid token request: key={}, tokens={}", key, tokens);
            return false;
        }

        long startTime = System.currentTimeMillis();
        BucketHolder holder = buckets.computeIfAbsent(key, k -> {
            RateLimitConfig config = configurationResolver.resolveConfig(k);
            RateLimiter rateLimiter = createRateLimiter(config);
            logger.debug("Created new bucket for key={}, capacity={}, refillRate={}, algorithm={}", 
                    k, config.getCapacity(), config.getRefillRate(), config.getAlgorithm());
            
            // Record bucket creation metric
            if (metricsService != null) {
                metricsService.recordBucketCreation(k);
            }
            
            return new BucketHolder(rateLimiter, config);
        });
        
        holder.updateAccessTime();
        boolean allowed = holder.tryConsume(tokens);
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Structured logging for rate limit events
        if (allowed) {
            logger.debug("Rate limit ALLOWED: key={}, tokens_requested={}, remaining_tokens={}, processing_time_ms={}", 
                    key, tokens, getCurrentTokens(holder), processingTime);
        } else {
            logger.warn("Rate limit VIOLATED: key={}, tokens_requested={}, available_tokens={}, capacity={}, refill_rate={}, processing_time_ms={}", 
                    key, tokens, getCurrentTokens(holder), holder.config.getCapacity(), 
                    holder.config.getRefillRate(), processingTime);
            
            // Add rate limit violation context to MDC
            MDC.put("rate_limit_violation", "true");
            MDC.put("rate_limit_key", key);
            MDC.put("rate_limit_tokens_requested", String.valueOf(tokens));
            MDC.put("rate_limit_available_tokens", String.valueOf(getCurrentTokens(holder)));
            try {
                logger.info("Rate limit violation details captured for analysis");
            } finally {
                // Clear rate limit specific MDC entries
                MDC.remove("rate_limit_violation");
                MDC.remove("rate_limit_key");
                MDC.remove("rate_limit_tokens_requested");
                MDC.remove("rate_limit_available_tokens");
            }
        }
        
        // Record metrics if available
        if (metricsService != null) {
            if (allowed) {
                metricsService.recordAllowedRequest(key);
            } else {
                metricsService.recordDeniedRequest(key);
            }
            metricsService.recordProcessingTime(key, processingTime);
        }
        
        return allowed;
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

    /**
     * Get current token count for logging purposes.
     */
    private int getCurrentTokens(BucketHolder holder) {
        if (holder.rateLimiter instanceof TokenBucket) {
            return ((TokenBucket) holder.rateLimiter).getCurrentTokens();
        }
        // For other algorithms, return -1 to indicate unavailable
        return -1;
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
        int initialCount = buckets.size();
        
        buckets.entrySet().removeIf(entry -> {
            BucketHolder holder = entry.getValue();
            // Use the cleanup interval from the bucket's configuration
            long bucketCleanupInterval = holder.config.getCleanupIntervalMs();
            boolean shouldRemove = (currentTime - holder.lastAccessTime) > bucketCleanupInterval;
            
            if (shouldRemove) {
                logger.debug("Cleaning up expired bucket: key={}, last_access={}ms_ago", 
                        entry.getKey(), currentTime - holder.lastAccessTime);
            }
            
            return shouldRemove;
        });
        
        int finalCount = buckets.size();
        int cleanedCount = initialCount - finalCount;
        
        if (cleanedCount > 0) {
            logger.info("Bucket cleanup completed: removed={}, remaining={}", 
                    cleanedCount, finalCount);
            
            // Record cleanup metrics
            if (metricsService != null) {
                metricsService.recordBucketCleanup(cleanedCount);
            }
        }
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

    /**
     * Get configuration for a specific key.
     * @param key the key to get configuration for
     * @return the configuration or null if not found
     */
    public RateLimitConfig getKeyConfiguration(String key) {
        BucketHolder holder = buckets.get(key);
        if (holder != null) {
            return holder.config;
        }
        // If no active bucket, resolve configuration without creating bucket
        return configurationResolver.resolveConfig(key);
    }

    /**
     * Remove a specific key's bucket.
     * @param key the key to remove
     * @return true if the key was removed, false if not found
     */
    public boolean removeKey(String key) {
        return buckets.remove(key) != null;
    }

    /**
     * Get all active keys with their statistics.
     * @return a list of key names with their stats
     */
    public java.util.List<String> getActiveKeys() {
        return new java.util.ArrayList<>(buckets.keySet());
    }

    /**
     * Get statistics for all active keys.
     * @return a map of key to bucket holder for admin purposes
     */
    public java.util.Map<String, BucketHolder> getBucketHolders() {
        return new java.util.HashMap<>(buckets);
    }

    /**
     * Get statistics for a specific active key.
     * @param key the key to get stats for
     * @return bucket holder or null if not found
     */
    public BucketHolder getBucketHolder(String key) {
        return buckets.get(key);
    }

    /**
     * Make BucketHolder accessible for admin operations.
     */
    public static class BucketHolder {
        final RateLimiter rateLimiter;
        final RateLimitConfig config;
        volatile long lastAccessTime;

        public BucketHolder(RateLimiter rateLimiter, RateLimitConfig config) {
            this.rateLimiter = rateLimiter;
            this.config = config;
            this.lastAccessTime = System.currentTimeMillis();
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        boolean tryConsume(int tokens) {
            return rateLimiter.tryConsume(tokens);
        }

        public RateLimitConfig getConfig() {
            return config;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
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