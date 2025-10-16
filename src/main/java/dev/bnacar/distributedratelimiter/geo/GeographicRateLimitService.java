package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.GeographicRateLimitResponse;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Main service for geographic rate limiting.
 * Orchestrates geographic location detection, rule resolution, and rate limit enforcement.
 */
@Service
public class GeographicRateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(GeographicRateLimitService.class);

    private final GeoLocationService geoLocationService;
    private final GeographicConfigurationResolver geographicConfigResolver;
    private final RateLimiterService rateLimiterService;
    private final ConfigurationResolver standardConfigResolver;
    private final GeographicAwareConfigurationResolver geoAwareConfigResolver;

    @Autowired
    public GeographicRateLimitService(GeoLocationService geoLocationService,
                                    GeographicConfigurationResolver geographicConfigResolver,
                                    RateLimiterService rateLimiterService,
                                    ConfigurationResolver standardConfigResolver,
                                    GeographicAwareConfigurationResolver geoAwareConfigResolver) {
        this.geoLocationService = geoLocationService;
        this.geographicConfigResolver = geographicConfigResolver;
        this.rateLimiterService = rateLimiterService;
        this.standardConfigResolver = standardConfigResolver;
        this.geoAwareConfigResolver = geoAwareConfigResolver;
    }

    /**
     * Check rate limit with geographic awareness.
     * 
     * @param key Rate limiting key
     * @param sourceIP Client IP address
     * @param tokens Number of tokens requested
     * @param headers HTTP headers for geographic detection
     * @return Geographic rate limit response with location and rule information
     */
    public GeographicRateLimitResponse checkGeographicRateLimit(String key, String sourceIP, 
                                                              int tokens, Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Detect geographic location
            GeoLocation geoLocation = geoLocationService.detectLocation(sourceIP, headers);
            
            if (geoLocation == null) {
                logger.debug("Could not detect geographic location for IP: {}, falling back to standard rate limiting", sourceIP);
                return createFallbackResponse(key, tokens, null, "NO_GEOGRAPHIC_LOCATION_DETECTED");
            }

            // 2. Resolve geographic configuration
            RateLimitConfig geographicConfig = geographicConfigResolver.resolveGeographicConfig(key, geoLocation);
            
            if (geographicConfig == null) {
                logger.debug("No geographic rules apply for key: {} location: {}, falling back to standard rate limiting", 
                           key, geoLocation.getCountryCode());
                return createFallbackResponse(key, tokens, geoLocation, "NO_GEOGRAPHIC_RULES_APPLY");
            }

            // 3. Apply geographic rate limiting using geographic-aware key
            String geographicKey = createGeographicKey(key, geoLocation);
            
            // Set geographic context for configuration resolution
            GeographicAwareConfigurationResolver.setGeographicContext(geoLocation);
            
            try {
                boolean allowed = rateLimiterService.isAllowed(geographicKey, tokens);
                
                String appliedRule = String.format("geo:%s:%s", 
                                                 geoLocation.getCountryCode(), 
                                                 geoLocation.getComplianceZone());
                
                long processingTime = System.currentTimeMillis() - startTime;
                logger.debug("Geographic rate limiting completed in {}ms for key: {} location: {} allowed: {}", 
                            processingTime, key, geoLocation.getCountryCode(), allowed);
                
                return new GeographicRateLimitResponse(key, tokens, allowed, geoLocation, appliedRule, geographicConfig);
            } finally {
                // Always clear geographic context
                GeographicAwareConfigurationResolver.clearGeographicContext();
            }
            
        } catch (Exception e) {
            logger.error("Error in geographic rate limiting for key: {} IP: {}", key, sourceIP, e);
            // Fallback to standard rate limiting on error
            return createFallbackResponse(key, tokens, null, "ERROR_IN_GEOGRAPHIC_PROCESSING");
        }
    }

    /**
     * Check rate limit using information from RateLimitRequest.
     */
    public GeographicRateLimitResponse checkGeographicRateLimit(RateLimitRequest request, 
                                                              String sourceIP, 
                                                              Map<String, String> headers) {
        // Extract geographic information from request if provided
        if (request.getClientInfo() != null) {
            // Use provided client info to override/supplement detection
            String requestSourceIP = request.getClientInfo().getSourceIP();
            if (requestSourceIP != null && !requestSourceIP.trim().isEmpty()) {
                sourceIP = requestSourceIP;
            }
            
            // Merge provided headers with request headers
            if (request.getClientInfo().getHeaders() != null) {
                headers = mergeHeaders(headers, request.getClientInfo().getHeaders());
            }
        }
        
        return checkGeographicRateLimit(request.getKey(), sourceIP, request.getTokens(), headers);
    }

    /**
     * Create a fallback response using standard rate limiting.
     */
    private GeographicRateLimitResponse createFallbackResponse(String key, int tokens, 
                                                             GeoLocation geoLocation, String fallbackReason) {
        // Use standard configuration resolver
        RateLimitConfig standardConfig = standardConfigResolver.resolveConfig(key);
        boolean allowed = rateLimiterService.isAllowed(key, tokens);
        
        return new GeographicRateLimitResponse(key, tokens, allowed, geoLocation, 
                                             "standard", standardConfig, fallbackReason);
    }

    /**
     * Create a geographic-specific rate limiting key.
     * This ensures that different geographic regions have separate rate limit buckets.
     */
    private String createGeographicKey(String originalKey, GeoLocation geoLocation) {
        return String.format("geo:%s:%s:%s", 
                           geoLocation.getCountryCode(),
                           geoLocation.getComplianceZone().name(),
                           originalKey);
    }

    /**
     * Merge two header maps, with priority given to the first map.
     */
    private Map<String, String> mergeHeaders(Map<String, String> primary, Map<String, String> secondary) {
        if (primary == null && secondary == null) {
            return null;
        }
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }
        
        java.util.Map<String, String> merged = new java.util.HashMap<>(secondary);
        merged.putAll(primary); // Primary headers override secondary
        return merged;
    }

    /**
     * Detect location only (for testing/debugging).
     */
    public GeoLocation detectLocation(String sourceIP, Map<String, String> headers) {
        return geoLocationService.detectLocation(sourceIP, headers);
    }

    /**
     * Check if geographic rate limiting is available for a given location.
     */
    public boolean hasGeographicRules(GeoLocation geoLocation) {
        return !geographicConfigResolver.getRulesForLocation(geoLocation).isEmpty();
    }

    /**
     * Get performance statistics for monitoring.
     */
    public Map<String, Object> getGeographicStats() {
        return Map.of(
            "geoLocationCache", geoLocationService.getCacheStats(),
            "geographicConfig", geographicConfigResolver.getCacheStats()
        );
    }
}