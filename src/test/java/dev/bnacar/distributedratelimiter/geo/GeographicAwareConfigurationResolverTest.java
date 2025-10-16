package dev.bnacar.distributedratelimiter.geo;

import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.ComplianceZone;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GeographicAwareConfigurationResolverTest {

    @Mock
    private RateLimiterConfiguration configuration;
    
    @Mock
    private GeographicConfigurationResolver geographicConfigResolver;
    
    private GeographicAwareConfigurationResolver resolver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new GeographicAwareConfigurationResolver(configuration, geographicConfigResolver);
        
        // Set up default configuration mocks
        when(configuration.getCapacity()).thenReturn(100);
        when(configuration.getRefillRate()).thenReturn(10);
        when(configuration.getCleanupIntervalMs()).thenReturn(60000L);
        when(configuration.getAlgorithm()).thenReturn(RateLimitAlgorithm.TOKEN_BUCKET);
        when(configuration.getKeys()).thenReturn(java.util.Collections.emptyMap());
        when(configuration.getPatterns()).thenReturn(java.util.Collections.emptyMap());
        
        RateLimitConfig defaultConfig = new RateLimitConfig(100, 10, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        when(configuration.getDefaultConfig()).thenReturn(defaultConfig);
    }

    @AfterEach
    void tearDown() {
        // Clear geographic context after each test
        GeographicAwareConfigurationResolver.clearGeographicContext();
    }

    @Test
    void testResolveConfig_WithoutGeographicContext() {
        // Given
        String key = "api:user:123";

        // When
        RateLimitConfig result = resolver.resolveConfig(key);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getCapacity());
        assertEquals(10, result.getRefillRate());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, result.getAlgorithm());
        
        // Should not try to resolve geographic configuration
        verify(geographicConfigResolver, never()).resolveGeographicConfig(anyString(), any());
    }

    @Test
    void testResolveConfig_WithGeographicContext_GeographicRuleFound() {
        // Given
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);
        
        // Set geographic context
        GeographicAwareConfigurationResolver.setGeographicContext(geoLocation);

        // When
        RateLimitConfig result = resolver.resolveConfig(key);

        // Then
        assertNotNull(result);
        assertEquals(500, result.getCapacity());
        assertEquals(50, result.getRefillRate());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, result.getAlgorithm());
        
        verify(geographicConfigResolver).resolveGeographicConfig(key, geoLocation);
    }

    @Test
    void testResolveConfig_WithGeographicContext_NoGeographicRule() {
        // Given
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();
        
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(null);
        
        // Set geographic context
        GeographicAwareConfigurationResolver.setGeographicContext(geoLocation);

        // When
        RateLimitConfig result = resolver.resolveConfig(key);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getCapacity());
        assertEquals(10, result.getRefillRate());
        assertEquals(RateLimitAlgorithm.TOKEN_BUCKET, result.getAlgorithm());
        
        verify(geographicConfigResolver).resolveGeographicConfig(key, geoLocation);
    }

    @Test
    void testResolveConfig_WithExplicitGeoLocation() {
        // Given
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("CA")
                .region("NA")
                .complianceZone(ComplianceZone.PIPEDA)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.SLIDING_WINDOW);
        
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);

        // When
        RateLimitConfig result = resolver.resolveConfig(key, geoLocation);

        // Then
        assertNotNull(result);
        assertEquals(1000, result.getCapacity());
        assertEquals(100, result.getRefillRate());
        assertEquals(RateLimitAlgorithm.SLIDING_WINDOW, result.getAlgorithm());
        
        verify(geographicConfigResolver).resolveGeographicConfig(key, geoLocation);
    }

    @Test
    void testResolveConfig_WithExplicitGeoLocation_Null() {
        // Given
        String key = "api:user:123";

        // When
        RateLimitConfig result = resolver.resolveConfig(key, null);

        // Then
        assertNotNull(result);
        assertEquals(100, result.getCapacity());
        assertEquals(10, result.getRefillRate());
        
        // Should not try to resolve geographic configuration
        verify(geographicConfigResolver, never()).resolveGeographicConfig(anyString(), any());
    }

    @Test
    void testGeographicCaching() {
        // Given
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);

        // When - resolve same key with same location multiple times
        RateLimitConfig result1 = resolver.resolveConfig(key, geoLocation);
        RateLimitConfig result2 = resolver.resolveConfig(key, geoLocation);

        // Then - should use cache on second call
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getCapacity(), result2.getCapacity());
        
        // Geographic resolver should only be called once due to caching
        verify(geographicConfigResolver, times(1)).resolveGeographicConfig(key, geoLocation);
    }

    @Test
    void testClearCache() {
        // Given
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);

        // Populate cache
        resolver.resolveConfig(key, geoLocation);
        
        // When
        resolver.clearCache();
        
        // Then - next call should hit the resolver again
        resolver.resolveConfig(key, geoLocation);
        
        verify(geographicConfigResolver, times(2)).resolveGeographicConfig(key, geoLocation);
    }

    @Test
    void testGetExtendedCacheStats() {
        // When
        java.util.Map<String, Object> stats = resolver.getExtendedCacheStats();

        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("standardCacheSize"));
        assertTrue(stats.containsKey("geographicCacheSize"));
        
        // Initial cache size should be 0
        assertEquals(0, stats.get("standardCacheSize"));
        assertEquals(0, stats.get("geographicCacheSize"));
    }

    @Test
    void testGetExtendedCacheStats_WithData() {
        // Given - populate cache
        String key = "api:user:123";
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("DE")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        RateLimitConfig geographicConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        when(geographicConfigResolver.resolveGeographicConfig(key, geoLocation)).thenReturn(geographicConfig);
        
        // Populate geographic cache
        resolver.resolveConfig(key, geoLocation);
        
        // Populate standard cache
        resolver.resolveConfig("another:key");

        // When
        java.util.Map<String, Object> stats = resolver.getExtendedCacheStats();

        // Then
        assertNotNull(stats);
        assertTrue((Integer) stats.get("standardCacheSize") >= 0);
        assertTrue((Integer) stats.get("geographicCacheSize") >= 0);
    }

    @Test
    void testSetAndGetGeographicContext() {
        // Given
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("FR")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();

        // Initially no context
        assertNull(GeographicAwareConfigurationResolver.getGeographicContext());

        // When
        GeographicAwareConfigurationResolver.setGeographicContext(geoLocation);

        // Then
        assertEquals(geoLocation, GeographicAwareConfigurationResolver.getGeographicContext());
    }

    @Test
    void testClearGeographicContext() {
        // Given
        GeoLocation geoLocation = GeoLocation.builder()
                .countryCode("FR")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        GeographicAwareConfigurationResolver.setGeographicContext(geoLocation);
        assertNotNull(GeographicAwareConfigurationResolver.getGeographicContext());

        // When
        GeographicAwareConfigurationResolver.clearGeographicContext();

        // Then
        assertNull(GeographicAwareConfigurationResolver.getGeographicContext());
    }

    @Test
    void testThreadLocalIsolation() {
        // Given
        GeoLocation geoLocation1 = GeoLocation.builder()
                .countryCode("DE")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        GeoLocation geoLocation2 = GeoLocation.builder()
                .countryCode("US")
                .complianceZone(ComplianceZone.NONE)
                .build();

        // Set context in main thread
        GeographicAwareConfigurationResolver.setGeographicContext(geoLocation1);
        assertEquals(geoLocation1, GeographicAwareConfigurationResolver.getGeographicContext());

        // Test that another thread has isolated context
        Thread testThread = new Thread(() -> {
            // Should not see the context from main thread
            assertNull(GeographicAwareConfigurationResolver.getGeographicContext());
            
            // Set different context in this thread
            GeographicAwareConfigurationResolver.setGeographicContext(geoLocation2);
            assertEquals(geoLocation2, GeographicAwareConfigurationResolver.getGeographicContext());
        });
        
        testThread.start();
        try {
            testThread.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        // Main thread should still have original context
        assertEquals(geoLocation1, GeographicAwareConfigurationResolver.getGeographicContext());
    }

    @Test
    void testGeographicContextPriority() {
        // Given
        String key = "api:user:123";
        
        GeoLocation contextLocation = GeoLocation.builder()
                .countryCode("DE")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        GeoLocation explicitLocation = GeoLocation.builder()
                .countryCode("US")
                .complianceZone(ComplianceZone.NONE)
                .build();
        
        RateLimitConfig contextConfig = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        RateLimitConfig explicitConfig = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geographicConfigResolver.resolveGeographicConfig(key, contextLocation)).thenReturn(contextConfig);
        when(geographicConfigResolver.resolveGeographicConfig(key, explicitLocation)).thenReturn(explicitConfig);
        
        // Set thread-local context
        GeographicAwareConfigurationResolver.setGeographicContext(contextLocation);

        // When - call with explicit location (should override context)
        RateLimitConfig result = resolver.resolveConfig(key, explicitLocation);

        // Then - should use explicit location, not context
        assertNotNull(result);
        assertEquals(1000, result.getCapacity());
        
        verify(geographicConfigResolver).resolveGeographicConfig(key, explicitLocation);
        verify(geographicConfigResolver, never()).resolveGeographicConfig(key, contextLocation);
    }

    @Test
    void testDifferentCacheKeysForDifferentLocations() {
        // Given
        String key = "api:user:123";
        
        GeoLocation location1 = GeoLocation.builder()
                .countryCode("DE")
                .region("EU")
                .complianceZone(ComplianceZone.GDPR)
                .build();
        
        GeoLocation location2 = GeoLocation.builder()
                .countryCode("US")
                .region("NA")
                .complianceZone(ComplianceZone.NONE)
                .build();
        
        RateLimitConfig config1 = new RateLimitConfig(500, 50, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        RateLimitConfig config2 = new RateLimitConfig(1000, 100, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        
        when(geographicConfigResolver.resolveGeographicConfig(key, location1)).thenReturn(config1);
        when(geographicConfigResolver.resolveGeographicConfig(key, location2)).thenReturn(config2);

        // When
        RateLimitConfig result1 = resolver.resolveConfig(key, location1);
        RateLimitConfig result2 = resolver.resolveConfig(key, location2);

        // Then - should get different configurations
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(500, result1.getCapacity());
        assertEquals(1000, result2.getCapacity());
        
        // Both calls should hit the resolver (different cache keys)
        verify(geographicConfigResolver).resolveGeographicConfig(key, location1);
        verify(geographicConfigResolver).resolveGeographicConfig(key, location2);
    }
}