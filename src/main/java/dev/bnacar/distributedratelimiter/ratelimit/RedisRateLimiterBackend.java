package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.dao.DataAccessException;

import java.util.Set;

/**
 * Redis-based distributed rate limiter backend.
 * Uses Redis for shared state across multiple application instances.
 */
public class RedisRateLimiterBackend implements RateLimiterBackend {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;
    
    public RedisRateLimiterBackend(RedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, "rate_limit:");
    }
    
    public RedisRateLimiterBackend(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }
    
    @Override
    public RateLimiter getRateLimiter(String key, RateLimitConfig config) {
        String redisKey = keyPrefix + key;
        return new RedisTokenBucket(redisKey, config.getCapacity(), config.getRefillRate(), redisTemplate);
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple ping to check Redis connectivity
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                connection.ping();
                return null;
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void clear() {
        try {
            // Delete all keys with our prefix
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // Ignore errors during cleanup
        }
    }
    
    @Override
    public int getActiveCount() {
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}