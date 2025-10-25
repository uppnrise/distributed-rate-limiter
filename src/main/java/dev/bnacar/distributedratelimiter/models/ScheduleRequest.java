package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.schedule.ScheduleType;
import dev.bnacar.distributedratelimiter.schedule.TransitionConfig;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Request model for creating or updating a rate limit schedule.
 */
public class ScheduleRequest {
    private String name;
    private String keyPattern;
    private ScheduleType type;
    private String cronExpression;
    private String timezone;
    private Instant startTime;
    private Instant endTime;
    private LimitsConfig limits;
    private LimitsConfig fallbackLimits;
    private TransitionConfig transition;
    private Integer priority;
    
    public static class LimitsConfig {
        private Integer capacity;
        private Integer refillRate;
        private String algorithm;
        
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
        
        public String getAlgorithm() {
            return algorithm;
        }
        
        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
    }
    
    // Getters and setters
    
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
    
    public ScheduleType getType() {
        return type;
    }
    
    public void setType(ScheduleType type) {
        this.type = type;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public ZoneId getTimezoneId() {
        return timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public LimitsConfig getLimits() {
        return limits;
    }
    
    public void setLimits(LimitsConfig limits) {
        this.limits = limits;
    }
    
    public LimitsConfig getFallbackLimits() {
        return fallbackLimits;
    }
    
    public void setFallbackLimits(LimitsConfig fallbackLimits) {
        this.fallbackLimits = fallbackLimits;
    }
    
    public TransitionConfig getTransition() {
        return transition;
    }
    
    public void setTransition(TransitionConfig transition) {
        this.transition = transition;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
