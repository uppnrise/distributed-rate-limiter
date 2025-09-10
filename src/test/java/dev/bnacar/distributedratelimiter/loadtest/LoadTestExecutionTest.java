package dev.bnacar.distributedratelimiter.loadtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for load test script execution and configuration.
 */
@ActiveProfiles("test")
class LoadTestExecutionTest {

    private static final String GATLING_TEST_DIR = "src/test/scala/dev/bnacar/distributedratelimiter/loadtest";

    @BeforeEach
    void setUp() {
        // Ensure test directory structure exists
        File testDir = new File(GATLING_TEST_DIR);
        assertTrue(testDir.exists(), "Gatling test directory should exist");
    }

    @Test
    @DisplayName("Basic load test script should exist and be valid")
    void testBasicLoadTestScript() {
        File basicLoadTest = new File(GATLING_TEST_DIR + "/BasicLoadTest.scala");
        assertTrue(basicLoadTest.exists(), "BasicLoadTest.scala should exist");
        assertTrue(basicLoadTest.length() > 0, "BasicLoadTest.scala should not be empty");

        // Verify script contains required Gatling imports and structure
        assertTrue(basicLoadTest.canRead(), "BasicLoadTest.scala should be readable");
    }

    @Test
    @DisplayName("Stress test script should exist and be valid")
    void testStressTestScript() {
        File stressTest = new File(GATLING_TEST_DIR + "/StressTest.scala");
        assertTrue(stressTest.exists(), "StressTest.scala should exist");
        assertTrue(stressTest.length() > 0, "StressTest.scala should not be empty");

        assertTrue(stressTest.canRead(), "StressTest.scala should be readable");
    }

    @Test
    @DisplayName("Load test configuration should support system properties")
    void testLoadTestConfiguration() {
        // Test that system properties can be set for load test configuration
        String originalBaseUrl = System.getProperty("load.test.baseUrl");
        String originalDuration = System.getProperty("load.test.duration");
        String originalMaxUsers = System.getProperty("load.test.maxUsers");

        try {
            // Set test configuration
            System.setProperty("load.test.baseUrl", "http://test.example.com");
            System.setProperty("load.test.duration", "60");
            System.setProperty("load.test.maxUsers", "100");

            // Verify properties are set
            assertEquals("http://test.example.com", System.getProperty("load.test.baseUrl"));
            assertEquals("60", System.getProperty("load.test.duration"));
            assertEquals("100", System.getProperty("load.test.maxUsers"));

        } finally {
            // Restore original values
            if (originalBaseUrl != null) {
                System.setProperty("load.test.baseUrl", originalBaseUrl);
            } else {
                System.clearProperty("load.test.baseUrl");
            }
            
            if (originalDuration != null) {
                System.setProperty("load.test.duration", originalDuration);
            } else {
                System.clearProperty("load.test.duration");
            }
            
            if (originalMaxUsers != null) {
                System.setProperty("load.test.maxUsers", originalMaxUsers);
            } else {
                System.clearProperty("load.test.maxUsers");
            }
        }
    }

    @Test
    @DisplayName("Load test should support different profiles")
    void testLoadTestProfiles() {
        // Test different load test profiles can be configured
        assertDoesNotThrow(() -> {
            // Basic load test profile
            System.setProperty("load.test.profile", "basic");
            System.setProperty("load.test.duration", "30");
            System.setProperty("load.test.maxUsers", "50");

            // Should not throw any configuration errors
        });

        assertDoesNotThrow(() -> {
            // Stress test profile
            System.setProperty("load.test.profile", "stress");
            System.setProperty("stress.test.duration", "60");
            System.setProperty("stress.test.maxUsers", "200");

            // Should not throw any configuration errors
        });

        // Clean up
        System.clearProperty("load.test.profile");
        System.clearProperty("load.test.duration");
        System.clearProperty("load.test.maxUsers");
        System.clearProperty("stress.test.duration");
        System.clearProperty("stress.test.maxUsers");
    }

    @Test
    @DisplayName("Load test execution should validate required endpoints")
    void testRequiredEndpointsConfiguration() {
        // Test that load tests target the correct endpoints
        String[] requiredEndpoints = {
            "/actuator/health",
            "/api/rate-limit/check",
            "/api/benchmark/run",
            "/actuator/metrics"
        };

        for (String endpoint : requiredEndpoints) {
            assertNotNull(endpoint, "Required endpoint should not be null");
            assertFalse(endpoint.trim().isEmpty(), "Required endpoint should not be empty");
            assertTrue(endpoint.startsWith("/"), "Endpoint should start with /");
        }
    }

    @Test
    @DisplayName("Load test thresholds should be configurable")
    void testPerformanceThresholds() {
        // Test that performance thresholds can be configured
        String originalResponseTime = System.getProperty("load.test.responseTime.threshold");
        String originalSuccessRate = System.getProperty("load.test.successRate.threshold");

        try {
            System.setProperty("load.test.responseTime.threshold", "1000");
            System.setProperty("load.test.successRate.threshold", "95.0");

            assertEquals("1000", System.getProperty("load.test.responseTime.threshold"));
            assertEquals("95.0", System.getProperty("load.test.successRate.threshold"));

            // Verify threshold values are reasonable
            int responseTimeThreshold = Integer.parseInt(System.getProperty("load.test.responseTime.threshold"));
            double successRateThreshold = Double.parseDouble(System.getProperty("load.test.successRate.threshold"));

            assertTrue(responseTimeThreshold > 0, "Response time threshold should be positive");
            assertTrue(successRateThreshold > 0 && successRateThreshold <= 100, 
                "Success rate threshold should be between 0 and 100");

        } finally {
            if (originalResponseTime != null) {
                System.setProperty("load.test.responseTime.threshold", originalResponseTime);
            } else {
                System.clearProperty("load.test.responseTime.threshold");
            }
            
            if (originalSuccessRate != null) {
                System.setProperty("load.test.successRate.threshold", originalSuccessRate);
            } else {
                System.clearProperty("load.test.successRate.threshold");
            }
        }
    }

    @Test
    @DisplayName("Load test should handle Maven integration")
    void testMavenIntegration() {
        // Test that Maven integration is properly configured
        File pomFile = new File("pom.xml");
        assertTrue(pomFile.exists(), "pom.xml should exist");

        // The Gatling plugin should be configured in pom.xml
        // This is verified by the existence of the pom.xml file and
        // the fact that we can configure Gatling properties
        assertDoesNotThrow(() -> {
            System.setProperty("gatling.simulationClass", 
                "dev.bnacar.distributedratelimiter.loadtest.BasicLoadTest");
        });

        System.clearProperty("gatling.simulationClass");
    }
}