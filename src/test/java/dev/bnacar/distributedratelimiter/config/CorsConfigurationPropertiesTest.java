package dev.bnacar.distributedratelimiter.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CorsConfigurationPropertiesTest {

    @Test
    public void testSettersHandleNullValues() {
        CorsConfigurationProperties properties = new CorsConfigurationProperties();

        properties.setAllowedOrigins(null);
        properties.setAllowedOriginPatterns(null);
        properties.setAllowedMethods(null);
        properties.setAllowedHeaders(null);
        properties.setExposedHeaders(null);

        assertTrue(properties.getAllowedOrigins().isEmpty());
        assertTrue(properties.getAllowedOriginPatterns().isEmpty());
        assertTrue(properties.getAllowedMethods().isEmpty());
        assertTrue(properties.getAllowedHeaders().isEmpty());
        assertTrue(properties.getExposedHeaders().isEmpty());
    }

    @Test
    public void testListPropertiesUseDefensiveCopies() {
        CorsConfigurationProperties properties = new CorsConfigurationProperties();
        List<String> mutableOrigins = new ArrayList<>(List.of("https://app.example.com"));

        properties.setAllowedOrigins(mutableOrigins);
        mutableOrigins.add("https://mutated.example.com");

        assertEquals(List.of("https://app.example.com"), properties.getAllowedOrigins());

        List<String> returnedOrigins = properties.getAllowedOrigins();
        returnedOrigins.add("https://another.example.com");

        assertEquals(List.of("https://app.example.com"), properties.getAllowedOrigins());
    }
}
