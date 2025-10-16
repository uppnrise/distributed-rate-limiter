package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationResponse model class.
 */
class ConfigurationResponseTest {

    @Test
    @DisplayName("Should create ConfigurationResponse with all properties")
    void testConfigurationResponseCreation() {
        // Given
        int capacity = 100;
        int refillRate = 10;
        long cleanupIntervalMs = 60000L;
        
        Map<String, RateLimiterConfiguration.KeyConfig> keyConfigs = new HashMap<>();
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(50);
        keyConfig.setRefillRate(5);
        keyConfig.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        keyConfigs.put("user1", keyConfig);
        
        Map<String, RateLimiterConfiguration.KeyConfig> patternConfigs = new HashMap<>();
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(200);
        patternConfig.setRefillRate(20);
        patternConfig.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
        patternConfigs.put("api:*", patternConfig);

        // When
        ConfigurationResponse response = new ConfigurationResponse(
            capacity, refillRate, cleanupIntervalMs, keyConfigs, patternConfigs
        );

        // Then
        assertEquals(capacity, response.getCapacity());
        assertEquals(refillRate, response.getRefillRate());
        assertEquals(cleanupIntervalMs, response.getCleanupIntervalMs());
        assertEquals(1, response.getKeyConfigs().size());
        assertEquals(1, response.getPatternConfigs().size());
        assertTrue(response.getKeyConfigs().containsKey("user1"));
        assertTrue(response.getPatternConfigs().containsKey("api:*"));
    }

    @Test
    @DisplayName("Should handle null config maps")
    void testNullConfigMaps() {
        // When
        ConfigurationResponse response = new ConfigurationResponse(
            100, 10, 60000L, null, null
        );

        // Then
        assertNotNull(response.getKeyConfigs());
        assertNotNull(response.getPatternConfigs());
        assertTrue(response.getKeyConfigs().isEmpty());
        assertTrue(response.getPatternConfigs().isEmpty());
    }

    @Test
    @DisplayName("Should return defensive copies of config maps")
    void testDefensiveCopies() {
        // Given
        Map<String, RateLimiterConfiguration.KeyConfig> keyConfigs = new HashMap<>();
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(50);
        keyConfig.setRefillRate(5);
        keyConfig.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        keyConfigs.put("user1", keyConfig);
        
        ConfigurationResponse response = new ConfigurationResponse(
            100, 10, 60000L, keyConfigs, new HashMap<>()
        );

        // When
        Map<String, RateLimiterConfiguration.KeyConfig> returnedConfigs = response.getKeyConfigs();
        RateLimiterConfiguration.KeyConfig newConfig = new RateLimiterConfiguration.KeyConfig();
        newConfig.setCapacity(75);
        newConfig.setRefillRate(7);
        newConfig.setAlgorithm(RateLimitAlgorithm.FIXED_WINDOW);
        returnedConfigs.put("user2", newConfig);

        // Then - original response should not be affected
        assertEquals(1, response.getKeyConfigs().size());
        assertFalse(response.getKeyConfigs().containsKey("user2"));
    }

    @Test
    @DisplayName("Should handle empty config maps")
    void testEmptyConfigMaps() {
        // Given
        Map<String, RateLimiterConfiguration.KeyConfig> emptyKeyConfigs = new HashMap<>();
        Map<String, RateLimiterConfiguration.KeyConfig> emptyPatternConfigs = new HashMap<>();

        // When
        ConfigurationResponse response = new ConfigurationResponse(
            100, 10, 60000L, emptyKeyConfigs, emptyPatternConfigs
        );

        // Then
        assertTrue(response.getKeyConfigs().isEmpty());
        assertTrue(response.getPatternConfigs().isEmpty());
    }
}