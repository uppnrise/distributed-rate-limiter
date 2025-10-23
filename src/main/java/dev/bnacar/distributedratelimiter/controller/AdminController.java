package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.AdminKeyStats;
import dev.bnacar.distributedratelimiter.models.AdminKeysResponse;
import dev.bnacar.distributedratelimiter.models.AdminLimitRequest;
import dev.bnacar.distributedratelimiter.models.AdminLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing rate limits and monitoring system state.
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "admin-controller", description = "Administrative operations for rate limiter management")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class AdminController {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterConfiguration configuration;
    private final ConfigurationResolver configurationResolver;

    public AdminController(RateLimiterService rateLimiterService,
                          RateLimiterConfiguration configuration,
                          ConfigurationResolver configurationResolver) {
        this.rateLimiterService = rateLimiterService;
        this.configuration = configuration;
        this.configurationResolver = configurationResolver;
    }

    /**
     * Get current limits for a specific key.
     */
    @GetMapping("/limits/{key}")
    @Operation(summary = "Get rate limit configuration for a specific key",
               description = "Retrieves the current rate limiting configuration for the specified key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Configuration found",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = AdminLimitResponse.class))),
        @ApiResponse(responseCode = "404", 
                    description = "Key configuration not found")
    })
    public ResponseEntity<AdminLimitResponse> getKeyLimits(
            @Parameter(description = "Rate limiting key", required = true, example = "user:123")
            @PathVariable("key") String key) {
        RateLimitConfig config = rateLimiterService.getKeyConfiguration(key);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        AdminLimitResponse response = new AdminLimitResponse(
            key,
            config.getCapacity(),
            config.getRefillRate(),
            config.getCleanupIntervalMs(),
            config.getAlgorithm()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Update limits for a specific key.
     */
    @PutMapping("/limits/{key}")
    @Operation(summary = "Update rate limit configuration for a specific key",
               description = "Sets new rate limiting configuration for the specified key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Configuration updated successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = AdminLimitResponse.class)))
    })
    public ResponseEntity<AdminLimitResponse> updateKeyLimits(
            @Parameter(description = "Rate limiting key", required = true, example = "user:123")
            @PathVariable("key") String key,
            @Parameter(description = "New rate limit configuration", required = true)
            @Valid @RequestBody AdminLimitRequest request) {
        // Create new key configuration
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(request.getCapacity());
        keyConfig.setRefillRate(request.getRefillRate());
        keyConfig.setCleanupIntervalMs(request.getCleanupIntervalMs() != null ? request.getCleanupIntervalMs() : 60000L);
        keyConfig.setAlgorithm(request.getAlgorithm() != null ? request.getAlgorithm() : dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm.TOKEN_BUCKET);

        // Update configuration
        configuration.putKey(key, keyConfig);
        
        // Clear existing bucket to apply new configuration
        rateLimiterService.removeKey(key);
        configurationResolver.clearCache();

        // Return updated configuration
        RateLimitConfig config = rateLimiterService.getKeyConfiguration(key);
        AdminLimitResponse response = new AdminLimitResponse(
            key,
            config.getCapacity(),
            config.getRefillRate(),
            config.getCleanupIntervalMs(),
            config.getAlgorithm()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Remove limits for a specific key.
     */
    @DeleteMapping("/limits/{key}")
    @Operation(summary = "Remove rate limit configuration for a specific key",
               description = "Removes custom rate limiting configuration for the specified key, reverting to default settings")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Configuration removed successfully"),
        @ApiResponse(responseCode = "404", 
                    description = "Key configuration not found")
    })
    public ResponseEntity<String> removeKeyLimits(
            @Parameter(description = "Rate limiting key", required = true, example = "user:123")
            @PathVariable("key") String key) {
        // Remove from configuration
        boolean configRemoved = configuration.removeKey(key) != null;
        
        // Remove active bucket
        boolean bucketRemoved = rateLimiterService.removeKey(key);
        
        // Clear cache
        configurationResolver.clearCache();

        if (configRemoved || bucketRemoved) {
            return ResponseEntity.ok("Limits removed for key: " + key);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * List all active keys with statistics.
     */
    @GetMapping("/keys")
    @Operation(summary = "List all active rate limiting keys",
               description = "Retrieves statistics for all currently active rate limiting keys")
    @ApiResponse(responseCode = "200", 
                description = "Active keys retrieved successfully",
                content = @Content(mediaType = "application/json",
                                 schema = @Schema(implementation = AdminKeysResponse.class)))
    public ResponseEntity<AdminKeysResponse> getAllKeys() {
        Map<String, RateLimiterService.BucketHolder> bucketHolders = rateLimiterService.getBucketHolders();
        long currentTime = System.currentTimeMillis();

        List<AdminKeyStats> keyStats = bucketHolders.entrySet().stream()
            .map(entry -> {
                String key = entry.getKey();
                RateLimiterService.BucketHolder holder = entry.getValue();
                RateLimitConfig config = holder.getConfig();
                long lastAccess = holder.getLastAccessTime();
                boolean isActive = (currentTime - lastAccess) < config.getCleanupIntervalMs();

                return new AdminKeyStats(
                    key,
                    config.getCapacity(),
                    config.getRefillRate(),
                    config.getCleanupIntervalMs(),
                    config.getAlgorithm(),
                    lastAccess,
                    isActive
                );
            })
            .collect(Collectors.toList());

        int totalKeys = keyStats.size();
        int activeKeys = (int) keyStats.stream().filter(AdminKeyStats::isActive).count();

        AdminKeysResponse response = new AdminKeysResponse(keyStats, totalKeys, activeKeys);
        return ResponseEntity.ok(response);
    }
}