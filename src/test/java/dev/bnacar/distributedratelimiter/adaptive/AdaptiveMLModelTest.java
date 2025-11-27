package dev.bnacar.distributedratelimiter.adaptive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdaptiveMLModelTest {

    private AdaptiveMLModel model;

    @BeforeEach
    void setUp() {
        model = new AdaptiveMLModel();
    }

    @Test
    void testPredict_SystemUnderStress_HighCPU() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.85)  // Above 80% threshold
            .memoryUtilization(0.5)
            .responseTimeP95(500)
            .errorRate(0.01)
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertTrue(decision.getRecommendedCapacity() < 1000);  // Should reduce
        assertEquals(0.85, decision.getConfidence());
    }

    @Test
    void testPredict_SystemUnderStress_HighResponseTime() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.5)
            .memoryUtilization(0.5)
            .responseTimeP95(2500)  // Above 2000ms threshold
            .errorRate(0.01)
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertTrue(decision.getRecommendedCapacity() < 1000);  // Should reduce
    }

    @Test
    void testPredict_CriticalAnomaly() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.5)
            .memoryUtilization(0.5)
            .responseTimeP95(500)
            .errorRate(0.01)
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = AnomalyScore.builder()
            .isAnomaly(true)
            .severity("CRITICAL")
            .type("SPIKE")
            .confidence(0.9)
            .zScore(7.0)
            .build();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertTrue(decision.getRecommendedCapacity() <= 600);  // Should reduce by 40%
        assertEquals(0.9, decision.getConfidence());
    }

    @Test
    void testPredict_HighAnomaly() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.5)
            .memoryUtilization(0.5)
            .responseTimeP95(500)
            .errorRate(0.001)
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = AnomalyScore.builder()
            .isAnomaly(true)
            .severity("HIGH")
            .type("SUSTAINED_HIGH")
            .confidence(0.8)
            .zScore(5.0)
            .build();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertTrue(decision.getRecommendedCapacity() <= 800);  // Should reduce by 20%
    }

    @Test
    void testPredict_SystemHasCapacity() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.2)  // Low CPU
            .memoryUtilization(0.3)
            .responseTimeP95(100)
            .errorRate(0.0005)  // Very low error rate
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertTrue(decision.getRecommendedCapacity() > 1000);  // Should increase
        assertEquals(0.75, decision.getConfidence());
    }

    @Test
    void testPredict_ModerateLoad() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.4)  // Moderate CPU
            .memoryUtilization(0.4)
            .responseTimeP95(200)
            .errorRate(0.002)  // Low error rate
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertTrue(decision.shouldAdapt());
        assertEquals(1100, decision.getRecommendedCapacity());  // Should increase by 10%
    }

    @Test
    void testPredict_NoAdaptationNeeded() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.6)  // Normal load
            .memoryUtilization(0.5)
            .responseTimeP95(800)
            .errorRate(0.01)  // Higher error rate prevents increase
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertFalse(decision.shouldAdapt());
        assertNotNull(decision.getReasoning());
    }

    @Test
    void testPredict_IncludesReasoning() {
        // Arrange
        TrafficPattern pattern = createDefaultTrafficPattern();
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.85)
            .memoryUtilization(0.5)
            .responseTimeP95(500)
            .errorRate(0.01)
            .redisHealthy(true)
            .build();
        UserBehavior behavior = createDefaultUserBehavior();
        AnomalyScore anomaly = createNoAnomaly();

        // Act
        AdaptationDecision decision = model.predict(pattern, health, behavior, anomaly, 1000, 100);

        // Assert
        assertNotNull(decision.getReasoning());
        assertTrue(decision.getReasoning().containsKey("decision"));
        assertTrue(decision.getReasoning().containsKey("systemMetrics"));
    }

    private TrafficPattern createDefaultTrafficPattern() {
        return TrafficPattern.builder()
            .key("test:key")
            .trend(new TrafficPattern.TrendInfo("STABLE", 0.0, 0.7))
            .volatility(new TrafficPattern.VolatilityMetrics(1.0, 0.5, "MEDIUM"))
            .confidence(0.8)
            .build();
    }

    private UserBehavior createDefaultUserBehavior() {
        return UserBehavior.builder()
            .averageRequestRate(10.0)
            .burstiness(0.5)
            .sessionDuration(300)
            .timeOfDayPattern(new UserBehavior.TimeOfDayPattern(14, 3, 2.0))
            .anomalyScore(0.0)
            .build();
    }

    private AnomalyScore createNoAnomaly() {
        return AnomalyScore.builder()
            .isAnomaly(false)
            .severity("NONE")
            .type("NONE")
            .confidence(0.0)
            .zScore(0.0)
            .build();
    }
}
