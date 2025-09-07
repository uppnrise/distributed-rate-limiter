package dev.bnacar.distributedratelimiter.ratelimit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // Default configuration: 10 tokens capacity, 2 tokens per second refill rate
        rateLimiterService = new RateLimiterService(10, 2);
    }

    @Test
    void test_shouldCreateNewBucketOnFirstAccess() {
        String key = "user123";
        
        // First access should create a new bucket and allow tokens
        assertTrue(rateLimiterService.isAllowed(key, 5));
    }

    @Test
    void test_shouldRateLimitDifferentKeysIndependently() {
        String key1 = "user1";
        String key2 = "user2";
        
        // Consume all tokens for key1
        assertTrue(rateLimiterService.isAllowed(key1, 10));
        assertFalse(rateLimiterService.isAllowed(key1, 1));
        
        // key2 should still have all tokens available
        assertTrue(rateLimiterService.isAllowed(key2, 10));
        assertFalse(rateLimiterService.isAllowed(key2, 1));
    }

    @Test
    void test_shouldHandleHighConcurrencyWithMultipleKeys() throws InterruptedException {
        int threadCount = 20;
        int requestsPerThread = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String key = "user" + i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiterService.isAllowed(key, 1)) {
                            successfulRequests.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
            thread.start();
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));

        // Each key should allow exactly 10 requests (bucket capacity)
        assertEquals(threadCount * 10, successfulRequests.get());
    }

    @Test
    void test_shouldCleanupExpiredBuckets() throws InterruptedException {
        // Use a service with shorter cleanup interval for testing
        RateLimiterService serviceWithCleanup = new RateLimiterService(10, 2, 100); // 100ms cleanup interval
        
        String key = "temporaryUser";
        
        // Access the bucket to create it
        serviceWithCleanup.isAllowed(key, 1);
        
        // Verify bucket exists by checking internal state
        assertEquals(1, serviceWithCleanup.getBucketCount());
        
        // Wait for cleanup to happen (buckets older than the cleanup interval should be removed)
        Awaitility.await()
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .atMost(500, TimeUnit.MILLISECONDS)
                .until(() -> serviceWithCleanup.getBucketCount() == 0);
    }

    @Test
    void test_shouldHandleConcurrentAccessToSameKey() throws InterruptedException {
        String key = "sharedUser";
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulRequests = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    if (rateLimiterService.isAllowed(key, 1)) {
                        successfulRequests.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
            thread.start();
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS));

        // Should allow exactly 10 requests (bucket capacity) despite concurrent access
        assertEquals(10, successfulRequests.get());
    }

    @Test
    void test_shouldRejectRequestsWhenTokensNotAvailable() {
        String key = "user123";
        
        // Consume all tokens
        assertTrue(rateLimiterService.isAllowed(key, 10));
        
        // Next request should be rejected
        assertFalse(rateLimiterService.isAllowed(key, 1));
    }

    @Test
    void test_shouldHandleZeroTokenRequest() {
        String key = "user123";
        
        // Zero token requests should be rejected
        assertFalse(rateLimiterService.isAllowed(key, 0));
    }
}