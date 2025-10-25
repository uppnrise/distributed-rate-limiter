package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.EmergencyScheduleRequest;
import dev.bnacar.distributedratelimiter.models.ScheduleRequest;
import dev.bnacar.distributedratelimiter.models.ScheduleResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.schedule.RateLimitSchedule;
import dev.bnacar.distributedratelimiter.schedule.ScheduleManagerService;
import dev.bnacar.distributedratelimiter.schedule.ScheduleType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing scheduled rate limit configurations.
 */
@RestController
@RequestMapping("/api/ratelimit/schedule")
@Tag(name = "Rate Limit Scheduling", description = "Time-based dynamic rate limit scheduling and management")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class ScheduleController {
    
    private final ScheduleManagerService scheduleManager;
    
    public ScheduleController(ScheduleManagerService scheduleManager) {
        this.scheduleManager = scheduleManager;
    }
    
    /**
     * Create a new rate limit schedule.
     */
    @PostMapping
    @Operation(summary = "Create a new rate limit schedule",
               description = "Creates a new scheduled rate limit configuration for recurring, one-time, or event-driven adjustments")
    @ApiResponse(responseCode = "200", description = "Schedule created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid schedule configuration")
    public ResponseEntity<String> createSchedule(@RequestBody ScheduleRequest request) {
        try {
            RateLimitSchedule schedule = convertToSchedule(request);
            scheduleManager.createSchedule(schedule);
            return ResponseEntity.ok("Schedule created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid schedule configuration");
        }
    }
    
    /**
     * Get all schedules.
     */
    @GetMapping
    @Operation(summary = "Get all rate limit schedules",
               description = "Retrieves all configured rate limit schedules with their active status")
    @ApiResponse(responseCode = "200", 
                description = "List of schedules retrieved successfully",
                content = @Content(mediaType = "application/json",
                                 schema = @Schema(implementation = ScheduleResponse.class)))
    public ResponseEntity<List<ScheduleResponse>> getSchedules() {
        List<RateLimitSchedule> allSchedules = scheduleManager.getAllSchedules();
        List<RateLimitSchedule> activeSchedules = scheduleManager.findActiveSchedules();
        
        List<ScheduleResponse> responses = allSchedules.stream()
            .map(schedule -> ScheduleResponse.from(schedule, activeSchedules.contains(schedule)))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get a specific schedule by name.
     */
    @GetMapping("/{name}")
    @Operation(summary = "Get a specific rate limit schedule",
               description = "Retrieves a rate limit schedule by name")
    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Schedule not found")
    public ResponseEntity<?> getSchedule(@PathVariable("name") String name) {
        RateLimitSchedule schedule = scheduleManager.getSchedule(name);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<RateLimitSchedule> activeSchedules = scheduleManager.findActiveSchedules();
        ScheduleResponse response = ScheduleResponse.from(schedule, activeSchedules.contains(schedule));
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update an existing schedule.
     */
    @PutMapping("/{name}")
    @Operation(summary = "Update a rate limit schedule",
               description = "Updates an existing rate limit schedule configuration")
    @ApiResponse(responseCode = "200", description = "Schedule updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid schedule configuration")
    @ApiResponse(responseCode = "404", description = "Schedule not found")
    public ResponseEntity<String> updateSchedule(@PathVariable("name") String name, 
                                                @RequestBody ScheduleRequest request) {
        try {
            RateLimitSchedule schedule = convertToSchedule(request);
            scheduleManager.updateSchedule(name, schedule);
            return ResponseEntity.ok("Schedule updated successfully");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body("Invalid schedule configuration");
        }
    }
    
    /**
     * Delete a schedule.
     */
    @DeleteMapping("/{name}")
    @Operation(summary = "Delete a rate limit schedule",
               description = "Removes a rate limit schedule from the system")
    @ApiResponse(responseCode = "200", description = "Schedule deleted successfully")
    public ResponseEntity<String> deleteSchedule(@PathVariable("name") String name) {
        scheduleManager.deleteSchedule(name);
        return ResponseEntity.ok("Schedule deleted successfully");
    }
    
    /**
     * Activate a schedule.
     */
    @PostMapping("/{name}/activate")
    @Operation(summary = "Activate a rate limit schedule",
               description = "Enables a previously deactivated schedule")
    @ApiResponse(responseCode = "200", description = "Schedule activated successfully")
    @ApiResponse(responseCode = "404", description = "Schedule not found")
    public ResponseEntity<String> activateSchedule(@PathVariable("name") String name) {
        try {
            scheduleManager.activateSchedule(name);
            return ResponseEntity.ok("Schedule activated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Deactivate a schedule.
     */
    @PostMapping("/{name}/deactivate")
    @Operation(summary = "Deactivate a rate limit schedule",
               description = "Temporarily disables a schedule without deleting it")
    @ApiResponse(responseCode = "200", description = "Schedule deactivated successfully")
    @ApiResponse(responseCode = "404", description = "Schedule not found")
    public ResponseEntity<String> deactivateSchedule(@PathVariable("name") String name) {
        try {
            scheduleManager.deactivateSchedule(name);
            return ResponseEntity.ok("Schedule deactivated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create an emergency rate limit adjustment.
     */
    @PostMapping("/emergency")
    @Operation(summary = "Create emergency rate limit adjustment",
               description = "Creates a temporary rate limit adjustment for emergency situations (e.g., DDoS mitigation)")
    @ApiResponse(responseCode = "200", description = "Emergency schedule created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid emergency schedule configuration")
    public ResponseEntity<String> createEmergencySchedule(@RequestBody EmergencyScheduleRequest request) {
        try {
            RateLimitSchedule schedule = new RateLimitSchedule();
            schedule.setName(request.getName() != null ? request.getName() : "emergency-" + System.currentTimeMillis());
            schedule.setKeyPattern(request.getKeyPattern() != null ? request.getKeyPattern() : "*");
            schedule.setType(ScheduleType.EVENT_DRIVEN);
            
            Instant now = Instant.now();
            schedule.setStartTime(now);
            schedule.setEndTime(now.plus(request.getDurationValue()));
            
            RateLimitConfig limits = ScheduleManagerService.createRateLimitConfig(
                request.getCapacity(),
                request.getRefillRate(),
                null
            );
            schedule.setActiveLimits(limits);
            schedule.setPriority(1000); // High priority for emergency schedules
            
            scheduleManager.createSchedule(schedule);
            
            return ResponseEntity.ok("Emergency schedule created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid emergency schedule configuration");
        }
    }
    
    /**
     * Convert a ScheduleRequest to a RateLimitSchedule entity.
     */
    private RateLimitSchedule convertToSchedule(ScheduleRequest request) {
        RateLimitSchedule schedule = new RateLimitSchedule();
        schedule.setName(request.getName());
        schedule.setKeyPattern(request.getKeyPattern());
        schedule.setType(request.getType());
        schedule.setCronExpression(request.getCronExpression());
        schedule.setTimezone(request.getTimezoneId());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        
        if (request.getPriority() != null) {
            schedule.setPriority(request.getPriority());
        }
        
        // Convert limits
        if (request.getLimits() != null) {
            RateLimitConfig activeLimits = ScheduleManagerService.createRateLimitConfig(
                request.getLimits().getCapacity(),
                request.getLimits().getRefillRate(),
                request.getLimits().getAlgorithm()
            );
            schedule.setActiveLimits(activeLimits);
        }
        
        // Convert fallback limits
        if (request.getFallbackLimits() != null) {
            RateLimitConfig fallbackLimits = ScheduleManagerService.createRateLimitConfig(
                request.getFallbackLimits().getCapacity(),
                request.getFallbackLimits().getRefillRate(),
                request.getFallbackLimits().getAlgorithm()
            );
            schedule.setFallbackLimits(fallbackLimits);
        }
        
        // Set transition config
        if (request.getTransition() != null) {
            schedule.setTransition(request.getTransition());
        }
        
        return schedule;
    }
}
