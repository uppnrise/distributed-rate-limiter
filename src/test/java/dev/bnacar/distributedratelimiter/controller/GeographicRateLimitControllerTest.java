package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.geo.GeographicConfigurationResolver;
import dev.bnacar.distributedratelimiter.geo.GeoLocationService;
import dev.bnacar.distributedratelimiter.models.*;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeographicRateLimitControllerTest {

    @Mock
    private GeographicConfigurationResolver geographicConfigResolver;
    
    @Mock
    private GeoLocationService geoLocationService;
    
    private GeographicRateLimitController controller;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new GeographicRateLimitController(geographicConfigResolver, geoLocationService);
    }

    @Test
    void testGetAllRules() {
        // Given
        List<GeographicRateLimitConfig> expectedRules = Arrays.asList(
            createTestRule("rule1", "US"),
            createTestRule("rule2", "EU")
        );
        when(geographicConfigResolver.getAllGeographicRules()).thenReturn(expectedRules);

        // When
        ResponseEntity<List<GeographicRateLimitConfig>> response = controller.getAllRules();

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(2, response.getBody().size());
        verify(geographicConfigResolver).getAllGeographicRules();
    }

    @Test
    void testAddRule_Success() {
        // Given
        GeographicRateLimitConfig rule = createTestRule("test-rule", "US");
        doNothing().when(geographicConfigResolver).addGeographicRule(any(GeographicRateLimitConfig.class));

        // When
        ResponseEntity<String> response = controller.addRule(rule);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("test-rule"));
        verify(geographicConfigResolver).addGeographicRule(rule);
    }

    @Test
    void testAddRule_Failure() {
        // Given
        GeographicRateLimitConfig rule = createTestRule("invalid-rule", "US");
        doThrow(new RuntimeException("Invalid rule")).when(geographicConfigResolver).addGeographicRule(any());

        // When
        ResponseEntity<String> response = controller.addRule(rule);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Failed to add rule"));
    }

    @Test
    void testRemoveRule_Success() {
        // Given
        when(geographicConfigResolver.removeGeographicRule("rule-1")).thenReturn(true);

        // When
        ResponseEntity<String> response = controller.removeRule("rule-1");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("rule-1"));
        verify(geographicConfigResolver).removeGeographicRule("rule-1");
    }

    @Test
    void testRemoveRule_NotFound() {
        // Given
        when(geographicConfigResolver.removeGeographicRule("nonexistent")).thenReturn(false);

        // When
        ResponseEntity<String> response = controller.removeRule("nonexistent");

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testDetectLocation_WithSourceIP() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-IPCountry", "US");
        request.setRemoteAddr("192.168.1.1");
        
        GeoLocation mockLocation = GeoLocation.builder()
                .countryCode("US")
                .countryName("United States")
                .complianceZone(ComplianceZone.NONE)
                .detectionSource("CDN_HEADER_CLOUDFLARE")
                .build();
        
        List<GeographicRateLimitConfig> mockRules = Arrays.asList(createTestRule("us-rule", "US"));
        
        when(geoLocationService.detectLocation(eq("192.168.1.1"), any())).thenReturn(mockLocation);
        when(geographicConfigResolver.getRulesForLocation(mockLocation)).thenReturn(mockRules);

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("192.168.1.1", body.get("sourceIP"));
        assertEquals(mockLocation, body.get("geoLocation"));
        assertEquals(1, body.get("applicableRules"));
        
        verify(geoLocationService).detectLocation(eq("192.168.1.1"), any());
        verify(geographicConfigResolver).getRulesForLocation(mockLocation);
    }

    @Test
    void testDetectLocation_WithOverrideIP() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        String overrideIP = "73.162.1.100";
        
        GeoLocation mockLocation = GeoLocation.builder()
                .countryCode("US")
                .countryName("United States")
                .complianceZone(ComplianceZone.NONE)
                .detectionSource("IP_RANGE")
                .build();
        
        when(geoLocationService.detectLocation(eq(overrideIP), any())).thenReturn(mockLocation);
        when(geographicConfigResolver.getRulesForLocation(mockLocation)).thenReturn(new ArrayList<>());

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, overrideIP);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(overrideIP, body.get("sourceIP"));
        assertEquals(0, body.get("applicableRules"));
        
        verify(geoLocationService).detectLocation(eq(overrideIP), any());
    }

    @Test
    void testDetectLocation_NullLocation() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        
        when(geoLocationService.detectLocation(eq("127.0.0.1"), any())).thenReturn(null);

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("127.0.0.1", body.get("sourceIP"));
        assertNull(body.get("geoLocation"));
        assertFalse(body.containsKey("applicableRules"));
    }

    @Test
    void testDetectLocation_Error() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        
        when(geoLocationService.detectLocation(anyString(), any())).thenThrow(new RuntimeException("Detection failed"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        assertEquals(400, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").toString().contains("Detection failed"));
    }

    @Test
    void testGetStatistics() {
        // Given
        Map<String, Object> geoLocationStats = Map.of("cacheSize", 100, "maxCacheSize", 10000);
        Map<String, Object> configStats = Map.of("cacheSize", 50, "rulesCount", 5);
        
        when(geoLocationService.getCacheStats()).thenReturn(geoLocationStats);
        when(geographicConfigResolver.getCacheStats()).thenReturn(configStats);
        when(geographicConfigResolver.getAllGeographicRules()).thenReturn(Arrays.asList(
            createTestRule("rule1", "US"),
            createTestRule("rule2", "DE"),
            createTestRule("rule3", "CA")
        ));

        // When
        ResponseEntity<Map<String, Object>> response = controller.getStatistics();

        // Then
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(geoLocationStats, body.get("geoLocationCache"));
        assertEquals(configStats, body.get("geographicConfigCache"));
        assertEquals(3, body.get("totalRules"));
    }

    @Test
    void testClearCaches() {
        // Given
        doNothing().when(geoLocationService).clearCache();
        doNothing().when(geographicConfigResolver).clearCache();

        // When
        ResponseEntity<String> response = controller.clearCaches();

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("cleared successfully"));
        verify(geoLocationService).clearCache();
        verify(geographicConfigResolver).clearCache();
    }

    @Test
    void testExtractClientIP_XForwardedFor() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        request.setRemoteAddr("10.0.0.1");

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        // The method should extract the first IP from X-Forwarded-For
        verify(geoLocationService).detectLocation(eq("203.0.113.1"), any());
    }

    @Test
    void testExtractClientIP_XRealIP() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.2");
        request.setRemoteAddr("10.0.0.1");

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        // The method should use X-Real-IP when X-Forwarded-For is not present
        verify(geoLocationService).detectLocation(eq("203.0.113.2"), any());
    }

    @Test
    void testExtractClientIP_RemoteAddr() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        // When
        ResponseEntity<Map<String, Object>> response = controller.detectLocation(request, null);

        // Then
        // Should fall back to remote address when no proxy headers
        verify(geoLocationService).detectLocation(eq("10.0.0.1"), any());
    }

    @Test
    void testExtractGeographicHeaders() {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-IPCountry", "US");
        request.addHeader("CF-IPContinent", "NA");
        request.addHeader("CloudFront-Viewer-Country", "CA");
        request.addHeader("X-Country-Code", "MX");
        request.setRemoteAddr("192.168.1.1");

        // When
        controller.detectLocation(request, null);

        // Then
        // Verify that geographic headers are extracted and passed to geo service
        verify(geoLocationService).detectLocation(eq("192.168.1.1"), argThat(headers -> 
            headers.containsKey("CF-IPCountry") &&
            headers.containsKey("CF-IPContinent") &&
            headers.containsKey("CloudFront-Viewer-Country") &&
            headers.containsKey("X-Country-Code") &&
            "US".equals(headers.get("CF-IPCountry")) &&
            "NA".equals(headers.get("CF-IPContinent")) &&
            "CA".equals(headers.get("CloudFront-Viewer-Country")) &&
            "MX".equals(headers.get("X-Country-Code"))
        ));
    }

    private GeographicRateLimitConfig createTestRule(String name, String countryCode) {
        RateLimitConfig limits = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        GeographicRateLimitConfig rule = new GeographicRateLimitConfig(name, "api:*", countryCode, limits);
        rule.setId(name + "-id");
        return rule;
    }
}