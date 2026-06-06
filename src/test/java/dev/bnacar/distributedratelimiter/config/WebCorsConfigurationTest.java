package dev.bnacar.distributedratelimiter.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WebCorsConfigurationTest {

    @Test
    public void testCorsConfigurationUsesConfiguredProperties() {
        CorsConfigurationProperties properties = new CorsConfigurationProperties();
        properties.setAllowedOrigins(List.of("https://app.example.com"));
        properties.setAllowedOriginPatterns(List.of("https://*.example.org"));
        properties.setAllowedMethods(List.of("GET", "POST"));
        properties.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        properties.setExposedHeaders(List.of("X-RateLimit-Remaining"));
        properties.setAllowCredentials(false);
        properties.setMaxAge(120L);

        WebCorsConfiguration webCorsConfiguration = new WebCorsConfiguration(properties);
        CorsConfigurationSource source = webCorsConfiguration.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/ratelimit/check");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertEquals(List.of("https://app.example.com"), configuration.getAllowedOrigins());
        assertEquals(List.of("https://*.example.org"), configuration.getAllowedOriginPatterns());
        assertEquals(List.of("GET", "POST"), configuration.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), configuration.getAllowedHeaders());
        assertEquals(List.of("X-RateLimit-Remaining"), configuration.getExposedHeaders());
        assertEquals(Boolean.FALSE, configuration.getAllowCredentials());
        assertEquals(120L, configuration.getMaxAge());
    }

    @Test
    public void testCorsConfigurationDefaultsSupportLocalDevelopment() {
        WebCorsConfiguration webCorsConfiguration = new WebCorsConfiguration(new CorsConfigurationProperties());
        CorsConfigurationSource source = webCorsConfiguration.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/ratelimit/check");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertTrue(configuration.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(configuration.getAllowedOrigins().contains("http://[::1]:3000"));
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }
}
