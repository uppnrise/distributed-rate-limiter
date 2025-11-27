package dev.bnacar.distributedratelimiter.adaptive;

import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveRateLimitEngineTest {

    @Mock
    private TrafficPatternAnalyzer trafficAnalyzer;

    @Mock
    private SystemMetricsCollector metricsCollector;

    @Mock
    private UserBehaviorModeler behaviorModeler;

    @Mock
    private AnomalyDetector anomalyDetector;

    @Mock
    private AdaptiveMLModel adaptiveModel;

    @Mock
    private ConfigurationResolver configurationResolver;

    private AdaptiveRateLimitEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AdaptiveRateLimitEngine(
            trafficAnalyzer,
            metricsCollector,
            behaviorModeler,
            anomalyDetector,
            adaptiveModel,
            configurationResolver,
            true,        // enabled
            300000L,     // evaluation interval
            0.7,         // min confidence
            2.0,         // max adjustment factor
            10,          // min capacity
            100000       // max capacity
        );
    }

    @Test
    void testRecordTrafficEvent_WhenEnabled() {
        // Act
        engine.recordTrafficEvent("test:key", 1, true);

        // Assert
        verify(trafficAnalyzer).recordTrafficEvent("test:key", 1, true);
        verify(behaviorModeler).recordRequest("test:key", 1, true);
        verify(anomalyDetector).recordTrafficRate("test:key", 1);
    }

    @Test
    void testRecordTrafficEvent_WhenDisabled() {
        // Create a disabled engine
        AdaptiveRateLimitEngine disabledEngine = new AdaptiveRateLimitEngine(
            trafficAnalyzer,
            metricsCollector,
            behaviorModeler,
            anomalyDetector,
            adaptiveModel,
            configurationResolver,
            false,       // disabled
            300000L,
            0.7,
            2.0,
            10,
            100000
        );

        // Act
        disabledEngine.recordTrafficEvent("test:key", 1, true);

        // Assert - no interactions with analyzers
        verifyNoInteractions(trafficAnalyzer, behaviorModeler, anomalyDetector);
    }

    @Test
    void testGetStatus_NoAdaptedLimits() {
        // Arrange
        RateLimitConfig config = new RateLimitConfig(100, 10);
        when(configurationResolver.resolveConfig("test:key")).thenReturn(config);

        // Act
        AdaptiveRateLimitEngine.AdaptiveStatusInfo status = engine.getStatus("test:key");

        // Assert
        assertEquals("STATIC", status.mode);
        assertEquals(0.0, status.confidence);
        assertEquals(100, status.originalCapacity);
        assertEquals(10, status.originalRefillRate);
    }

    @Test
    void testSetOverride() {
        // Act
        AdaptiveRateLimitEngine.AdaptationOverride override = 
            new AdaptiveRateLimitEngine.AdaptationOverride(500, 50, "Test override");
        engine.setOverride("test:key", override);

        // Assert - getting adapted limits should return null (override is separate)
        assertNull(engine.getAdaptedLimits("test:key"));
    }

    @Test
    void testRemoveOverride() {
        // Arrange
        AdaptiveRateLimitEngine.AdaptationOverride override = 
            new AdaptiveRateLimitEngine.AdaptationOverride(500, 50, "Test override");
        engine.setOverride("test:key", override);

        // Act
        engine.removeOverride("test:key");

        // Assert - no exception thrown
        RateLimitConfig config = new RateLimitConfig(100, 10);
        when(configurationResolver.resolveConfig("test:key")).thenReturn(config);
        AdaptiveRateLimitEngine.AdaptiveStatusInfo status = engine.getStatus("test:key");
        assertEquals("STATIC", status.mode);
    }

    @Test
    void testEvaluateAdaptations_WhenDisabled() {
        // Create a disabled engine
        AdaptiveRateLimitEngine disabledEngine = new AdaptiveRateLimitEngine(
            trafficAnalyzer,
            metricsCollector,
            behaviorModeler,
            anomalyDetector,
            adaptiveModel,
            configurationResolver,
            false,       // disabled
            300000L,
            0.7,
            2.0,
            10,
            100000
        );

        // Act
        disabledEngine.evaluateAdaptations();

        // Assert - no interactions
        verifyNoInteractions(metricsCollector, trafficAnalyzer, behaviorModeler, anomalyDetector, adaptiveModel);
    }

    @Test
    void testAdaptedLimitsClass() {
        // Test the AdaptedLimits inner class
        java.time.Instant now = java.time.Instant.now();
        java.util.Map<String, String> reasoning = new java.util.HashMap<>();
        reasoning.put("test", "value");

        AdaptiveRateLimitEngine.AdaptedLimits limits = new AdaptiveRateLimitEngine.AdaptedLimits(
            100, 10, 150, 15, now, reasoning, 0.85
        );

        assertEquals(100, limits.originalCapacity);
        assertEquals(10, limits.originalRefillRate);
        assertEquals(150, limits.adaptedCapacity);
        assertEquals(15, limits.adaptedRefillRate);
        assertEquals(now, limits.timestamp);
        assertEquals(reasoning, limits.reasoning);
        assertEquals(0.85, limits.confidence);
    }

    @Test
    void testAdaptationOverrideClass() {
        AdaptiveRateLimitEngine.AdaptationOverride override = 
            new AdaptiveRateLimitEngine.AdaptationOverride(500, 50, "Test reason");

        assertEquals(500, override.capacity);
        assertEquals(50, override.refillRate);
        assertEquals("Test reason", override.reason);
    }
}
