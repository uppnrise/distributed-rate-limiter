package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import dev.bnacar.distributedratelimiter.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "ratelimiter.redis.enabled=true",
    "ratelimiter.capacity=5",
    "ratelimiter.refillRate=1"
})
class DistributedRateLimiterServiceTest {

    @Autowired
    private DistributedRateLimiterService distributedRateLimiterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        distributedRateLimiterService.clearAllBuckets();
    }

    @Test
    void testRedisBackendIsUsed() {
        assertTrue(distributedRateLimiterService.isUsingRedis());
        assertFalse(distributedRateLimiterService.isUsingFallback());
    }

    @Test
    void testBasicRateLimit() {
        String key = "test:basic";
        
        // Should allow consuming within capacity
        assertTrue(distributedRateLimiterService.isAllowed(key, 3));
        assertTrue(distributedRateLimiterService.isAllowed(key, 2));
        
        // Should reject when capacity is exceeded
        assertFalse(distributedRateLimiterService.isAllowed(key, 1));
    }

    @Test
    void testDistributedRateLimit() {
        String key = "test:distributed";
        
        // Create two instances (simulating different app instances)
        // Use a configuration that matches the test properties
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(5);
        config.setRefillRate(1);
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        DistributedRateLimiterService instance1 = new DistributedRateLimiterService(resolver, redisTemplate);
        DistributedRateLimiterService instance2 = new DistributedRateLimiterService(resolver, redisTemplate);
        
        // Consume tokens from both instances
        assertTrue(instance1.isAllowed(key, 2));
        assertTrue(instance2.isAllowed(key, 2));
        
        // Should have consumed 4 out of 5 tokens total
        assertTrue(instance1.isAllowed(key, 1));
        
        // No more tokens should be available from either instance
        assertFalse(instance1.isAllowed(key, 1));
        assertFalse(instance2.isAllowed(key, 1));
    }

    @Test
    void testZeroTokenConsumption() {
        String key = "test:zero";
        assertFalse(distributedRateLimiterService.isAllowed(key, 0));
    }

    @Test
    void testNegativeTokenConsumption() {
        String key = "test:negative";
        assertFalse(distributedRateLimiterService.isAllowed(key, -1));
    }

    @Test
    void testMultipleKeys() {
        String key1 = "test:key1";
        String key2 = "test:key2";
        
        // Each key should have independent limits
        assertTrue(distributedRateLimiterService.isAllowed(key1, 5));
        assertTrue(distributedRateLimiterService.isAllowed(key2, 5));
        
        // Both keys should be exhausted independently
        assertFalse(distributedRateLimiterService.isAllowed(key1, 1));
        assertFalse(distributedRateLimiterService.isAllowed(key2, 1));
    }

    @Test
    void testTokenRefill() throws InterruptedException {
        String key = "test:refill";
        
        // Consume all tokens
        assertTrue(distributedRateLimiterService.isAllowed(key, 5));
        assertFalse(distributedRateLimiterService.isAllowed(key, 1));
        
        // Wait for refill (refill rate is 1 token per second)
        Thread.sleep(1100);
        
        // Should have refilled 1 token
        assertTrue(distributedRateLimiterService.isAllowed(key, 1));
        assertFalse(distributedRateLimiterService.isAllowed(key, 1));
    }

    @Test
    void testConcurrentAccessAcrossInstances() throws InterruptedException {
        String key = "test:concurrent";
        final int threadCount = 10;
        final int tokensPerThread = 1;
        
        // Create multiple service instances with proper configuration
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(5);
        config.setRefillRate(1);
        ConfigurationResolver resolver = new ConfigurationResolver(config);
        DistributedRateLimiterService[] instances = new DistributedRateLimiterService[5];
        for (int i = 0; i < instances.length; i++) {
            instances[i] = new DistributedRateLimiterService(resolver, redisTemplate);
        }
        
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        
        // Create threads that use different service instances
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final DistributedRateLimiterService instance = instances[i % instances.length];
            threads[i] = new Thread(() -> {
                results[index] = instance.isAllowed(key, tokensPerThread);
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Count successful requests
        int successCount = 0;
        for (boolean result : results) {
            if (result) successCount++;
        }
        
        // Should only allow 5 successful requests (the capacity)
        assertEquals(5, successCount);
    }

    @Test
    void testClearAllBuckets() {
        String key = "test:clear";
        
        // Use some tokens
        assertTrue(distributedRateLimiterService.isAllowed(key, 3));
        
        // Clear buckets
        distributedRateLimiterService.clearAllBuckets();
        
        // Should have full capacity again
        assertTrue(distributedRateLimiterService.isAllowed(key, 5));
    }
}