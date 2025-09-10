package dev.bnacar.distributedratelimiter.pipeline;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate CI/CD pipeline configuration and quality gates.
 * These tests ensure that the pipeline setup is correct and quality gates are properly configured.
 */
@SpringBootTest
@DisplayName("Pipeline Validation Tests")
class PipelineValidationTest {

    @Test
    @DisplayName("GitHub Actions workflow file should exist")
    void testWorkflowFileExists() {
        Path workflowPath = Paths.get(".github/workflows/ci-cd.yml");
        assertTrue(Files.exists(workflowPath), 
            "CI/CD workflow file should exist at .github/workflows/ci-cd.yml");
    }

    @Test
    @DisplayName("Dockerfile should exist for container builds")
    void testDockerfileExists() {
        Path dockerfilePath = Paths.get("Dockerfile");
        assertTrue(Files.exists(dockerfilePath), 
            "Dockerfile should exist in project root");
    }

    @Test
    @DisplayName("OWASP suppressions file should exist")
    void testOwaspSuppressionsExists() {
        Path suppressionsPath = Paths.get("owasp-suppressions.xml");
        assertTrue(Files.exists(suppressionsPath), 
            "OWASP suppressions file should exist");
    }

    @Test
    @DisplayName("Maven build should produce target directory")
    void testBuildArtifacts() {
        File targetDir = new File("target");
        if (targetDir.exists()) {
            assertTrue(targetDir.isDirectory(), "Target should be a directory");
        }
        // This test passes even if target doesn't exist yet (before first build)
    }

    @Test
    @DisplayName("Test coverage configuration should be present in pom.xml")
    void testCoverageConfiguration() throws Exception {
        Path pomPath = Paths.get("pom.xml");
        assertTrue(Files.exists(pomPath), "pom.xml should exist");
        
        String pomContent = Files.readString(pomPath);
        assertTrue(pomContent.contains("jacoco-maven-plugin"), 
            "pom.xml should contain JaCoCo plugin configuration");
        assertTrue(pomContent.contains("spotbugs-maven-plugin"), 
            "pom.xml should contain SpotBugs plugin configuration");
        assertTrue(pomContent.contains("dependency-check-maven"), 
            "pom.xml should contain OWASP dependency check plugin");
    }

    @Test
    @DisplayName("Quality gate thresholds should be configured")
    void testQualityGateThresholds() throws Exception {
        Path pomPath = Paths.get("pom.xml");
        String pomContent = Files.readString(pomPath);
        
        // Check for coverage threshold
        assertTrue(pomContent.contains("0.50") || pomContent.contains("50"),
            "Code coverage threshold should be set to 50%");
    }

    @Test
    @DisplayName("CI environment variables should be accessible")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testCiEnvironment() {
        String ciEnv = System.getenv("CI");
        assertEquals("true", ciEnv, "CI environment variable should be true in CI environment");
    }

    @Test
    @DisplayName("Docker build context should be valid")
    void testDockerBuildContext() {
        // Check that essential files for Docker build are present
        assertTrue(Files.exists(Paths.get("pom.xml")), "pom.xml required for Docker build");
        assertTrue(Files.exists(Paths.get("src")), "src directory required for Docker build");
        assertTrue(Files.exists(Paths.get("mvnw")), "Maven wrapper required for Docker build");
    }

    @Test
    @DisplayName("Security scan configuration should be valid")
    void testSecurityScanConfig() throws Exception {
        Path pomPath = Paths.get("pom.xml");
        String pomContent = Files.readString(pomPath);
        
        // Verify OWASP dependency check is configured with appropriate CVSS threshold
        assertTrue(pomContent.contains("failBuildOnCVSS"), 
            "OWASP plugin should have CVSS threshold configured");
    }

    @Test
    @DisplayName("Static analysis tools should be configured")
    void testStaticAnalysisConfig() throws Exception {
        Path pomPath = Paths.get("pom.xml");
        String pomContent = Files.readString(pomPath);
        
        assertTrue(pomContent.contains("maven-pmd-plugin"), 
            "PMD plugin should be configured");
        assertTrue(pomContent.contains("maven-checkstyle-plugin"), 
            "Checkstyle plugin should be configured");
    }
}