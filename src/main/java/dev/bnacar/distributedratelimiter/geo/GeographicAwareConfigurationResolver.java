package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Geographic-aware configuration resolver that extends the standard ConfigurationResolver.
 * Handles geographic rate limit configurations with fallback to standard configurations.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
public class GeographicAwareConfigurationResolver extends ConfigurationResolver {
    private static final Logger logger = LoggerFactory.getLogger(GeographicAwareConfigurationResolver.class);

    private final GeographicConfigurationResolver geographicConfigResolver;
    
    // Thread-local storage for geographic context during rate limiting
    private static final ThreadLocal<GeoLocation> GEOGRAPHIC_CONTEXT = new ThreadLocal<>();
    
    // Cache for geographic-aware configurations
    private final ConcurrentHashMap<String, RateLimitConfig> geoConfigCache = new ConcurrentHashMap<>();

    @Autowired
    public GeographicAwareConfigurationResolver(RateLimiterConfiguration configuration,
                                              GeographicConfigurationResolver geographicConfigResolver) {
        super(configuration);
        this.geographicConfigResolver = geographicConfigResolver;
    }

    /**
     * Set the geographic context for the current thread.
     * This should be called before resolving configuration for geographic rate limiting.
     */
    public static void setGeographicContext(GeoLocation geoLocation) {
        GEOGRAPHIC_CONTEXT.set(geoLocation);
    }

    /**
     * Clear the geographic context for the current thread.
     */
    public static void clearGeographicContext() {
        GEOGRAPHIC_CONTEXT.remove();
    }

    /**
     * Get the current geographic context.
     */
    public static GeoLocation getGeographicContext() {
        return GEOGRAPHIC_CONTEXT.get();
    }

    /**
     * Resolve configuration with geographic awareness.
     * If geographic context is set, try to resolve geographic configuration first.
     */
    @Override
    public RateLimitConfig resolveConfig(String key) {
        GeoLocation geoLocation = GEOGRAPHIC_CONTEXT.get();
        
        if (geoLocation != null) {
            // Try geographic configuration first
            String cacheKey = createGeoCacheKey(key, geoLocation);
            RateLimitConfig cached = geoConfigCache.get(cacheKey);
            
            if (cached != null) {
                logger.debug("Geographic configuration found in cache for key: {} location: {}", 
                           key, geoLocation.getCountryCode());
                return cached;
            }
            
            RateLimitConfig geographicConfig = geographicConfigResolver.resolveGeographicConfig(key, geoLocation);
            if (geographicConfig != null) {
                logger.debug("Using geographic configuration for key: {} location: {} rule: {}", 
                           key, geoLocation.getCountryCode(), geographicConfig);
                
                // Cache the geographic configuration
                geoConfigCache.put(cacheKey, geographicConfig);
                return geographicConfig;
            }
        }
        
        // Fallback to standard configuration
        logger.debug("Using standard configuration for key: {}", key);
        return super.resolveConfig(key);
    }

    /**
     * Resolve configuration with explicit geographic context.
     */
    public RateLimitConfig resolveConfig(String key, GeoLocation geoLocation) {
        if (geoLocation == null) {
            return super.resolveConfig(key);
        }
        
        String cacheKey = createGeoCacheKey(key, geoLocation);
        RateLimitConfig cached = geoConfigCache.get(cacheKey);
        
        if (cached != null) {
            return cached;
        }
        
        RateLimitConfig geographicConfig = geographicConfigResolver.resolveGeographicConfig(key, geoLocation);
        if (geographicConfig != null) {
            geoConfigCache.put(cacheKey, geographicConfig);
            return geographicConfig;
        }
        
        return super.resolveConfig(key);
    }

    /**
     * Clear all caches (both standard and geographic).
     */
    @Override
    public void clearCache() {
        super.clearCache();
        geoConfigCache.clear();
        logger.debug("Geographic configuration cache cleared");
    }

    /**
     * Create a cache key for geographic configurations.
     */
    private String createGeoCacheKey(String key, GeoLocation geoLocation) {
        return String.format("geo:%s:%s:%s:%s", 
                key, 
                geoLocation.getCountryCode(),
                geoLocation.getRegion(),
                geoLocation.getComplianceZone());
    }

    /**
     * Get cache statistics including geographic cache.
     */
    public java.util.Map<String, Object> getExtendedCacheStats() {
        return java.util.Map.of(
            "standardCacheSize", getCacheSize(),
            "geographicCacheSize", geoConfigCache.size()
        );
    }
}