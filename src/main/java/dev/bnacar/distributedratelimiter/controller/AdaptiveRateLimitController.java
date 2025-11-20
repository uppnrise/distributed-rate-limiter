package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.adaptive.AdaptiveRateLimitEngine;
import dev.bnacar.distributedratelimiter.models.AdaptiveStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for adaptive rate limiting operations
 */
@RestController
@RequestMapping("/api/ratelimit/adaptive")
@Tag(name = "Adaptive Rate Limiting", description = "Machine learning-driven adaptive rate limit management")
public class AdaptiveRateLimitController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveRateLimitController.class);
    
    private final AdaptiveRateLimitEngine adaptiveEngine;
    
    public AdaptiveRateLimitController(AdaptiveRateLimitEngine adaptiveEngine) {
        this.adaptiveEngine = adaptiveEngine;
    }
    
    /**
     * Get adaptive status for a key
     */
    @GetMapping("/{key}/status")
    @Operation(
        summary = "Get adaptive status",
        description = "Retrieve adaptive rate limiting status for a specific key including current limits, recommendations, and reasoning"
    )
    public ResponseEntity<AdaptiveStatus> getAdaptiveStatus(
            @Parameter(description = "Rate limit key", required = true)
            @PathVariable String key) {
        
        logger.debug("Getting adaptive status for key: {}", key);
        
        AdaptiveRateLimitEngine.AdaptiveStatusInfo statusInfo = adaptiveEngine.getStatus(key);
        
        // Build response
        AdaptiveStatus.CurrentLimits currentLimits = new AdaptiveStatus.CurrentLimits(
            statusInfo.currentCapacity,
            statusInfo.currentRefillRate
        );
        
        AdaptiveStatus.RecommendedLimits recommendedLimits = new AdaptiveStatus.RecommendedLimits(
            statusInfo.currentCapacity,
            statusInfo.currentRefillRate
        );
        
        AdaptiveStatus.AdaptiveStatusInfo adaptiveStatusInfo = new AdaptiveStatus.AdaptiveStatusInfo(
            statusInfo.mode,
            statusInfo.confidence,
            recommendedLimits,
            statusInfo.reasoning
        );
        
        AdaptiveStatus response = new AdaptiveStatus(key, currentLimits, adaptiveStatusInfo);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Set manual override for a key
     */
    @PostMapping("/{key}/override")
    @Operation(
        summary = "Set manual override",
        description = "Manually override adaptive decisions for a specific key"
    )
    public ResponseEntity<Void> overrideAdaptation(
            @Parameter(description = "Rate limit key", required = true)
            @PathVariable String key,
            @Valid @RequestBody AdaptationOverrideRequest override) {
        
        logger.info("Setting manual override for key {}: capacity={}, refillRate={}", 
                   key, override.capacity, override.refillRate);
        
        AdaptiveRateLimitEngine.AdaptationOverride engineOverride = 
            new AdaptiveRateLimitEngine.AdaptationOverride(
                override.capacity,
                override.refillRate,
                override.reason
            );
        
        adaptiveEngine.setOverride(key, engineOverride);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Remove manual override for a key
     */
    @DeleteMapping("/{key}/override")
    @Operation(
        summary = "Remove manual override",
        description = "Remove manual override and resume adaptive rate limiting for a key"
    )
    public ResponseEntity<Void> removeOverride(
            @Parameter(description = "Rate limit key", required = true)
            @PathVariable String key) {
        
        logger.info("Removing manual override for key: {}", key);
        
        adaptiveEngine.removeOverride(key);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Get adaptive configuration
     */
    @GetMapping("/config")
    @Operation(
        summary = "Get adaptive configuration",
        description = "Retrieve current adaptive rate limiting configuration"
    )
    public ResponseEntity<AdaptiveConfigResponse> getAdaptiveConfig() {
        logger.debug("Getting adaptive configuration");
        
        // Return static configuration (in real implementation, read from properties)
        AdaptiveConfigResponse config = new AdaptiveConfigResponse(
            true,
            300000L,
            0.7,
            2.0,
            10,
            100000
        );
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * Request for manual override
     */
    public static class AdaptationOverrideRequest {
        
        @Min(value = 1, message = "Capacity must be at least 1")
        private int capacity;
        
        @Min(value = 1, message = "Refill rate must be at least 1")
        private int refillRate;
        
        @NotBlank(message = "Reason is required")
        private String reason;
        
        public AdaptationOverrideRequest() {}
        
        public AdaptationOverrideRequest(int capacity, int refillRate, String reason) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.reason = reason;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
        
        public int getRefillRate() {
            return refillRate;
        }
        
        public void setRefillRate(int refillRate) {
            this.refillRate = refillRate;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
    
    /**
     * Adaptive configuration response
     */
    public static class AdaptiveConfigResponse {
        private boolean enabled;
        private long evaluationIntervalMs;
        private double minConfidenceThreshold;
        private double maxAdjustmentFactor;
        private int minCapacity;
        private int maxCapacity;
        
        public AdaptiveConfigResponse(boolean enabled, long evaluationIntervalMs, 
                                     double minConfidenceThreshold, double maxAdjustmentFactor,
                                     int minCapacity, int maxCapacity) {
            this.enabled = enabled;
            this.evaluationIntervalMs = evaluationIntervalMs;
            this.minConfidenceThreshold = minConfidenceThreshold;
            this.maxAdjustmentFactor = maxAdjustmentFactor;
            this.minCapacity = minCapacity;
            this.maxCapacity = maxCapacity;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getEvaluationIntervalMs() {
            return evaluationIntervalMs;
        }
        
        public void setEvaluationIntervalMs(long evaluationIntervalMs) {
            this.evaluationIntervalMs = evaluationIntervalMs;
        }
        
        public double getMinConfidenceThreshold() {
            return minConfidenceThreshold;
        }
        
        public void setMinConfidenceThreshold(double minConfidenceThreshold) {
            this.minConfidenceThreshold = minConfidenceThreshold;
        }
        
        public double getMaxAdjustmentFactor() {
            return maxAdjustmentFactor;
        }
        
        public void setMaxAdjustmentFactor(double maxAdjustmentFactor) {
            this.maxAdjustmentFactor = maxAdjustmentFactor;
        }
        
        public int getMinCapacity() {
            return minCapacity;
        }
        
        public void setMinCapacity(int minCapacity) {
            this.minCapacity = minCapacity;
        }
        
        public int getMaxCapacity() {
            return maxCapacity;
        }
        
        public void setMaxCapacity(int maxCapacity) {
            this.maxCapacity = maxCapacity;
        }
    }
}
