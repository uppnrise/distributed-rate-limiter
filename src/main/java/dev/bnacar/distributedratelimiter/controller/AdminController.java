package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.AdminKeyStats;
import dev.bnacar.distributedratelimiter.models.AdminKeysResponse;
import dev.bnacar.distributedratelimiter.models.AdminLimitRequest;
import dev.bnacar.distributedratelimiter.models.AdminLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing rate limits and monitoring system state.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterConfiguration configuration;
    private final ConfigurationResolver configurationResolver;

    @Autowired
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
    public ResponseEntity<AdminLimitResponse> getKeyLimits(@PathVariable("key") String key) {
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
    public ResponseEntity<AdminLimitResponse> updateKeyLimits(@PathVariable("key") String key,
                                                             @Valid @RequestBody AdminLimitRequest request) {
        // Create new key configuration
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(request.getCapacity());
        keyConfig.setRefillRate(request.getRefillRate());
        keyConfig.setCleanupIntervalMs(request.getCleanupIntervalMs() != null ? request.getCleanupIntervalMs() : 60000L);
        keyConfig.setAlgorithm(request.getAlgorithm() != null ? request.getAlgorithm() : dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm.TOKEN_BUCKET);

        // Update configuration
        configuration.getKeys().put(key, keyConfig);
        
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
    public ResponseEntity<String> removeKeyLimits(@PathVariable("key") String key) {
        // Remove from configuration
        boolean configRemoved = configuration.getKeys().remove(key) != null;
        
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