package dev.bnacar.distributedratelimiter.models;

import java.util.Arrays;
import java.util.List;

/**
 * Compliance zones for geographic rate limiting.
 * Each zone defines specific geographic regions subject to particular data protection regulations.
 */
public enum ComplianceZone {
    GDPR("General Data Protection Regulation", 
         Arrays.asList("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE")),
    
    CCPA("California Consumer Privacy Act", 
         Arrays.asList("US-CA")),
    
    PIPEDA("Personal Information Protection and Electronic Documents Act", 
           Arrays.asList("CA")),
    
    LGPD("Lei Geral de Proteção de Dados", 
         Arrays.asList("BR")),
    
    PDPA_SG("Personal Data Protection Act Singapore", 
            Arrays.asList("SG")),
    
    DPA_UK("Data Protection Act UK", 
           Arrays.asList("GB")),
    
    NONE("No specific compliance zone", 
         Arrays.asList());

    private final String name;
    private final List<String> applicableRegions;

    ComplianceZone(String name, List<String> applicableRegions) {
        this.name = name;
        this.applicableRegions = applicableRegions;
    }

    public String getName() {
        return name;
    }

    public List<String> getApplicableRegions() {
        return applicableRegions;
    }

    /**
     * Determine the compliance zone for a given country code.
     * 
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g., "US", "DE", "CA")
     * @return The applicable compliance zone, or NONE if no specific zone applies
     */
    public static ComplianceZone fromCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return NONE;
        }

        for (ComplianceZone zone : values()) {
            if (zone.getApplicableRegions().contains(countryCode.toUpperCase())) {
                return zone;
            }
        }

        return NONE;
    }

    /**
     * Check if a country code falls under this compliance zone.
     * 
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return true if the country is subject to this compliance zone
     */
    public boolean appliesTo(String countryCode) {
        return countryCode != null && applicableRegions.contains(countryCode.toUpperCase());
    }
}