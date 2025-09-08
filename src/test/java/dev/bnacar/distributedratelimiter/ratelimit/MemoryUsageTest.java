package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Tests for memory usage optimization, particularly focusing on bucket cleanup.
 */
class MemoryUsageTest {

    private InMemoryRateLimiterBackend backend;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        // Use shorter cleanup interval for testing
        backend = new InMemoryRateLimiterBackend(1000); // 1 second cleanup
        executorService = Executors.newFixedThreadPool(20);
    }

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Test
    void testMemoryUsageUnderLoad() throws InterruptedException {
        final int threadCount = 50;
        final int operationsPerThread = 1000;
        final AtomicLong totalOperations = new AtomicLong(0);
        
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // Create high load with many different keys
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "load_test_" + threadId + "_" + j;
                        RateLimitConfig config = new RateLimitConfig(10, 5, 2000, RateLimitAlgorithm.TOKEN_BUCKET);
                        
                        RateLimiter limiter = backend.getRateLimiter(key, config);
                        limiter.tryConsume(1);
                        
                        totalOperations.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        
        // Verify all operations completed
        assertEquals(threadCount * operationsPerThread, totalOperations.get());
        
        // Check that we have created a lot of buckets
        int activeBuckets = backend.getActiveCount();
        assertTrue(activeBuckets > 0, "Should have active buckets after load test");
        
        // Memory should be manageable even with high load
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;
        
        // Should not use more than 80% of available memory
        assertTrue(memoryUsagePercentage < 80, 
            "Memory usage too high: " + memoryUsagePercentage + "%");
    }

    @Test
    void testBucketCleanupReducesMemoryUsage() throws InterruptedException {
        final int numBuckets = 1000;
        
        // Create many buckets
        for (int i = 0; i < numBuckets; i++) {
            String key = "cleanup_test_" + i;
            RateLimitConfig config = new RateLimitConfig(10, 5, 500, RateLimitAlgorithm.TOKEN_BUCKET);
            backend.getRateLimiter(key, config);
        }
        
        int initialBucketCount = backend.getActiveCount();
        assertEquals(numBuckets, initialBucketCount);
        
        // Wait for cleanup to occur (buckets should expire after 500ms + cleanup interval of 1000ms)
        await().atMost(3, TimeUnit.SECONDS)
               .until(() -> backend.getActiveCount() < initialBucketCount);
        
        int bucketsAfterCleanup = backend.getActiveCount();
        assertTrue(bucketsAfterCleanup < initialBucketCount, 
                  "Cleanup should have reduced bucket count from " + initialBucketCount + 
                  " to " + bucketsAfterCleanup);
        
        // Verify cleanup counter was incremented
        assertTrue(backend.getCleanupCount() > 0, "Cleanup counter should be incremented");
        assertTrue(backend.getLastCleanupTime() > 0, "Last cleanup time should be set");
    }

    @Test
    void testMemoryLeakPrevention() throws InterruptedException {
        // Simulate continuous creation and expiration of buckets
        AtomicLong keyCounter = new AtomicLong(0);
        
        // Run for a few seconds, creating buckets that will expire quickly
        long endTime = System.currentTimeMillis() + 5000; // Run for 5 seconds
        
        while (System.currentTimeMillis() < endTime) {
            String key = "leak_test_" + keyCounter.incrementAndGet();
            RateLimitConfig config = new RateLimitConfig(10, 5, 100, RateLimitAlgorithm.TOKEN_BUCKET); // Expire after 100ms
            backend.getRateLimiter(key, config);
            
            Thread.sleep(10); // Small delay
        }
        
        // Wait for cleanup cycles to process expired buckets
        Thread.sleep(3000);
        
        // Force a cleanup to ensure expired buckets are removed
        backend.forceCleanup();
        
        // Bucket count should be much lower than total created
        long totalCreated = keyCounter.get();
        int remainingBuckets = backend.getActiveCount();
        
        assertTrue(remainingBuckets < totalCreated * 0.1, 
                  "Too many buckets remaining: " + remainingBuckets + " out of " + totalCreated + " created");
    }

    @Test
    void testConcurrentAccessDuringCleanup() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 500;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicLong successCount = new AtomicLong(0);
        
        // Start concurrent operations while cleanup is happening
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "concurrent_" + threadId + "_" + (j % 100); // Reuse some keys
                        RateLimitConfig config = new RateLimitConfig(10, 5, 200, RateLimitAlgorithm.TOKEN_BUCKET);
                        
                        try {
                            RateLimiter limiter = backend.getRateLimiter(key, config);
                            limiter.tryConsume(1);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            // Should not throw exceptions during concurrent access
                            fail("Concurrent access failed: " + e.getMessage());
                        }
                        
                        if (j % 50 == 0) {
                            // Periodically force cleanup
                            backend.forceCleanup();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        
        // All operations should succeed
        assertEquals(threadCount * operationsPerThread, successCount.get());
    }
}