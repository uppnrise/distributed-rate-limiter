package dev.bnacar.distributedratelimiter.ratelimit;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding window rate limiter implementation.
 * Tracks requests within a fixed time window and allows requests
 * if they don't exceed the configured capacity within that window.
 */
public class SlidingWindow {
    
    private final int capacity;
    private final int refillRate; // For interface compatibility - represents requests per second
    private final long windowSizeMs;
    private final ConcurrentLinkedDeque<RequestRecord> requests;
    private final AtomicInteger currentCount;
    
    private static class RequestRecord {
        final long timestamp;
        final int tokens;
        
        RequestRecord(long timestamp, int tokens) {
            this.timestamp = timestamp;
            this.tokens = tokens;
        }
    }
    
    public SlidingWindow(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.windowSizeMs = 1000; // 1 second window - can be made configurable later
        this.requests = new ConcurrentLinkedDeque<>();
        this.currentCount = new AtomicInteger(0);
    }
    
    /**
     * Attempts to consume the specified number of tokens.
     * @param tokens Number of tokens to consume
     * @return true if tokens were successfully consumed, false otherwise
     */
    public synchronized boolean tryConsume(int tokens) {
        if (tokens <= 0) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        cleanupExpiredRequests(currentTime);
        
        int currentUsage = currentCount.get();
        if (currentUsage + tokens > capacity) {
            return false;
        }
        
        // Add the new request
        requests.addLast(new RequestRecord(currentTime, tokens));
        currentCount.addAndGet(tokens);
        
        return true;
    }
    
    /**
     * Remove requests that are outside the sliding window.
     */
    private void cleanupExpiredRequests(long currentTime) {
        long windowStart = currentTime - windowSizeMs;
        
        while (!requests.isEmpty() && requests.peekFirst().timestamp < windowStart) {
            RequestRecord expired = requests.removeFirst();
            currentCount.addAndGet(-expired.tokens);
        }
    }
    
    /**
     * Get current number of tokens consumed in the window.
     * This is equivalent to getCurrentTokens() in TokenBucket but inverted
     * (shows used rather than available).
     */
    public int getCurrentTokens() {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            cleanupExpiredRequests(currentTime);
            return Math.max(0, capacity - currentCount.get());
        }
    }
    
    /**
     * Get the maximum capacity.
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Get the refill rate (for compatibility).
     */
    public int getRefillRate() {
        return refillRate;
    }
    
    /**
     * Get the window size in milliseconds.
     */
    public long getWindowSizeMs() {
        return windowSizeMs;
    }
    
    /**
     * Get current usage within the window.
     */
    public int getCurrentUsage() {
        long currentTime = System.currentTimeMillis();
        synchronized (this) {
            cleanupExpiredRequests(currentTime);
            return currentCount.get();
        }
    }
    
    /**
     * For compatibility with TokenBucket interface.
     * Returns the last cleanup time as current time.
     */
    public long getLastRefillTime() {
        return System.currentTimeMillis();
    }
}