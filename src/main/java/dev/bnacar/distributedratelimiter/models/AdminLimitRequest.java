package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for updating admin limits.
 */
public class AdminLimitRequest {
    @NotNull(message = "Capacity must be specified")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
    
    @NotNull(message = "Refill rate must be specified")
    @Min(value = 1, message = "Refill rate must be at least 1")
    private Integer refillRate;
    
    @Min(value = 1000, message = "Cleanup interval must be at least 1000ms")
    private Long cleanupIntervalMs;
    
    private RateLimitAlgorithm algorithm;

    public AdminLimitRequest() {}

    public AdminLimitRequest(Integer capacity, Integer refillRate, Long cleanupIntervalMs, RateLimitAlgorithm algorithm) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
        this.algorithm = algorithm;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getRefillRate() {
        return refillRate;
    }

    public void setRefillRate(Integer refillRate) {
        this.refillRate = refillRate;
    }

    public Long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(Long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
}