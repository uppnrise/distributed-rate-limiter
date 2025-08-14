package dev.bnacar.distributedratelimiter.ratelimit;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
