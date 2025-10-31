package dev.bnacar.distributedratelimiter.schedule;

/**
 * Types of rate limit schedules.
 */
public enum ScheduleType {
    /**
     * Recurring schedule based on cron expression (e.g., business hours daily)
     */
    RECURRING,
    
    /**
     * One-time event with specific start and end times (e.g., flash sale)
     */
    ONE_TIME,
    
    /**
     * Event-driven schedule triggered by API calls (e.g., emergency adjustments)
     */
    EVENT_DRIVEN
}
