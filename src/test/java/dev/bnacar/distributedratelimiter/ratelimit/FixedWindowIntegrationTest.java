package dev.bnacar.distributedratelimiter.ratelimit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for FixedWindow algorithm with RateLimiterService.
 */
public class FixedWindowIntegrationTest {
    
    private RateLimiterService service;
    
    @BeforeEach
    void setUp() {
        // Configure service with FixedWindow algorithm
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(5);
        config.setRefillRate(1);
        config.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        service = new RateLimiterService(resolver, config);
    }
    
    @Test
    void test_fixedWindowBasicBehavior() {
        String key = "test:fixed-window";
        
        // Should allow up to capacity
        for (int i = 0; i < 5; i++) {
            assertTrue(service.isAllowed(key, 1), "Request " + i + " should be allowed");
        }
        
        // 6th request should fail
        assertFalse(service.isAllowed(key, 1), "6th request should be denied");
    }
    
    @Test
    void test_fixedWindowWithDifferentKeys() {
        String key1 = "test:user1";
        String key2 = "test:user2";
        
        // Each key should have its own window
        assertTrue(service.isAllowed(key1, 5)); // Fill key1
        assertTrue(service.isAllowed(key2, 5)); // Fill key2
        
        // Both should be at capacity
        assertFalse(service.isAllowed(key1, 1));
        assertFalse(service.isAllowed(key2, 1));
    }
    
    @Test
    void test_fixedWindowPartialConsumption() {
        String key = "test:partial";
        
        // Consume part of the capacity
        assertTrue(service.isAllowed(key, 3));
        
        // Should have 2 tokens remaining
        assertTrue(service.isAllowed(key, 2));
        
        // Should be at capacity now
        assertFalse(service.isAllowed(key, 1));
    }
    
    @Test
    void test_fixedWindowConfigurationResolution() {
        // Create configuration with pattern matching
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(10);
        config.setRefillRate(5);
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET); // Default
        
        // Configure FixedWindow for admin operations
        RateLimiterConfiguration.KeyConfig adminConfig = new RateLimiterConfiguration.KeyConfig();
        adminConfig.setCapacity(3);
        adminConfig.setRefillRate(1);
        adminConfig.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        config.putPattern("admin:*", adminConfig);
        
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        RateLimiterService testService = new RateLimiterService(resolver, config);
        
        // Test that admin keys use FixedWindow with capacity 3
        String adminKey = "admin:operation";
        String regularKey = "user:operation";
        
        // Admin key should only allow 3 requests
        assertTrue(testService.isAllowed(adminKey, 3));
        assertFalse(testService.isAllowed(adminKey, 1));
        
        // Regular key should allow more (uses default TokenBucket with capacity 10)
        assertTrue(testService.isAllowed(regularKey, 10));
        assertFalse(testService.isAllowed(regularKey, 1));
    }
    
    @Test
    void test_fixedWindowLargeRequestRejection() {
        String key = "test:large-request";
        
        // Request larger than capacity should be rejected
        assertFalse(service.isAllowed(key, 10)); // Capacity is only 5
        
        // Window should still be empty, so normal requests should work
        assertTrue(service.isAllowed(key, 5));
    }
    
    @Test
    void test_fixedWindowZeroAndNegativeTokens() {
        String key = "test:invalid-tokens";
        
        // Zero and negative token requests should be rejected
        assertFalse(service.isAllowed(key, 0));
        assertFalse(service.isAllowed(key, -1));
        
        // Window should still be empty
        assertTrue(service.isAllowed(key, 5));
    }
}