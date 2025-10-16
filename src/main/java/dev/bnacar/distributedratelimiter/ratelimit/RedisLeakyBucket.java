package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.core.io.ClassPathResource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Redis-based distributed leaky bucket implementation.
 * Uses Lua scripts for atomic queue operations and consistent leak rate processing.
 */
public class RedisLeakyBucket implements RateLimiter {
    
    private final String key;
    private final int queueCapacity;
    private final double leakRatePerSecond;
    private final long maxQueueTimeMs;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> leakyBucketScript;
    
    public RedisLeakyBucket(String key, int queueCapacity, double leakRatePerSecond, 
                           long maxQueueTimeMs, RedisTemplate<String, Object> redisTemplate) {
        this.key = "leaky_bucket:" + key;
        this.queueCapacity = queueCapacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.maxQueueTimeMs = maxQueueTimeMs;
        this.redisTemplate = redisTemplate;
        
        // Load Lua script
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/leaky-bucket.lua"));
        script.setResultType(List.class);
        this.leakyBucketScript = script;
    }
    
    /**
     * Convenience constructor with default max queue time.
     */
    public RedisLeakyBucket(String key, int queueCapacity, double leakRatePerSecond, 
                           RedisTemplate<String, Object> redisTemplate) {
        this(key, queueCapacity, leakRatePerSecond, 5000, redisTemplate);
    }
    
    @Override
    public boolean tryConsume(int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                leakyBucketScript,
                Collections.singletonList(key),
                queueCapacity, leakRatePerSecond, tokens, currentTime, maxQueueTimeMs
            );
            
            if (result != null && !result.isEmpty()) {
                // Result format: {success, queue_size, capacity, leak_rate, last_leak_time, estimated_wait_ms}
                Object successValue = result.get(0);
                if (successValue instanceof Number) {
                    return ((Number) successValue).intValue() == 1;
                }
            }
            
            return false;
        } catch (Exception e) {
            // Log error and return false to fail closed
            // In production, you might want to fall back to local rate limiting
            throw new RuntimeException("Redis leaky bucket operation failed for key: " + key, e);
        }
    }
    
    @Override
    public int getCurrentTokens() {
        try {
            long currentTime = System.currentTimeMillis();
            // Use a dummy consume of 0 tokens to get current state
            List<Object> result = redisTemplate.execute(
                leakyBucketScript,
                Collections.singletonList(key),
                queueCapacity, leakRatePerSecond, 0, currentTime, maxQueueTimeMs
            );
            
            if (result != null && result.size() >= 2) {
                Object queueSizeValue = result.get(1);
                if (queueSizeValue instanceof Number) {
                    int currentQueueSize = ((Number) queueSizeValue).intValue();
                    // Return available queue capacity (similar to available tokens)
                    return Math.max(0, queueCapacity - currentQueueSize);
                }
            }
            
            return queueCapacity; // Default to full capacity if we can't read state
        } catch (Exception e) {
            throw new RuntimeException("Redis leaky bucket state query failed for key: " + key, e);
        }
    }
    
    @Override
    public int getCapacity() {
        return queueCapacity;
    }
    
    @Override
    public int getRefillRate() {
        return (int) Math.ceil(leakRatePerSecond);
    }
    
    /**
     * Get the exact leak rate as a double.
     */
    public double getLeakRatePerSecond() {
        return leakRatePerSecond;
    }
    
    /**
     * Get the maximum queue time in milliseconds.
     */
    public long getMaxQueueTimeMs() {
        return maxQueueTimeMs;
    }
    
    @Override
    public long getLastRefillTime() {
        try {
            long currentTime = System.currentTimeMillis();
            // Use a dummy consume of 0 tokens to get current state
            List<Object> result = redisTemplate.execute(
                leakyBucketScript,
                Collections.singletonList(key),
                queueCapacity, leakRatePerSecond, 0, currentTime, maxQueueTimeMs
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
    
    /**
     * Get current queue size from Redis.
     */
    public int getQueueSize() {
        try {
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(
                leakyBucketScript,
                Collections.singletonList(key),
                queueCapacity, leakRatePerSecond, 0, currentTime, maxQueueTimeMs
            );
            
            if (result != null && result.size() >= 2) {
                Object queueSizeValue = result.get(1);
                if (queueSizeValue instanceof Number) {
                    return ((Number) queueSizeValue).intValue();
                }
            }
            
            return 0;
        } catch (Exception e) {
            return 0; // Return 0 on error
        }
    }
    
    /**
     * Get estimated wait time for a new request.
     * Returns the estimated time in milliseconds before a request would be processed.
     * 
     * @param tokens Number of tokens to estimate for
     * @return Estimated wait time in milliseconds, or -1 if would be rejected
     */
    public long getEstimatedWaitTime(int tokens) {
        if (tokens <= 0) {
            return 0;
        }
        
        try {
            // Simulate adding the request to see estimated wait time
            int currentQueueSize = getQueueSize();
            int availableCapacity = queueCapacity - currentQueueSize;
            
            if (tokens > availableCapacity) {
                return -1; // Would be rejected
            }
            
            // Estimate processing time based on current queue and leak rate
            double totalProcessingTime = (currentQueueSize + tokens) / leakRatePerSecond * 1000;
            return (long) totalProcessingTime;
            
        } catch (Exception e) {
            return -1; // Error case
        }
    }
    
    /**
     * Attempt to enqueue a request asynchronously (simulation).
     * Note: This is a simulation since Redis operations are synchronous,
     * but it provides the estimated behavior of the leaky bucket.
     * 
     * @param tokens Number of tokens to consume
     * @return CompletableFuture that represents the eventual processing result
     */
    public CompletableFuture<Boolean> enqueueRequest(int tokens) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            boolean success = tryConsume(tokens);
            if (success) {
                // In a real implementation, this would wait for actual processing
                // For now, we complete immediately for simplicity
                future.complete(true);
            } else {
                future.complete(false);
            }
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Clear all queued requests (admin operation).
     * Useful for testing or emergency situations.
     */
    public void clearQueue() {
        try {
            String queueListKey = key + ":queue";
            String metadataKey = key + ":meta";
            
            redisTemplate.delete(queueListKey);
            redisTemplate.delete(metadataKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear leaky bucket queue for key: " + key, e);
        }
    }
}