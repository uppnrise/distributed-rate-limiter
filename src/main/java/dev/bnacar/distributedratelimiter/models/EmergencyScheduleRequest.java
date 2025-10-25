package dev.bnacar.distributedratelimiter.models;

import java.time.Duration;

/**
 * Request model for creating an emergency rate limit adjustment.
 */
public class EmergencyScheduleRequest {
    private String name;
    private String keyPattern;
    private String duration; // ISO 8601 duration format (e.g., "PT1H" for 1 hour)
    private Integer capacity;
    private Integer refillRate;
    private String reason;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getKeyPattern() {
        return keyPattern;
    }
    
    public void setKeyPattern(String keyPattern) {
        this.keyPattern = keyPattern;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
    
    public Duration getDurationValue() {
        return duration != null ? Duration.parse(duration) : Duration.ofHours(1);
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
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
