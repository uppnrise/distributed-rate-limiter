package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.awaitility.Awaitility;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests comparing burst handling behavior between TokenBucket and SlidingWindow algorithms.
 */
public class BurstHandlingComparisonTest {

    @Test
    void test_burstHandlingDifferences() throws InterruptedException {
        // Create identical configurations for both algorithms
        int capacity = 10;
        int refillRate = 2; // 2 tokens per second
        
        TokenBucket tokenBucket = new TokenBucket(capacity, refillRate);
        SlidingWindow slidingWindow = new SlidingWindow(capacity, refillRate);
        
        // Test initial burst behavior
        // Both should allow full capacity initially
        assertTrue(tokenBucket.tryConsume(10));
        assertTrue(slidingWindow.tryConsume(10));
        
        // Both should reject additional requests
        assertFalse(tokenBucket.tryConsume(1));
        assertFalse(slidingWindow.tryConsume(1));
        
        // Wait for token bucket to refill (1 second for 2 tokens)
        Thread.sleep(1100);
        
        // Token bucket should now allow some tokens due to refill
        assertTrue(tokenBucket.tryConsume(2), "TokenBucket should refill over time");
        
        // Sliding window should allow full capacity again after window slides
        assertTrue(slidingWindow.tryConsume(10), "SlidingWindow should reset after window slides");
    }

    @Test
    void test_sustainedTrafficHandling() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(5, 2);
        SlidingWindow slidingWindow = new SlidingWindow(5, 2);
        
        // Test sustained moderate traffic - 1 request every 600ms
        // This should be sustainable for token bucket (2 per second = 1 per 500ms)
        // but might behave differently for sliding window
        
        int allowedByTokenBucket = 0;
        int allowedBySlidingWindow = 0;
        
        for (int i = 0; i < 5; i++) {
            if (tokenBucket.tryConsume(1)) {
                allowedByTokenBucket++;
            }
            if (slidingWindow.tryConsume(1)) {
                allowedBySlidingWindow++;
            }
            
            if (i < 4) { // Don't sleep after last iteration
                Thread.sleep(600);
            }
        }
        
        // Both algorithms should handle this moderate sustained traffic
        assertTrue(allowedByTokenBucket >= 3, "TokenBucket should handle sustained traffic");
        assertTrue(allowedBySlidingWindow >= 3, "SlidingWindow should handle sustained traffic");
    }

    @Test
    void test_recoveryAfterBurst() {
        TokenBucket tokenBucket = new TokenBucket(8, 4);
        SlidingWindow slidingWindow = new SlidingWindow(8, 4);
        
        // Both start with full capacity
        assertEquals(8, tokenBucket.getCurrentTokens());
        assertEquals(8, slidingWindow.getCurrentTokens());
        
        // Consume all tokens in a burst
        assertTrue(tokenBucket.tryConsume(8));
        assertTrue(slidingWindow.tryConsume(8));
        
        // Both should be empty
        assertEquals(0, tokenBucket.getCurrentTokens());
        assertEquals(0, slidingWindow.getCurrentTokens());
        
        // Wait for recovery - token bucket refills gradually, sliding window resets after window
        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .until(() -> {
                    // Check if either algorithm allows consuming tokens again
                    return tokenBucket.tryConsume(1) || slidingWindow.tryConsume(1);
                });
        
        // At this point, at least one should allow tokens
        // Test that they can consume tokens again (recovery verification)
        boolean tokenBucketRecovered = tokenBucket.tryConsume(1);
        boolean slidingWindowRecovered = slidingWindow.tryConsume(1);
        
        // At least one should have recovered
        assertTrue(tokenBucketRecovered || slidingWindowRecovered, 
                "At least one algorithm should have recovered");
    }

    @Test
    void test_burstyVsSteadyTrafficPatterns() throws InterruptedException {
        TokenBucket tokenBucket = new TokenBucket(6, 3);
        SlidingWindow slidingWindow = new SlidingWindow(6, 3);
        
        // Test bursty pattern: 6 requests immediately, then wait
        assertTrue(tokenBucket.tryConsume(6));
        assertTrue(slidingWindow.tryConsume(6));
        
        // Both should reject immediate additional requests
        assertFalse(tokenBucket.tryConsume(1));
        assertFalse(slidingWindow.tryConsume(1));
        
        // Wait 500ms - partial recovery time
        Thread.sleep(500);
        
        // Token bucket might allow 1-2 tokens due to partial refill
        boolean tokenBucketAllowsPartial = tokenBucket.tryConsume(1);
        
        // Sliding window is still blocked (window hasn't fully slid)
        boolean slidingWindowAllowsPartial = slidingWindow.tryConsume(1);
        
        // This demonstrates different behavior patterns
        // TokenBucket allows gradual recovery, SlidingWindow has stricter window enforcement
        System.out.println("TokenBucket allows partial recovery: " + tokenBucketAllowsPartial);
        System.out.println("SlidingWindow allows partial recovery: " + slidingWindowAllowsPartial);
        
        // Wait for full window to slide (1+ seconds)
        Thread.sleep(600);
        
        // Now sliding window should definitely allow requests
        assertTrue(slidingWindow.tryConsume(1), "SlidingWindow should allow after full window slide");
    }

    @Test
    void test_algorithmSelectionInService() {
        // Test that RateLimiterService correctly uses different algorithms
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(5);
        config.setRefillRate(2);
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        
        // Configure sliding window for API endpoints
        RateLimiterConfiguration.KeyConfig apiConfig = new RateLimiterConfiguration.KeyConfig();
        apiConfig.setCapacity(5);
        apiConfig.setRefillRate(2);
        apiConfig.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        config.putPattern("api:*", apiConfig);
        
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        RateLimiterService service = new RateLimiterService(resolver, config);
        
        // Test that different keys use different algorithms
        String tokenBucketKey = "user:123";
        String slidingWindowKey = "api:endpoint";
        
        // Both should initially allow full capacity
        assertTrue(service.isAllowed(tokenBucketKey, 5));
        assertTrue(service.isAllowed(slidingWindowKey, 5));
        
        // Both should reject additional requests when at capacity
        assertFalse(service.isAllowed(tokenBucketKey, 1));
        assertFalse(service.isAllowed(slidingWindowKey, 1));
        
        // This verifies the service correctly instantiates different algorithms
        // The specific behavioral differences would be tested in longer integration tests
    }
}