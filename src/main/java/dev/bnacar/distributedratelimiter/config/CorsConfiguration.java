package dev.bnacar.distributedratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        // Allow the React development server
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite dev server default port
            "http://localhost:3000",  // Create React App default port
            "http://127.0.0.1:5173",  // IPv4 localhost
            "http://127.0.0.1:3000",  // IPv4 localhost
            "http://[::1]:5173",      // IPv6 localhost
            "http://[::1]:3000"       // IPv6 localhost
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}