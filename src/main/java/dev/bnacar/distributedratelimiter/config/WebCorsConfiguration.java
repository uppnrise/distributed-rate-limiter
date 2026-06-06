package dev.bnacar.distributedratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
    private final CorsConfigurationProperties corsConfigurationProperties;

    public WebCorsConfiguration(CorsConfigurationProperties corsConfigurationProperties) {
        this.corsConfigurationProperties = corsConfigurationProperties;
    }

    /**
     * Configures CORS settings for all endpoints.
     *
     * @return CorsConfigurationSource with global CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(corsConfigurationProperties.getAllowedOrigins());
        configuration.setAllowedOriginPatterns(corsConfigurationProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(corsConfigurationProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsConfigurationProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsConfigurationProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsConfigurationProperties.isAllowCredentials());
        configuration.setMaxAge(corsConfigurationProperties.getMaxAge());

        // Apply configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
