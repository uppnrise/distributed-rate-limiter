package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.GeographicRateLimitConfig;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class GeographicConfigurationResolverTest {

    private GeographicConfigurationResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new GeographicConfigurationResolver();
    }

    @Test
    void testAddAndResolveGeographicRule() {
        // Create a geographic rule for US
        RateLimitConfig usLimits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig usRule = new GeographicRateLimitConfig("us-rule", "api:*", "US", usLimits);
        usRule.setPriority(100);

        resolver.addGeographicRule(usRule);

        // Create a GeoLocation for US
        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        // Resolve configuration for API key in US
        RateLimitConfig resolved = resolver.resolveGeographicConfig("api:user:123", usLocation);

        assertNotNull(resolved);
        assertEquals(1000, resolved.getCapacity());
        assertEquals(100, resolved.getRefillRate());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, resolved.getAlgorithm());
    }

    @Test
    void testPriorityBasedRuleResolution() {
        // Create two overlapping rules with different priorities
        RateLimitConfig highPriorityLimits = new RateLimitConfig(2000, 200, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig highPriorityRule = new GeographicRateLimitConfig("high-priority", "api:*", "US", highPriorityLimits);
        highPriorityRule.setPriority(200);

        RateLimitConfig lowPriorityLimits = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig lowPriorityRule = new GeographicRateLimitConfig("low-priority", "api:*", "US", lowPriorityLimits);
        lowPriorityRule.setPriority(100);

        resolver.addGeographicRule(lowPriorityRule);
        resolver.addGeographicRule(highPriorityRule);

        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        RateLimitConfig resolved = resolver.resolveGeographicConfig("api:user:123", usLocation);

        assertNotNull(resolved);
        // Should return high priority rule
        assertEquals(2000, resolved.getCapacity());
        assertEquals(200, resolved.getRefillRate());
    }

    @Test
    void testComplianceZoneBasedRule() {
        // Create a rule for GDPR compliance zone
        RateLimitConfig gdprLimits = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig gdprRule = new GeographicRateLimitConfig("gdpr-rule", "api:*", "EU", ComplianceZone.GDPR, gdprLimits, 100);

        resolver.addGeographicRule(gdprRule);

        // Create a GeoLocation for Germany (GDPR zone)
        GeoLocation germanyLocation = GeoLocation.builder()
                .countryCode("DE")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();

        RateLimitConfig resolved = resolver.resolveGeographicConfig("api:user:123", germanyLocation);

        assertNotNull(resolved);
        assertEquals(500, resolved.getCapacity());
    }

    @Test
    void testNoMatchingRule() {
        // Add a rule for US only
        RateLimitConfig usLimits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig usRule = new GeographicRateLimitConfig("us-rule", "api:*", "US", usLimits);

        resolver.addGeographicRule(usRule);

        // Try to resolve for Canada
        GeoLocation canadaLocation = GeoLocation.builder()
                .countryCode("CA")
                .region("NA")
                .complianceZone(ComplianceZone.PIPEDA)
                .build();

        RateLimitConfig resolved = resolver.resolveGeographicConfig("api:user:123", canadaLocation);

        assertNull(resolved);
    }

    @Test
    void testKeyPatternMatching() {
        // Create a rule that only applies to user API keys
        RateLimitConfig userLimits = new RateLimitConfig(200, 20, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig userRule = new GeographicRateLimitConfig("user-rule", "api:user:*", "US", userLimits);

        resolver.addGeographicRule(userRule);

        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        // Should match user API keys
        RateLimitConfig userResolved = resolver.resolveGeographicConfig("api:user:123", usLocation);
        assertNotNull(userResolved);
        assertEquals(200, userResolved.getCapacity());

        // Should not match admin API keys
        RateLimitConfig adminResolved = resolver.resolveGeographicConfig("api:admin:123", usLocation);
        assertNull(adminResolved);
    }

    @Test
    void testRemoveGeographicRule() {
        // Add a rule
        RateLimitConfig limits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule = new GeographicRateLimitConfig("test-rule", "api:*", "US", limits);
        rule.setId("rule-1");

        resolver.addGeographicRule(rule);

        // Verify rule exists
        assertEquals(1, resolver.getAllGeographicRules().size());

        // Remove rule
        boolean removed = resolver.removeGeographicRule("rule-1");
        assertTrue(removed);
        assertEquals(0, resolver.getAllGeographicRules().size());

        // Try to remove non-existent rule
        boolean notRemoved = resolver.removeGeographicRule("non-existent");
        assertFalse(notRemoved);
    }

    @Test
    void testGetRulesForLocation() {
        // Add multiple rules for US
        RateLimitConfig rule1Limits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule1 = new GeographicRateLimitConfig("rule1", "api:*", "US", rule1Limits);
        rule1.setPriority(100);

        RateLimitConfig rule2Limits = new RateLimitConfig(2000, 200, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule2 = new GeographicRateLimitConfig("rule2", "user:*", "US", rule2Limits);
        rule2.setPriority(200);

        resolver.addGeographicRule(rule1);
        resolver.addGeographicRule(rule2);

        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        var rulesForUS = resolver.getRulesForLocation(usLocation);

        assertEquals(2, rulesForUS.size());
        // Should be sorted by priority (highest first)
        assertEquals("rule2", rulesForUS.get(0).getName());
        assertEquals("rule1", rulesForUS.get(1).getName());
    }

    @Test
    void testCaching() {
        // Add a rule
        RateLimitConfig limits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule = new GeographicRateLimitConfig("test-rule", "api:*", "US", limits);

        resolver.addGeographicRule(rule);

        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        // First call should populate cache
        RateLimitConfig resolved1 = resolver.resolveGeographicConfig("api:user:123", usLocation);
        
        // Second call should use cache
        RateLimitConfig resolved2 = resolver.resolveGeographicConfig("api:user:123", usLocation);

        assertNotNull(resolved1);
        assertNotNull(resolved2);
        assertEquals(resolved1.getCapacity(), resolved2.getCapacity());

        // Cache stats should show some cached entries
        var stats = resolver.getCacheStats();
        assertTrue((Integer) stats.get("cacheSize") > 0);
    }

    @Test
    void testClearCache() {
        // Add a rule and resolve to populate cache
        RateLimitConfig limits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule = new GeographicRateLimitConfig("test-rule", "api:*", "US", limits);

        resolver.addGeographicRule(rule);

        GeoLocation usLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();

        resolver.resolveGeographicConfig("api:user:123", usLocation);

        // Clear cache
        resolver.clearCache();

        // Cache should be empty
        var stats = resolver.getCacheStats();
        assertEquals(0, stats.get("cacheSize"));
    }
}