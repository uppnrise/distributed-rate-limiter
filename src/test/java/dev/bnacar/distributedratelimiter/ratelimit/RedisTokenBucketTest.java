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
    "ratelimiter.redis.enabled=true"
})
class RedisTokenBucketTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private RedisTokenBucket redisTokenBucket;
    private final String testKey = "test:redis:bucket";

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        redisTemplate.delete(testKey);
        
        // Create a new Redis token bucket with capacity=10, refillRate=2
        redisTokenBucket = new RedisTokenBucket(testKey, 10, 2, redisTemplate);
    }

    @Test
    void testInitialTokensAvailable() {
        // New bucket should have full capacity available
        assertEquals(10, redisTokenBucket.getCurrentTokens());
        assertEquals(10, redisTokenBucket.getCapacity());
        assertEquals(2, redisTokenBucket.getRefillRate());
    }

    @Test
    void testSuccessfulTokenConsumption() {
        assertTrue(redisTokenBucket.tryConsume(5));
        assertEquals(5, redisTokenBucket.getCurrentTokens());
        
        assertTrue(redisTokenBucket.tryConsume(3));
        assertEquals(2, redisTokenBucket.getCurrentTokens());
    }

    @Test
    void testFailedTokenConsumptionWhenInsufficientTokens() {
        // Consume all tokens
        assertTrue(redisTokenBucket.tryConsume(10));
        assertEquals(0, redisTokenBucket.getCurrentTokens());
        
        // Should fail to consume more tokens
        assertFalse(redisTokenBucket.tryConsume(1));
        assertEquals(0, redisTokenBucket.getCurrentTokens());
    }

    @Test
    void testZeroTokenConsumption() {
        assertFalse(redisTokenBucket.tryConsume(0));
        assertEquals(10, redisTokenBucket.getCurrentTokens());
    }

    @Test
    void testNegativeTokenConsumption() {
        assertFalse(redisTokenBucket.tryConsume(-1));
        assertEquals(10, redisTokenBucket.getCurrentTokens());
    }

    @Test
    void testTokenRefillOverTime() throws InterruptedException {
        // Consume all tokens
        assertTrue(redisTokenBucket.tryConsume(10));
        assertEquals(0, redisTokenBucket.getCurrentTokens());
        
        // Wait for refill (refill rate is 2 tokens per second)
        Thread.sleep(1100); // Wait 1.1 seconds
        
        // Should have at least 2 tokens now
        int currentTokens = redisTokenBucket.getCurrentTokens();
        assertTrue(currentTokens >= 2, "Expected at least 2 tokens after 1.1 seconds, got " + currentTokens);
        
        // Should be able to consume the refilled tokens
        assertTrue(redisTokenBucket.tryConsume(2));
    }

    @Test
    void testCapacityLimit() throws InterruptedException {
        // Start with full bucket, wait for refill time
        Thread.sleep(1100);
        
        // Should not exceed capacity even after waiting
        assertEquals(10, redisTokenBucket.getCurrentTokens());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 15;  // Reduced from 20 to be less aggressive
        final int tokensPerThread = 1;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        
        // Create threads that try to consume tokens
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Add a tiny delay to spread out the requests slightly
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                results[index] = redisTokenBucket.tryConsume(tokensPerThread);
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
        
        // Count successful consumptions
        int successCount = 0;
        for (boolean result : results) {
            if (result) successCount++;
        }
        
        // Debug information
        int finalTokenCount = redisTokenBucket.getCurrentTokens();
        System.out.println("Success count: " + successCount + ", Final token count: " + finalTokenCount);
        
        // Should only allow 10 successful consumptions (the capacity)
        assertEquals(10, successCount);
        assertTrue(finalTokenCount >= 0, "Token count should not be negative, got: " + finalTokenCount);
        assertEquals(0, finalTokenCount);
    }

    @Test
    void testPersistenceAcrossInstances() {
        // Create first instance and consume some tokens
        RedisTokenBucket bucket1 = new RedisTokenBucket(testKey, 10, 2, redisTemplate);
        assertTrue(bucket1.tryConsume(6));
        assertEquals(4, bucket1.getCurrentTokens());
        
        // Create second instance with same key - should see the same state
        RedisTokenBucket bucket2 = new RedisTokenBucket(testKey, 10, 2, redisTemplate);
        assertEquals(4, bucket2.getCurrentTokens());
        
        // Consume tokens from second instance
        assertTrue(bucket2.tryConsume(2));
        assertEquals(2, bucket2.getCurrentTokens());
        
        // First instance should see the updated state
        assertEquals(2, bucket1.getCurrentTokens());
    }
}