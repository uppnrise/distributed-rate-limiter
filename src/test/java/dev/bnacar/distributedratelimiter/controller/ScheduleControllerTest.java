package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.EmergencyScheduleRequest;
import dev.bnacar.distributedratelimiter.models.ScheduleRequest;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.schedule.RateLimitSchedule;
import dev.bnacar.distributedratelimiter.schedule.ScheduleManagerService;
import dev.bnacar.distributedratelimiter.schedule.ScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ScheduleController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"ratelimiter.geographic.enabled=false"})
@AutoConfigureMockMvc
class ScheduleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ScheduleManagerService scheduleManager;
    
    @BeforeEach
    void setUp() {
        // Clean up any existing schedules
        for (RateLimitSchedule schedule : scheduleManager.getAllSchedules()) {
            scheduleManager.deleteSchedule(schedule.getName());
        }
    }
    
    @Test
    void testCreateSchedule() throws Exception {
        ScheduleRequest request = createScheduleRequest();
        
        mockMvc.perform(post("/api/ratelimit/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("created successfully")));
    }
    
    @Test
    void testCreateSchedule_InvalidRequest() throws Exception {
        ScheduleRequest request = createScheduleRequest();
        request.setName(null); // Invalid - no name
        
        mockMvc.perform(post("/api/ratelimit/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid schedule")));
    }
    
    @Test
    void testGetSchedules() throws Exception {
        // Create test schedules
        createTestScheduleInService("schedule1");
        createTestScheduleInService("schedule2");
        
        mockMvc.perform(get("/api/ratelimit/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[1].name").isNotEmpty());
    }
    
    @Test
    void testGetSchedule() throws Exception {
        createTestScheduleInService("test-schedule");
        
        mockMvc.perform(get("/api/ratelimit/schedule/test-schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-schedule"))
                .andExpect(jsonPath("$.keyPattern").value("api:*"));
    }
    
    @Test
    void testGetSchedule_NotFound() throws Exception {
        mockMvc.perform(get("/api/ratelimit/schedule/non-existent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testUpdateSchedule() throws Exception {
        createTestScheduleInService("test-schedule");
        ScheduleRequest request = createScheduleRequest();
        request.getLimits().setCapacity(200); // Update capacity
        
        mockMvc.perform(put("/api/ratelimit/schedule/test-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("updated successfully")));
    }
    
    @Test
    void testUpdateSchedule_NotFound() throws Exception {
        ScheduleRequest request = createScheduleRequest();
        
        mockMvc.perform(put("/api/ratelimit/schedule/non-existent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testDeleteSchedule() throws Exception {
        createTestScheduleInService("test-schedule");
        
        mockMvc.perform(delete("/api/ratelimit/schedule/test-schedule"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deleted successfully")));
    }
    
    @Test
    void testActivateSchedule() throws Exception {
        RateLimitSchedule schedule = createTestSchedule("test-schedule");
        schedule.setEnabled(false);
        scheduleManager.createSchedule(schedule);
        
        mockMvc.perform(post("/api/ratelimit/schedule/test-schedule/activate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("activated successfully")));
    }
    
    @Test
    void testActivateSchedule_NotFound() throws Exception {
        mockMvc.perform(post("/api/ratelimit/schedule/non-existent/activate"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testDeactivateSchedule() throws Exception {
        createTestScheduleInService("test-schedule");
        
        mockMvc.perform(post("/api/ratelimit/schedule/test-schedule/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("deactivated successfully")));
    }
    
    @Test
    void testCreateEmergencySchedule() throws Exception {
        EmergencyScheduleRequest request = new EmergencyScheduleRequest();
        request.setName("emergency-ddos");
        request.setKeyPattern("*");
        request.setDuration("PT1H");
        request.setCapacity(100);
        request.setRefillRate(10);
        request.setReason("DDoS attack detected");
        
        mockMvc.perform(post("/api/ratelimit/schedule/emergency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("created successfully")));
    }
    
    @Test
    void testCreateEmergencySchedule_InvalidRequest() throws Exception {
        EmergencyScheduleRequest request = new EmergencyScheduleRequest();
        request.setDuration("PT1H");
        // Missing capacity and refillRate
        
        mockMvc.perform(post("/api/ratelimit/schedule/emergency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid")));
    }
    
    /**
     * Helper method to create a test schedule request.
     */
    private ScheduleRequest createScheduleRequest() {
        ScheduleRequest request = new ScheduleRequest();
        request.setName("test-schedule");
        request.setKeyPattern("api:*");
        request.setType(ScheduleType.ONE_TIME);
        request.setTimezone("UTC");
        request.setStartTime(Instant.now());
        request.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        
        ScheduleRequest.LimitsConfig limits = new ScheduleRequest.LimitsConfig();
        limits.setCapacity(100);
        limits.setRefillRate(10);
        request.setLimits(limits);
        
        return request;
    }
    
    /**
     * Helper method to create a test schedule.
     */
    private RateLimitSchedule createTestSchedule(String name) {
        RateLimitSchedule schedule = new RateLimitSchedule(name, "api:*", ScheduleType.ONE_TIME);
        schedule.setStartTime(Instant.now());
        schedule.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        schedule.setActiveLimits(new RateLimitConfig(100, 10));
        return schedule;
    }
    
    /**
     * Helper method to create and persist a test schedule in the service.
     */
    private void createTestScheduleInService(String name) {
        RateLimitSchedule schedule = createTestSchedule(name);
        scheduleManager.createSchedule(schedule);
    }
}
