package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Parses geographic information from CDN headers.
 * Supports CloudFlare, AWS CloudFront, Azure CDN, and other popular CDN providers.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "ratelimiter.geographic.enabled", havingValue = "true", matchIfMissing = false)
public class CDNHeaderParser {
    private static final Logger logger = LoggerFactory.getLogger(CDNHeaderParser.class);

    /**
     * Parse geographic location from various CDN headers.
     * Priority order: CloudFlare -> AWS CloudFront -> Azure CDN -> Generic headers
     */
    public GeoLocation parseHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        // Try CloudFlare headers first (most comprehensive)
        GeoLocation cloudflareLocation = parseCloudFlareHeaders(headers);
        if (cloudflareLocation != null) {
            logger.debug("Geographic location detected from CloudFlare headers: {}", cloudflareLocation);
            return cloudflareLocation;
        }

        // Try AWS CloudFront headers
        GeoLocation awsLocation = parseAWSHeaders(headers);
        if (awsLocation != null) {
            logger.debug("Geographic location detected from AWS CloudFront headers: {}", awsLocation);
            return awsLocation;
        }

        // Try Azure CDN headers
        GeoLocation azureLocation = parseAzureHeaders(headers);
        if (azureLocation != null) {
            logger.debug("Geographic location detected from Azure CDN headers: {}", azureLocation);
            return azureLocation;
        }

        // Try generic geographic headers
        GeoLocation genericLocation = parseGenericHeaders(headers);
        if (genericLocation != null) {
            logger.debug("Geographic location detected from generic headers: {}", genericLocation);
            return genericLocation;
        }

        return null;
    }

    /**
     * Parse CloudFlare geographic headers.
     * Headers: CF-IPCountry, CF-IPContinent, CF-IPCity, CF-Timezone
     */
    public GeoLocation parseCloudFlareHeaders(Map<String, String> headers) {
        String countryCode = getHeader(headers, "CF-IPCountry");
        if (countryCode == null || countryCode.equals("XX")) {
            return null;
        }

        return GeoLocation.builder()
                .countryCode(countryCode)
                .region(getHeader(headers, "CF-IPContinent"))
                .city(getHeader(headers, "CF-IPCity"))
                .timezone(getHeader(headers, "CF-Timezone"))
                .complianceZone(ComplianceZone.fromCountryCode(countryCode))
                .detectionSource("CDN_HEADER_CLOUDFLARE")
                .build();
    }

    /**
     * Parse AWS CloudFront geographic headers.
     * Headers: CloudFront-Viewer-Country, CloudFront-Viewer-Country-Region
     */
    public GeoLocation parseAWSHeaders(Map<String, String> headers) {
        String countryCode = getHeader(headers, "CloudFront-Viewer-Country");
        if (countryCode == null || countryCode.equals("XX")) {
            return null;
        }

        return GeoLocation.builder()
                .countryCode(countryCode)
                .region(getHeader(headers, "CloudFront-Viewer-Country-Region"))
                .complianceZone(ComplianceZone.fromCountryCode(countryCode))
                .detectionSource("CDN_HEADER_AWS")
                .build();
    }

    /**
     * Parse Azure CDN geographic headers.
     * Headers: X-Azure-FDID, X-Azure-ClientIP, X-Azure-Ref
     */
    public GeoLocation parseAzureHeaders(Map<String, String> headers) {
        // Azure CDN doesn't provide direct country headers, but we can try common patterns
        String countryCode = getHeader(headers, "X-MS-Country-Code");
        if (countryCode == null) {
            countryCode = getHeader(headers, "X-Country-Code");
        }
        
        if (countryCode == null || countryCode.equals("XX")) {
            return null;
        }

        return GeoLocation.builder()
                .countryCode(countryCode)
                .complianceZone(ComplianceZone.fromCountryCode(countryCode))
                .detectionSource("CDN_HEADER_AZURE")
                .build();
    }

    /**
     * Parse generic geographic headers that might be set by load balancers or proxies.
     * Headers: X-Country-Code, X-Region, X-City, X-Timezone
     */
    public GeoLocation parseGenericHeaders(Map<String, String> headers) {
        String countryCode = getHeader(headers, "X-Country-Code");
        if (countryCode == null) {
            countryCode = getHeader(headers, "X-Country");
        }
        if (countryCode == null) {
            countryCode = getHeader(headers, "X-GeoIP-Country");
        }
        
        if (countryCode == null || countryCode.equals("XX")) {
            return null;
        }

        return GeoLocation.builder()
                .countryCode(countryCode)
                .region(getHeader(headers, "X-Region"))
                .city(getHeader(headers, "X-City"))
                .timezone(getHeader(headers, "X-Timezone"))
                .complianceZone(ComplianceZone.fromCountryCode(countryCode))
                .detectionSource("CDN_HEADER_GENERIC")
                .build();
    }

    /**
     * Get header value in a case-insensitive manner.
     */
    private String getHeader(Map<String, String> headers, String headerName) {
        // Try exact match first
        String value = headers.get(headerName);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // Try case-insensitive match
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (headerName.equalsIgnoreCase(entry.getKey())) {
                String headerValue = entry.getValue();
                if (headerValue != null && !headerValue.trim().isEmpty()) {
                    return headerValue.trim();
                }
            }
        }

        return null;
    }

    /**
     * Check if the headers contain any recognizable CDN geographic information.
     */
    public boolean hasGeographicHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }

        // Check for CloudFlare headers
        if (getHeader(headers, "CF-IPCountry") != null) {
            return true;
        }

        // Check for AWS headers
        if (getHeader(headers, "CloudFront-Viewer-Country") != null) {
            return true;
        }

        // Check for Azure headers
        if (getHeader(headers, "X-MS-Country-Code") != null) {
            return true;
        }

        // Check for generic headers
        if (getHeader(headers, "X-Country-Code") != null || 
            getHeader(headers, "X-Country") != null ||
            getHeader(headers, "X-GeoIP-Country") != null) {
            return true;
        }

        return false;
    }
}