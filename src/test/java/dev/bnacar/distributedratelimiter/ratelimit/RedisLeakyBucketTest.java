package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import dev.bnacar.distributedratelimiter.TestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "ratelimiter.redis.enabled=true"
})
class RedisLeakyBucketTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private RedisLeakyBucket redisLeakyBucket;
    private final String testKey = "test:redis:leaky";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        String queueKey = "leaky_bucket:" + testKey + ":queue";
        String metaKey = "leaky_bucket:" + testKey + ":meta";
        redisTemplate.delete(queueKey);
        redisTemplate.delete(metaKey);
        
        // Create a new Redis leaky bucket with queueCapacity=10, leakRate=2.0/sec, maxQueueTime=5s
        redisLeakyBucket = new RedisLeakyBucket(testKey, 10, 2.0, 5000, redisTemplate);
    }

    @Test
    void testInitialState() {
        // New bucket should have full queue capacity available
        assertEquals(10, redisLeakyBucket.getCurrentTokens());
        assertEquals(10, redisLeakyBucket.getCapacity());
        assertEquals(2, redisLeakyBucket.getRefillRate());
        assertEquals(2.0, redisLeakyBucket.getLeakRatePerSecond());
        assertEquals(5000, redisLeakyBucket.getMaxQueueTimeMs());
        assertEquals(0, redisLeakyBucket.getQueueSize());
    }

    @Test
    void testSuccessfulTokenConsumption() {
        // Should be able to add requests to queue
        assertTrue(redisLeakyBucket.tryConsume(1));
        assertEquals(1, redisLeakyBucket.getQueueSize());
        assertEquals(9, redisLeakyBucket.getCurrentTokens()); // 10 - 1 queued
        
        assertTrue(redisLeakyBucket.tryConsume(3));
        assertEquals(4, redisLeakyBucket.getQueueSize());
        assertEquals(6, redisLeakyBucket.getCurrentTokens()); // 10 - 4 queued
    }

    @Test
    void testQueueCapacityLimit() {
        // Fill queue to capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(redisLeakyBucket.tryConsume(1), "Should accept request " + (i + 1));
        }
        
        assertEquals(10, redisLeakyBucket.getQueueSize());
        assertEquals(0, redisLeakyBucket.getCurrentTokens());
        
        // Next request should be rejected
        assertFalse(redisLeakyBucket.tryConsume(1));
        assertEquals(10, redisLeakyBucket.getQueueSize()); // Should not change
    }

    @Test
    void testLeakRateProcessing() throws InterruptedException {
        // Add some requests to queue
        assertTrue(redisLeakyBucket.tryConsume(4));
        assertEquals(4, redisLeakyBucket.getQueueSize());
        
        // Wait for leak processing (2 requests per second)
        // After 1 second, ~2 requests should be processed
        Thread.sleep(1100); // Wait 1.1 seconds
        
        // Check that some requests were processed
        int queueSizeAfter = redisLeakyBucket.getQueueSize();
        assertTrue(queueSizeAfter < 4, "Some requests should have been processed. Queue size: " + queueSizeAfter);
        
        // Wait for all to be processed
        await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> redisLeakyBucket.getQueueSize() == 0);
        
        assertEquals(0, redisLeakyBucket.getQueueSize());
        assertEquals(10, redisLeakyBucket.getCurrentTokens()); // Full capacity available again
    }

    @Test
    void testEstimatedWaitTime() {
        // Empty queue should have minimal wait time
        long emptyWaitTime = redisLeakyBucket.getEstimatedWaitTime(1);
        assertTrue(emptyWaitTime >= 0, "Wait time should be non-negative");
        
        // Add some requests
        redisLeakyBucket.tryConsume(4);
        
        // Estimate wait time for 1 more request
        long waitTime = redisLeakyBucket.getEstimatedWaitTime(1);
        assertTrue(waitTime > 0, "Should have positive wait time");
        
        // With 4 in queue + 1 new = 5 total, at 2/sec rate = 2.5 seconds = 2500ms
        assertTrue(waitTime >= 2000 && waitTime <= 3000, 
            "Wait time should be around 2.5 seconds, got: " + waitTime + "ms");
        
        // Request that would exceed capacity should return -1
        long rejectedWaitTime = redisLeakyBucket.getEstimatedWaitTime(7); // 4 + 7 = 11 > 10
        assertEquals(-1, rejectedWaitTime, "Should indicate rejection");
    }

    @Test 
    @Timeout(10)
    void testRequestTimeout() throws InterruptedException {
        // Create bucket with very short timeout
        RedisLeakyBucket shortTimeoutBucket = new RedisLeakyBucket(
            testKey + ":timeout", 5, 0.2, 500, redisTemplate); // 0.5 second timeout, very slow leak
        
        try {
            // Fill queue
            for (int i = 0; i < 5; i++) {
                assertTrue(shortTimeoutBucket.tryConsume(1));
            }
            
            assertEquals(5, shortTimeoutBucket.getQueueSize());
            
            // Wait longer than timeout
            Thread.sleep(800);
            
            // Some requests should have timed out and been cleaned up
            int finalQueueSize = shortTimeoutBucket.getQueueSize();
            assertTrue(finalQueueSize < 5, 
                "Some requests should have timed out. Final queue size: " + finalQueueSize);
            
        } finally {
            shortTimeoutBucket.clearQueue();
        }
    }

    @Test
    void testAsynchronousEnqueueRequest() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Boolean> future1 = redisLeakyBucket.enqueueRequest(1);
        CompletableFuture<Boolean> future2 = redisLeakyBucket.enqueueRequest(2);
        
        assertNotNull(future1);
        assertNotNull(future2);
        
        // Should complete successfully
        assertTrue(future1.get(5, TimeUnit.SECONDS));
        assertTrue(future2.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testClearQueue() {
        // Add some requests
        redisLeakyBucket.tryConsume(5);
        assertEquals(5, redisLeakyBucket.getQueueSize());
        
        // Clear queue
        redisLeakyBucket.clearQueue();
        
        // Queue should be empty
        assertEquals(0, redisLeakyBucket.getQueueSize());
        assertEquals(10, redisLeakyBucket.getCurrentTokens());
    }

    @Test
    void testInvalidInputs() {
        assertFalse(redisLeakyBucket.tryConsume(0));
        assertFalse(redisLeakyBucket.tryConsume(-1));
        
        long zeroWaitTime = redisLeakyBucket.getEstimatedWaitTime(0);
        assertTrue(zeroWaitTime >= 0, "Zero token request should have non-negative wait time");
        
        long negativeWaitTime = redisLeakyBucket.getEstimatedWaitTime(-1);
        assertTrue(negativeWaitTime >= -1, "Negative token request should return -1 or 0");
    }

    @Test
    void testRedisStateConsistency() throws InterruptedException {
        // Test that multiple operations maintain consistent state
        
        // Add requests
        assertTrue(redisLeakyBucket.tryConsume(3));
        assertEquals(3, redisLeakyBucket.getQueueSize());
        
        // Create another instance with same key - should see same state
        RedisLeakyBucket anotherInstance = new RedisLeakyBucket(testKey, 10, 2.0, 5000, redisTemplate);
        assertEquals(3, anotherInstance.getQueueSize());
        assertEquals(7, anotherInstance.getCurrentTokens());
        
        // Add more through second instance
        assertTrue(anotherInstance.tryConsume(2));
        
        // First instance should see the change
        assertEquals(5, redisLeakyBucket.getQueueSize());
        assertEquals(5, redisLeakyBucket.getCurrentTokens());
    }

    @Test
    void testDistributedBehavior() {
        // Test distributed behavior with multiple bucket instances
        RedisLeakyBucket bucket1 = new RedisLeakyBucket("shared:key", 5, 1.0, redisTemplate);
        RedisLeakyBucket bucket2 = new RedisLeakyBucket("shared:key", 5, 1.0, redisTemplate);
        
        try {
            // Both should see same initial state
            assertEquals(0, bucket1.getQueueSize());
            assertEquals(0, bucket2.getQueueSize());
            
            // Add through first bucket
            assertTrue(bucket1.tryConsume(2));
            
            // Second bucket should see the change
            assertEquals(2, bucket2.getQueueSize());
            assertEquals(3, bucket2.getCurrentTokens());
            
            // Add through second bucket
            assertTrue(bucket2.tryConsume(1));
            
            // First bucket should see total state
            assertEquals(3, bucket1.getQueueSize());
            assertEquals(2, bucket1.getCurrentTokens());
            
            // Try to exceed capacity from either bucket
            assertFalse(bucket1.tryConsume(3)); // 3 + 3 > 5
            assertFalse(bucket2.tryConsume(3)); // 3 + 3 > 5
            
        } finally {
            bucket1.clearQueue();
        }
    }

    @Test
    void testLastRefillTime() {
        long before = System.currentTimeMillis();
        
        // Make a request to initialize state
        redisLeakyBucket.tryConsume(1);
        
        long after = System.currentTimeMillis();
        long lastRefillTime = redisLeakyBucket.getLastRefillTime();
        
        assertTrue(lastRefillTime >= before && lastRefillTime <= after,
            "Last refill time should be within test execution window");
    }
}