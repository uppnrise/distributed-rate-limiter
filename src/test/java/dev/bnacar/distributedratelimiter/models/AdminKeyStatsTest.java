package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AdminKeyStats model class.
 */
class AdminKeyStatsTest {

    @Test
    @DisplayName("Should create AdminKeyStats with all properties")
    void testAdminKeyStatsCreation() {
        // Given
        String key = "test-key";
        int capacity = 100;
        int refillRate = 10;
        long cleanupIntervalMs = 60000L;
        RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
        long lastAccessTime = System.currentTimeMillis();
        boolean isActive = true;

        // When
        AdminKeyStats stats = new AdminKeyStats(
            key, capacity, refillRate, cleanupIntervalMs, 
            algorithm, lastAccessTime, isActive
        );

        // Then
        assertEquals(key, stats.getKey());
        assertEquals(capacity, stats.getCapacity());
        assertEquals(refillRate, stats.getRefillRate());
        assertEquals(cleanupIntervalMs, stats.getCleanupIntervalMs());
        assertEquals(algorithm, stats.getAlgorithm());
        assertEquals(lastAccessTime, stats.getLastAccessTime());
        assertTrue(stats.isActive());
    }

    @Test
    @DisplayName("Should handle inactive key stats")
    void testInactiveKeyStats() {
        // Given
        AdminKeyStats stats = new AdminKeyStats(
            "inactive-key", 50, 5, 30000L, 
            RateLimitAlgorithm.SLIDING_WINDOW, 0L, false
        );

        // Then
        assertFalse(stats.isActive());
        assertEquals(0L, stats.getLastAccessTime());
        assertEquals("inactive-key", stats.getKey());
    }

    @Test
    @DisplayName("Should handle different algorithms")
    void testDifferentAlgorithms() {
        // Test all algorithm types
        for (RateLimitAlgorithm algorithm : RateLimitAlgorithm.values()) {
            AdminKeyStats stats = new AdminKeyStats(
                "key-" + algorithm.name(), 100, 10, 60000L,
                algorithm, System.currentTimeMillis(), true
            );
            
            assertEquals(algorithm, stats.getAlgorithm());
        }
    }
}