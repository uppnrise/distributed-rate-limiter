package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Configuration for rate limiting parameters.
 */
public class RateLimitConfig {
    private final int capacity;
    private final int refillRate;
    private final long cleanupIntervalMs;

    public RateLimitConfig(int capacity, int refillRate, long cleanupIntervalMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
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

    @Override
    public String toString() {
        return "RateLimitConfig{" +
                "capacity=" + capacity +
                ", refillRate=" + refillRate +
                ", cleanupIntervalMs=" + cleanupIntervalMs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RateLimitConfig that = (RateLimitConfig) o;
        return capacity == that.capacity && 
               refillRate == that.refillRate && 
               cleanupIntervalMs == that.cleanupIntervalMs;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(capacity, refillRate, cleanupIntervalMs);
    }
}