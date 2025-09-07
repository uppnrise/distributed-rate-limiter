package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for resolving rate limit configuration for specific keys.
 * Supports exact key matches, pattern matching, and fallback to default configuration.
 */
@Service
public class ConfigurationResolver {
    
    private final RateLimiterConfiguration configuration;
    
    // Cache for resolved configurations to avoid repeated pattern matching
    private final ConcurrentHashMap<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();
    
    @Autowired
    public ConfigurationResolver(RateLimiterConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Resolve the appropriate rate limit configuration for the given key.
     * Order of precedence:
     * 1. Exact key match in per-key overrides
     * 2. Pattern match in pattern configurations (first match wins)
     * 3. Default configuration
     */
    public RateLimitConfig resolveConfig(String key) {
        // Check cache first
        RateLimitConfig cached = configCache.get(key);
        if (cached != null) {
            return cached;
        }
        
        RateLimitConfig resolved = doResolveConfig(key);
        
        // Cache the resolved configuration
        configCache.put(key, resolved);
        
        return resolved;
    }
    
    private RateLimitConfig doResolveConfig(String key) {
        // 1. Check exact key match
        RateLimiterConfiguration.KeyConfig keyConfig = configuration.getKeys().get(key);
        if (keyConfig != null) {
            return createConfig(keyConfig);
        }
        
        // 2. Check pattern matches
        for (Map.Entry<String, RateLimiterConfiguration.KeyConfig> entry : configuration.getPatterns().entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(key, pattern)) {
                return createConfig(entry.getValue());
            }
        }
        
        // 3. Return default configuration
        return configuration.getDefaultConfig();
    }
    
    /**
     * Create RateLimitConfig from KeyConfig, using defaults for unspecified values.
     */
    private RateLimitConfig createConfig(RateLimiterConfiguration.KeyConfig keyConfig) {
        int capacity = keyConfig.getCapacity() > 0 ? keyConfig.getCapacity() : configuration.getCapacity();
        int refillRate = keyConfig.getRefillRate() > 0 ? keyConfig.getRefillRate() : configuration.getRefillRate();
        long cleanupInterval = keyConfig.getCleanupIntervalMs() != null && keyConfig.getCleanupIntervalMs() > 0 
            ? keyConfig.getCleanupIntervalMs() 
            : configuration.getCleanupIntervalMs();
            
        return new RateLimitConfig(capacity, refillRate, cleanupInterval);
    }
    
    /**
     * Simple pattern matching supporting '*' wildcard.
     * Examples:
     * - "user:*" matches "user:123", "user:abc", etc.
     * - "*:admin" matches "user:admin", "system:admin", etc.
     * - "api:v1:*" matches "api:v1:users", "api:v1:orders", etc.
     */
    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return key.equals(pattern);
        }
        
        // Convert pattern to regex: escape special chars except *, then replace * with .*
        String regex = pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("|", "\\|")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("*", ".*");
            
        return key.matches("^" + regex + "$");
    }
    
    /**
     * Clear the configuration cache. Useful when configuration is updated.
     */
    public void clearCache() {
        configCache.clear();
    }
    
    /**
     * Get cache size for testing/monitoring purposes.
     */
    public int getCacheSize() {
        return configCache.size();
    }
}