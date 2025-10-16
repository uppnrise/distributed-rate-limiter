package dev.bnacar.distributedratelimiter.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationStats model class.
 */
class ConfigurationStatsTest {

    @Test
    @DisplayName("Should create ConfigurationStats with all properties")
    void testConfigurationStatsCreation() {
        // Given
        int cacheSize = 1000;
        int bucketCount = 50;
        int keyConfigCount = 25;
        int patternConfigCount = 10;

        // When
        ConfigurationStats stats = new ConfigurationStats(
            cacheSize, bucketCount, keyConfigCount, patternConfigCount
        );

        // Then
        assertEquals(cacheSize, stats.getCacheSize());
        assertEquals(bucketCount, stats.getBucketCount());
        assertEquals(keyConfigCount, stats.getKeyConfigCount());
        assertEquals(patternConfigCount, stats.getPatternConfigCount());
    }

    @Test
    @DisplayName("Should handle zero values")
    void testZeroValues() {
        // Given
        ConfigurationStats stats = new ConfigurationStats(0, 0, 0, 0);

        // Then
        assertEquals(0, stats.getCacheSize());
        assertEquals(0, stats.getBucketCount());
        assertEquals(0, stats.getKeyConfigCount());
        assertEquals(0, stats.getPatternConfigCount());
    }

    @Test
    @DisplayName("Should handle large values")
    void testLargeValues() {
        // Given
        int cacheSize = Integer.MAX_VALUE;
        int bucketCount = 999999;
        int keyConfigCount = 500000;
        int patternConfigCount = 100000;

        // When
        ConfigurationStats stats = new ConfigurationStats(
            cacheSize, bucketCount, keyConfigCount, patternConfigCount
        );

        // Then
        assertEquals(cacheSize, stats.getCacheSize());
        assertEquals(bucketCount, stats.getBucketCount());
        assertEquals(keyConfigCount, stats.getKeyConfigCount());
        assertEquals(patternConfigCount, stats.getPatternConfigCount());
    }
}