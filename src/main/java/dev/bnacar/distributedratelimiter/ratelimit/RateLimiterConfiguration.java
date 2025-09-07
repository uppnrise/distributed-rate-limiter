package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterConfiguration {
    
    private int capacity = 10;
    private int refillRate = 2;
    private long cleanupIntervalMs = 60000; // 60 seconds
    
    public int getCapacity() {
        return capacity;
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public int getRefillRate() {
        return refillRate;
    }
    
    public void setRefillRate(int refillRate) {
        this.refillRate = refillRate;
    }
    
    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }
    
    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
}