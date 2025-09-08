package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Configuration for rate limiting parameters.
 */
public class RateLimitConfig {
    private final int capacity;
    private final int refillRate;
    private final long cleanupIntervalMs;
    private final RateLimitAlgorithm algorithm;

    public RateLimitConfig(int capacity, int refillRate, long cleanupIntervalMs, RateLimitAlgorithm algorithm) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.algorithm = algorithm;
    }

    public RateLimitConfig(int capacity, int refillRate, long cleanupIntervalMs) {
        this(capacity, refillRate, cleanupIntervalMs, RateLimitAlgorithm.TOKEN_BUCKET);
    }

    public RateLimitConfig(int capacity, int refillRate) {
        this(capacity, refillRate, 60000); // Default 60s cleanup interval
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

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "capacity=" + capacity +
                ", refillRate=" + refillRate +
                ", cleanupIntervalMs=" + cleanupIntervalMs +
                ", algorithm=" + algorithm +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimitConfig that = (RateLimitConfig) o;
        return capacity == that.capacity && 
               refillRate == that.refillRate && 
               cleanupIntervalMs == that.cleanupIntervalMs &&
               algorithm == that.algorithm;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(capacity, refillRate, cleanupIntervalMs, algorithm);
    }
}