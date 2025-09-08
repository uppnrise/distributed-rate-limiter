package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Distributed rate limiter service that uses Redis as primary backend
 * with automatic fallback to in-memory when Redis is unavailable.
 */
@Service
@Primary
@ConditionalOnProperty(name = "ratelimiter.redis.enabled", havingValue = "true", matchIfMissing = true)
public class DistributedRateLimiterService {
    
    private final ConfigurationResolver configurationResolver;
    private final RateLimiterBackend primaryBackend;
    private final RateLimiterBackend fallbackBackend;
    private volatile boolean usingFallback = false;
    
    @Autowired
    public DistributedRateLimiterService(
            ConfigurationResolver configurationResolver,
            RedisTemplate<String, Object> redisTemplate) {
        this.configurationResolver = configurationResolver;
        this.primaryBackend = new RedisRateLimiterBackend(redisTemplate);
        this.fallbackBackend = new InMemoryRateLimiterBackend();
    }
    
    // Constructor for testing
    public DistributedRateLimiterService(
            ConfigurationResolver configurationResolver,
            RateLimiterBackend primaryBackend,
            RateLimiterBackend fallbackBackend) {
        this.configurationResolver = configurationResolver;
        this.primaryBackend = primaryBackend;
        this.fallbackBackend = fallbackBackend;
    }
    
    public boolean isAllowed(String key, int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        RateLimitConfig config = configurationResolver.resolveConfig(key);
        RateLimiterBackend backend = getAvailableBackend();
        RateLimiter rateLimiter = backend.getRateLimiter(key, config);
        
        return rateLimiter.tryConsume(tokens);
    }
    
    /**
     * Get the currently available backend, with fallback logic.
     */
    private RateLimiterBackend getAvailableBackend() {
        // Check if primary backend (Redis) is available
        if (primaryBackend.isAvailable()) {
            if (usingFallback) {
                // We were using fallback but Redis is back - log the recovery
                usingFallback = false;
            }
            return primaryBackend;
        } else {
            if (!usingFallback) {
                // We just switched to fallback - log the failure
                usingFallback = true;
            }
            return fallbackBackend;
        }
    }
    
    /**
     * Check if currently using Redis backend.
     */
    public boolean isUsingRedis() {
        return !usingFallback && primaryBackend.isAvailable();
    }
    
    /**
     * Check if currently using fallback backend.
     */
    public boolean isUsingFallback() {
        return usingFallback || !primaryBackend.isAvailable();
    }
    
    /**
     * Get the number of active rate limiters in current backend.
     */
    public int getActiveBucketCount() {
        return getAvailableBackend().getActiveCount();
    }
    
    /**
     * Clear all rate limiters from all backends.
     */
    public void clearAllBuckets() {
        primaryBackend.clear();
        fallbackBackend.clear();
        configurationResolver.clearCache();
    }
    
    @PreDestroy
    public void shutdown() {
        if (fallbackBackend instanceof InMemoryRateLimiterBackend) {
            ((InMemoryRateLimiterBackend) fallbackBackend).shutdown();
        }
    }
}