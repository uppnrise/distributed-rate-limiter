package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.HashMap;

@Configuration
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterConfiguration {
    
    // Default configuration
    private int capacity = 10;
    private int refillRate = 2;
    private long cleanupIntervalMs = 60000; // 60 seconds
    
    // Per-key overrides: key -> config properties
    private Map<String, KeyConfig> keys = new HashMap<>();
    
    // Pattern-based configurations: pattern -> config properties
    private Map<String, KeyConfig> patterns = new HashMap<>();
    
    public static class KeyConfig {
        private int capacity;
        private int refillRate;
        private Long cleanupIntervalMs;
        
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
        
        public Long getCleanupIntervalMs() {
            return cleanupIntervalMs;
        }
        
        public void setCleanupIntervalMs(Long cleanupIntervalMs) {
            this.cleanupIntervalMs = cleanupIntervalMs;
        }
    }
    
    // Default getters and setters
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
    
    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }
    
    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }
    
    // Per-key and pattern configuration getters and setters
    public Map<String, KeyConfig> getKeys() {
        return keys;
    }
    
    public void setKeys(Map<String, KeyConfig> keys) {
        this.keys = keys;
    }
    
    public Map<String, KeyConfig> getPatterns() {
        return patterns;
    }
    
    public void setPatterns(Map<String, KeyConfig> patterns) {
        this.patterns = patterns;
    }
    
    /**
     * Get the default rate limit configuration.
     */
    public RateLimitConfig getDefaultConfig() {
        return new RateLimitConfig(capacity, refillRate, cleanupIntervalMs);
    }
}