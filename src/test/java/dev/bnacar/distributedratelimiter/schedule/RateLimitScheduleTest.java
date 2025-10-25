package dev.bnacar.distributedratelimiter.schedule;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimitSchedule model.
 */
class RateLimitScheduleTest {
    
    @Test
    void testDefaultConstructor() {
        RateLimitSchedule schedule = new RateLimitSchedule();
        
        assertEquals(ZoneId.of("UTC"), schedule.getTimezone());
        assertEquals(0, schedule.getPriority());
        assertTrue(schedule.isEnabled());
    }
    
    @Test
    void testParameterizedConstructor() {
        RateLimitSchedule schedule = new RateLimitSchedule("test-schedule", "api:*", ScheduleType.RECURRING);
        
        assertEquals("test-schedule", schedule.getName());
        assertEquals("api:*", schedule.getKeyPattern());
        assertEquals(ScheduleType.RECURRING, schedule.getType());
        assertTrue(schedule.isEnabled());
    }
    
    @Test
    void testSettersAndGetters() {
        RateLimitSchedule schedule = new RateLimitSchedule();
        
        schedule.setName("flash-sale");
        schedule.setKeyPattern("checkout:*");
        schedule.setType(ScheduleType.ONE_TIME);
        schedule.setCronExpression("0 9-17 * * MON-FRI");
        schedule.setTimezone(ZoneId.of("America/New_York"));
        schedule.setPriority(10);
        schedule.setEnabled(false);
        
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        
        RateLimitConfig config = new RateLimitConfig(100, 10);
        schedule.setActiveLimits(config);
        schedule.setFallbackLimits(config);
        
        TransitionConfig transition = new TransitionConfig(5, 10);
        schedule.setTransition(transition);
        
        assertEquals("flash-sale", schedule.getName());
        assertEquals("checkout:*", schedule.getKeyPattern());
        assertEquals(ScheduleType.ONE_TIME, schedule.getType());
        assertEquals("0 9-17 * * MON-FRI", schedule.getCronExpression());
        assertEquals(ZoneId.of("America/New_York"), schedule.getTimezone());
        assertEquals(start, schedule.getStartTime());
        assertEquals(end, schedule.getEndTime());
        assertEquals(config, schedule.getActiveLimits());
        assertEquals(config, schedule.getFallbackLimits());
        assertEquals(transition, schedule.getTransition());
        assertEquals(10, schedule.getPriority());
        assertFalse(schedule.isEnabled());
    }
    
    @Test
    void testToString() {
        RateLimitSchedule schedule = new RateLimitSchedule("test", "key:*", ScheduleType.RECURRING);
        String str = schedule.toString();
        
        assertTrue(str.contains("name='test'"));
        assertTrue(str.contains("keyPattern='key:*'"));
        assertTrue(str.contains("type=RECURRING"));
    }
}
