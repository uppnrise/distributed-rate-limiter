package dev.bnacar.distributedratelimiter.config;

import dev.bnacar.distributedratelimiter.geo.GeographicRateLimitService;
import dev.bnacar.distributedratelimiter.geo.GeographicAwareConfigurationResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Configuration class for geographic rate limiting features.
 * Provides conditional beans that are only created when geographic rate limiting is enabled.
 */
@Configuration
public class GeographicRateLimitingConfiguration {

    /**
     * Create GeographicRateLimitService bean only when geographic rate limiting is enabled.
     * This prevents Spring context issues when the feature is disabled.
     */
    @Bean
    @ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
    public GeographicRateLimitService geographicRateLimitService(
            dev.bnacar.distributedratelimiter.geo.GeoLocationService geoLocationService,
            dev.bnacar.distributedratelimiter.geo.GeographicConfigurationResolver geographicConfigResolver,
            dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService rateLimiterService,
            dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver standardConfigResolver,
            GeographicAwareConfigurationResolver geoAwareConfigResolver) {
        
        return new GeographicRateLimitService(
                geoLocationService,
                geographicConfigResolver,
                rateLimiterService,
                standardConfigResolver,
                geoAwareConfigResolver
        );
    }

    /**
     * Create GeographicAwareConfigurationResolver as primary configuration resolver 
     * when geographic rate limiting is enabled.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
    public GeographicAwareConfigurationResolver geographicAwareConfigurationResolver(
            dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration configuration,
            dev.bnacar.distributedratelimiter.geo.GeographicConfigurationResolver geographicConfigResolver) {
        
        return new GeographicAwareConfigurationResolver(configuration, geographicConfigResolver);
    }

    /**
     * Create a no-op GeographicRateLimitService when geographic rate limiting is disabled.
     * This prevents injection failures in controllers that depend on this service.
     */
    @Bean
    @ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "false", matchIfMissing = true)
    @ConditionalOnMissingBean(GeographicRateLimitService.class)
    public GeographicRateLimitService noOpGeographicRateLimitService() {
        return new NoOpGeographicRateLimitService();
    }

    /**
     * No-op implementation of GeographicRateLimitService for when geographic rate limiting is disabled.
     */
    static class NoOpGeographicRateLimitService extends GeographicRateLimitService {
        public NoOpGeographicRateLimitService() {
            super(null, null, null, null, null);
        }

        @Override
        public dev.bnacar.distributedratelimiter.models.GeographicRateLimitResponse checkGeographicRateLimit(
                String key, String sourceIP, int tokens, java.util.Map<String, String> headers) {
            // Return a fallback response indicating geographic rate limiting is disabled
            return new dev.bnacar.distributedratelimiter.models.GeographicRateLimitResponse(
                    key, tokens, true, null, "geographic-disabled", null, "GEOGRAPHIC_RATE_LIMITING_DISABLED");
        }

        @Override
        public dev.bnacar.distributedratelimiter.models.GeographicRateLimitResponse checkGeographicRateLimit(
                dev.bnacar.distributedratelimiter.models.RateLimitRequest request, String sourceIP, 
                java.util.Map<String, String> headers) {
            return checkGeographicRateLimit(request.getKey(), sourceIP, request.getTokens(), headers);
        }

        @Override
        public dev.bnacar.distributedratelimiter.models.GeoLocation detectLocation(String sourceIP, 
                java.util.Map<String, String> headers) {
            return null;
        }

        @Override
        public boolean hasGeographicRules(dev.bnacar.distributedratelimiter.models.GeoLocation geoLocation) {
            return false;
        }

        @Override
        public java.util.Map<String, Object> getGeographicStats() {
            return java.util.Map.of("enabled", false);
        }
    }
}