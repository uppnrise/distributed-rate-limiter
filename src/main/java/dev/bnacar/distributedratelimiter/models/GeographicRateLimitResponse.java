package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;

/**
 * Enhanced rate limit response that includes geographic information.
 * Extends the basic response with details about the geographic rule applied and location detection.
 */
public class GeographicRateLimitResponse extends RateLimitResponse {
    private final GeoLocation geoLocation;
    private final String appliedRule;
    private final RateLimitConfig appliedLimits;
    private final String fallbackReason;

    public GeographicRateLimitResponse(String key, int tokensRequested, boolean allowed, 
                                     GeoLocation geoLocation, String appliedRule, 
                                     RateLimitConfig appliedLimits) {
        super(key, tokensRequested, allowed);
        this.geoLocation = geoLocation;
        this.appliedRule = appliedRule;
        this.appliedLimits = appliedLimits;
        this.fallbackReason = null;
    }

    public GeographicRateLimitResponse(String key, int tokensRequested, boolean allowed, 
                                     GeoLocation geoLocation, String appliedRule, 
                                     RateLimitConfig appliedLimits, String fallbackReason) {
        super(key, tokensRequested, allowed);
        this.geoLocation = geoLocation;
        this.appliedRule = appliedRule;
        this.appliedLimits = appliedLimits;
        this.fallbackReason = fallbackReason;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public String getAppliedRule() {
        return appliedRule;
    }

    public RateLimitConfig getAppliedLimits() {
        return appliedLimits;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    /**
     * Get geographic information suitable for API responses.
     * Includes detected country, compliance zone, and applied limits.
     */
    public GeoInfo getGeoInfo() {
        return new GeoInfo(geoLocation, appliedRule, appliedLimits, fallbackReason);
    }

    /**
     * Geographic information for API responses.
     */
    public static class GeoInfo {
        private final String detectedCountry;
        private final String detectedRegion;
        private final String detectedCity;
        private final ComplianceZone complianceZone;
        private final String appliedRule;
        private final AppliedLimits appliedLimits;
        private final String detectionSource;
        private final boolean isVpnOrProxy;
        private final String fallbackReason;

        public GeoInfo(GeoLocation geoLocation, String appliedRule, RateLimitConfig limits, String fallbackReason) {
            this.detectedCountry = geoLocation != null ? geoLocation.getCountryName() : "Unknown";
            this.detectedRegion = geoLocation != null ? geoLocation.getRegion() : "Unknown";
            this.detectedCity = geoLocation != null ? geoLocation.getCity() : "Unknown";
            this.complianceZone = geoLocation != null ? geoLocation.getComplianceZone() : ComplianceZone.NONE;
            this.appliedRule = appliedRule;
            this.appliedLimits = limits != null ? new AppliedLimits(limits) : null;
            this.detectionSource = geoLocation != null ? geoLocation.getDetectionSource() : "UNKNOWN";
            this.isVpnOrProxy = geoLocation != null && geoLocation.isVpnOrProxy();
            this.fallbackReason = fallbackReason;
        }

        public String getDetectedCountry() {
            return detectedCountry;
        }

        public String getDetectedRegion() {
            return detectedRegion;
        }

        public String getDetectedCity() {
            return detectedCity;
        }

        public ComplianceZone getComplianceZone() {
            return complianceZone;
        }

        public String getAppliedRule() {
            return appliedRule;
        }

        public AppliedLimits getAppliedLimits() {
            return appliedLimits;
        }

        public String getDetectionSource() {
            return detectionSource;
        }

        public boolean isVpnOrProxy() {
            return isVpnOrProxy;
        }

        public String getFallbackReason() {
            return fallbackReason;
        }
    }

    /**
     * Applied rate limit configuration for API responses.
     */
    public static class AppliedLimits {
        private final int capacity;
        private final int refillRate;
        private final String algorithm;

        public AppliedLimits(RateLimitConfig config) {
            this.capacity = config.getCapacity();
            this.refillRate = config.getRefillRate();
            this.algorithm = config.getAlgorithm().name();
        }

        public int getCapacity() {
            return capacity;
        }

        public int getRefillRate() {
            return refillRate;
        }

        public String getAlgorithm() {
            return algorithm;
        }
    }
}