package dev.bnacar.distributedratelimiter.loadtest;

import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for performance metrics collection during load tests.
 */
class PerformanceMetricsCollectionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Performance baseline should capture all required metrics")
    void testPerformanceBaselineMetrics() {
        // Create a performance baseline with all required metrics
        PerformanceBaseline baseline = new PerformanceBaseline(
            "BasicLoadTest",
            250.5,  // averageResponseTime
            500.0,  // maxResponseTime
            1000.0, // throughputPerSecond
            95.5    // successRate
        );

        // Verify all metrics are captured
        assertEquals("BasicLoadTest", baseline.getTestName());
        assertEquals(250.5, baseline.getAverageResponseTime());
        assertEquals(500.0, baseline.getMaxResponseTime());
        assertEquals(1000.0, baseline.getThroughputPerSecond());
        assertEquals(95.5, baseline.getSuccessRate());
        assertNotNull(baseline.getTimestamp());
    }

    @Test
    @DisplayName("Performance baseline should support additional metadata")
    void testPerformanceBaselineMetadata() {
        PerformanceBaseline baseline = new PerformanceBaseline(
            "StressTest",
            300.0, 750.0, 800.0, 92.0,
            "abc123", "build-456", "CI"
        );

        assertEquals("abc123", baseline.getCommitHash());
        assertEquals("build-456", baseline.getBuildNumber());
        assertEquals("CI", baseline.getEnvironment());
    }

    @Test
    @DisplayName("Performance baseline should serialize to JSON correctly")
    void testPerformanceBaselineSerialization() throws Exception {
        PerformanceBaseline baseline = new PerformanceBaseline(
            "SerializationTest",
            200.0, 400.0, 1200.0, 98.0
        );
        baseline.setCommitHash("def456");
        baseline.setEnvironment("test");

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(baseline);
        assertNotNull(json);
        assertTrue(json.contains("SerializationTest"));
        assertTrue(json.contains("200.0"));
        assertTrue(json.contains("def456"));

        // Deserialize from JSON
        PerformanceBaseline deserialized = objectMapper.readValue(json, PerformanceBaseline.class);
        assertEquals(baseline.getTestName(), deserialized.getTestName());
        assertEquals(baseline.getAverageResponseTime(), deserialized.getAverageResponseTime());
        assertEquals(baseline.getCommitHash(), deserialized.getCommitHash());
    }

    @Test
    @DisplayName("Performance metrics should validate reasonable values")
    void testPerformanceMetricsValidation() {
        // Test valid metrics
        PerformanceBaseline validBaseline = new PerformanceBaseline(
            "ValidTest", 100.0, 200.0, 1000.0, 95.0
        );
        
        assertTrue(validBaseline.getAverageResponseTime() > 0);
        assertTrue(validBaseline.getMaxResponseTime() >= validBaseline.getAverageResponseTime());
        assertTrue(validBaseline.getThroughputPerSecond() > 0);
        assertTrue(validBaseline.getSuccessRate() >= 0 && validBaseline.getSuccessRate() <= 100);

        // Test edge cases
        PerformanceBaseline edgeCase = new PerformanceBaseline(
            "EdgeTest", 0.1, 0.1, 0.1, 0.0
        );
        
        assertTrue(edgeCase.getAverageResponseTime() >= 0);
        assertTrue(edgeCase.getThroughputPerSecond() >= 0);
        assertTrue(edgeCase.getSuccessRate() >= 0);
    }

    @Test
    @DisplayName("Performance metrics should handle timestamp correctly")
    void testPerformanceMetricsTimestamp() {
        LocalDateTime beforeCreation = LocalDateTime.now();
        PerformanceBaseline baseline = new PerformanceBaseline(
            "TimestampTest", 150.0, 300.0, 900.0, 96.0
        );
        LocalDateTime afterCreation = LocalDateTime.now();

        assertNotNull(baseline.getTimestamp());
        assertTrue(baseline.getTimestamp().isAfter(beforeCreation.minusSeconds(1)));
        assertTrue(baseline.getTimestamp().isBefore(afterCreation.plusSeconds(1)));

        // Test manual timestamp setting
        LocalDateTime customTimestamp = LocalDateTime.of(2023, 1, 1, 12, 0);
        baseline.setTimestamp(customTimestamp);
        assertEquals(customTimestamp, baseline.getTimestamp());
    }

    @Test
    @DisplayName("Performance metrics should support toString formatting")
    void testPerformanceMetricsToString() {
        PerformanceBaseline baseline = new PerformanceBaseline(
            "ToStringTest", 175.5, 350.0, 1100.0, 97.5
        );
        baseline.setCommitHash("xyz789");

        String result = baseline.toString();
        assertNotNull(result);
        assertTrue(result.contains("ToStringTest"));
        // Support both dot and comma decimal separators (locale-dependent)
        assertTrue(result.contains("175.50ms") || result.contains("175,50ms"));
        assertTrue(result.contains("350.00ms") || result.contains("350,00ms"));
        assertTrue(result.contains("1100.00 req/sec") || result.contains("1100,00 req/sec"));
        assertTrue(result.contains("97.50%") || result.contains("97,50%"));
        assertTrue(result.contains("xyz789"));
    }

    @Test
    @DisplayName("Performance metrics collection should handle concurrent updates")
    void testConcurrentMetricsCollection() throws InterruptedException {
        final int threadCount = 10;
        final Thread[] threads = new Thread[threadCount];
        final PerformanceBaseline[] baselines = new PerformanceBaseline[threadCount];

        // Create multiple baselines concurrently
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                baselines[index] = new PerformanceBaseline(
                    "ConcurrentTest" + index,
                    100.0 + index,
                    200.0 + index,
                    1000.0 + index,
                    90.0 + index
                );
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all baselines were created correctly
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(baselines[i]);
            assertEquals("ConcurrentTest" + i, baselines[i].getTestName());
            assertEquals(100.0 + i, baselines[i].getAverageResponseTime());
        }
    }

    @Test
    @DisplayName("Performance metrics should support comparison operations")
    void testPerformanceMetricsComparison() {
        PerformanceBaseline baseline1 = new PerformanceBaseline(
            "CompareTest", 100.0, 200.0, 1000.0, 95.0
        );
        
        PerformanceBaseline baseline2 = new PerformanceBaseline(
            "CompareTest", 150.0, 250.0, 800.0, 90.0
        );

        // Compare metrics
        assertTrue(baseline1.getAverageResponseTime() < baseline2.getAverageResponseTime());
        assertTrue(baseline1.getThroughputPerSecond() > baseline2.getThroughputPerSecond());
        assertTrue(baseline1.getSuccessRate() > baseline2.getSuccessRate());

        // Calculate percentage differences
        double responseTimeIncrease = ((baseline2.getAverageResponseTime() - baseline1.getAverageResponseTime()) 
            / baseline1.getAverageResponseTime()) * 100;
        assertEquals(50.0, responseTimeIncrease, 0.01);

        double throughputDecrease = ((baseline1.getThroughputPerSecond() - baseline2.getThroughputPerSecond()) 
            / baseline1.getThroughputPerSecond()) * 100;
        assertEquals(20.0, throughputDecrease, 0.01);
    }
}