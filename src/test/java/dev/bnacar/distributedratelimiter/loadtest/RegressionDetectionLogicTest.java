package dev.bnacar.distributedratelimiter.loadtest;

import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult;
import dev.bnacar.distributedratelimiter.monitoring.PerformanceRegressionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for performance regression detection logic.
 */
@ActiveProfiles("test")
class RegressionDetectionLogicTest {

    private PerformanceRegressionService regressionService;
    private String testBaselinePath;

    @BeforeEach
    void setUp() {
        // Use a test-specific file path
        testBaselinePath = "./target/test-performance-baselines.json";
        regressionService = new PerformanceRegressionService(testBaselinePath);
    }

    @AfterEach
    void tearDown() {
        // Clean up test file
        File testFile = new File(testBaselinePath);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    @DisplayName("Should detect no regression when no historical data exists")
    void testNoRegressionWithoutHistoricalData() {
        PerformanceBaseline newBaseline = new PerformanceBaseline(
            "NewTest", 100.0, 200.0, 1000.0, 95.0
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(newBaseline, null);

        assertFalse(result.isHasRegression());
        assertEquals(PerformanceRegressionResult.RegressionSeverity.NONE, result.getRegressionSeverity());
        assertNotNull(result.getRegressionDetails());
        assertTrue(result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("No historical data available")));
    }

    @Test
    @DisplayName("Should detect no regression when performance is stable")
    void testNoRegressionWithStablePerformance() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "StableTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with similar performance
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "StableTest", 105.0, 210.0, 980.0, 94.5
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, null);

        assertFalse(result.isHasRegression());
        assertEquals(PerformanceRegressionResult.RegressionSeverity.NONE, result.getRegressionSeverity());
        assertNotNull(result.getPreviousBaseline());
        assertEquals(historicalBaseline.getTestName(), result.getPreviousBaseline().getTestName());
    }

    @Test
    @DisplayName("Should detect response time regression")
    void testResponseTimeRegression() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "ResponseTimeTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with significantly worse response time (50% increase, above 20% threshold)
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "ResponseTimeTest", 150.0, 300.0, 1000.0, 95.0
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, null);

        assertTrue(result.isHasRegression());
        assertTrue(result.getRegressionSeverity().ordinal() > PerformanceRegressionResult.RegressionSeverity.NONE.ordinal());
        assertTrue(result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Response time increased")));
    }

    @Test
    @DisplayName("Should detect throughput regression")
    void testThroughputRegression() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "ThroughputTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with significantly lower throughput (30% decrease, above 15% threshold)
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "ThroughputTest", 100.0, 200.0, 700.0, 95.0
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, null);

        assertTrue(result.isHasRegression());
        assertTrue(result.getRegressionSeverity().ordinal() > PerformanceRegressionResult.RegressionSeverity.NONE.ordinal());
        assertTrue(result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Throughput decreased")));
    }

    @Test
    @DisplayName("Should detect success rate regression")
    void testSuccessRateRegression() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "SuccessRateTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with significantly lower success rate (10% decrease, above 5% threshold)
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "SuccessRateTest", 100.0, 200.0, 1000.0, 85.0
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, null);

        assertTrue(result.isHasRegression());
        assertTrue(result.getRegressionSeverity().ordinal() > PerformanceRegressionResult.RegressionSeverity.NONE.ordinal());
        assertTrue(result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Success rate decreased")));
    }

    @Test
    @DisplayName("Should support custom regression thresholds")
    void testCustomRegressionThresholds() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "CustomThresholdTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with custom thresholds
        PerformanceRegressionResult.RegressionThresholds customThresholds = 
            new PerformanceRegressionResult.RegressionThresholds(50.0, 30.0, 10.0);

        // Performance that would trigger default thresholds but not custom ones
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "CustomThresholdTest", 130.0, 260.0, 800.0, 90.0
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, customThresholds);

        // Should not trigger regression with relaxed thresholds
        assertFalse(result.isHasRegression());
        assertEquals(customThresholds.getResponseTimeThreshold(), result.getThresholds().getResponseTimeThreshold());
    }

    @Test
    @DisplayName("Should classify regression severity correctly")
    void testRegressionSeverityClassification() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "SeverityTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test minor regression (just above threshold)
        PerformanceBaseline minorRegression = new PerformanceBaseline(
            "SeverityTest", 125.0, 250.0, 1000.0, 95.0  // 25% response time increase
        );
        PerformanceRegressionResult minorResult = regressionService.analyzeRegression(minorRegression, null);
        assertEquals(PerformanceRegressionResult.RegressionSeverity.MINOR, minorResult.getRegressionSeverity());

        // Test major regression
        PerformanceBaseline majorRegression = new PerformanceBaseline(
            "SeverityTest", 300.0, 600.0, 1000.0, 95.0  // 200% response time increase
        );
        PerformanceRegressionResult majorResult = regressionService.analyzeRegression(majorRegression, null);
        assertTrue(majorResult.getRegressionSeverity().ordinal() >= PerformanceRegressionResult.RegressionSeverity.MAJOR.ordinal());
    }

    @Test
    @DisplayName("Should handle multiple regression types in single analysis")
    void testMultipleRegressionTypes() {
        // Store historical baseline
        PerformanceBaseline historicalBaseline = new PerformanceBaseline(
            "MultipleTest", 100.0, 200.0, 1000.0, 95.0
        );
        historicalBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(historicalBaseline);

        // Test with multiple regressions
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "MultipleTest", 200.0, 400.0, 600.0, 80.0  // All metrics regressed
        );

        PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, null);

        assertTrue(result.isHasRegression());
        assertEquals(3, result.getRegressionDetails().size()); // Should detect all three types
        
        boolean hasResponseTimeRegression = result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Response time increased"));
        boolean hasThroughputRegression = result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Throughput decreased"));
        boolean hasSuccessRateRegression = result.getRegressionDetails().stream()
            .anyMatch(detail -> detail.contains("Success rate decreased"));

        assertTrue(hasResponseTimeRegression);
        assertTrue(hasThroughputRegression);
        assertTrue(hasSuccessRateRegression);
    }

    @Test
    @DisplayName("Should store and analyze baseline in single operation")
    void testStoreAndAnalyze() {
        // Store initial baseline
        PerformanceBaseline initialBaseline = new PerformanceBaseline(
            "StoreAnalyzeTest", 100.0, 200.0, 1000.0, 95.0
        );
        initialBaseline.setTimestamp(LocalDateTime.now().minusDays(1));
        regressionService.storeBaseline(initialBaseline);

        // Test store and analyze operation
        PerformanceBaseline newBaseline = new PerformanceBaseline(
            "StoreAnalyzeTest", 150.0, 300.0, 1000.0, 95.0
        );

        // First analyze (should detect regression)
        PerformanceRegressionResult result = regressionService.analyzeRegression(newBaseline, null);
        assertTrue(result.isHasRegression());

        // Then store for future comparisons
        regressionService.storeBaseline(newBaseline);

        // Verify the baseline was stored by testing against it
        PerformanceBaseline thirdBaseline = new PerformanceBaseline(
            "StoreAnalyzeTest", 155.0, 310.0, 1000.0, 95.0
        );

        PerformanceRegressionResult secondResult = regressionService.analyzeRegression(thirdBaseline, null);
        assertEquals(newBaseline.getAverageResponseTime(), secondResult.getPreviousBaseline().getAverageResponseTime());
    }
}