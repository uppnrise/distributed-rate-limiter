package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for detecting geographic location from IP addresses and headers.
 * Supports multiple detection methods with fallback strategy.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
public class GeoLocationService {
    private static final Logger logger = LoggerFactory.getLogger(GeoLocationService.class);

    private final CDNHeaderParser cdnHeaderParser;
    
    // LRU cache for IP geolocation results to improve performance
    private final Map<String, GeoLocation> locationCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 10000;

    @Autowired
    public GeoLocationService(CDNHeaderParser cdnHeaderParser) {
        this.cdnHeaderParser = cdnHeaderParser;
    }

    /**
     * Detect geographic location using multiple detection methods.
     * Priority order:
     * 1. CDN headers (CloudFlare, AWS, etc.)
     * 2. MaxMind GeoIP2 database (future implementation)
     * 3. IP range-based detection (basic implementation)
     * 4. Default to "UNKNOWN"
     */
    public GeoLocation detectLocation(String sourceIP, Map<String, String> headers) {
        if (sourceIP == null || sourceIP.trim().isEmpty()) {
            logger.debug("No source IP provided for geolocation");
            return createUnknownLocation("NO_IP_PROVIDED");
        }

        String cacheKey = createCacheKey(sourceIP, headers);
        
        // Check cache first
        GeoLocation cached = locationCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Geographic location found in cache for IP: {}", sourceIP);
            return cached;
        }

        GeoLocation location = doDetectLocation(sourceIP, headers);
        
        // Cache the result (with size limit)
        if (locationCache.size() < MAX_CACHE_SIZE) {
            locationCache.put(cacheKey, location);
        }
        
        return location;
    }

    private GeoLocation doDetectLocation(String sourceIP, Map<String, String> headers) {
        // 1. Try CDN headers first (most accurate)
        GeoLocation cdnLocation = cdnHeaderParser.parseHeaders(headers);
        if (cdnLocation != null) {
            logger.debug("Geographic location detected from CDN headers for IP {}: {}", sourceIP, cdnLocation);
            return cdnLocation;
        }

        // 2. Try MaxMind GeoIP2 database (future implementation)
        // GeoLocation maxMindLocation = maxMindService.lookup(sourceIP);
        // if (maxMindLocation != null) {
        //     return maxMindLocation;
        // }

        // 3. Basic IP range detection for common cases
        GeoLocation basicLocation = detectFromIpRange(sourceIP);
        if (basicLocation != null) {
            logger.debug("Geographic location detected from IP range for IP {}: {}", sourceIP, basicLocation);
            return basicLocation;
        }

        // 4. Default to unknown
        logger.debug("Could not determine geographic location for IP: {}", sourceIP);
        return createUnknownLocation("NO_DETECTION_METHOD_SUCCEEDED");
    }

    /**
     * Basic IP range detection for common cases.
     * This is a simple implementation - in production, MaxMind GeoIP2 should be used.
     */
    private GeoLocation detectFromIpRange(String sourceIP) {
        // Local/private IP addresses
        if (isLocalIP(sourceIP)) {
            return GeoLocation.builder()
                    .countryCode("LOCAL")
                    .countryName("Local Network")
                    .region("LOCAL")
                    .complianceZone(ComplianceZone.NONE)
                    .detectionSource("IP_RANGE_LOCAL")
                    .build();
        }

        // This is a very basic implementation
        // In a real implementation, you would use MaxMind GeoIP2 database
        // or similar service for accurate geolocation
        return null;
    }

    /**
     * Check if an IP address is a local/private address.
     */
    private boolean isLocalIP(String ip) {
        if (ip == null) return false;
        
        // IPv4 private ranges
        if (ip.startsWith("127.") ||           // Loopback
            ip.startsWith("10.") ||            // Class A private
            ip.startsWith("192.168.") ||       // Class C private
            ip.matches("172\\.(1[6-9]|2[0-9]|3[01])\\..*")) {  // Class B private
            return true;
        }
        
        // IPv6 loopback and local
        if (ip.equals("::1") || ip.startsWith("fc") || ip.startsWith("fd")) {
            return true;
        }
        
        return false;
    }

    /**
     * Create a cache key from IP and relevant headers.
     */
    private String createCacheKey(String sourceIP, Map<String, String> headers) {
        StringBuilder keyBuilder = new StringBuilder(sourceIP);
        
        if (headers != null) {
            // Include CDN headers in cache key as they might change
            String cfCountry = headers.get("CF-IPCountry");
            if (cfCountry != null) {
                keyBuilder.append(":CF:").append(cfCountry);
            }
            
            String awsCountry = headers.get("CloudFront-Viewer-Country");
            if (awsCountry != null) {
                keyBuilder.append(":AWS:").append(awsCountry);
            }
        }
        
        return keyBuilder.toString();
    }

    /**
     * Create an unknown location result.
     */
    private GeoLocation createUnknownLocation(String reason) {
        return GeoLocation.builder()
                .countryCode("UNKNOWN")
                .countryName("Unknown")
                .region("UNKNOWN")
                .complianceZone(ComplianceZone.NONE)
                .detectionSource("UNKNOWN")
                .build();
    }

    /**
     * Clear the location cache.
     */
    public void clearCache() {
        locationCache.clear();
        logger.info("Geographic location cache cleared");
    }

    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "cacheSize", locationCache.size(),
            "maxCacheSize", MAX_CACHE_SIZE
        );
    }
}