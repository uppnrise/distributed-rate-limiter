package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced RateLimiterService with configurable limits per key/pattern.
 */
public class ConfigurableRateLimiterServiceTest {

    private RateLimiterConfiguration configuration;
    private ConfigurationResolver configurationResolver;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        configuration = new RateLimiterConfiguration();
        configuration.setCapacity(10);
        configuration.setRefillRate(2);
        configuration.setCleanupIntervalMs(60000);
        
        configurationResolver = new ConfigurationResolver(configuration);
        rateLimiterService = new RateLimiterService(configurationResolver, configuration);
    }

    @Test
    void test_shouldUseDefaultConfiguration() {
        String key = "regular_user";
        
        // Should be able to consume up to default capacity (10)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.isAllowed(key, 1), "Should allow token " + (i + 1));
        }
        
        // Should reject next request
        assertFalse(rateLimiterService.isAllowed(key, 1), "Should reject when capacity exceeded");
    }

    @Test
    void test_shouldUsePerKeyConfiguration() {
        // Setup per-key configuration for premium user
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig premiumConfig = new RateLimiterConfiguration.KeyConfig();
        premiumConfig.setCapacity(20);
        premiumConfig.setRefillRate(5);
        keys.put("premium_user", premiumConfig);
        configuration.setKeys(keys);
        
        // Clear cache to pick up new configuration
        configurationResolver.clearCache();
        
        String premiumKey = "premium_user";
        String regularKey = "regular_user";
        
        // Premium user should have 20 tokens capacity
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiterService.isAllowed(premiumKey, 1), "Premium user should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(premiumKey, 1), "Premium user should reject when capacity exceeded");
        
        // Regular user should still have 10 tokens capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.isAllowed(regularKey, 1), "Regular user should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(regularKey, 1), "Regular user should reject when capacity exceeded");
    }

    @Test
    void test_shouldUsePatternConfiguration() {
        // Setup pattern configuration for API keys
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig apiConfig = new RateLimiterConfiguration.KeyConfig();
        apiConfig.setCapacity(100);
        apiConfig.setRefillRate(50);
        patterns.put("api:*", apiConfig);
        configuration.setPatterns(patterns);
        
        // Clear cache to pick up new configuration
        configurationResolver.clearCache();
        
        String apiKey1 = "api:service1";
        String apiKey2 = "api:service2";
        String userKey = "user:123";
        
        // API keys should have 100 tokens capacity
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiterService.isAllowed(apiKey1, 1), "API key 1 should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(apiKey1, 1), "API key 1 should reject when capacity exceeded");
        
        // Another API key should also have 100 tokens capacity (independent bucket)
        for (int i = 0; i < 100; i++) {
            assertTrue(rateLimiterService.isAllowed(apiKey2, 1), "API key 2 should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(apiKey2, 1), "API key 2 should reject when capacity exceeded");
        
        // User key should use default configuration (10 tokens)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.isAllowed(userKey, 1), "User key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(userKey, 1), "User key should reject when capacity exceeded");
    }

    @Test
    void test_shouldPreferExactKeyOverPattern() {
        // Setup both exact key and pattern configuration
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig exactConfig = new RateLimiterConfiguration.KeyConfig();
        exactConfig.setCapacity(50);
        keys.put("api:special", exactConfig);
        configuration.setKeys(keys);
        
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(30);
        patterns.put("api:*", patternConfig);
        configuration.setPatterns(patterns);
        
        // Clear cache to pick up new configuration
        configurationResolver.clearCache();
        
        String specialKey = "api:special";
        String regularApiKey = "api:regular";
        
        // Special API key should use exact configuration (50 tokens)
        for (int i = 0; i < 50; i++) {
            assertTrue(rateLimiterService.isAllowed(specialKey, 1), "Special API key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(specialKey, 1), "Special API key should reject when capacity exceeded");
        
        // Regular API key should use pattern configuration (30 tokens)
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiterService.isAllowed(regularApiKey, 1), "Regular API key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(regularApiKey, 1), "Regular API key should reject when capacity exceeded");
    }

    @Test
    void test_shouldHandleMultiplePatterns() {
        // Setup multiple pattern configurations
        Map<String, RateLimiterConfiguration.KeyConfig> patterns = new HashMap<>();
        
        RateLimiterConfiguration.KeyConfig userPattern = new RateLimiterConfiguration.KeyConfig();
        userPattern.setCapacity(15);
        patterns.put("user:*", userPattern);
        
        RateLimiterConfiguration.KeyConfig adminPattern = new RateLimiterConfiguration.KeyConfig();
        adminPattern.setCapacity(200);
        patterns.put("*:admin", adminPattern);
        
        configuration.setPatterns(patterns);
        
        // Clear cache to pick up new configuration
        configurationResolver.clearCache();
        
        String userKey = "user:123";
        String adminKey = "system:admin";
        String otherKey = "other:service";
        
        // User key should match user pattern (15 tokens)
        for (int i = 0; i < 15; i++) {
            assertTrue(rateLimiterService.isAllowed(userKey, 1), "User key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(userKey, 1), "User key should reject when capacity exceeded");
        
        // Admin key should match admin pattern (200 tokens)
        for (int i = 0; i < 200; i++) {
            assertTrue(rateLimiterService.isAllowed(adminKey, 1), "Admin key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(adminKey, 1), "Admin key should reject when capacity exceeded");
        
        // Other key should use default configuration (10 tokens)
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiterService.isAllowed(otherKey, 1), "Other key should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(otherKey, 1), "Other key should reject when capacity exceeded");
    }

    @Test
    void test_shouldClearBucketsAndCache() {
        String key1 = "test:key1";
        String key2 = "test:key2";
        
        // Create some buckets
        rateLimiterService.isAllowed(key1, 1);
        rateLimiterService.isAllowed(key2, 1);
        
        assertTrue(rateLimiterService.getBucketCount() >= 2, "Should have created buckets");
        assertTrue(configurationResolver.getCacheSize() >= 2, "Should have cached configurations");
        
        // Clear buckets and cache
        rateLimiterService.clearBuckets();
        
        assertEquals(0, rateLimiterService.getBucketCount(), "Should have cleared all buckets");
        assertEquals(0, configurationResolver.getCacheSize(), "Should have cleared configuration cache");
    }

    @Test
    void test_shouldUsePartialConfiguration() {
        // Setup configuration with only capacity specified
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig partialConfig = new RateLimiterConfiguration.KeyConfig();
        partialConfig.setCapacity(25);
        // refillRate and cleanupIntervalMs not set - should use defaults
        keys.put("partial_config", partialConfig);
        configuration.setKeys(keys);
        
        // Clear cache to pick up new configuration
        configurationResolver.clearCache();
        
        String key = "partial_config";
        
        // Should be able to consume 25 tokens (custom capacity)
        for (int i = 0; i < 25; i++) {
            assertTrue(rateLimiterService.isAllowed(key, 1), "Should allow token " + (i + 1));
        }
        assertFalse(rateLimiterService.isAllowed(key, 1), "Should reject when capacity exceeded");
        
        // Verify the configuration is correctly resolved
        RateLimitConfig config = configurationResolver.resolveConfig(key);
        assertEquals(25, config.getCapacity());
        assertEquals(2, config.getRefillRate()); // default
        assertEquals(60000, config.getCleanupIntervalMs()); // default
    }
}