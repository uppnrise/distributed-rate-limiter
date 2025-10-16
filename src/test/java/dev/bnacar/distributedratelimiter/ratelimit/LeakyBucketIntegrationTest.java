package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for LeakyBucket with RateLimiterService.
 * Tests configuration resolution and service integration.
 */
public class LeakyBucketIntegrationTest {
    
    private RateLimiterService service;
    
    @BeforeEach
    void setUp() {
        // Create configuration with leaky bucket algorithm
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(10);
        config.setRefillRate(5);
        config.setAlgorithm(RateLimitAlgorithm.LEAKY_BUCKET);
        config.setCleanupIntervalMs(60000);
        
        // Configure pattern for leaky bucket
        RateLimiterConfiguration.KeyConfig leakyConfig = new RateLimiterConfiguration.KeyConfig();
        leakyConfig.setCapacity(20);
        leakyConfig.setRefillRate(3);
        leakyConfig.setAlgorithm(RateLimitAlgorithm.LEAKY_BUCKET);
        config.putPattern("leaky:*", leakyConfig);
        
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        service = new RateLimiterService(resolver, config);
    }
    
    @AfterEach
    void tearDown() {
        if (service != null) {
            service.shutdown();
        }
    }
    
    @Test
    void testLeakyBucketCreation() {
        // Test that leaky bucket is created correctly
        assertTrue(service.isAllowed("leaky:test", 1));
        
        RateLimiterService.BucketHolder holder = service.getBucketHolder("leaky:test");
        assertNotNull(holder);
        assertTrue(holder.rateLimiter instanceof LeakyBucket);
        assertEquals(RateLimitAlgorithm.LEAKY_BUCKET, holder.config.getAlgorithm());
        
        LeakyBucket leakyBucket = (LeakyBucket) holder.rateLimiter;
        assertEquals(20, leakyBucket.getCapacity());
        assertEquals(3, leakyBucket.getRefillRate());
        assertEquals(3.0, leakyBucket.getLeakRatePerSecond());
    }
    
    @Test
    void testDefaultLeakyBucketConfiguration() {
        // Test default configuration
        assertTrue(service.isAllowed("default:key", 1));
        
        RateLimiterService.BucketHolder holder = service.getBucketHolder("default:key");
        assertNotNull(holder);
        assertTrue(holder.rateLimiter instanceof LeakyBucket);
        
        LeakyBucket leakyBucket = (LeakyBucket) holder.rateLimiter;
        assertEquals(10, leakyBucket.getCapacity());
        assertEquals(5, leakyBucket.getRefillRate());
    }
    
    @Test
    void testLeakyBucketBehaviorThroughService() {
        String key = "leaky:traffic-shaping";
        
        // Test that leaky bucket is created and responds appropriately
        // Note: The synchronous RateLimiter interface doesn't fully utilize the queue behavior
        // but provides an approximation for rate limiting
        
        int successCount = 0;
        for (int i = 0; i < 10; i++) {
            if (service.isAllowed(key, 1)) {
                successCount++;
            }
        }
        
        // Leaky bucket should allow some requests (approximating queue behavior)
        assertTrue(successCount > 0, "Should allow some requests");
        
        RateLimiterService.BucketHolder holder = service.getBucketHolder(key);
        assertNotNull(holder);
        assertTrue(holder.rateLimiter instanceof LeakyBucket);
        
        LeakyBucket leakyBucket = (LeakyBucket) holder.rateLimiter;
        assertEquals(20, leakyBucket.getCapacity());
        assertEquals(3, leakyBucket.getRefillRate());
    }
    
    @Test
    void testGetCurrentTokensIntegration() {
        String key = "leaky:monitoring";
        
        // Make some requests
        service.isAllowed(key, 1);
        service.isAllowed(key, 2);
        
        RateLimiterService.BucketHolder holder = service.getBucketHolder(key);
        assertNotNull(holder);
        
        // getCurrentTokens should work (represents available queue capacity)
        int currentTokens = holder.rateLimiter.getCurrentTokens();
        assertTrue(currentTokens >= 0, "Current tokens should be non-negative");
        assertTrue(currentTokens <= 20, "Current tokens should not exceed capacity");
    }
    
    @Test
    void testConfigurationResolution() {
        // Test that configuration is resolved correctly for different keys
        
        // Pattern match
        RateLimitConfig config1 = service.getKeyConfiguration("leaky:api");
        assertEquals(RateLimitAlgorithm.LEAKY_BUCKET, config1.getAlgorithm());
        assertEquals(20, config1.getCapacity());
        assertEquals(3, config1.getRefillRate());
        
        // Default configuration
        RateLimitConfig config2 = service.getKeyConfiguration("other:key");
        assertEquals(RateLimitAlgorithm.LEAKY_BUCKET, config2.getAlgorithm());
        assertEquals(10, config2.getCapacity());
        assertEquals(5, config2.getRefillRate());
    }
    
    @Test
    void testMultipleLeakyBuckets() {
        // Test multiple independent leaky buckets
        String key1 = "leaky:service1";
        String key2 = "leaky:service2";
        String key3 = "other:service";
        
        // Create buckets
        assertTrue(service.isAllowed(key1, 1));
        assertTrue(service.isAllowed(key2, 1));
        assertTrue(service.isAllowed(key3, 1));
        
        // Verify they're all leaky buckets but independent
        RateLimiterService.BucketHolder holder1 = service.getBucketHolder(key1);
        RateLimiterService.BucketHolder holder2 = service.getBucketHolder(key2);
        RateLimiterService.BucketHolder holder3 = service.getBucketHolder(key3);
        
        assertNotNull(holder1);
        assertNotNull(holder2);
        assertNotNull(holder3);
        
        assertTrue(holder1.rateLimiter instanceof LeakyBucket);
        assertTrue(holder2.rateLimiter instanceof LeakyBucket);
        assertTrue(holder3.rateLimiter instanceof LeakyBucket);
        
        // They should be different instances
        assertNotSame(holder1.rateLimiter, holder2.rateLimiter);
        assertNotSame(holder2.rateLimiter, holder3.rateLimiter);
        
        // Configuration should be different based on patterns
        LeakyBucket bucket1 = (LeakyBucket) holder1.rateLimiter;
        LeakyBucket bucket2 = (LeakyBucket) holder2.rateLimiter;
        LeakyBucket bucket3 = (LeakyBucket) holder3.rateLimiter;
        
        assertEquals(20, bucket1.getCapacity()); // Pattern config
        assertEquals(20, bucket2.getCapacity()); // Pattern config
        assertEquals(10, bucket3.getCapacity()); // Default config
    }
    
    @Test
    void testBucketCleanup() throws InterruptedException {
        // Test that leaky buckets are properly cleaned up
        String key = "leaky:cleanup-test";
        
        assertTrue(service.isAllowed(key, 1));
        assertEquals(1, service.getBucketCount());
        
        RateLimiterService.BucketHolder holder = service.getBucketHolder(key);
        assertNotNull(holder);
        assertTrue(holder.rateLimiter instanceof LeakyBucket);
        
        // Buckets should be cleaned up properly when service shuts down
        service.clearBuckets();
        assertEquals(0, service.getBucketCount());
        assertNull(service.getBucketHolder(key));
    }
}