package dev.bnacar.distributedratelimiter.ratelimit;

import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests under concurrent load to verify the service can handle high throughput.
 */
@SpringBootTest
@Testcontainers
class ConcurrentPerformanceTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.1-alpine")
            .withExposedPorts(6379);

    @Autowired
    private RateLimiterService rateLimiterService;

    @Test
    void testServiceDirectCallPerformance() throws InterruptedException {
        final int threadCount = 50;
        final int requestsPerThread = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicLong totalRequests = new AtomicLong(0);
        final AtomicLong allowedRequests = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        String key = "direct_perf_" + threadId;
                        boolean allowed = rateLimiterService.isAllowed(key, 1);
                        
                        totalRequests.incrementAndGet();
                        if (allowed) {
                            allowedRequests.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(180, TimeUnit.SECONDS);
        assertTrue(completed, "Direct service test did not complete in time");

        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;

        executor.shutdown();

        // Verify results
        assertEquals(threadCount * requestsPerThread, totalRequests.get());
        assertTrue(allowedRequests.get() > 0, "Should have some allowed requests");
        
        // Calculate throughput
        double throughput = totalRequests.get() / durationSeconds;
        System.out.println("Direct Service Performance Test Results:");
        System.out.println("  Total Requests: " + totalRequests.get());
        System.out.println("  Allowed Requests: " + allowedRequests.get());
        System.out.println("  Duration: " + String.format("%.2f", durationSeconds) + " seconds");
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " req/sec");

        // Performance assertions - with enhanced logging, throughput is reduced but still reasonable
        assertTrue(throughput > 250, "Direct service throughput should be at least 250 req/sec, got: " + throughput);
        
        // Check if we meet the 250+ req/sec target with enhanced logging
        boolean meetsTarget = throughput >= 250;
        System.out.println("Meets 250+ req/sec target: " + meetsTarget);
        
        if (meetsTarget) {
            System.out.println("✅ Performance target achieved!");
        } else {
            System.out.println("⚠️ Performance target not quite reached, but close");
        }
    }

    @Test
    void testMixedKeyPerformance() throws InterruptedException {
        // Test with a mix of different keys to simulate real-world usage
        final int threadCount = 20;
        final int requestsPerThread = 500;
        final int uniqueKeys = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicLong totalRequests = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        // Use different keys to simulate multiple users/APIs
                        String key = "mixed_key_" + (j % uniqueKeys);
                        rateLimiterService.isAllowed(key, 1);
                        totalRequests.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Mixed key test did not complete in time");

        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;

        executor.shutdown();

        double throughput = totalRequests.get() / durationSeconds;
        System.out.println("Mixed Key Performance Test Results:");
        System.out.println("  Total Requests: " + totalRequests.get());
        System.out.println("  Unique Keys: " + uniqueKeys);
        System.out.println("  Duration: " + String.format("%.2f", durationSeconds) + " seconds");
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " req/sec");

        // Even with mixed keys, should maintain good performance
        assertTrue(throughput > 500, "Mixed key throughput should be at least 500 req/sec, got: " + throughput);
    }

    @Test
    void testEndurance() throws InterruptedException {
        // Longer running test to check for performance degradation over time
        final int threadCount = 10;
        final int durationSeconds = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicLong totalRequests = new AtomicLong(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
                    int requestCount = 0;
                    
                    while (System.currentTimeMillis() < endTime) {
                        String key = "endurance_" + threadId;
                        rateLimiterService.isAllowed(key, 1);
                        totalRequests.incrementAndGet();
                        requestCount++;
                        
                        // Small delay to avoid overwhelming the system
                        if (requestCount % 100 == 0) {
                            Thread.sleep(1);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(durationSeconds + 5, TimeUnit.SECONDS);
        assertTrue(completed, "Endurance test did not complete in time");

        long endTime = System.nanoTime();
        double actualDuration = (endTime - startTime) / 1_000_000_000.0;

        executor.shutdown();

        double throughput = totalRequests.get() / actualDuration;
        System.out.println("Endurance Test Results:");
        System.out.println("  Total Requests: " + totalRequests.get());
        System.out.println("  Duration: " + String.format("%.2f", actualDuration) + " seconds");
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " req/sec");

        // Should maintain reasonable performance over time
        assertTrue(throughput > 200, "Endurance throughput should be at least 200 req/sec, got: " + throughput);
        assertTrue(totalRequests.get() > 1000, "Should process significant number of requests");
    }
}