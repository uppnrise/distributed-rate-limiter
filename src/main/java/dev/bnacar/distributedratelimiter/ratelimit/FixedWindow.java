package dev.bnacar.distributedratelimiter.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed window rate limiter implementation.
 * Uses fixed time windows with counter reset at window boundaries.
 * Provides memory-efficient rate limiting with predictable reset times.
 */
public class FixedWindow implements RateLimiter {
    
    private final int capacity;
    private final int refillRate; // For compatibility - represents requests per window
    private final long windowDurationMs;
    private final AtomicInteger currentCount;
    private volatile long windowStartTime;
    
    public FixedWindow(int capacity, int refillRate) {
        this(capacity, refillRate, 60000); // Default 1-minute window
    }
    
    public FixedWindow(int capacity, int refillRate, long windowDurationMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.windowDurationMs = windowDurationMs;
        this.currentCount = new AtomicInteger(0);
        this.windowStartTime = System.currentTimeMillis();
    }
    
    @Override
    public synchronized boolean tryConsume(int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check if we need to reset the window
        if (currentTime - windowStartTime >= windowDurationMs) {
            resetWindow(currentTime);
        }
        
        // Check if adding tokens would exceed capacity
        int current = currentCount.get();
        if (current + tokens > capacity) {
            return false;
        }
        
        // Consume tokens
        currentCount.addAndGet(tokens);
        return true;
    }
    
    @Override
    public int getCurrentTokens() {
        long currentTime = System.currentTimeMillis();
        
        synchronized (this) {
            // Check if we need to reset the window
            if (currentTime - windowStartTime >= windowDurationMs) {
                resetWindow(currentTime);
            }
            
            return capacity - currentCount.get();
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
        return windowStartTime;
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
        long currentTime = System.currentTimeMillis();
        
        synchronized (this) {
            // Check if we need to reset the window
            if (currentTime - windowStartTime >= windowDurationMs) {
                resetWindow(currentTime);
            }
            
            return currentCount.get();
        }
    }
    
    /**
     * Get the time remaining in current window.
     */
    public long getWindowTimeRemaining() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - windowStartTime;
        
        if (elapsed >= windowDurationMs) {
            return 0;
        }
        
        return windowDurationMs - elapsed;
    }
    
    private void resetWindow(long currentTime) {
        this.windowStartTime = currentTime;
        this.currentCount.set(0);
    }
}