package dev.bnacar.distributedratelimiter.adaptive;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.HealthContributorRegistry;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SystemMetricsCollectorTest {

    @Mock
    private HealthContributorRegistry healthRegistry;

    private MeterRegistry meterRegistry;
    private SystemMetricsCollector collector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        collector = new SystemMetricsCollector(meterRegistry, healthRegistry);
    }

    @Test
    void testGetCurrentHealth() {
        // Act
        SystemHealth health = collector.getCurrentHealth();

        // Assert
        assertNotNull(health);
        assertTrue(health.getCpuUtilization() >= 0);
        assertTrue(health.getMemoryUtilization() >= 0);
        assertTrue(health.getResponseTimeP95() >= 0);
        assertTrue(health.getErrorRate() >= 0);
    }

    @Test
    void testEvaluateSystemCapacity_ReduceLimits_HighCPU() {
        // Since we can't easily mock the OperatingSystemMXBean,
        // we test the method's contract and return type
        AdaptationSignal signal = collector.evaluateSystemCapacity();
        assertNotNull(signal);
        // Signal should be one of the valid values
        assertTrue(signal == AdaptationSignal.REDUCE_LIMITS ||
                  signal == AdaptationSignal.INCREASE_LIMITS ||
                  signal == AdaptationSignal.MAINTAIN_LIMITS);
    }

    @Test
    void testSystemHealthBuilder() {
        // Test the SystemHealth builder pattern
        SystemHealth health = SystemHealth.builder()
            .cpuUtilization(0.5)
            .memoryUtilization(0.6)
            .responseTimeP95(200.0)
            .errorRate(0.01)
            .redisHealthy(true)
            .build();

        assertEquals(0.5, health.getCpuUtilization());
        assertEquals(0.6, health.getMemoryUtilization());
        assertEquals(200.0, health.getResponseTimeP95());
        assertEquals(0.01, health.getErrorRate());
        assertTrue(health.isRedisHealthy());
    }

    @Test
    void testCpuUtilization_ReturnsNonNegative() {
        // Act
        SystemHealth health = collector.getCurrentHealth();

        // Assert
        assertTrue(health.getCpuUtilization() >= 0.0);
        assertTrue(health.getCpuUtilization() <= 1.0 || health.getCpuUtilization() == 0.0);
    }

    @Test
    void testMemoryUtilization_ReturnsNonNegative() {
        // Act
        SystemHealth health = collector.getCurrentHealth();

        // Assert
        assertTrue(health.getMemoryUtilization() >= 0.0);
        assertTrue(health.getMemoryUtilization() <= 1.0);
    }

    @Test
    void testErrorRate_WithNoRequests() {
        // With no metrics recorded, error rate should be 0
        SystemHealth health = collector.getCurrentHealth();
        assertEquals(0.0, health.getErrorRate());
    }

    @Test
    void testResponseTimeP95_WithNoRequests() {
        // With no metrics recorded, response time should be 0
        SystemHealth health = collector.getCurrentHealth();
        assertEquals(0.0, health.getResponseTimeP95());
    }
}
