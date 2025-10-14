package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.core.io.ClassPathResource;

import java.util.Collections;
import java.util.List;

/**
 * Redis-based distributed fixed window implementation.
 * Uses Lua scripts for atomic operations.
 */
public class RedisFixedWindow implements RateLimiter {
    
    private final String key;
    private final int capacity;
    private final int refillRate; // For compatibility
    private final long windowDurationMs;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> fixedWindowScript;
    
    public RedisFixedWindow(String key, int capacity, int refillRate, RedisTemplate<String, Object> redisTemplate) {
        this(key, capacity, refillRate, 60000, redisTemplate); // Default 1-minute window
    }
    
    public RedisFixedWindow(String key, int capacity, int refillRate, long windowDurationMs, RedisTemplate<String, Object> redisTemplate) {
        this.key = key;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.windowDurationMs = windowDurationMs;
        this.redisTemplate = redisTemplate;
        
        // Load Lua script
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/fixed-window.lua"));
        script.setResultType(List.class);
        this.fixedWindowScript = script;
    }
    
    @Override
    public boolean tryConsume(int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                capacity, windowDurationMs, tokens, currentTime
            );
            
            if (result != null && !result.isEmpty()) {
                // Result: {success, remaining_tokens, current_count, window_start, current_time}
                Number success = (Number) result.get(0);
                return success != null && success.intValue() == 1;
            }
            
            return false;
        } catch (Exception e) {
            // In case of Redis failure, fail closed (deny request)
            return false;
        }
    }
    
    @Override
    public int getCurrentTokens() {
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                capacity, windowDurationMs, 0, currentTime // 0 tokens = query only
            );
            
            if (result != null && result.size() >= 2) {
                // Result: {success, remaining_tokens, current_count, window_start, current_time}
                Number remainingTokens = (Number) result.get(1);
                return remainingTokens != null ? remainingTokens.intValue() : 0;
            }
            
            return capacity; // Default to full capacity if query fails
        } catch (Exception e) {
            return capacity; // Default to full capacity on error
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
            List<Object> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                capacity, windowDurationMs, 0, currentTime // 0 tokens = query only
            );
            
            if (result != null && result.size() >= 4) {
                // Result: {success, remaining_tokens, current_count, window_start, current_time}
                Number windowStart = (Number) result.get(3);
                return windowStart != null ? windowStart.longValue() : currentTime;
            }
            
            return currentTime;
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
    
    /**
     * Get the window duration in milliseconds.
     */
    public long getWindowDurationMs() {
        return windowDurationMs;
    }
    
    /**
     * Get current usage within the window.
     */
    public int getCurrentUsage() {
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                fixedWindowScript,
                Collections.singletonList(key),
                capacity, windowDurationMs, 0, currentTime // 0 tokens = query only
            );
            
            if (result != null && result.size() >= 3) {
                // Result: {success, remaining_tokens, current_count, window_start, current_time}
                Number currentCount = (Number) result.get(2);
                return currentCount != null ? currentCount.intValue() : 0;
            }
            
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get the time remaining in current window.
     */
    public long getWindowTimeRemaining() {
        try {
            long currentTime = System.currentTimeMillis();
            long currentWindowStart = (currentTime / windowDurationMs) * windowDurationMs;
            long windowEnd = currentWindowStart + windowDurationMs;
            return Math.max(0, windowEnd - currentTime);
        } catch (Exception e) {
            return 0;
        }
    }
}