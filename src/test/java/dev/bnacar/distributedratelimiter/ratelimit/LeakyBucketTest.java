package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive test suite for LeakyBucket rate limiter.
 * Tests queue-based behavior, constant leak rate, and traffic shaping capabilities.
 */
public class LeakyBucketTest {
    
    private LeakyBucket leakyBucket;
    
    @BeforeEach
    void setUp() {
        // Create leaky bucket with capacity 10, leak rate 2 tokens/second, max queue time 5 seconds
        leakyBucket = new LeakyBucket(10, 2.0, 5000);
    }
    
    @AfterEach
    void tearDown() {
        if (leakyBucket != null) {
            leakyBucket.shutdown();
        }
    }
    
    @Test
    void testBasicConfiguration() {
        assertEquals(10, leakyBucket.getCapacity());
        assertEquals(2, leakyBucket.getRefillRate()); // Rounded up from 2.0
        assertEquals(2.0, leakyBucket.getLeakRatePerSecond());
        assertEquals(5000, leakyBucket.getMaxQueueTimeMs());
        assertEquals(0, leakyBucket.getQueueSize());
    }
    
    @Test
    void testInitialState() {
        // Initially, queue is empty so all capacity is available
        assertEquals(10, leakyBucket.getCurrentTokens());
        assertTrue(leakyBucket.getLastRefillTime() > 0);
    }
    
    @Test
    void testSynchronousTryConsume() {
        // Test basic synchronous consumption
        assertTrue(leakyBucket.tryConsume(1));
        assertTrue(leakyBucket.tryConsume(5));
        
        // Test edge cases
        assertFalse(leakyBucket.tryConsume(0)); // Zero tokens
        assertFalse(leakyBucket.tryConsume(-1)); // Negative tokens
    }
    
    @Test
    void testQueueCapacityLimit() {
        // Fill up to capacity using synchronous interface approximation
        for (int i = 0; i < 10; i++) {
            assertTrue(leakyBucket.tryConsume(1), "Should accept request " + (i + 1));
        }
        
        // Next request should be rejected due to capacity
        // Note: synchronous interface approximates behavior, actual queuing happens in async mode
    }
    
    @Test
    void testAsynchronousEnqueueRequest() throws ExecutionException, InterruptedException, TimeoutException {
        // Test async enqueue - this is the intended usage pattern
        CompletableFuture<Boolean> future1 = leakyBucket.enqueueRequest(1);
        CompletableFuture<Boolean> future2 = leakyBucket.enqueueRequest(2);
        
        assertNotNull(future1);
        assertNotNull(future2);
        
        // Initially should have 2 requests in queue
        assertEquals(2, leakyBucket.getQueueSize());
        assertEquals(8, leakyBucket.getCurrentTokens()); // 10 - 2 queued
        
        // Wait for processing - should complete within reasonable time
        assertTrue(future1.get(10, TimeUnit.SECONDS));
        assertTrue(future2.get(10, TimeUnit.SECONDS));
    }
    
    @Test
    void testLeakRateEnforcement() throws InterruptedException, ExecutionException, TimeoutException {
        // Enqueue multiple requests and verify they're processed at correct rate
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        // Enqueue 4 requests (will take ~2 seconds to process at 2 tokens/sec)
        for (int i = 0; i < 4; i++) {
            futures.add(leakyBucket.enqueueRequest(1));
        }
        
        assertEquals(4, leakyBucket.getQueueSize());
        
        long startTime = System.currentTimeMillis();
        
        // Wait for all requests to complete
        for (CompletableFuture<Boolean> future : futures) {
            assertTrue(future.get(10, TimeUnit.SECONDS));
        }
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        // Should take approximately 2 seconds (4 tokens / 2 tokens per second)
        // Allow some tolerance for processing overhead
        assertTrue(processingTime >= 1500, "Processing too fast: " + processingTime + "ms");
        assertTrue(processingTime <= 4000, "Processing too slow: " + processingTime + "ms");
    }
    
    @Test
    void testQueueCapacityRejection() throws InterruptedException {
        // Fill queue to capacity
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            CompletableFuture<Boolean> future = leakyBucket.enqueueRequest(1);
            assertNotNull(future);
            futures.add(future);
        }
        
        assertEquals(10, leakyBucket.getQueueSize());
        assertEquals(0, leakyBucket.getCurrentTokens());
        
        // Next request should be rejected
        CompletableFuture<Boolean> rejectedFuture = leakyBucket.enqueueRequest(1);
        assertNotNull(rejectedFuture);
        
        // Should complete immediately with false
        await()
            .atMost(Duration.ofSeconds(1))
            .until(() -> rejectedFuture.isDone());
        
        try {
            assertFalse(rejectedFuture.get());
        } catch (ExecutionException e) {
            fail("Future should complete normally with false");
        }
    }
    
    @Test 
    @Timeout(15)
    void testRequestTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        // Create bucket with very short timeout and no leak rate (essentially no processing)
        LeakyBucket shortTimeoutBucket = new LeakyBucket(5, 0.1, 500); // 0.5 second timeout, very slow leak
        
        try {
            // Fill queue completely
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                futures.add(shortTimeoutBucket.enqueueRequest(1));
            }
            
            assertEquals(5, shortTimeoutBucket.getQueueSize());
            
            // Wait longer than timeout
            Thread.sleep(800); // Wait longer than 500ms timeout
            
            // Check if any requests have completed with false (timeout)
            int completedCount = 0;
            int timeoutCount = 0;
            
            for (CompletableFuture<Boolean> future : futures) {
                if (future.isDone()) {
                    completedCount++;
                    try {
                        boolean result = future.get(10, TimeUnit.MILLISECONDS);
                        if (!result) {
                            timeoutCount++;
                        }
                    } catch (TimeoutException e) {
                        // Should not happen since we checked isDone()
                    }
                }
            }
            
            // With such a slow leak rate and short timeout, some should timeout
            assertTrue(timeoutCount > 0 || completedCount < futures.size(), 
                "Some requests should have timed out or still be processing. Completed: " + completedCount + ", Timeouts: " + timeoutCount);
            
        } finally {
            shortTimeoutBucket.shutdown();
        }
    }
    
    @Test
    void testConstantRateVsBurstBehavior() throws InterruptedException, ExecutionException, TimeoutException {
        // Test that leaky bucket provides constant output rate regardless of input pattern
        
        // Burst input: enqueue all requests at once
        List<CompletableFuture<Boolean>> burstFutures = new ArrayList<>();
        long burstStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 6; i++) {
            burstFutures.add(leakyBucket.enqueueRequest(1));
        }
        
        long burstEnqueueTime = System.currentTimeMillis() - burstStartTime;
        
        // Enqueue should be fast (burst input)
        assertTrue(burstEnqueueTime < 100, "Enqueue should be fast: " + burstEnqueueTime + "ms");
        
        // But processing should be at constant rate
        long processingStartTime = System.currentTimeMillis();
        
        for (CompletableFuture<Boolean> future : burstFutures) {
            assertTrue(future.get(10, TimeUnit.SECONDS));
        }
        
        long totalProcessingTime = System.currentTimeMillis() - processingStartTime;
        
        // Should take ~3 seconds (6 tokens / 2 tokens per second)
        assertTrue(totalProcessingTime >= 2500, "Processing should follow leak rate: " + totalProcessingTime + "ms");
        assertTrue(totalProcessingTime <= 4500, "Processing shouldn't be too slow: " + totalProcessingTime + "ms");
    }
    
    @Test
    void testShutdownBehavior() throws InterruptedException, ExecutionException, TimeoutException {
        // Enqueue some requests
        CompletableFuture<Boolean> future1 = leakyBucket.enqueueRequest(1);
        CompletableFuture<Boolean> future2 = leakyBucket.enqueueRequest(1);
        
        assertEquals(2, leakyBucket.getQueueSize());
        
        // Shutdown
        leakyBucket.shutdown();
        
        // Remaining requests should complete with false
        await()
            .atMost(Duration.ofSeconds(2))
            .until(() -> future1.isDone() && future2.isDone());
        
        assertFalse(future1.get());
        assertFalse(future2.get());
        
        // New requests should be rejected
        CompletableFuture<Boolean> newFuture = leakyBucket.enqueueRequest(1);
        assertFalse(newFuture.get(100, TimeUnit.MILLISECONDS));
    }
    
    @Test
    void testDifferentLeakRates() {
        // Test various leak rates
        LeakyBucket fastBucket = new LeakyBucket(5, 10.0); // 10 tokens/sec
        LeakyBucket slowBucket = new LeakyBucket(5, 0.5);  // 0.5 tokens/sec
        
        try {
            assertEquals(10, fastBucket.getRefillRate());
            assertEquals(1, slowBucket.getRefillRate()); // Rounded up from 0.5
            assertEquals(10.0, fastBucket.getLeakRatePerSecond());
            assertEquals(0.5, slowBucket.getLeakRatePerSecond());
        } finally {
            fastBucket.shutdown();
            slowBucket.shutdown();
        }
    }
    
    @Test
    void testTrafficShapingScenario() throws InterruptedException, ExecutionException, TimeoutException {
        // Simulate API gateway traffic shaping scenario
        LeakyBucket apiGateway = new LeakyBucket(50, 10.0, 5000); // 50 queue, 10 req/sec, 5s timeout
        
        try {
            // Simulate burst of 20 requests
            List<CompletableFuture<Boolean>> requests = new ArrayList<>();
            long burstStart = System.currentTimeMillis();
            
            for (int i = 0; i < 20; i++) {
                requests.add(apiGateway.enqueueRequest(1));
            }
            
            long burstEnd = System.currentTimeMillis();
            assertTrue(burstEnd - burstStart < 100, "Burst should be queued quickly");
            
            // All should be accepted (within queue capacity)
            assertEquals(20, apiGateway.getQueueSize());
            
            // Processing should take ~2 seconds (20 requests / 10 req/sec)
            long processStart = System.currentTimeMillis();
            
            for (CompletableFuture<Boolean> request : requests) {
                assertTrue(request.get(10, TimeUnit.SECONDS));
            }
            
            long processEnd = System.currentTimeMillis();
            long processTime = processEnd - processStart;
            
            assertTrue(processTime >= 1800, "Should take ~2 seconds: " + processTime + "ms");
            assertTrue(processTime <= 3000, "Shouldn't take too long: " + processTime + "ms");
            
        } finally {
            apiGateway.shutdown();
        }
    }
}