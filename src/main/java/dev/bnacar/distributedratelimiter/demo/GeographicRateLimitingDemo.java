package dev.bnacar.distributedratelimiter.demo;

import dev.bnacar.distributedratelimiter.geo.GeographicConfigurationResolver;
import dev.bnacar.distributedratelimiter.geo.GeographicRateLimitService;
import dev.bnacar.distributedratelimiter.models.*;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo component that initializes example geographic rate limiting rules 
 * and demonstrates the functionality when the application starts.
 */
@Component
@ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true")
public class GeographicRateLimitingDemo {
    private static final Logger logger = LoggerFactory.getLogger(GeographicRateLimitingDemo.class);

    @Autowired
    private GeographicConfigurationResolver geographicConfigResolver;

    @Autowired
    private GeographicRateLimitService geographicRateLimitService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGeographicRules() {
        logger.info("Initializing demo geographic rate limiting rules...");

        try {
            // 1. EU/GDPR region limits - moderate limits for compliance
            RateLimitConfig euLimits = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
            GeographicRateLimitConfig euRule = new GeographicRateLimitConfig(
                "eu-gdpr-demo", "api:*", null, ComplianceZone.GDPR, euLimits, 100);
            euRule.setId("eu-gdpr-rule");
            geographicConfigResolver.addGeographicRule(euRule);

            // 2. US premium limits - higher limits for US users
            RateLimitConfig usLimits = new RateLimitConfig(2000, 200, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
            GeographicRateLimitConfig usRule = new GeographicRateLimitConfig(
                "us-premium-demo", "api:*", "US", usLimits);
            usRule.setId("us-premium-rule");
            usRule.setPriority(90);
            geographicConfigResolver.addGeographicRule(usRule);

            // 3. Canada PIPEDA limits
            RateLimitConfig caLimits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
            GeographicRateLimitConfig caRule = new GeographicRateLimitConfig(
                "canada-pipeda-demo", "api:*", "CA", caLimits);
            caRule.setId("canada-pipeda-rule");
            caRule.setPriority(80);
            geographicConfigResolver.addGeographicRule(caRule);

            // 4. Default restrictive rule for unknown regions
            RateLimitConfig defaultLimits = new RateLimitConfig(100, 10, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
            GeographicRateLimitConfig defaultRule = new GeographicRateLimitConfig(
                "default-restrictive", "*", "UNKNOWN", defaultLimits);
            defaultRule.setId("default-restrictive-rule");
            defaultRule.setPriority(10); // Lowest priority
            geographicConfigResolver.addGeographicRule(defaultRule);

            logger.info("Successfully initialized {} geographic rate limiting rules", 
                      geographicConfigResolver.getAllGeographicRules().size());

            // Demonstrate functionality with some test scenarios
            demonstrateGeographicRateLimiting();

        } catch (Exception e) {
            logger.error("Failed to initialize geographic rate limiting rules", e);
        }
    }

    private void demonstrateGeographicRateLimiting() {
        logger.info("Demonstrating geographic rate limiting functionality...");

        // Test scenario 1: CloudFlare headers from Germany (GDPR zone)
        Map<String, String> germanyHeaders = new HashMap<>();
        germanyHeaders.put("CF-IPCountry", "DE");
        germanyHeaders.put("CF-IPContinent", "EU");
        germanyHeaders.put("CF-IPCity", "Berlin");

        GeographicRateLimitResponse germanyResponse = geographicRateLimitService.checkGeographicRateLimit(
            "api:user:123", "92.168.1.100", 5, germanyHeaders);

        logger.info("Germany test - Allowed: {}, Applied Rule: {}, Capacity: {}", 
                   germanyResponse.isAllowed(), 
                   germanyResponse.getAppliedRule(),
                   germanyResponse.getAppliedLimits() != null ? germanyResponse.getAppliedLimits().getCapacity() : "N/A");

        // Test scenario 2: AWS CloudFront headers from US
        Map<String, String> usHeaders = new HashMap<>();
        usHeaders.put("CloudFront-Viewer-Country", "US");
        usHeaders.put("CloudFront-Viewer-Country-Region", "CA");

        GeographicRateLimitResponse usResponse = geographicRateLimitService.checkGeographicRateLimit(
            "api:user:456", "73.162.1.100", 5, usHeaders);

        logger.info("US test - Allowed: {}, Applied Rule: {}, Capacity: {}", 
                   usResponse.isAllowed(), 
                   usResponse.getAppliedRule(),
                   usResponse.getAppliedLimits() != null ? usResponse.getAppliedLimits().getCapacity() : "N/A");

        // Test scenario 3: No geographic headers (should use IP-based detection)
        Map<String, String> noHeaders = new HashMap<>();

        GeographicRateLimitResponse noHeadersResponse = geographicRateLimitService.checkGeographicRateLimit(
            "api:user:789", "127.0.0.1", 5, noHeaders);

        logger.info("Local IP test - Allowed: {}, Applied Rule: {}, Fallback Reason: {}", 
                   noHeadersResponse.isAllowed(), 
                   noHeadersResponse.getAppliedRule(),
                   noHeadersResponse.getFallbackReason());

        // Test scenario 4: Generic headers from Canada
        Map<String, String> canadaHeaders = new HashMap<>();
        canadaHeaders.put("X-Country-Code", "CA");
        canadaHeaders.put("X-Region", "NA");

        GeographicRateLimitResponse canadaResponse = geographicRateLimitService.checkGeographicRateLimit(
            "api:user:999", "142.168.1.100", 5, canadaHeaders);

        logger.info("Canada test - Allowed: {}, Applied Rule: {}, Capacity: {}", 
                   canadaResponse.isAllowed(), 
                   canadaResponse.getAppliedRule(),
                   canadaResponse.getAppliedLimits() != null ? canadaResponse.getAppliedLimits().getCapacity() : "N/A");

        logger.info("Geographic rate limiting demonstration completed!");
    }
}