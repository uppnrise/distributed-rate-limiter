package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Configuration for geographic-based rate limiting rules.
 * Defines rate limits that apply to specific geographic regions, countries, or compliance zones.
 */
public class GeographicRateLimitConfig {
    private String id;
    private String name;                    // Human-readable name for the rule
    private String keyPattern;              // Pattern that this geographic rule applies to (e.g., "api:*", "user:*")
    private String countryCode;             // Specific country code (ISO 3166-1 alpha-2, e.g., "US", "DE")
    private String region;                  // Geographic region (e.g., "EU", "NA", "APAC")
    private ComplianceZone complianceZone;  // Compliance zone (GDPR, CCPA, etc.)
    private RateLimitConfig limits;         // The rate limit configuration to apply
    private int priority;                   // Priority for resolving conflicts (higher = more priority)
    private LocalDateTime validFrom;        // When this rule becomes effective
    private LocalDateTime validUntil;       // When this rule expires
    private boolean enabled;                // Whether this rule is currently active

    public GeographicRateLimitConfig() {
        this.enabled = true;
        this.priority = 0;
    }

    public GeographicRateLimitConfig(String name, String keyPattern, String countryCode, 
                                   RateLimitConfig limits) {
        this();
        this.name = name;
        this.keyPattern = keyPattern;
        this.countryCode = countryCode;
        this.limits = limits;
    }

    public GeographicRateLimitConfig(String name, String keyPattern, String region, 
                                   ComplianceZone complianceZone, RateLimitConfig limits, 
                                   int priority) {
        this();
        this.name = name;
        this.keyPattern = keyPattern;
        this.region = region;
        this.complianceZone = complianceZone;
        this.limits = limits;
        this.priority = priority;
    }

    /**
     * Check if this geographic rule applies to the given location and key.
     * 
     * @param geoLocation The detected geographic location
     * @param key The rate limiting key
     * @return true if this rule should be applied
     */
    public boolean appliesTo(GeoLocation geoLocation, String key) {
        if (!enabled) {
            return false;
        }

        // Check time validity
        LocalDateTime now = LocalDateTime.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }

        // Check key pattern match
        if (keyPattern != null && !matchesPattern(key, keyPattern)) {
            return false;
        }

        // Check geographic criteria
        if (geoLocation == null) {
            return false;
        }

        // Check country code match
        if (countryCode != null && !countryCode.equalsIgnoreCase(geoLocation.getCountryCode())) {
            return false;
        }

        // Check region match
        if (region != null && !region.equalsIgnoreCase(geoLocation.getRegion())) {
            return false;
        }

        // Check compliance zone match
        if (complianceZone != null && !complianceZone.equals(geoLocation.getComplianceZone())) {
            return false;
        }

        return true;
    }

    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return key.equals(pattern);
        }
        
        // Convert pattern to regex
        String regex = pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("|", "\\|")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("*", ".*");
            
        return key.matches("^" + regex + "$");
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeyPattern() {
        return keyPattern;
    }

    public void setKeyPattern(String keyPattern) {
        this.keyPattern = keyPattern;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public ComplianceZone getComplianceZone() {
        return complianceZone;
    }

    public void setComplianceZone(ComplianceZone complianceZone) {
        this.complianceZone = complianceZone;
    }

    public RateLimitConfig getLimits() {
        return limits;
    }

    public void setLimits(RateLimitConfig limits) {
        this.limits = limits;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeographicRateLimitConfig that = (GeographicRateLimitConfig) o;
        return priority == that.priority &&
                enabled == that.enabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(keyPattern, that.keyPattern) &&
                Objects.equals(countryCode, that.countryCode) &&
                Objects.equals(region, that.region) &&
                complianceZone == that.complianceZone &&
                Objects.equals(limits, that.limits) &&
                Objects.equals(validFrom, that.validFrom) &&
                Objects.equals(validUntil, that.validUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, keyPattern, countryCode, region, complianceZone, limits, priority, validFrom, validUntil, enabled);
    }

    @Override
    public String toString() {
        return "GeographicRateLimitConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", keyPattern='" + keyPattern + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", region='" + region + '\'' +
                ", complianceZone=" + complianceZone +
                ", limits=" + limits +
                ", priority=" + priority +
                ", validFrom=" + validFrom +
                ", validUntil=" + validUntil +
                ", enabled=" + enabled +
                '}';
    }
}