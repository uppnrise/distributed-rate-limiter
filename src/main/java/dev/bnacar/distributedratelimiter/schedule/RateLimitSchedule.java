package dev.bnacar.distributedratelimiter.schedule;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Entity representing a scheduled rate limit configuration.
 * Supports recurring schedules, one-time events, and event-driven adjustments.
 */
public class RateLimitSchedule {
    private String name;
    private String keyPattern;
    private ScheduleType type;
    private String cronExpression;
    private ZoneId timezone;
    private Instant startTime;
    private Instant endTime;
    private RateLimitConfig activeLimits;
    private RateLimitConfig fallbackLimits;
    private TransitionConfig transition;
    private int priority;
    private boolean enabled;
    
    public RateLimitSchedule() {
        this.timezone = ZoneId.of("UTC");
        this.priority = 0;
        this.enabled = true;
    }
    
    public RateLimitSchedule(String name, String keyPattern, ScheduleType type) {
        this.name = name;
        this.keyPattern = keyPattern;
        this.type = type;
        this.timezone = ZoneId.of("UTC");
        this.priority = 0;
        this.enabled = true;
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
    
    public ZoneId getTimezone() {
        return timezone;
    }
    
    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone;
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
    
    public RateLimitConfig getActiveLimits() {
        return activeLimits;
    }
    
    public void setActiveLimits(RateLimitConfig activeLimits) {
        this.activeLimits = activeLimits;
    }
    
    public RateLimitConfig getFallbackLimits() {
        return fallbackLimits;
    }
    
    public void setFallbackLimits(RateLimitConfig fallbackLimits) {
        this.fallbackLimits = fallbackLimits;
    }
    
    public TransitionConfig getTransition() {
        return transition;
    }
    
    public void setTransition(TransitionConfig transition) {
        this.transition = transition;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "RateLimitSchedule{" +
                "name='" + name + '\'' +
                ", keyPattern='" + keyPattern + '\'' +
                ", type=" + type +
                ", cronExpression='" + cronExpression + '\'' +
                ", timezone=" + timezone +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
