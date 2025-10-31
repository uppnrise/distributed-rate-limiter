package dev.bnacar.distributedratelimiter.schedule;

import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing scheduled rate limit configurations.
 * Evaluates schedules and applies appropriate rate limits based on time-based rules.
 */
@Service
public class ScheduleManagerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduleManagerService.class);
    
    private final Map<String, RateLimitSchedule> schedules = new ConcurrentHashMap<>();
    private final Map<String, RateLimitConfig> activeScheduleConfigs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService transitionExecutor;
    
    public ScheduleManagerService() {
        this.transitionExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Schedule-Transition");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Evaluate schedules every minute to check for active schedules.
     */
    @Scheduled(fixedRate = 60000)
    public void evaluateSchedules() {
        Instant now = Instant.now();
        logger.debug("Evaluating schedules at {}", now);
        
        List<RateLimitSchedule> activeSchedules = findActiveSchedules(now);
        
        // Update active schedule configurations
        activeScheduleConfigs.clear();
        for (RateLimitSchedule schedule : activeSchedules) {
            if (schedule.getActiveLimits() != null) {
                activeScheduleConfigs.put(schedule.getKeyPattern(), schedule.getActiveLimits());
                logger.debug("Applied active schedule '{}' for pattern '{}'", 
                    schedule.getName(), schedule.getKeyPattern());
            }
        }
        
        logger.debug("Active schedules: {}", activeSchedules.size());
    }
    
    /**
     * Find all currently active schedules.
     */
    public List<RateLimitSchedule> findActiveSchedules() {
        return findActiveSchedules(Instant.now());
    }
    
    /**
     * Find all currently active schedules at a specific time.
     */
    public List<RateLimitSchedule> findActiveSchedules(Instant time) {
        List<RateLimitSchedule> active = new ArrayList<>();
        
        for (RateLimitSchedule schedule : schedules.values()) {
            if (!schedule.isEnabled()) {
                continue;
            }
            
            if (isScheduleActive(schedule, time)) {
                active.add(schedule);
            }
        }
        
        // Sort by priority (higher priority first)
        active.sort(Comparator.comparingInt(RateLimitSchedule::getPriority).reversed());
        
        return active;
    }
    
    /**
     * Check if a schedule is active at the given time.
     */
    private boolean isScheduleActive(RateLimitSchedule schedule, Instant time) {
        switch (schedule.getType()) {
            case ONE_TIME:
                return isOneTimeScheduleActive(schedule, time);
            case RECURRING:
                return isRecurringScheduleActive(schedule, time);
            case EVENT_DRIVEN:
                return isEventDrivenScheduleActive(schedule, time);
            default:
                return false;
        }
    }
    
    /**
     * Check if a one-time schedule is active.
     */
    private boolean isOneTimeScheduleActive(RateLimitSchedule schedule, Instant time) {
        Instant start = schedule.getStartTime();
        Instant end = schedule.getEndTime();
        
        if (start == null || end == null) {
            return false;
        }
        
        return !time.isBefore(start) && time.isBefore(end);
    }
    
    /**
     * Check if a recurring schedule is active based on cron expression.
     */
    private boolean isRecurringScheduleActive(RateLimitSchedule schedule, Instant time) {
        String cronExpr = schedule.getCronExpression();
        if (cronExpr == null || cronExpr.trim().isEmpty()) {
            return false;
        }
        
        try {
            CronExpression cron = CronExpression.parse(cronExpr);
            ZonedDateTime zonedTime = time.atZone(schedule.getTimezone());
            
            // Get the next scheduled execution time
            ZonedDateTime nextExecution = cron.next(zonedTime);
            
            if (nextExecution == null) {
                return false;
            }
            
            // Check if the next execution is within the next minute
            // This means we're currently in a scheduled window
            long minutesUntilNext = java.time.Duration.between(zonedTime, nextExecution).toMinutes();
            return minutesUntilNext == 0;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid cron expression '{}' for schedule '{}'", cronExpr, schedule.getName(), e);
            return false;
        }
    }
    
    /**
     * Check if an event-driven schedule is active.
     */
    private boolean isEventDrivenScheduleActive(RateLimitSchedule schedule, Instant time) {
        Instant start = schedule.getStartTime();
        Instant end = schedule.getEndTime();
        
        if (start == null || end == null) {
            return false;
        }
        
        return !time.isBefore(start) && time.isBefore(end);
    }
    
    /**
     * Get the active configuration for a specific key.
     * Returns null if no schedule is active for the key.
     */
    public RateLimitConfig getActiveConfig(String key) {
        // Check exact matches first
        RateLimitConfig exactMatch = activeScheduleConfigs.get(key);
        if (exactMatch != null) {
            return exactMatch;
        }
        
        // Check pattern matches
        for (Map.Entry<String, RateLimitConfig> entry : activeScheduleConfigs.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(key, pattern)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Pattern matching supporting '*' wildcard (same as ConfigurationResolver).
     */
    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return key.equals(pattern);
        }
        
        String regex = pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("|", "\\|")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("*", ".*");
            
        return key.matches("^" + regex + "$");
    }
    
    /**
     * Create a new schedule.
     */
    public void createSchedule(RateLimitSchedule schedule) {
        if (schedule.getName() == null || schedule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Schedule name is required");
        }
        
        validateSchedule(schedule);
        schedules.put(schedule.getName(), schedule);
        logger.info("Created schedule: {}", schedule.getName());
    }
    
    /**
     * Update an existing schedule.
     */
    public void updateSchedule(String name, RateLimitSchedule schedule) {
        if (!schedules.containsKey(name)) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }
        
        schedule.setName(name);
        validateSchedule(schedule);
        schedules.put(name, schedule);
        logger.info("Updated schedule: {}", name);
    }
    
    /**
     * Delete a schedule.
     */
    public void deleteSchedule(String name) {
        RateLimitSchedule removed = schedules.remove(name);
        if (removed != null) {
            logger.info("Deleted schedule: {}", name);
        }
    }
    
    /**
     * Get a schedule by name.
     */
    public RateLimitSchedule getSchedule(String name) {
        return schedules.get(name);
    }
    
    /**
     * Get all schedules.
     */
    public List<RateLimitSchedule> getAllSchedules() {
        return new ArrayList<>(schedules.values());
    }
    
    /**
     * Activate a schedule.
     */
    public void activateSchedule(String name) {
        RateLimitSchedule schedule = schedules.get(name);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }
        
        schedule.setEnabled(true);
        logger.info("Activated schedule: {}", name);
    }
    
    /**
     * Deactivate a schedule.
     */
    public void deactivateSchedule(String name) {
        RateLimitSchedule schedule = schedules.get(name);
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule not found: " + name);
        }
        
        schedule.setEnabled(false);
        logger.info("Deactivated schedule: {}", name);
    }
    
    /**
     * Validate a schedule configuration.
     */
    private void validateSchedule(RateLimitSchedule schedule) {
        if (schedule.getType() == null) {
            throw new IllegalArgumentException("Schedule type is required");
        }
        
        if (schedule.getKeyPattern() == null || schedule.getKeyPattern().trim().isEmpty()) {
            throw new IllegalArgumentException("Key pattern is required");
        }
        
        switch (schedule.getType()) {
            case RECURRING:
                if (schedule.getCronExpression() == null || schedule.getCronExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("Cron expression is required for recurring schedules");
                }
                // Validate cron expression
                try {
                    CronExpression.parse(schedule.getCronExpression());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid cron expression: " + e.getMessage(), e);
                }
                break;
                
            case ONE_TIME:
            case EVENT_DRIVEN:
                if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
                    throw new IllegalArgumentException("Start time and end time are required for one-time and event-driven schedules");
                }
                if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
                    throw new IllegalArgumentException("End time must be after start time");
                }
                break;
        }
        
        if (schedule.getActiveLimits() == null) {
            throw new IllegalArgumentException("Active limits configuration is required");
        }
    }
    
    /**
     * Convert a limits configuration from the request model.
     */
    public static RateLimitConfig createRateLimitConfig(
            Integer capacity, 
            Integer refillRate, 
            String algorithm) {
        if (capacity == null || refillRate == null) {
            throw new IllegalArgumentException("Capacity and refill rate are required");
        }
        
        RateLimitAlgorithm algo = algorithm != null 
            ? RateLimitAlgorithm.valueOf(algorithm.toUpperCase())
            : RateLimitAlgorithm.TOKEN_BUCKET;
            
        return new RateLimitConfig(capacity, refillRate, 60000, algo);
    }
    
    @PreDestroy
    public void shutdown() {
        transitionExecutor.shutdown();
        try {
            if (!transitionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                transitionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            transitionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
