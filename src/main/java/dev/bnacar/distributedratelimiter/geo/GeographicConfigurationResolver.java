package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.GeographicRateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves geographic rate limit configurations based on location and key patterns.
 * Handles priority-based rule resolution and caching for performance.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
public class GeographicConfigurationResolver {
    private static final Logger logger = LoggerFactory.getLogger(GeographicConfigurationResolver.class);

    // In-memory storage for geographic rules (in production, this could be externalized)
    private final List<GeographicRateLimitConfig> geographicRules = new ArrayList<>();
    
    // Cache for resolved configurations
    private final ConcurrentHashMap<String, RateLimitConfig> configCache = new ConcurrentHashMap<>();

    /**
     * Resolve the appropriate geographic rate limit configuration.
     * Returns null if no geographic rule applies (fallback to standard configuration).
     */
    public RateLimitConfig resolveGeographicConfig(String key, GeoLocation geoLocation) {
        if (geoLocation == null) {
            logger.debug("No geographic location provided, skipping geographic rules");
            return null;
        }

        String cacheKey = createCacheKey(key, geoLocation);
        
        // Check cache first
        RateLimitConfig cached = configCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Geographic configuration found in cache for key: {} location: {}", key, geoLocation.getCountryCode());
            return cached;
        }

        RateLimitConfig resolved = doResolveGeographicConfig(key, geoLocation);
        
        // Cache the result if found
        if (resolved != null) {
            configCache.put(cacheKey, resolved);
        }
        
        return resolved;
    }

    private RateLimitConfig doResolveGeographicConfig(String key, GeoLocation geoLocation) {
        // Find all applicable rules and sort by priority (highest first)
        List<GeographicRateLimitConfig> applicableRules = geographicRules.stream()
                .filter(rule -> rule.appliesTo(geoLocation, key))
                .sorted(Comparator.comparingInt(GeographicRateLimitConfig::getPriority).reversed())
                .toList();

        if (applicableRules.isEmpty()) {
            logger.debug("No geographic rules apply for key: {} location: {}", key, geoLocation.getCountryCode());
            return null;
        }

        // Return the highest priority rule
        GeographicRateLimitConfig selectedRule = applicableRules.get(0);
        logger.debug("Selected geographic rule '{}' for key: {} location: {}", 
                    selectedRule.getName(), key, geoLocation.getCountryCode());
        
        return selectedRule.getLimits();
    }

    /**
     * Add a geographic rate limiting rule.
     */
    public void addGeographicRule(GeographicRateLimitConfig rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Geographic rule cannot be null");
        }
        
        geographicRules.add(rule);
        clearCache(); // Clear cache when rules change
        
        logger.info("Added geographic rule: {}", rule.getName());
    }

    /**
     * Remove a geographic rate limiting rule by ID.
     */
    public boolean removeGeographicRule(String ruleId) {
        boolean removed = geographicRules.removeIf(rule -> ruleId.equals(rule.getId()));
        if (removed) {
            clearCache(); // Clear cache when rules change
            logger.info("Removed geographic rule with ID: {}", ruleId);
        }
        return removed;
    }

    /**
     * Get all geographic rules (for administration).
     */
    public List<GeographicRateLimitConfig> getAllGeographicRules() {
        return new ArrayList<>(geographicRules);
    }

    /**
     * Get geographic rules that apply to a specific location.
     */
    public List<GeographicRateLimitConfig> getRulesForLocation(GeoLocation geoLocation) {
        return geographicRules.stream()
                .filter(rule -> appliesLocationOnly(rule, geoLocation))  // Check location only, ignore key patterns
                .sorted(Comparator.comparingInt(GeographicRateLimitConfig::getPriority).reversed())
                .toList();
    }
    
    /**
     * Check if a rule applies to a location (ignoring key patterns).
     */
    private boolean appliesLocationOnly(GeographicRateLimitConfig rule, GeoLocation geoLocation) {
        if (!rule.isEnabled()) {
            return false;
        }

        // Check time validity
        LocalDateTime now = LocalDateTime.now();
        if (rule.getValidFrom() != null && now.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidUntil() != null && now.isAfter(rule.getValidUntil())) {
            return false;
        }

        // Check geographic criteria only (ignore key patterns)
        if (geoLocation == null) {
            return false;
        }

        // Check country code match
        if (rule.getCountryCode() != null && !rule.getCountryCode().equalsIgnoreCase(geoLocation.getCountryCode())) {
            return false;
        }

        // Check region match
        if (rule.getRegion() != null && !rule.getRegion().equalsIgnoreCase(geoLocation.getRegion())) {
            return false;
        }

        // Check compliance zone match
        if (rule.getComplianceZone() != null && !rule.getComplianceZone().equals(geoLocation.getComplianceZone())) {
            return false;
        }

        return true;
    }

    /**
     * Update an existing geographic rule.
     */
    public boolean updateGeographicRule(String ruleId, GeographicRateLimitConfig updatedRule) {
        for (int i = 0; i < geographicRules.size(); i++) {
            if (ruleId.equals(geographicRules.get(i).getId())) {
                updatedRule.setId(ruleId); // Ensure ID consistency
                geographicRules.set(i, updatedRule);
                clearCache(); // Clear cache when rules change
                logger.info("Updated geographic rule: {}", ruleId);
                return true;
            }
        }
        return false;
    }

    /**
     * Clear the configuration cache.
     */
    public void clearCache() {
        configCache.clear();
        logger.debug("Geographic configuration cache cleared");
    }

    /**
     * Create a cache key from the rate limiting key and geographic location.
     */
    private String createCacheKey(String key, GeoLocation geoLocation) {
        return String.format("%s:%s:%s:%s", 
                key, 
                geoLocation.getCountryCode(), 
                geoLocation.getRegion(), 
                geoLocation.getComplianceZone());
    }

    /**
     * Get cache statistics for monitoring.
     */
    public java.util.Map<String, Object> getCacheStats() {
        return java.util.Map.of(
            "cacheSize", configCache.size(),
            "rulesCount", geographicRules.size()
        );
    }

    /**
     * Initialize with default geographic rules for common scenarios.
     */
    public void initializeDefaultRules() {
        logger.info("Initializing default geographic rate limiting rules");
        
        // Clear existing rules
        geographicRules.clear();
        clearCache();
        
        // Example rules - these would typically be loaded from configuration
        // Note: In a real implementation, these would come from application.properties or database
        logger.info("Default geographic rules initialized");
    }
}