package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CDNHeaderParserTest {

    private CDNHeaderParser cdnHeaderParser;

    @BeforeEach
    void setUp() {
        cdnHeaderParser = new CDNHeaderParser();
    }

    @Test
    void testParseCloudFlareHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "US");
        headers.put("CF-IPContinent", "NA");
        headers.put("CF-IPCity", "New York");
        headers.put("CF-Timezone", "America/New_York");

        GeoLocation location = cdnHeaderParser.parseCloudFlareHeaders(headers);

        assertNotNull(location);
        assertEquals("US", location.getCountryCode());
        assertEquals("NA", location.getRegion());
        assertEquals("New York", location.getCity());
        assertEquals("America/New_York", location.getTimezone());
        assertEquals("CDN_HEADER_CLOUDFLARE", location.getDetectionSource());
        assertEquals(ComplianceZone.NONE, location.getComplianceZone());
    }

    @Test
    void testParseCloudFlareHeadersWithGDPRCountry() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "DE");
        headers.put("CF-IPContinent", "EU");
        headers.put("CF-IPCity", "Berlin");

        GeoLocation location = cdnHeaderParser.parseCloudFlareHeaders(headers);

        assertNotNull(location);
        assertEquals("DE", location.getCountryCode());
        assertEquals("EU", location.getRegion());
        assertEquals("Berlin", location.getCity());
        assertEquals(ComplianceZone.GDPR, location.getComplianceZone());
    }

    @Test
    void testParseAWSHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CloudFront-Viewer-Country", "CA");
        headers.put("CloudFront-Viewer-Country-Region", "ON");

        GeoLocation location = cdnHeaderParser.parseAWSHeaders(headers);

        assertNotNull(location);
        assertEquals("CA", location.getCountryCode());
        assertEquals("ON", location.getRegion());
        assertEquals(ComplianceZone.PIPEDA, location.getComplianceZone());
        assertEquals("CDN_HEADER_AWS", location.getDetectionSource());
    }

    @Test
    void testParseGenericHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Country-Code", "BR");
        headers.put("X-Region", "SA");
        headers.put("X-City", "São Paulo");

        GeoLocation location = cdnHeaderParser.parseGenericHeaders(headers);

        assertNotNull(location);
        assertEquals("BR", location.getCountryCode());
        assertEquals("SA", location.getRegion());
        assertEquals("São Paulo", location.getCity());
        assertEquals(ComplianceZone.LGPD, location.getComplianceZone());
        assertEquals("CDN_HEADER_GENERIC", location.getDetectionSource());
    }

    @Test
    void testParseHeadersWithPriority() {
        Map<String, String> headers = new HashMap<>();
        // Add multiple CDN headers - CloudFlare should take priority
        headers.put("CF-IPCountry", "US");
        headers.put("CloudFront-Viewer-Country", "CA");
        headers.put("X-Country-Code", "MX");

        GeoLocation location = cdnHeaderParser.parseHeaders(headers);

        assertNotNull(location);
        assertEquals("US", location.getCountryCode());
        assertEquals("CDN_HEADER_CLOUDFLARE", location.getDetectionSource());
    }

    @Test
    void testParseHeadersNoCountry() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "XX"); // Unknown country code
        headers.put("CF-IPCity", "Unknown");

        GeoLocation location = cdnHeaderParser.parseHeaders(headers);

        assertNull(location);
    }

    @Test
    void testParseHeadersEmpty() {
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = cdnHeaderParser.parseHeaders(headers);

        assertNull(location);
    }

    @Test
    void testParseHeadersNull() {
        GeoLocation location = cdnHeaderParser.parseHeaders(null);

        assertNull(location);
    }

    @Test
    void testHasGeographicHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "US");

        assertTrue(cdnHeaderParser.hasGeographicHeaders(headers));

        headers.clear();
        headers.put("CloudFront-Viewer-Country", "CA");

        assertTrue(cdnHeaderParser.hasGeographicHeaders(headers));

        headers.clear();
        headers.put("X-Country-Code", "MX");

        assertTrue(cdnHeaderParser.hasGeographicHeaders(headers));

        headers.clear();
        headers.put("Some-Other-Header", "value");

        assertFalse(cdnHeaderParser.hasGeographicHeaders(headers));
    }

    @Test
    void testCaseInsensitiveHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("cf-ipcountry", "US"); // lowercase
        headers.put("CLOUDFRONT-VIEWER-COUNTRY", "CA"); // uppercase
        headers.put("x-Country-Code", "MX"); // mixed case

        // CloudFlare should still work with lowercase
        GeoLocation cfLocation = cdnHeaderParser.parseCloudFlareHeaders(headers);
        assertNotNull(cfLocation);
        assertEquals("US", cfLocation.getCountryCode());

        // AWS should work with uppercase
        GeoLocation awsLocation = cdnHeaderParser.parseAWSHeaders(headers);
        assertNotNull(awsLocation);
        assertEquals("CA", awsLocation.getCountryCode());

        // Generic should work with mixed case
        GeoLocation genericLocation = cdnHeaderParser.parseGenericHeaders(headers);
        assertNotNull(genericLocation);
        assertEquals("MX", genericLocation.getCountryCode());
    }
}