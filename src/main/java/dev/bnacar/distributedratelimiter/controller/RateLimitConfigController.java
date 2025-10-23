package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.ConfigurationResponse;
import dev.bnacar.distributedratelimiter.models.ConfigurationStats;
import dev.bnacar.distributedratelimiter.models.DefaultConfigRequest;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Controller for managing rate limiter configuration.
 */
@RestController
@RequestMapping("/api/ratelimit/config")
@Tag(name = "Rate Limit Configuration", description = "Configuration management for rate limiter settings")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class RateLimitConfigController {

    private final RateLimiterConfiguration configuration;
    private final ConfigurationResolver configurationResolver;
    private final RateLimiterService rateLimiterService;

    public RateLimitConfigController(RateLimiterConfiguration configuration,
                                   ConfigurationResolver configurationResolver,
                                   RateLimiterService rateLimiterService) {
        this.configuration = configuration;
        this.configurationResolver = configurationResolver;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Get the current configuration.
     */
    @GetMapping
    @Operation(summary = "Get current rate limiter configuration",
               description = "Retrieves the current configuration including default settings, per-key configurations, and patterns")
    @ApiResponse(responseCode = "200", 
                description = "Current configuration retrieved successfully",
                content = @Content(mediaType = "application/json",
                                 schema = @Schema(implementation = ConfigurationResponse.class)))
    public ResponseEntity<ConfigurationResponse> getConfiguration() {
        ConfigurationResponse response = new ConfigurationResponse(
            configuration.getCapacity(),
            configuration.getRefillRate(),
            configuration.getCleanupIntervalMs(),
            configuration.getKeys(),
            configuration.getPatterns()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Update per-key configuration.
     */
    @PostMapping("/keys/{key}")
    public ResponseEntity<String> updateKeyConfiguration(@PathVariable("key") String key,
                                                        @RequestBody RateLimiterConfiguration.KeyConfig keyConfig) {
        configuration.putKey(key, keyConfig);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration updated for key: " + key);
    }

    /**
     * Update pattern configuration.
     */
    @PostMapping("/patterns/{pattern}")
    public ResponseEntity<String> updatePatternConfiguration(@PathVariable("pattern") String pattern,
                                                            @RequestBody RateLimiterConfiguration.KeyConfig keyConfig) {
        configuration.putPattern(pattern, keyConfig);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration updated for pattern: " + pattern);
    }

    /**
     * Update default configuration.
     */
    @PostMapping("/default")
    public ResponseEntity<String> updateDefaultConfiguration(@RequestBody DefaultConfigRequest request) {
        if (request.getCapacity() != null) {
            configuration.setCapacity(request.getCapacity());
        }
        if (request.getRefillRate() != null) {
            configuration.setRefillRate(request.getRefillRate());
        }
        if (request.getCleanupIntervalMs() != null) {
            configuration.setCleanupIntervalMs(request.getCleanupIntervalMs());
        }
        reloadConfiguration();
        return ResponseEntity.ok("Default configuration updated");
    }

    /**
     * Remove per-key configuration.
     */
    @DeleteMapping("/keys/{key}")
    public ResponseEntity<String> removeKeyConfiguration(@PathVariable("key") String key) {
        configuration.removeKey(key);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration removed for key: " + key);
    }

    /**
     * Remove pattern configuration.
     */
    @DeleteMapping("/patterns/{pattern}")
    public ResponseEntity<String> removePatternConfiguration(@PathVariable("pattern") String pattern) {
        configuration.removePattern(pattern);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration removed for pattern: " + pattern);
    }

    /**
     * Reload configuration - clears caches and buckets.
     */
    @PostMapping("/reload")
    public ResponseEntity<String> reloadConfiguration() {
        configurationResolver.clearCache();
        rateLimiterService.clearBuckets();
        return ResponseEntity.ok("Configuration reloaded successfully");
    }

    /**
     * Get configuration statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ConfigurationStats> getConfigurationStats() {
        ConfigurationStats stats = new ConfigurationStats(
            configurationResolver.getCacheSize(),
            rateLimiterService.getBucketCount(),
            configuration.getKeys().size(),
            configuration.getPatterns().size()
        );
        return ResponseEntity.ok(stats);
    }

}