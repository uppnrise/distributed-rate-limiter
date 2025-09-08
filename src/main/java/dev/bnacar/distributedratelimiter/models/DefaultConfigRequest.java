package dev.bnacar.distributedratelimiter.models;

public class DefaultConfigRequest {
    private Integer capacity;
    private Integer refillRate;
    private Long cleanupIntervalMs;

    public DefaultConfigRequest() {}

    public DefaultConfigRequest(Integer capacity, Integer refillRate, Long cleanupIntervalMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.cleanupIntervalMs = cleanupIntervalMs;
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
}