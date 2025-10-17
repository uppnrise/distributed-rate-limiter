package dev.bnacar.distributedratelimiter.models;

import java.util.Objects;

/**
 * Represents geographic location information for rate limiting decisions.
 * Contains country, region, city, and compliance zone information.
 */
public class GeoLocation {
    private final String countryCode;      // ISO 3166-1 alpha-2 country code (e.g., "US", "DE", "CN")
    private final String countryName;      // Full country name (e.g., "United States", "Germany", "China")
    private final String region;           // Geographic region (e.g., "NA", "EU", "APAC")
    private final String city;             // City name (e.g., "New York", "Berlin", "Beijing")
    private final String timezone;         // Timezone (e.g., "America/New_York", "Europe/Berlin")
    private final ComplianceZone complianceZone;  // Applicable compliance zone
    private final boolean isVpnOrProxy;    // Whether IP is identified as VPN/proxy
    private final String detectionSource;  // Source of detection (CDN_HEADER, MAXMIND, IP_RANGE, UNKNOWN)

    private GeoLocation(Builder builder) {
        this.countryCode = builder.countryCode;
        this.countryName = builder.countryName;
        this.region = builder.region;
        this.city = builder.city;
        this.timezone = builder.timezone;
        this.complianceZone = builder.complianceZone;
        this.isVpnOrProxy = builder.isVpnOrProxy;
        this.detectionSource = builder.detectionSource;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getRegion() {
        return region;
    }

    public String getCity() {
        return city;
    }

    public String getTimezone() {
        return timezone;
    }

    public ComplianceZone getComplianceZone() {
        return complianceZone;
    }

    public boolean isVpnOrProxy() {
        return isVpnOrProxy;
    }

    public String getDetectionSource() {
        return detectionSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoLocation that = (GeoLocation) o;
        return isVpnOrProxy == that.isVpnOrProxy &&
                Objects.equals(countryCode, that.countryCode) &&
                Objects.equals(countryName, that.countryName) &&
                Objects.equals(region, that.region) &&
                Objects.equals(city, that.city) &&
                Objects.equals(timezone, that.timezone) &&
                complianceZone == that.complianceZone &&
                Objects.equals(detectionSource, that.detectionSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCode, countryName, region, city, timezone, complianceZone, isVpnOrProxy, detectionSource);
    }

    @Override
    public String toString() {
        return "GeoLocation{" +
                "countryCode='" + countryCode + '\'' +
                ", countryName='" + countryName + '\'' +
                ", region='" + region + '\'' +
                ", city='" + city + '\'' +
                ", timezone='" + timezone + '\'' +
                ", complianceZone=" + complianceZone +
                ", isVpnOrProxy=" + isVpnOrProxy +
                ", detectionSource='" + detectionSource + '\'' +
                '}';
    }

    public static class Builder {
        private String countryCode;
        private String countryName;
        private String region;
        private String city;
        private String timezone;
        private ComplianceZone complianceZone;
        private boolean isVpnOrProxy = false;
        private String detectionSource = "UNKNOWN";

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder countryName(String countryName) {
            this.countryName = countryName;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder complianceZone(ComplianceZone complianceZone) {
            this.complianceZone = complianceZone;
            return this;
        }

        public Builder isVpnOrProxy(boolean isVpnOrProxy) {
            this.isVpnOrProxy = isVpnOrProxy;
            return this;
        }

        public Builder detectionSource(String detectionSource) {
            this.detectionSource = detectionSource;
            return this;
        }

        public GeoLocation build() {
            return new GeoLocation(this);
        }
    }
}