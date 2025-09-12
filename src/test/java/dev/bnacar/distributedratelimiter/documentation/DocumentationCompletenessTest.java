package dev.bnacar.distributedratelimiter.documentation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate that documentation examples exist and are complete.
 */
class DocumentationCompletenessTest {

    @Test
    void testExampleDocumentationExists() {
        // Test that all required example files exist
        String[] requiredExamples = {
            "docs/examples/README.md",
            "docs/examples/java-client.md",
            "docs/examples/python-client.md", 
            "docs/examples/nodejs-client.md",
            "docs/examples/curl-examples.md"
        };
        
        for (String exampleFile : requiredExamples) {
            Path path = Paths.get(exampleFile);
            assertThat(Files.exists(path))
                .as("Example file %s should exist", exampleFile)
                .isTrue();
            
            assertThat(Files.isReadable(path))
                .as("Example file %s should be readable", exampleFile)
                .isTrue();
        }
    }

    @Test
    void testAdrDocumentationExists() {
        // Test that ADR documentation exists
        String[] requiredAdrFiles = {
            "docs/adr/README.md",
            "docs/adr/001-token-bucket-algorithm.md",
            "docs/adr/002-redis-distributed-state.md"
        };
        
        for (String adrFile : requiredAdrFiles) {
            Path path = Paths.get(adrFile);
            assertThat(Files.exists(path))
                .as("ADR file %s should exist", adrFile)
                .isTrue();
        }
    }

    @Test
    void testDeploymentDocumentationExists() {
        // Test that deployment documentation exists
        Path deploymentGuide = Paths.get("docs/deployment/README.md");
        assertThat(Files.exists(deploymentGuide))
            .as("Deployment guide should exist")
            .isTrue();
    }

    @Test
    void testJavaExampleCodeQuality() throws IOException {
        // Test that Java example contains key components
        Path javaExample = Paths.get("docs/examples/java-client.md");
        assertThat(Files.exists(javaExample)).isTrue();
        
        List<String> lines = Files.readAllLines(javaExample);
        String content = String.join("\n", lines);
        
        // Check for essential Java code patterns
        assertThat(content).contains("@Service");
        assertThat(content).contains("WebClient");
        assertThat(content).contains("RateLimitRequest");
        assertThat(content).contains("RateLimitResponse");
        assertThat(content).contains("checkRateLimit");
        assertThat(content).contains("@RestController");
        assertThat(content).contains("application.properties");
    }

    @Test
    void testPythonExampleCodeQuality() throws IOException {
        // Test that Python example contains key components
        Path pythonExample = Paths.get("docs/examples/python-client.md");
        assertThat(Files.exists(pythonExample)).isTrue();
        
        List<String> lines = Files.readAllLines(pythonExample);
        String content = String.join("\n", lines);
        
        // Check for essential Python code patterns
        assertThat(content).contains("import requests");
        assertThat(content).contains("class RateLimiterClient");
        assertThat(content).contains("check_rate_limit");
        assertThat(content).contains("from flask import Flask");
        assertThat(content).contains("@rate_limit");
    }

    @Test
    void testCurlExampleCompleteness() throws IOException {
        // Test that cURL examples cover main API endpoints
        Path curlExample = Paths.get("docs/examples/curl-examples.md");
        assertThat(Files.exists(curlExample)).isTrue();
        
        List<String> lines = Files.readAllLines(curlExample);
        String content = String.join("\n", lines);
        
        // Check for coverage of main endpoints
        assertThat(content).contains("/api/ratelimit/check");
        assertThat(content).contains("/api/ratelimit/config");
        assertThat(content).contains("/actuator/health");
        assertThat(content).contains("curl -X POST");
        assertThat(content).contains("curl -X GET");
        assertThat(content).contains("Content-Type: application/json");
    }

    @Test
    void testReadmeDocumentationLinks() throws IOException {
        // Test that README contains links to documentation
        Path readme = Paths.get("README.md");
        assertThat(Files.exists(readme)).isTrue();
        
        List<String> lines = Files.readAllLines(readme);
        String content = String.join("\n", lines);
        
        // Check for documentation links
        assertThat(content).contains("docs/examples/");
        assertThat(content).contains("docs/adr/");
        assertThat(content).contains("docs/deployment/");
        assertThat(content).contains("swagger-ui/index.html");
        assertThat(content).contains("/v3/api-docs");
    }
}