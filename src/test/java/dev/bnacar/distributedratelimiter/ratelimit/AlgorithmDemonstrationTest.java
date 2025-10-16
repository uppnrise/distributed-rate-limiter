package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration test showing all three rate limiting algorithms working together.
 * This validates the complete implementation of the FIXED_WINDOW algorithm.
 */
public class AlgorithmDemonstrationTest {
    
    private RateLimiterService service;
    
    @BeforeEach
    void setUp() {
        // Create a configuration that demonstrates all three algorithms
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(10);
        config.setRefillRate(5);
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET); // Default
        
        // Configure Token Bucket for general users
        RateLimiterConfiguration.KeyConfig tokenBucketConfig = new RateLimiterConfiguration.KeyConfig();
        tokenBucketConfig.setCapacity(5);
        tokenBucketConfig.setRefillRate(2);
        tokenBucketConfig.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        config.putPattern("token:*", tokenBucketConfig);
        
        // Configure Sliding Window for API endpoints
        RateLimiterConfiguration.KeyConfig slidingWindowConfig = new RateLimiterConfiguration.KeyConfig();
        slidingWindowConfig.setCapacity(5);
        slidingWindowConfig.setRefillRate(2);
        slidingWindowConfig.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        config.putPattern("sliding:*", slidingWindowConfig);
        
        // Configure Fixed Window for batch operations
        RateLimiterConfiguration.KeyConfig fixedWindowConfig = new RateLimiterConfiguration.KeyConfig();
        fixedWindowConfig.setCapacity(3);
        fixedWindowConfig.setRefillRate(1);
        fixedWindowConfig.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        config.putPattern("fixed:*", fixedWindowConfig);
        
        // Configure Leaky Bucket for traffic shaping
        RateLimiterConfiguration.KeyConfig leakyBucketConfig = new RateLimiterConfiguration.KeyConfig();
        leakyBucketConfig.setCapacity(4);
        leakyBucketConfig.setRefillRate(2);
        leakyBucketConfig.setAlgorithm(RateLimitAlgorithm.LEAKY_BUCKET);
        config.putPattern("leaky:*", leakyBucketConfig);
        
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        service = new RateLimiterService(resolver, config);
    }
    
    @Test
    void testAllFourAlgorithmsWorking() {
        String tokenKey = "token:user1";
        String slidingKey = "sliding:api";
        String fixedKey = "fixed:batch";
        String leakyKey = "leaky:traffic";
        
        System.out.println("\n=== Demonstrating All Four Rate Limiting Algorithms ===\n");
        
        // Test Token Bucket Algorithm
        System.out.println("1. TOKEN BUCKET Algorithm (token:user1):");
        System.out.printf("   Capacity: 5, Should allow 5 requests then deny\n");
        for (int i = 1; i <= 6; i++) {
            boolean allowed = service.isAllowed(tokenKey, 1);
            System.out.printf("   Request %d: %s\n", i, allowed ? "ALLOWED" : "DENIED");
        }
        
        // Test Sliding Window Algorithm
        System.out.println("\n2. SLIDING WINDOW Algorithm (sliding:api):");
        System.out.printf("   Capacity: 5, Should allow 5 requests then deny\n");
        for (int i = 1; i <= 6; i++) {
            boolean allowed = service.isAllowed(slidingKey, 1);
            System.out.printf("   Request %d: %s\n", i, allowed ? "ALLOWED" : "DENIED");
        }
        
        // Test Fixed Window Algorithm
        System.out.println("\n3. FIXED WINDOW Algorithm (fixed:batch):");
        System.out.printf("   Capacity: 3, Should allow 3 requests then deny\n");
        for (int i = 1; i <= 4; i++) {
            boolean allowed = service.isAllowed(fixedKey, 1);
            System.out.printf("   Request %d: %s\n", i, allowed ? "ALLOWED" : "DENIED");
        }
        
        // Test Leaky Bucket Algorithm
        System.out.println("\n4. LEAKY BUCKET Algorithm (leaky:traffic):");
        System.out.printf("   Queue Capacity: 4, Should handle requests with traffic shaping\n");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = service.isAllowed(leakyKey, 1);
            System.out.printf("   Request %d: %s\n", i, allowed ? "ALLOWED" : "DENIED");
        }
        
        System.out.println("\n=== Algorithm Implementation Complete ===\n");
        
        // Verify the expected behavior
        // Token Bucket: Should allow 5 then deny
        assertTrue(service.isAllowed("token:newuser", 5));
        assertFalse(service.isAllowed("token:newuser", 1));
        
        // Sliding Window: Should allow 5 then deny
        assertTrue(service.isAllowed("sliding:newapi", 5));
        assertFalse(service.isAllowed("sliding:newapi", 1));
        
        // Fixed Window: Should allow 3 then deny
        assertTrue(service.isAllowed("fixed:newbatch", 3));
        assertFalse(service.isAllowed("fixed:newbatch", 1));
    }
    
    @Test 
    void testFixedWindowSpecificBehavior() {
        String key = "fixed:test";
        
        // Test that Fixed Window behaves differently from Token Bucket
        // Fixed Window resets at fixed intervals, not gradually
        
        // Fill the window completely
        assertTrue(service.isAllowed(key, 3)); // Use all capacity
        assertEquals(0, service.getBucketHolder(key).rateLimiter.getCurrentTokens());
        
        // Should be denied now
        assertFalse(service.isAllowed(key, 1));
        
        // Verify it's using FixedWindow algorithm
        assertTrue(service.getBucketHolder(key).rateLimiter instanceof FixedWindow);
        
        FixedWindow fw = (FixedWindow) service.getBucketHolder(key).rateLimiter;
        assertEquals(3, fw.getCapacity());
        assertEquals(3, fw.getCurrentUsage()); // Should have used all capacity
        assertEquals(0, fw.getCurrentTokens()); // No tokens remaining
    }
    
    @Test
    void testAlgorithmConfigurationResolution() {
        // Verify that the service correctly resolves different algorithms for different keys
        
        // Check Token Bucket
        service.isAllowed("token:test", 1);
        RateLimiter tokenRL = service.getBucketHolder("token:test").rateLimiter;
        assertTrue(tokenRL instanceof TokenBucket);
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, service.getBucketHolder("token:test").config.getAlgorithm());
        
        // Check Sliding Window  
        service.isAllowed("sliding:test", 1);
        RateLimiter slidingRL = service.getBucketHolder("sliding:test").rateLimiter;
        assertTrue(slidingRL instanceof SlidingWindow);
        assertEquals(RateLimitAlgorithm.SLIDING_WINDOW, service.getBucketHolder("sliding:test").config.getAlgorithm());
        
        // Check Fixed Window
        service.isAllowed("fixed:test", 1);
        RateLimiter fixedRL = service.getBucketHolder("fixed:test").rateLimiter;
        assertTrue(fixedRL instanceof FixedWindow);
        assertEquals(RateLimitAlgorithm.FIXED_WINDOW, service.getBucketHolder("fixed:test").config.getAlgorithm());
        
        // Check Leaky Bucket
        service.isAllowed("leaky:test", 1);
        RateLimiter leakyRL = service.getBucketHolder("leaky:test").rateLimiter;
        assertTrue(leakyRL instanceof LeakyBucket);
        assertEquals(RateLimitAlgorithm.LEAKY_BUCKET, service.getBucketHolder("leaky:test").config.getAlgorithm());
    }
}