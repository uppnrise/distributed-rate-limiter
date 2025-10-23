package dev.bnacar.distributedratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration for the distributed rate limiter.
 * <p>
 * This configuration allows the web dashboard and other frontend applications
 * to communicate with the backend API from different origins.
 * </p>
 * 
 * <p>Security Note: In production, restrict allowed origins to specific domains
 * rather than using wildcards.</p>
 */
@Configuration
public class WebCorsConfiguration {

    /**
     * Allowed origins for CORS requests.
     * Includes localhost variations for development (IPv4, IPv6, domain name).
     */
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "http://localhost:3000",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:3000",
        "http://[::1]:5173",
        "http://[::1]:3000"
    );

    /**
     * Allowed HTTP methods for CORS requests.
     */
    private static final List<String> ALLOWED_METHODS = Arrays.asList(
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "OPTIONS",
        "PATCH"
    );

    /**
     * Allowed headers for CORS requests.
     */
    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "X-Api-Key"
    );

    /**
     * Headers exposed to the client.
     */
    private static final List<String> EXPOSED_HEADERS = Arrays.asList(
        "X-Correlation-ID",
        "X-Trace-ID",
        "X-Span-ID",
        "X-RateLimit-Limit",
        "X-RateLimit-Remaining",
        "X-RateLimit-Reset"
    );

    /**
     * Maximum age (in seconds) for caching preflight requests.
     */
    private static final Long MAX_AGE = 3600L;

    /**
     * Configures CORS settings for all endpoints.
     *
     * @return CorsConfigurationSource with global CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        
        // Set allowed methods
        configuration.setAllowedMethods(ALLOWED_METHODS);
        
        // Set allowed headers
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        
        // Set exposed headers
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Set max age for preflight cache
        configuration.setMaxAge(MAX_AGE);

        // Apply configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
