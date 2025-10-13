package dev.bnacar.distributedratelimiter.ratelimit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class FixedWindowTest {

    private FixedWindow fixedWindow;

    @BeforeEach
    void setUp() {
        // Create a fixed window with capacity 10, refill rate 2, and 1-second window for testing
        fixedWindow = new FixedWindow(10, 2, 1000);
    }

    @Test
    void test_zeroTokenConsumeRequest() {
        assertFalse(fixedWindow.tryConsume(0));
    }

    @Test
    void test_negativeTokenConsumeRequest() {
        assertFalse(fixedWindow.tryConsume(-1));
    }

    @Test
    void test_shouldAllowConsumingTokensWhenWindowIsEmpty() {
        assertTrue(fixedWindow.tryConsume(5));
        assertTrue(fixedWindow.tryConsume(3));
        assertEquals(2, fixedWindow.getCurrentTokens()); // 10 - 8 = 2 remaining
    }

    @Test
    void test_shouldFailConsumingTokensWhenCapacityExceeded() {
        assertTrue(fixedWindow.tryConsume(10)); // Fill capacity
        assertFalse(fixedWindow.tryConsume(1)); // Should fail
        assertEquals(0, fixedWindow.getCurrentTokens());
    }

    @Test
    void test_shouldFailConsumingMoreThanCapacity() {
        assertFalse(fixedWindow.tryConsume(11)); // More than capacity
        assertEquals(10, fixedWindow.getCurrentTokens()); // Nothing consumed
    }

    @Test
    void test_shouldAllowConsumingAfterWindowReset() throws InterruptedException {
        // Fill the window completely
        assertTrue(fixedWindow.tryConsume(10));
        assertEquals(0, fixedWindow.getCurrentTokens());
        assertFalse(fixedWindow.tryConsume(1));

        // Wait for window to reset (1 second + buffer)
        Thread.sleep(1100);

        // Should be able to consume again after window reset
        assertTrue(fixedWindow.tryConsume(5));
        assertEquals(5, fixedWindow.getCurrentTokens());
    }

    @Test
    void test_multipleRequestsInSameWindow() {
        // Multiple small requests should work until capacity is reached
        for (int i = 0; i < 10; i++) {
            assertTrue(fixedWindow.tryConsume(1), "Request " + i + " should succeed");
            assertEquals(9 - i, fixedWindow.getCurrentTokens());
        }
        
        // 11th request should fail
        assertFalse(fixedWindow.tryConsume(1));
        assertEquals(0, fixedWindow.getCurrentTokens());
    }

    @Test
    void test_windowResetResetsCounter() throws InterruptedException {
        // Consume some tokens
        assertTrue(fixedWindow.tryConsume(7));
        assertEquals(7, fixedWindow.getCurrentUsage()); // Used tokens
        assertEquals(3, fixedWindow.getCurrentTokens()); // Remaining tokens

        // Wait for window to reset
        Thread.sleep(1100);

        // Usage should be reset, but we need to trigger it with a check
        assertEquals(10, fixedWindow.getCurrentTokens()); // This triggers window reset
        assertEquals(0, fixedWindow.getCurrentUsage());
    }

    @Test
    void test_getters() {
        assertEquals(10, fixedWindow.getCapacity());
        assertEquals(2, fixedWindow.getRefillRate());
        assertEquals(1000, fixedWindow.getWindowDurationMs());
        assertTrue(fixedWindow.getLastRefillTime() > 0);
    }

    @Test
    void test_windowTimeRemaining() throws InterruptedException {
        long timeRemaining = fixedWindow.getWindowTimeRemaining();
        assertTrue(timeRemaining > 0 && timeRemaining <= 1000);
        
        // Wait a bit and check it decreases
        Thread.sleep(100);
        long newTimeRemaining = fixedWindow.getWindowTimeRemaining();
        assertTrue(newTimeRemaining < timeRemaining);
    }

    @Test
    void test_defaultConstructor() {
        FixedWindow defaultWindow = new FixedWindow(5, 1);
        assertEquals(5, defaultWindow.getCapacity());
        assertEquals(1, defaultWindow.getRefillRate());
        assertEquals(60000, defaultWindow.getWindowDurationMs()); // Default 1-minute window
    }

    @Test
    void test_concurrentAccess() throws InterruptedException {
        final int numThreads = 10;
        final Thread[] threads = new Thread[numThreads];
        final boolean[] results = new boolean[numThreads];

        // Create threads that each try to consume 1 token
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = fixedWindow.tryConsume(1);
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

        // Exactly 10 requests should succeed (capacity = 10)
        assertEquals(10, successCount);
        assertEquals(0, fixedWindow.getCurrentTokens());
    }

    @Test
    void test_windowBoundaryBehavior() {
        // Test that window resets work correctly at boundaries
        FixedWindow window = new FixedWindow(5, 1, 100); // 100ms window
        
        // Fill the window
        assertTrue(window.tryConsume(5));
        assertFalse(window.tryConsume(1));
        
        // Wait for window to reset using Awaitility for more reliable timing
        Awaitility.await()
            .atMost(200, TimeUnit.MILLISECONDS)
            .until(() -> window.getCurrentTokens() == 5);
            
        // Should be able to consume again
        assertTrue(window.tryConsume(3));
        assertEquals(2, window.getCurrentTokens());
    }
}