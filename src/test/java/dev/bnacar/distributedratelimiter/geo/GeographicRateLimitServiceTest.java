package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.*;
import dev.bnacar.distributedratelimiter.ratelimit.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeographicRateLimitServiceTest {

    @Mock
    private GeoLocationService geoLocationService;
    
    @Mock
    private GeographicConfigurationResolver geographicConfigResolver;
    
    @Mock
    private RateLimiterService rateLimiterService;
    
    @Mock
    private ConfigurationResolver standardConfigResolver;
    
    @Mock
    private GeographicAwareConfigurationResolver geoAwareConfigResolver;
    
    private GeographicRateLimitService geographicRateLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        geographicRateLimitService = new GeographicRateLimitService(
            geoLocationService,
            geographicConfigResolver,
            rateLimiterService,
            standardConfigResolver,
            geoAwareConfigResolver
        );
    }

    @Test
    void testCheckGeographicRateLimit_WithGeographicRule() {
        // Given
        String key = "api:user:123";
        String sourceIP = "92.168.1.100";
        int tokens = 1;
        Map<String, String> headers = Map.of("CF-IPCountry", "DE");
        
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .countryName("Germany")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .detectionSource("CDN_HEADER_CLOUDFLARE")
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(geoLocation);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);
        when(rateLimiterService.isAllowed(anyString(), eq(tokens))).thenReturn(true);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        assertNotNull(response);
        assertTrue(response.isAllowed());
        assertEquals(key, response.getKey());
        assertEquals(tokens, response.getTokensRequested());
        assertEquals(geoLocation, response.getGeoLocation());
        assertEquals("geo:DE:GDPR", response.getAppliedRule());
        assertEquals(geographicConfig, response.getAppliedLimits());
        assertNull(response.getFallbackReason());
        
        verify(geoLocationService).detectLocation(sourceIP, headers);
        verify(geographicConfigResolver).resolveGeographicConfig(key, geoLocation);
        verify(rateLimiterService).isAllowed(argThat(k -> k.startsWith("geo:DE:GDPR:")), eq(tokens));
    }

    @Test
    void testCheckGeographicRateLimit_NoGeographicLocation() {
        // Given
        String key = "api:user:123";
        String sourceIP = "192.168.1.1";
        int tokens = 1;
        Map<String, String> headers = new HashMap<>();
        
        RateLimitConfig standardConfig = new RateLimitConfig(100, 10, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(null);
        when(standardConfigResolver.resolveConfig(key)).thenReturn(standardConfig);
        when(rateLimiterService.isAllowed(key, tokens)).thenReturn(true);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        assertNotNull(response);
        assertTrue(response.isAllowed());
        assertEquals(key, response.getKey());
        assertNull(response.getGeoLocation());
        assertEquals("standard", response.getAppliedRule());
        assertEquals("NO_GEOGRAPHIC_LOCATION_DETECTED", response.getFallbackReason());
        
        verify(geoLocationService).detectLocation(sourceIP, headers);
        verify(standardConfigResolver).resolveConfig(key);
        verify(rateLimiterService).isAllowed(key, tokens);
    }

    @Test
    void testCheckGeographicRateLimit_NoGeographicRules() {
        // Given
        String key = "api:user:123";
        String sourceIP = "73.162.1.100";
        int tokens = 1;
        Map<String, String> headers = Map.of("CloudFront-Viewer-Country", "US");
        
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("US")
                .countryName("United States")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .detectionSource("CDN_HEADER_AWS")
                .build();
        
        RateLimitConfig standardConfig = new RateLimitConfig(100, 10, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(geoLocation);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(null);
        when(standardConfigResolver.resolveConfig(key)).thenReturn(standardConfig);
        when(rateLimiterService.isAllowed(key, tokens)).thenReturn(false);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        assertNotNull(response);
        assertFalse(response.isAllowed());
        assertEquals(key, response.getKey());
        assertEquals(geoLocation, response.getGeoLocation());
        assertEquals("standard", response.getAppliedRule());
        assertEquals("NO_GEOGRAPHIC_RULES_APPLY", response.getFallbackReason());
        
        verify(geoLocationService).detectLocation(sourceIP, headers);
        verify(geographicConfigResolver).resolveGeographicConfig(key, geoLocation);
        verify(standardConfigResolver).resolveConfig(key);
        verify(rateLimiterService).isAllowed(key, tokens);
    }

    @Test
    void testCheckGeographicRateLimit_RateLimitExceeded() {
        // Given
        String key = "api:user:123";
        String sourceIP = "142.168.1.100";
        int tokens = 1;
        Map<String, String> headers = Map.of("X-Country-Code", "CA");
        
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("CA")
                .countryName("Canada")
                .region("NA")
                .complianceZone(ComplianceZone.PIPEDA)
                .detectionSource("CDN_HEADER_GENERIC")
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(geoLocation);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);
        when(rateLimiterService.isAllowed(anyString(), eq(tokens))).thenReturn(false);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        assertNotNull(response);
        assertFalse(response.isAllowed());
        assertEquals(key, response.getKey());
        assertEquals(tokens, response.getTokensRequested());
        assertEquals(geoLocation, response.getGeoLocation());
        assertEquals("geo:CA:PIPEDA", response.getAppliedRule());
        assertEquals(geographicConfig, response.getAppliedLimits());
        assertNull(response.getFallbackReason());
    }

    @Test
    void testCheckGeographicRateLimit_WithRateLimitRequest() {
        // Given
        RateLimitRequest request = new RateLimitRequest("api:user:456", 2);
        String sourceIP = "192.168.1.1";
        Map<String, String> headers = new HashMap<>();
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(null);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(request, sourceIP, headers);

        // Then
        assertNotNull(response);
        assertEquals("api:user:456", response.getKey());
        assertEquals(2, response.getTokensRequested());
        
        verify(geoLocationService).detectLocation(sourceIP, headers);
    }

    @Test
    void testCheckGeographicRateLimit_WithClientInfoOverride() {
        // Given
        RateLimitRequest.ClientInfo clientInfo = new RateLimitRequest.ClientInfo();
        clientInfo.setSourceIP("203.0.113.1");
        Map<String, String> clientHeaders = Map.of("CF-IPCountry", "FR");
        clientInfo.setHeaders(clientHeaders);
        
        RateLimitRequest request = new RateLimitRequest("api:user:789", 1);
        request.setClientInfo(clientInfo);
        
        String originalSourceIP = "192.168.1.1";
        Map<String, String> originalHeaders = Map.of("X-Country-Code", "US");
        
        when(geoLocationService.detectLocation(eq("203.0.113.1"), any())).thenReturn(null);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(request, originalSourceIP, originalHeaders);

        // Then
        assertNotNull(response);
        assertEquals("api:user:789", response.getKey());
        
        // Should use the overridden source IP and merged headers
        verify(geoLocationService).detectLocation(eq("203.0.113.1"), argThat(headers -> 
            headers.containsKey("CF-IPCountry") && 
            headers.containsKey("X-Country-Code") &&
            "FR".equals(headers.get("CF-IPCountry")) &&
            "US".equals(headers.get("X-Country-Code")) // Original headers should be preserved
        ));
    }

    @Test
    void testCheckGeographicRateLimit_ErrorHandling() {
        // Given
        String key = "api:user:123";
        String sourceIP = "invalid-ip";
        int tokens = 1;
        Map<String, String> headers = new HashMap<>();
        
        RateLimitConfig standardConfig = new RateLimitConfig(100, 10, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenThrow(new RuntimeException("Geolocation error"));
        when(standardConfigResolver.resolveConfig(key)).thenReturn(standardConfig);
        when(rateLimiterService.isAllowed(key, tokens)).thenReturn(true);

        // When
        GeographicRateLimitResponse response = geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        assertNotNull(response);
        assertTrue(response.isAllowed());
        assertEquals(key, response.getKey());
        assertNull(response.getGeoLocation());
        assertEquals("standard", response.getAppliedRule());
        assertEquals("ERROR_IN_GEOGRAPHIC_PROCESSING", response.getFallbackReason());
        
        verify(geoLocationService).detectLocation(sourceIP, headers);
        verify(standardConfigResolver).resolveConfig(key);
        verify(rateLimiterService).isAllowed(key, tokens);
    }

    @Test
    void testDetectLocation() {
        // Given
        String sourceIP = "192.168.1.1";
        Map<String, String> headers = Map.of("CF-IPCountry", "US");
        
        GeoLocation expectedLocation = GeoLocation.builder()
                .countryCode("US")
                .detectionSource("CDN_HEADER_CLOUDFLARE")
                .build();
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(expectedLocation);

        // When
        GeoLocation result = geographicRateLimitService.detectLocation(sourceIP, headers);

        // Then
        assertEquals(expectedLocation, result);
        verify(geoLocationService).detectLocation(sourceIP, headers);
    }

    @Test
    void testHasGeographicRules() {
        // Given
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        when(geographicConfigResolver.getRulesForLocation(geoLocation)).thenReturn(
            java.util.Arrays.asList(new GeographicRateLimitConfig())
        );

        // When
        boolean hasRules = geographicRateLimitService.hasGeographicRules(geoLocation);

        // Then
        assertTrue(hasRules);
        verify(geographicConfigResolver).getRulesForLocation(geoLocation);
    }

    @Test
    void testHasGeographicRules_Empty() {
        // Given
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("XX")
                .complianceZone(ComplianceZone.NONE)
                .build();
        
        when(geographicConfigResolver.getRulesForLocation(geoLocation)).thenReturn(java.util.Collections.emptyList());

        // When
        boolean hasRules = geographicRateLimitService.hasGeographicRules(geoLocation);

        // Then
        assertFalse(hasRules);
        verify(geographicConfigResolver).getRulesForLocation(geoLocation);
    }

    @Test
    void testGetGeographicStats() {
        // Given
        Map<String, Object> geoLocationStats = Map.of("cacheSize", 100, "maxCacheSize", 10000);
        Map<String, Object> geographicConfigStats = Map.of("cacheSize", 50, "rulesCount", 5);
        
        when(geoLocationService.getCacheStats()).thenReturn(geoLocationStats);
        when(geographicConfigResolver.getCacheStats()).thenReturn(geographicConfigStats);

        // When
        Map<String, Object> stats = geographicRateLimitService.getGeographicStats();

        // Then
        assertNotNull(stats);
        assertEquals(geoLocationStats, stats.get("geoLocationCache"));
        assertEquals(geographicConfigStats, stats.get("geographicConfig"));
        
        verify(geoLocationService).getCacheStats();
        verify(geographicConfigResolver).getCacheStats();
    }

    @Test
    void testCreateGeographicKey() {
        // Test the geographic key creation logic indirectly through the service call
        // Given
        String key = "api:user:123";
        String sourceIP = "92.168.1.100";
        int tokens = 1;
        Map<String, String> headers = Map.of("CF-IPCountry", "DE");
        
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geoLocationService.detectLocation(sourceIP, headers)).thenReturn(geoLocation);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);
        when(rateLimiterService.isAllowed(anyString(), eq(tokens))).thenReturn(true);

        // When
        geographicRateLimitService.checkGeographicRateLimit(key, sourceIP, tokens, headers);

        // Then
        // Verify that the geographic key is created properly (starts with geo: and contains country and compliance zone)
        verify(rateLimiterService).isAllowed(argThat(geographicKey -> 
            geographicKey.startsWith("geo:DE:GDPR:") &&
            geographicKey.endsWith(":" + key)
        ), eq(tokens));
    }
}