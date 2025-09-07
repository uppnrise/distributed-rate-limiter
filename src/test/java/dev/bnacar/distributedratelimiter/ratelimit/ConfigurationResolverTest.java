package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationResolverTest {

    private ConfigurationResolver configurationResolver;
    private RateLimiterConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new RateLimiterConfiguration();
        // Set default configuration
        configuration.setCapacity(10);
        configuration.setRefillRate(2);
        configuration.setCleanupIntervalMs(60000);
        
        configurationResolver = new ConfigurationResolver(configuration);
    }

    @Test
    void test_shouldReturnDefaultConfiguration() {
        RateLimitConfig config = configurationResolver.resolveConfig("unknown_key");
        
        assertEquals(10, config.getCapacity());
        assertEquals(2, config.getRefillRate());
        assertEquals(60000, config.getCleanupIntervalMs());
    }

    @Test
    void test_shouldReturnPerKeyOverride() {
        // Setup per-key override
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(20);
        keyConfig.setRefillRate(5);
        keyConfig.setCleanupIntervalMs(30000L);
        keys.put("premium_user", keyConfig);
        configuration.setKeys(keys);
        
        RateLimitConfig config = configurationResolver.resolveConfig("premium_user");
        
        assertEquals(20, config.getCapacity());
        assertEquals(5, config.getRefillRate());
        assertEquals(30000, config.getCleanupIntervalMs());
    }

    @Test
    void test_shouldReturnPartialPerKeyOverride() {
        // Setup per-key override with only capacity specified
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(15);
        // refillRate and cleanupIntervalMs not set, should use defaults
        keys.put("semi_premium", keyConfig);
        configuration.setKeys(keys);
        
        RateLimitConfig config = configurationResolver.resolveConfig("semi_premium");
        
        assertEquals(15, config.getCapacity());
        assertEquals(2, config.getRefillRate()); // default
        assertEquals(60000, config.getCleanupIntervalMs()); // default
    }

    @Test
    void test_shouldMatchWildcardPattern() {
        // Setup pattern-based configuration
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(30);
        patternConfig.setRefillRate(10);
        patterns.put("user:*", patternConfig);
        configuration.setPatterns(patterns);
        
        RateLimitConfig config1 = configurationResolver.resolveConfig("user:123");
        RateLimitConfig config2 = configurationResolver.resolveConfig("user:abc");
        
        assertEquals(30, config1.getCapacity());
        assertEquals(10, config1.getRefillRate());
        assertEquals(60000, config1.getCleanupIntervalMs()); // default
        
        assertEquals(30, config2.getCapacity());
        assertEquals(10, config2.getRefillRate());
        assertEquals(60000, config2.getCleanupIntervalMs()); // default
    }

    @Test
    void test_shouldMatchComplexPatterns() {
        // Setup multiple pattern-based configurations
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        
        RateLimiterConfiguration.KeyConfig apiPattern = new RateLimiterConfiguration.KeyConfig();
        apiPattern.setCapacity(100);
        apiPattern.setRefillRate(50);
        patterns.put("api:v1:*", apiPattern);
        
        RateLimiterConfiguration.KeyConfig adminPattern = new RateLimiterConfiguration.KeyConfig();
        adminPattern.setCapacity(1000);
        adminPattern.setRefillRate(100);
        patterns.put("*:admin", adminPattern);
        
        configuration.setPatterns(patterns);
        
        // Test API pattern match
        RateLimitConfig apiConfig = configurationResolver.resolveConfig("api:v1:users");
        assertEquals(100, apiConfig.getCapacity());
        assertEquals(50, apiConfig.getRefillRate());
        
        // Test admin pattern match
        RateLimitConfig adminConfig = configurationResolver.resolveConfig("system:admin");
        assertEquals(1000, adminConfig.getCapacity());
        assertEquals(100, adminConfig.getRefillRate());
        
        // Test no pattern match
        RateLimitConfig defaultConfig = configurationResolver.resolveConfig("other:service");
        assertEquals(10, defaultConfig.getCapacity());
        assertEquals(2, defaultConfig.getRefillRate());
    }

    @Test
    void test_shouldPreferExactKeyOverPattern() {
        // Setup both exact key and pattern that would match
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig exactConfig = new RateLimiterConfiguration.KeyConfig();
        exactConfig.setCapacity(50);
        exactConfig.setRefillRate(25);
        keys.put("user:special", exactConfig);
        configuration.setKeys(keys);
        
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(30);
        patternConfig.setRefillRate(10);
        patterns.put("user:*", patternConfig);
        configuration.setPatterns(patterns);
        
        // Exact key should win over pattern
        RateLimitConfig config = configurationResolver.resolveConfig("user:special");
        assertEquals(50, config.getCapacity());
        assertEquals(25, config.getRefillRate());
        
        // Other user keys should use pattern
        RateLimitConfig patternMatch = configurationResolver.resolveConfig("user:regular");
        assertEquals(30, patternMatch.getCapacity());
        assertEquals(10, patternMatch.getRefillRate());
    }

    @Test
    void test_shouldCacheResolvedConfigurations() {
        // Setup pattern configuration
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(30);
        patterns.put("user:*", patternConfig);
        configuration.setPatterns(patterns);
        
        assertEquals(0, configurationResolver.getCacheSize());
        
        // First resolution should cache the result
        configurationResolver.resolveConfig("user:123");
        assertEquals(1, configurationResolver.getCacheSize());
        
        // Second resolution should use cache (same result)
        RateLimitConfig config = configurationResolver.resolveConfig("user:123");
        assertEquals(1, configurationResolver.getCacheSize());
        assertEquals(30, config.getCapacity());
        
        // Different key should be cached separately
        configurationResolver.resolveConfig("user:456");
        assertEquals(2, configurationResolver.getCacheSize());
    }

    @Test
    void test_shouldClearCache() {
        // Setup and populate cache
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(30);
        patterns.put("user:*", patternConfig);
        configuration.setPatterns(patterns);
        
        configurationResolver.resolveConfig("user:123");
        configurationResolver.resolveConfig("user:456");
        assertEquals(2, configurationResolver.getCacheSize());
        
        // Clear cache
        configurationResolver.clearCache();
        assertEquals(0, configurationResolver.getCacheSize());
    }

    @Test
    void test_shouldHandleSpecialCharactersInPatterns() {
        // Setup patterns with special regex characters
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        
        RateLimiterConfiguration.KeyConfig dotPattern = new RateLimiterConfiguration.KeyConfig();
        dotPattern.setCapacity(40);
        patterns.put("api.v1.*", dotPattern);
        
        RateLimiterConfiguration.KeyConfig bracketPattern = new RateLimiterConfiguration.KeyConfig();
        bracketPattern.setCapacity(50);
        patterns.put("test[a-z]*", bracketPattern);
        
        configuration.setPatterns(patterns);
        
        // These should match literally, not as regex
        RateLimitConfig dotConfig = configurationResolver.resolveConfig("api.v1.users");
        assertEquals(40, dotConfig.getCapacity());
        
        RateLimitConfig bracketConfig = configurationResolver.resolveConfig("test[a-z]abc");
        assertEquals(50, bracketConfig.getCapacity());
        
        // These should NOT match
        RateLimitConfig noMatch1 = configurationResolver.resolveConfig("apixv1xusers"); // . should not be wildcard
        assertEquals(10, noMatch1.getCapacity()); // default
        
        RateLimitConfig noMatch2 = configurationResolver.resolveConfig("testaabc"); // [a-z] should not be regex
        assertEquals(10, noMatch2.getCapacity()); // default
    }

    @Test
    void test_shouldMatchGlobalWildcard() {
        // Setup global wildcard pattern
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig globalConfig = new RateLimiterConfiguration.KeyConfig();
        globalConfig.setCapacity(5);
        globalConfig.setRefillRate(1);
        patterns.put("*", globalConfig);
        configuration.setPatterns(patterns);
        
        RateLimitConfig config = configurationResolver.resolveConfig("any_key_here");
        assertEquals(5, config.getCapacity());
        assertEquals(1, config.getRefillRate());
    }
}