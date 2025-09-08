package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.core.io.ClassPathResource;

import java.util.Collections;
import java.util.List;

/**
 * Redis-based distributed token bucket implementation.
 * Uses Lua scripts for atomic operations.
 */
public class RedisTokenBucket implements RateLimiter {
    
    private final String key;
    private final int capacity;
    private final int refillRate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> tokenBucketScript;
    
    public RedisTokenBucket(String key, int capacity, int refillRate, RedisTemplate<String, Object> redisTemplate) {
        this.key = key;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.redisTemplate = redisTemplate;
        
        // Load Lua script
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/token-bucket.lua"));
        script.setResultType(List.class);
        this.tokenBucketScript = script;
    }
    
    @Override
    public boolean tryConsume(int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                capacity, refillRate, tokens, currentTime
            );
            
            if (result != null && !result.isEmpty()) {
                // Result format: {success, current_tokens, capacity, refill_rate, last_refill}
                Object successValue = result.get(0);
                if (successValue instanceof Number) {
                    return ((Number) successValue).intValue() == 1;
                }
            }
            
            return false;
        } catch (Exception e) {
            // Log error and return false to fail closed
            // In production, you might want to fall back to local rate limiting
            throw new RuntimeException("Redis operation failed", e);
        }
    }
    
    @Override
    public int getCurrentTokens() {
        try {
            long currentTime = System.currentTimeMillis();
            // Use a dummy consume of 0 tokens to get current state
            List<Object> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                capacity, refillRate, 0, currentTime
            );
            
            if (result != null && result.size() >= 2) {
                Object tokensValue = result.get(1);
                if (tokensValue instanceof Number) {
                    return ((Number) tokensValue).intValue();
                }
            }
            
            return capacity; // Default to full bucket if we can't read state
        } catch (Exception e) {
            throw new RuntimeException("Redis operation failed", e);
        }
    }
    
    @Override
    public int getCapacity() {
        return capacity;
    }
    
    @Override
    public int getRefillRate() {
        return refillRate;
    }
    
    @Override
    public long getLastRefillTime() {
        try {
            long currentTime = System.currentTimeMillis();
            // Use a dummy consume of 0 tokens to get current state
            List<Object> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                capacity, refillRate, 0, currentTime
            );
            
            if (result != null && result.size() >= 5) {
                Object timeValue = result.get(4);
                if (timeValue instanceof Number) {
                    return ((Number) timeValue).longValue();
                }
            }
            
            return System.currentTimeMillis(); // Default to current time
        } catch (Exception e) {
            return System.currentTimeMillis(); // Default to current time on error
        }
    }
}