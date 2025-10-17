package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeoLocationServiceTest {

    private GeoLocationService geoLocationService;
    private CDNHeaderParser cdnHeaderParser;

    @BeforeEach
    void setUp() {
        cdnHeaderParser = new CDNHeaderParser();
        geoLocationService = new GeoLocationService(cdnHeaderParser);
    }

    @Test
    void testDetectLocationFromCDNHeaders() {
        String sourceIP = "192.168.1.1";
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "US");
        headers.put("CF-IPContinent", "NA");
        headers.put("CF-IPCity", "New York");

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("US", location.getCountryCode());
        assertEquals("NA", location.getRegion());
        assertEquals("New York", location.getCity());
        assertEquals("CDN_HEADER_CLOUDFLARE", location.getDetectionSource());
    }

    @Test
    void testDetectLocationLocalIP() {
        String sourceIP = "127.0.0.1";
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("LOCAL", location.getCountryCode());
        assertEquals("Local Network", location.getCountryName());
        assertEquals("LOCAL", location.getRegion());
        assertEquals("IP_RANGE_LOCAL", location.getDetectionSource());
        assertEquals(ComplianceZone.NONE, location.getComplianceZone());
    }

    @Test
    void testDetectLocationPrivateIP() {
        String sourceIP = "192.168.1.100";
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("LOCAL", location.getCountryCode());
        assertEquals("IP_RANGE_LOCAL", location.getDetectionSource());
    }

    @Test
    void testDetectLocationNoIPProvided() {
        String sourceIP = null;
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("UNKNOWN", location.getCountryCode());
        assertEquals("Unknown", location.getCountryName());
        assertEquals("UNKNOWN", location.getDetectionSource());
    }

    @Test
    void testDetectLocationEmptyIP() {
        String sourceIP = "";
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("UNKNOWN", location.getCountryCode());
    }

    @Test
    void testDetectLocationIPv6Local() {
        String sourceIP = "::1";
        Map<String, String> headers = new HashMap<>();

        GeoLocation location = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location);
        assertEquals("LOCAL", location.getCountryCode());
        assertEquals("IP_RANGE_LOCAL", location.getDetectionSource());
    }

    @Test
    void testDetectLocationCaching() {
        String sourceIP = "192.168.1.1";
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "US");

        // First call
        GeoLocation location1 = geoLocationService.detectLocation(sourceIP, headers);
        
        // Second call with same parameters should return same result (from cache)
        GeoLocation location2 = geoLocationService.detectLocation(sourceIP, headers);

        assertNotNull(location1);
        assertNotNull(location2);
        assertEquals(location1.getCountryCode(), location2.getCountryCode());
        assertEquals(location1.getDetectionSource(), location2.getDetectionSource());
    }

    @Test
    void testClearCache() {
        String sourceIP = "192.168.1.1";
        Map<String, String> headers = new HashMap<>();
        headers.put("CF-IPCountry", "US");

        // Detect location to populate cache
        geoLocationService.detectLocation(sourceIP, headers);
        
        // Clear cache
        geoLocationService.clearCache();
        
        // Cache stats should show empty cache
        Map<String, Object> stats = geoLocationService.getCacheStats();
        assertEquals(0, stats.get("cacheSize"));
    }

    @Test
    void testGetCacheStats() {
        Map<String, Object> stats = geoLocationService.getCacheStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("cacheSize"));
        assertTrue(stats.containsKey("maxCacheSize"));
        assertEquals(10000, stats.get("maxCacheSize"));
    }

    @Test
    void testPrivateIPRanges() {
        // Test various private IP ranges
        assertTrue(isDetectedAsLocal("10.0.0.1"));
        assertTrue(isDetectedAsLocal("172.16.0.1"));
        assertTrue(isDetectedAsLocal("172.31.255.255"));
        assertTrue(isDetectedAsLocal("192.168.0.1"));
        assertTrue(isDetectedAsLocal("127.0.0.1"));
        
        // IPv6 local addresses
        assertTrue(isDetectedAsLocal("::1"));
        assertTrue(isDetectedAsLocal("fc00::1"));
        assertTrue(isDetectedAsLocal("fd00::1"));
    }

    private boolean isDetectedAsLocal(String ip) {
        GeoLocation location = geoLocationService.detectLocation(ip, new HashMap<>());
        return "LOCAL".equals(location.getCountryCode());
    }
}