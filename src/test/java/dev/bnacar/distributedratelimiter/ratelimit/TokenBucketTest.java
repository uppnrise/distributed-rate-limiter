package dev.bnacar.distributedratelimiter.ratelimit;


import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TokenBucketTest {

    private TokenBucket tokenBucket;

    @BeforeEach
    void setUp() {
        tokenBucket = new TokenBucket(10, 2);
    }

    @Test
    void test_shouldAllowConsumingTokensWhenBucketIsFull() {

        if (tokenBucket.getCurrentTokens() > 0) {
            int tokensToConsume = 5;
            assertTrue(tokenBucket.tryConsume(tokensToConsume));

            tokensToConsume = 3;
            assertTrue(tokenBucket.tryConsume(tokensToConsume));
        }

    }

    @Test
    void test_shouldFailConsumingTokensWhenBucketIsEmpty() {

        if( tokenBucket.getCurrentTokens() > 0) {
            int tokensToConsume = 10;
            assertTrue(tokenBucket.tryConsume(tokensToConsume));
        }

        assertFalse(tokenBucket.tryConsume(10));
    }

    @Test
    void test_shouldRefillTokensOverTime() {
        tokenBucket.tryConsume(10);

        // Simulate passage of time
        Awaitility.await()
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> tokenBucket.refill() > 0);

        assertTrue(tokenBucket.getCurrentTokens() > 0);
    }

    @Test
    void test_threadSafetyOfTokenConsumption() throws InterruptedException {
        int threads = 20;
        int tokensPerThread = 1;
        tokenBucket = new TokenBucket(10, 0); // No refill for this test

        Runnable consumeTask = () -> tokenBucket.tryConsume(tokensPerThread);

        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            threadArray[i] = new Thread(consumeTask);
            threadArray[i].start();
        }
        for (Thread t : threadArray) {
            t.join();
        }

        // At most 10 tokens should be consumed
        assertEquals(0, tokenBucket.getCurrentTokens());
    }

}
