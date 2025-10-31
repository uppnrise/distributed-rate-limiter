package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.schedule.RateLimitSchedule;
import dev.bnacar.distributedratelimiter.schedule.ScheduleType;

import java.time.Instant;

/**
 * Response model for rate limit schedule information.
 */
public class ScheduleResponse {
    private String name;
    private String keyPattern;
    private ScheduleType type;
    private String cronExpression;
    private String timezone;
    private Instant startTime;
    private Instant endTime;
    private boolean active;
    private boolean enabled;
    private int priority;
    
    public static ScheduleResponse from(RateLimitSchedule schedule, boolean active) {
        ScheduleResponse response = new ScheduleResponse();
        response.name = schedule.getName();
        response.keyPattern = schedule.getKeyPattern();
        response.type = schedule.getType();
        response.cronExpression = schedule.getCronExpression();
        response.timezone = schedule.getTimezone() != null ? schedule.getTimezone().getId() : "UTC";
        response.startTime = schedule.getStartTime();
        response.endTime = schedule.getEndTime();
        response.active = active;
        response.enabled = schedule.isEnabled();
        response.priority = schedule.getPriority();
        return response;
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
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
}
