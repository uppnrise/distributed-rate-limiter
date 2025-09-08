package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;

/**
 * Response model for admin limit operations.
 */
public class AdminLimitResponse {
    private final String key;
    private final int capacity;
    private final int refillRate;
    private final long cleanupIntervalMs;
    private final RateLimitAlgorithm algorithm;

    public AdminLimitResponse(String key, int capacity, int refillRate, long cleanupIntervalMs, RateLimitAlgorithm algorithm) {
        this.key = key;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.algorithm = algorithm;
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
}