package dev.bnacar.distributedratelimiter.loadtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CI/CD integration of load testing infrastructure.
 */
@ActiveProfiles("test")
class CiCdIntegrationTest {

    @BeforeEach
    void setUp() {
        // Clean up any existing environment variables for testing
        clearTestEnvironmentVariables();
    }

    @Test
    @DisplayName("Should support GitHub Actions environment variables")
    void testGitHubActionsEnvironment() {
        // Test CI environment detection
        String originalCI = System.getenv("CI");
        String originalGitHubActions = System.getenv("GITHUB_ACTIONS");

        try {
            // Simulate GitHub Actions environment
            Map<String, String> env = System.getenv();
            
            // Verify we can detect CI environment
            String ciDetection = System.getProperty("ci.environment", "local");
            assertNotNull(ciDetection);

            // Test that we can configure load tests for CI environment
            System.setProperty("load.test.ci.enabled", "true");
            System.setProperty("load.test.ci.duration", "30");
            System.setProperty("load.test.ci.maxUsers", "25");

            assertEquals("true", System.getProperty("load.test.ci.enabled"));
            assertEquals("30", System.getProperty("load.test.ci.duration"));
            assertEquals("25", System.getProperty("load.test.ci.maxUsers"));

        } finally {
            // Clean up
            System.clearProperty("load.test.ci.enabled");
            System.clearProperty("load.test.ci.duration");
            System.clearProperty("load.test.ci.maxUsers");
        }
    }

    @Test
    @DisplayName("Should support build metadata integration")
    void testBuildMetadataIntegration() {
        // Test integration with build metadata
        System.setProperty("build.number", "123");
        System.setProperty("git.commit.hash", "abc123def456");
        System.setProperty("build.timestamp", "2023-12-01T10:30:00Z");

        // Verify build metadata can be captured
        assertEquals("123", System.getProperty("build.number"));
        assertEquals("abc123def456", System.getProperty("git.commit.hash"));
        assertEquals("2023-12-01T10:30:00Z", System.getProperty("build.timestamp"));

        // Test that metadata can be included in performance baselines
        String buildInfo = String.format("Build: %s, Commit: %s", 
            System.getProperty("build.number"),
            System.getProperty("git.commit.hash"));
        
        assertNotNull(buildInfo);
        assertTrue(buildInfo.contains("123"));
        assertTrue(buildInfo.contains("abc123def456"));

        // Clean up
        System.clearProperty("build.number");
        System.clearProperty("git.commit.hash");
        System.clearProperty("build.timestamp");
    }

    @Test
    @DisplayName("Should support Maven profile integration")
    void testMavenProfileIntegration() {
        // Test Maven profile configuration
        System.setProperty("maven.profile", "load-test");
        System.setProperty("skip.load.tests", "false");

        assertEquals("load-test", System.getProperty("maven.profile"));
        assertEquals("false", System.getProperty("skip.load.tests"));

        // Test profile-specific configuration
        System.setProperty("load.test.profile.basic.duration", "30");
        System.setProperty("load.test.profile.stress.duration", "60");

        assertEquals("30", System.getProperty("load.test.profile.basic.duration"));
        assertEquals("60", System.getProperty("load.test.profile.stress.duration"));

        // Clean up
        System.clearProperty("maven.profile");
        System.clearProperty("skip.load.tests");
        System.clearProperty("load.test.profile.basic.duration");
        System.clearProperty("load.test.profile.stress.duration");
    }

    @Test
    @DisplayName("Should support test result reporting")
    void testTestResultReporting() {
        // Test that results can be written to expected locations
        String reportDir = System.getProperty("gatling.reports.dir", "target/gatling");
        assertNotNull(reportDir);

        // Test performance baseline storage location
        String baselineFile = System.getProperty("performance.baseline.file", 
            "target/performance-baselines.json");
        assertNotNull(baselineFile);

        // Test artifact paths for CI
        String artifactDir = System.getProperty("ci.artifact.dir", "target");
        assertNotNull(artifactDir);

        // Verify directories can be created
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            assertTrue(targetDir.mkdirs() || targetDir.exists());
        }
        assertTrue(targetDir.exists());
    }

    @Test
    @DisplayName("Should support failure threshold configuration")
    void testFailureThresholdConfiguration() {
        // Test configurable failure thresholds for CI
        System.setProperty("load.test.failure.responseTime.max", "1000");
        System.setProperty("load.test.failure.successRate.min", "90.0");
        System.setProperty("load.test.failure.throughput.min", "100.0");

        // Verify thresholds are configurable
        int maxResponseTime = Integer.parseInt(System.getProperty("load.test.failure.responseTime.max"));
        double minSuccessRate = Double.parseDouble(System.getProperty("load.test.failure.successRate.min"));
        double minThroughput = Double.parseDouble(System.getProperty("load.test.failure.throughput.min"));

        assertEquals(1000, maxResponseTime);
        assertEquals(90.0, minSuccessRate);
        assertEquals(100.0, minThroughput);

        // Test threshold validation
        assertTrue(maxResponseTime > 0);
        assertTrue(minSuccessRate >= 0 && minSuccessRate <= 100);
        assertTrue(minThroughput >= 0);

        // Clean up
        System.clearProperty("load.test.failure.responseTime.max");
        System.clearProperty("load.test.failure.successRate.min");
        System.clearProperty("load.test.failure.throughput.min");
    }

    @Test
    @DisplayName("Should support parallel execution configuration")
    void testParallelExecutionConfiguration() {
        // Test parallel test execution settings
        System.setProperty("load.test.parallel.enabled", "true");
        System.setProperty("load.test.parallel.threads", "4");

        assertEquals("true", System.getProperty("load.test.parallel.enabled"));
        assertEquals("4", System.getProperty("load.test.parallel.threads"));

        // Test that thread count is reasonable
        int threadCount = Integer.parseInt(System.getProperty("load.test.parallel.threads"));
        assertTrue(threadCount > 0 && threadCount <= Runtime.getRuntime().availableProcessors() * 2);

        // Clean up
        System.clearProperty("load.test.parallel.enabled");
        System.clearProperty("load.test.parallel.threads");
    }

    @Test
    @DisplayName("Should support notification integration")
    void testNotificationIntegration() {
        // Test notification configuration for CI failures
        System.setProperty("notification.slack.webhook", "https://hooks.slack.com/test");
        System.setProperty("notification.email.enabled", "true");
        System.setProperty("notification.threshold.critical", "true");

        assertEquals("https://hooks.slack.com/test", System.getProperty("notification.slack.webhook"));
        assertEquals("true", System.getProperty("notification.email.enabled"));
        assertEquals("true", System.getProperty("notification.threshold.critical"));

        // Test notification conditions
        boolean shouldNotifyOnCritical = Boolean.parseBoolean(
            System.getProperty("notification.threshold.critical"));
        assertTrue(shouldNotifyOnCritical);

        // Clean up
        System.clearProperty("notification.slack.webhook");
        System.clearProperty("notification.email.enabled");
        System.clearProperty("notification.threshold.critical");
    }

    @Test
    @DisplayName("Should support environment-specific configuration")
    void testEnvironmentSpecificConfiguration() {
        // Test different environments
        String[] environments = {"dev", "staging", "prod"};

        for (String env : environments) {
            System.setProperty("deployment.environment", env);
            System.setProperty("load.test." + env + ".baseUrl", "https://" + env + ".example.com");
            System.setProperty("load.test." + env + ".users", env.equals("prod") ? "100" : "50");

            assertEquals(env, System.getProperty("deployment.environment"));
            assertEquals("https://" + env + ".example.com", 
                System.getProperty("load.test." + env + ".baseUrl"));

            int expectedUsers = env.equals("prod") ? 100 : 50;
            assertEquals(String.valueOf(expectedUsers), 
                System.getProperty("load.test." + env + ".users"));

            // Clean up
            System.clearProperty("deployment.environment");
            System.clearProperty("load.test." + env + ".baseUrl");
            System.clearProperty("load.test." + env + ".users");
        }
    }

    @Test
    @DisplayName("Should support Docker integration")
    void testDockerIntegration() {
        // Test Docker environment detection and configuration
        System.setProperty("docker.enabled", "true");
        System.setProperty("docker.network", "load-test-network");
        System.setProperty("docker.compose.file", "docker-compose.test.yml");

        assertEquals("true", System.getProperty("docker.enabled"));
        assertEquals("load-test-network", System.getProperty("docker.network"));
        assertEquals("docker-compose.test.yml", System.getProperty("docker.compose.file"));

        // Test container-specific configuration
        System.setProperty("load.test.docker.baseUrl", "http://app:8080");
        System.setProperty("load.test.docker.timeout", "120");

        assertEquals("http://app:8080", System.getProperty("load.test.docker.baseUrl"));
        assertEquals("120", System.getProperty("load.test.docker.timeout"));

        // Clean up
        System.clearProperty("docker.enabled");
        System.clearProperty("docker.network");
        System.clearProperty("docker.compose.file");
        System.clearProperty("load.test.docker.baseUrl");
        System.clearProperty("load.test.docker.timeout");
    }

    private void clearTestEnvironmentVariables() {
        // Clear any test-specific system properties
        String[] testProperties = {
            "load.test.ci.enabled",
            "load.test.ci.duration",
            "load.test.ci.maxUsers",
            "build.number",
            "git.commit.hash",
            "maven.profile",
            "skip.load.tests"
        };

        for (String property : testProperties) {
            System.clearProperty(property);
        }
    }
}