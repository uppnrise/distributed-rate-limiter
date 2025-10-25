package dev.bnacar.distributedratelimiter.schedule;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ScheduleManagerService.
 */
class ScheduleManagerServiceTest {
    
    private ScheduleManagerService service;
    
    @BeforeEach
    void setUp() {
        service = new ScheduleManagerService();
    }
    
    @Test
    void testCreateSchedule() {
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        
        service.createSchedule(schedule);
        
        RateLimitSchedule retrieved = service.getSchedule("test-schedule");
        assertNotNull(retrieved);
        assertEquals("test-schedule", retrieved.getName());
    }
    
    @Test
    void testCreateScheduleWithoutName() {
        RateLimitSchedule schedule = new RateLimitSchedule();
        schedule.setType(ScheduleType.ONE_TIME);
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testUpdateSchedule() {
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        service.createSchedule(schedule);
        
        RateLimitSchedule updated = createTestSchedule("test-schedule", ScheduleType.RECURRING);
        updated.setCronExpression("0 0 9-17 * * MON-FRI");
        
        service.updateSchedule("test-schedule", updated);
        
        RateLimitSchedule retrieved = service.getSchedule("test-schedule");
        assertEquals(ScheduleType.RECURRING, retrieved.getType());
        assertEquals("0 0 9-17 * * MON-FRI", retrieved.getCronExpression());
    }
    
    @Test
    void testUpdateNonExistentSchedule() {
        RateLimitSchedule schedule = createTestSchedule("non-existent", ScheduleType.ONE_TIME);
        
        assertThrows(IllegalArgumentException.class, 
            () -> service.updateSchedule("non-existent", schedule));
    }
    
    @Test
    void testDeleteSchedule() {
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        service.createSchedule(schedule);
        
        service.deleteSchedule("test-schedule");
        
        assertNull(service.getSchedule("test-schedule"));
    }
    
    @Test
    void testActivateDeactivateSchedule() {
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        service.createSchedule(schedule);
        
        service.deactivateSchedule("test-schedule");
        assertFalse(service.getSchedule("test-schedule").isEnabled());
        
        service.activateSchedule("test-schedule");
        assertTrue(service.getSchedule("test-schedule").isEnabled());
    }
    
    @Test
    void testGetAllSchedules() {
        service.createSchedule(createTestSchedule("schedule1", ScheduleType.ONE_TIME));
        service.createSchedule(createTestSchedule("schedule2", ScheduleType.RECURRING));
        
        List<RateLimitSchedule> schedules = service.getAllSchedules();
        assertEquals(2, schedules.size());
    }
    
    @Test
    void testFindActiveSchedules_OneTime() {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule = createTestSchedule("active-schedule", ScheduleType.ONE_TIME);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        service.createSchedule(schedule);
        
        List<RateLimitSchedule> active = service.findActiveSchedules(now);
        assertEquals(1, active.size());
        assertEquals("active-schedule", active.get(0).getName());
    }
    
    @Test
    void testFindActiveSchedules_OneTimeNotActive() {
        Instant now = Instant.now();
        Instant start = now.plus(1, ChronoUnit.HOURS);
        Instant end = now.plus(2, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule = createTestSchedule("future-schedule", ScheduleType.ONE_TIME);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        service.createSchedule(schedule);
        
        List<RateLimitSchedule> active = service.findActiveSchedules(now);
        assertEquals(0, active.size());
    }
    
    @Test
    void testFindActiveSchedules_Disabled() {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule = createTestSchedule("disabled-schedule", ScheduleType.ONE_TIME);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setEnabled(false);
        service.createSchedule(schedule);
        
        List<RateLimitSchedule> active = service.findActiveSchedules(now);
        assertEquals(0, active.size());
    }
    
    @Test
    void testFindActiveSchedules_PriorityOrdering() {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule1 = createTestSchedule("low-priority", ScheduleType.ONE_TIME);
        schedule1.setStartTime(start);
        schedule1.setEndTime(end);
        schedule1.setPriority(1);
        service.createSchedule(schedule1);
        
        RateLimitSchedule schedule2 = createTestSchedule("high-priority", ScheduleType.ONE_TIME);
        schedule2.setStartTime(start);
        schedule2.setEndTime(end);
        schedule2.setPriority(10);
        service.createSchedule(schedule2);
        
        List<RateLimitSchedule> active = service.findActiveSchedules(now);
        assertEquals(2, active.size());
        assertEquals("high-priority", active.get(0).getName());
        assertEquals("low-priority", active.get(1).getName());
    }
    
    @Test
    void testGetActiveConfig_ExactMatch() {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        schedule.setKeyPattern("exact:key");
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        service.createSchedule(schedule);
        
        // Trigger evaluation
        service.evaluateSchedules();
        
        RateLimitConfig config = service.getActiveConfig("exact:key");
        assertNotNull(config);
        assertEquals(100, config.getCapacity());
    }
    
    @Test
    void testGetActiveConfig_PatternMatch() {
        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        
        RateLimitSchedule schedule = createTestSchedule("test-schedule", ScheduleType.ONE_TIME);
        schedule.setKeyPattern("api:*");
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        service.createSchedule(schedule);
        
        // Trigger evaluation
        service.evaluateSchedules();
        
        RateLimitConfig config = service.getActiveConfig("api:users");
        assertNotNull(config);
        assertEquals(100, config.getCapacity());
    }
    
    @Test
    void testGetActiveConfig_NoMatch() {
        service.evaluateSchedules();
        
        RateLimitConfig config = service.getActiveConfig("no:match");
        assertNull(config);
    }
    
    @Test
    void testValidateSchedule_RecurringWithoutCron() {
        RateLimitSchedule schedule = new RateLimitSchedule("test", "api:*", ScheduleType.RECURRING);
        schedule.setActiveLimits(new RateLimitConfig(100, 10));
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testValidateSchedule_RecurringWithInvalidCron() {
        RateLimitSchedule schedule = new RateLimitSchedule("test", "api:*", ScheduleType.RECURRING);
        schedule.setCronExpression("invalid cron");
        schedule.setActiveLimits(new RateLimitConfig(100, 10));
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testValidateSchedule_OneTimeWithoutStartEnd() {
        RateLimitSchedule schedule = new RateLimitSchedule("test", "api:*", ScheduleType.ONE_TIME);
        schedule.setActiveLimits(new RateLimitConfig(100, 10));
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testValidateSchedule_OneTimeWithInvalidTimeRange() {
        Instant now = Instant.now();
        
        RateLimitSchedule schedule = new RateLimitSchedule("test", "api:*", ScheduleType.ONE_TIME);
        schedule.setStartTime(now);
        schedule.setEndTime(now.minus(1, ChronoUnit.HOURS)); // End before start
        schedule.setActiveLimits(new RateLimitConfig(100, 10));
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testValidateSchedule_WithoutActiveLimits() {
        RateLimitSchedule schedule = new RateLimitSchedule("test", "api:*", ScheduleType.ONE_TIME);
        schedule.setStartTime(Instant.now());
        schedule.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        
        assertThrows(IllegalArgumentException.class, () -> service.createSchedule(schedule));
    }
    
    @Test
    void testCreateRateLimitConfig() {
        RateLimitConfig config = ScheduleManagerService.createRateLimitConfig(100, 10, "TOKEN_BUCKET");
        
        assertEquals(100, config.getCapacity());
        assertEquals(10, config.getRefillRate());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, config.getAlgorithm());
    }
    
    @Test
    void testCreateRateLimitConfig_DefaultAlgorithm() {
        RateLimitConfig config = ScheduleManagerService.createRateLimitConfig(100, 10, null);
        
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, config.getAlgorithm());
    }
    
    @Test
    void testCreateRateLimitConfig_InvalidParameters() {
        assertThrows(IllegalArgumentException.class, 
            () -> ScheduleManagerService.createRateLimitConfig(null, 10, null));
        assertThrows(IllegalArgumentException.class, 
            () -> ScheduleManagerService.createRateLimitConfig(100, null, null));
    }
    
    /**
     * Helper method to create a test schedule.
     */
    private RateLimitSchedule createTestSchedule(String name, ScheduleType type) {
        RateLimitSchedule schedule = new RateLimitSchedule(name, "test:*", type);
        
        if (type == ScheduleType.ONE_TIME || type == ScheduleType.EVENT_DRIVEN) {
            Instant now = Instant.now();
            schedule.setStartTime(now);
            schedule.setEndTime(now.plus(1, ChronoUnit.HOURS));
        } else if (type == ScheduleType.RECURRING) {
            schedule.setCronExpression("0 * * * * *"); // Every minute
        }
        
        RateLimitConfig config = new RateLimitConfig(100, 10);
        schedule.setActiveLimits(config);
        
        return schedule;
    }
}
