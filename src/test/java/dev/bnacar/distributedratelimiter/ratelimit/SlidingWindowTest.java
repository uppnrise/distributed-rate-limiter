package dev.bnacar.distributedratelimiter.ratelimit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class SlidingWindowTest {

    private SlidingWindow slidingWindow;

    @BeforeEach
    void setUp() {
        slidingWindow = new SlidingWindow(10, 2);
    }

    @Test
    void test_zeroTokenConsumeRequest() {
        assertFalse(slidingWindow.tryConsume(0));
    }

    @Test
    void test_negativeTokenConsumeRequest() {
        assertFalse(slidingWindow.tryConsume(-1));
    }

    @Test
    void test_shouldAllowConsumingTokensWhenWindowIsEmpty() {
        assertTrue(slidingWindow.tryConsume(5));
        assertTrue(slidingWindow.tryConsume(3));
        assertEquals(2, slidingWindow.getCurrentTokens()); // 10 - 8 = 2 remaining
    }

    @Test
    void test_shouldFailConsumingTokensWhenCapacityExceeded() {
        assertTrue(slidingWindow.tryConsume(10)); // Fill capacity
        assertFalse(slidingWindow.tryConsume(1)); // Should fail
        assertEquals(0, slidingWindow.getCurrentTokens());
    }

    @Test
    void test_shouldFailConsumingMoreThanCapacity() {
        assertFalse(slidingWindow.tryConsume(11)); // More than capacity
        assertEquals(10, slidingWindow.getCurrentTokens()); // Nothing consumed
    }

    @Test
    void test_shouldAllowTokensAfterWindowSlides() {
        // Fill the window
        assertTrue(slidingWindow.tryConsume(10));
        assertFalse(slidingWindow.tryConsume(1));

        // Wait for window to slide (1+ seconds)
        Awaitility.await()
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .until(() -> slidingWindow.tryConsume(1));

        // Should be able to consume again
        assertTrue(slidingWindow.getCurrentTokens() > 0);
    }

    @Test
    void test_windowSlidingBehavior() throws InterruptedException {
        // Consume tokens at start
        assertTrue(slidingWindow.tryConsume(5));
        assertEquals(5, slidingWindow.getCurrentTokens());

        // Wait 600ms
        Thread.sleep(600);
        
        // Consume more tokens
        assertTrue(slidingWindow.tryConsume(4));
        assertEquals(1, slidingWindow.getCurrentTokens());

        // Wait another 500ms (total 1100ms from start)
        Thread.sleep(500);
        
        // First batch should have expired, second batch still active
        assertEquals(6, slidingWindow.getCurrentTokens()); // 10 - 4 = 6
        
        // Should be able to consume more
        assertTrue(slidingWindow.tryConsume(6));
        assertEquals(0, slidingWindow.getCurrentTokens());
    }

    @Test
    void test_threadSafetyOfTokenConsumption() throws InterruptedException {
        int threads = 20;
        int tokensPerThread = 1;
        
        Runnable consumeTask = () -> slidingWindow.tryConsume(tokensPerThread);

        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            threadArray[i] = new Thread(consumeTask);
            threadArray[i].start();
        }
        for (Thread t : threadArray) {
            t.join();
        }

        // At most 10 tokens should be consumed (capacity limit)
        assertTrue(slidingWindow.getCurrentUsage() <= 10);
        assertTrue(slidingWindow.getCurrentTokens() >= 0);
    }

    @Test
    void test_getCurrentUsage() {
        assertEquals(0, slidingWindow.getCurrentUsage());
        
        slidingWindow.tryConsume(3);
        assertEquals(3, slidingWindow.getCurrentUsage());
        
        slidingWindow.tryConsume(2);
        assertEquals(5, slidingWindow.getCurrentUsage());
    }

    @Test
    void test_getCapacity() {
        assertEquals(10, slidingWindow.getCapacity());
    }

    @Test
    void test_getRefillRate() {
        assertEquals(2, slidingWindow.getRefillRate());
    }

    @Test
    void test_getWindowSizeMs() {
        assertEquals(1000, slidingWindow.getWindowSizeMs());
    }

    @Test
    void test_getLastRefillTime() {
        long before = System.currentTimeMillis();
        long refillTime = slidingWindow.getLastRefillTime();
        long after = System.currentTimeMillis();
        
        assertTrue(refillTime >= before && refillTime <= after);
    }

    @Test
    void test_burstHandlingBehavior() {
        // Sliding window should handle bursts differently than token bucket
        // It should allow full capacity immediately if window is empty
        
        // First burst - should allow full capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(slidingWindow.tryConsume(1), "Token " + (i + 1) + " should be allowed");
        }
        
        // 11th token should fail
        assertFalse(slidingWindow.tryConsume(1));
        
        assertEquals(0, slidingWindow.getCurrentTokens());
        assertEquals(10, slidingWindow.getCurrentUsage());
    }

    @Test
    void test_consistentRateEnforcement() throws InterruptedException {
        // Test that sliding window enforces rate more consistently than token bucket
        
        // Fill capacity
        assertTrue(slidingWindow.tryConsume(10));
        
        // Wait 500ms (half window)
        Thread.sleep(500);
        
        // Should still be blocked as window hasn't fully slid
        assertFalse(slidingWindow.tryConsume(1));
        
        // Wait another 600ms (total 1100ms, past window boundary)
        Thread.sleep(600);
        
        // Should now allow tokens as window has slid
        assertTrue(slidingWindow.tryConsume(1));
    }
}