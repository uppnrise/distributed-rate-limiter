package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing rate limiter configuration.
 */
@RestController
@RequestMapping("/api/ratelimit/config")
public class RateLimitConfigController {

    private final RateLimiterConfiguration configuration;
    private final ConfigurationResolver configurationResolver;
    private final RateLimiterService rateLimiterService;

    @Autowired
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
    public ResponseEntity<ConfigurationResponse> getConfiguration() {
        ConfigurationResponse response = new ConfigurationResponse();
        response.capacity = configuration.getCapacity();
        response.refillRate = configuration.getRefillRate();
        response.cleanupIntervalMs = configuration.getCleanupIntervalMs();
        response.keyConfigs = configuration.getKeys();
        response.patternConfigs = configuration.getPatterns();
        return ResponseEntity.ok(response);
    }

    /**
     * Update per-key configuration.
     */
    @PostMapping("/keys/{key}")
    public ResponseEntity<String> updateKeyConfiguration(@PathVariable String key,
                                                        @RequestBody RateLimiterConfiguration.KeyConfig keyConfig) {
        configuration.getKeys().put(key, keyConfig);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration updated for key: " + key);
    }

    /**
     * Update pattern configuration.
     */
    @PostMapping("/patterns/{pattern}")
    public ResponseEntity<String> updatePatternConfiguration(@PathVariable String pattern,
                                                            @RequestBody RateLimiterConfiguration.KeyConfig keyConfig) {
        configuration.getPatterns().put(pattern, keyConfig);
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
    public ResponseEntity<String> removeKeyConfiguration(@PathVariable String key) {
        configuration.getKeys().remove(key);
        reloadConfiguration();
        return ResponseEntity.ok("Configuration removed for key: " + key);
    }

    /**
     * Remove pattern configuration.
     */
    @DeleteMapping("/patterns/{pattern}")
    public ResponseEntity<String> removePatternConfiguration(@PathVariable String pattern) {
        configuration.getPatterns().remove(pattern);
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
        ConfigurationStats stats = new ConfigurationStats();
        stats.cacheSize = configurationResolver.getCacheSize();
        stats.bucketCount = rateLimiterService.getBucketCount();
        stats.keyConfigCount = configuration.getKeys().size();
        stats.patternConfigCount = configuration.getPatterns().size();
        return ResponseEntity.ok(stats);
    }

    public static class ConfigurationResponse {
        public int capacity;
        public int refillRate;
        public long cleanupIntervalMs;
        public Map<String, RateLimiterConfiguration.KeyConfig> keyConfigs;
        public Map<String, RateLimiterConfiguration.KeyConfig> patternConfigs;
    }

    public static class DefaultConfigRequest {
        private Integer capacity;
        private Integer refillRate;
        private Long cleanupIntervalMs;

        public Integer getCapacity() {
            return capacity;
        }

        public void setCapacity(Integer capacity) {
            this.capacity = capacity;
        }

        public Integer getRefillRate() {
            return refillRate;
        }

        public void setRefillRate(Integer refillRate) {
            this.refillRate = refillRate;
        }

        public Long getCleanupIntervalMs() {
            return cleanupIntervalMs;
        }

        public void setCleanupIntervalMs(Long cleanupIntervalMs) {
            this.cleanupIntervalMs = cleanupIntervalMs;
        }
    }

    public static class ConfigurationStats {
        public int cacheSize;
        public int bucketCount;
        public int keyConfigCount;
        public int patternConfigCount;
    }
}