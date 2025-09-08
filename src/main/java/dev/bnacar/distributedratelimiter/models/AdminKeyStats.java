package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;

/**
 * Response model for admin key statistics.
 */
public class AdminKeyStats {
    private final String key;
    private final int capacity;
    private final int refillRate;
    private final long cleanupIntervalMs;
    private final RateLimitAlgorithm algorithm;
    private final long lastAccessTime;
    private final boolean isActive;

    public AdminKeyStats(String key, int capacity, int refillRate, long cleanupIntervalMs, 
                         RateLimitAlgorithm algorithm, long lastAccessTime, boolean isActive) {
        this.key = key;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.algorithm = algorithm;
        this.lastAccessTime = lastAccessTime;
        this.isActive = isActive;
    }

    public String getKey() {
        return key;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRefillRate() {
        return refillRate;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public boolean isActive() {
        return isActive;
    }
}