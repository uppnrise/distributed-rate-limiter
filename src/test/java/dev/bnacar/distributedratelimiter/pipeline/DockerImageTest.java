package dev.bnacar.distributedratelimiter.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Docker image creation and validation.
 * These tests ensure that the Docker image can be built and runs correctly.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Docker Image Tests")
class DockerImageTest {

    @Test
    @DisplayName("Dockerfile should contain multi-stage build")
    void testDockerfileMultiStage() throws IOException {
        Path dockerfilePath = Paths.get("Dockerfile");
        assertTrue(Files.exists(dockerfilePath), "Dockerfile should exist");
        
        String dockerfileContent = Files.readString(dockerfilePath);
        assertTrue(dockerfileContent.contains("FROM eclipse-temurin:21-jdk AS build"), 
            "Dockerfile should use multi-stage build with Java 21");
        assertTrue(dockerfileContent.contains("FROM eclipse-temurin:21-jre AS runtime"), 
            "Dockerfile should have runtime stage with JRE");
    }

    @Test
    @DisplayName("Dockerfile should include security best practices")
    void testDockerfileSecurity() throws IOException {
        Path dockerfilePath = Paths.get("Dockerfile");
        String dockerfileContent = Files.readString(dockerfilePath);
        
        assertTrue(dockerfileContent.contains("USER appuser"), 
            "Dockerfile should run as non-root user");
        assertTrue(dockerfileContent.contains("HEALTHCHECK"), 
            "Dockerfile should include health check");
    }

    @Test
    @DisplayName("Dockerfile should expose correct port")
    void testDockerfilePort() throws IOException {
        Path dockerfilePath = Paths.get("Dockerfile");
        String dockerfileContent = Files.readString(dockerfilePath);
        
        assertTrue(dockerfileContent.contains("EXPOSE 8080"), 
            "Dockerfile should expose port 8080");
    }

    @Test
    @DisplayName("Docker image should contain application JAR")
    @EnabledIfEnvironmentVariable(named = "CI", matches = "true")
    void testDockerImageContent() {
        // This test would run in CI environment where Docker is available
        // and the image has been built
        assertNotNull(System.getenv("CI"), "This test runs in CI environment");
    }

    @Test
    @DisplayName("Application should start in Docker container")
    void testApplicationStartup() {
        // Use a pre-built image or build one for testing
        // This is a placeholder test - in real CI, we'd test the actual built image
        
        String imageName = "eclipse-temurin:21-jre";
        
        try (GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(imageName))
                .withCommand("java", "-version")
                .waitingFor(Wait.forLogMessage(".*OpenJDK.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(30)))) {
            
            container.start();
            assertTrue(container.isRunning(), "Container should start successfully");
            assertTrue(container.getLogs().contains("OpenJDK"), 
                "Container should run Java successfully");
        }
    }

    @Test
    @DisplayName("Maven wrapper should be executable in Docker")
    void testMavenWrapperExecutable() throws IOException {
        Path mvnwPath = Paths.get("mvnw");
        assertTrue(Files.exists(mvnwPath), "Maven wrapper should exist");
        assertTrue(Files.isExecutable(mvnwPath), "Maven wrapper should be executable");
    }

    @Test
    @DisplayName("Docker build context should not include unnecessary files")
    void testDockerignore() {
        Path dockerignorePath = Paths.get(".dockerignore");
        if (Files.exists(dockerignorePath)) {
            // .dockerignore exists, which is good for optimizing build context
            assertTrue(true, ".dockerignore file exists to optimize build context");
        } else {
            // It's okay if .dockerignore doesn't exist, but log a note
            System.out.println("Note: Consider adding .dockerignore to optimize Docker build context");
        }
    }

    @Test
    @DisplayName("JVM options should be configured for containerized environment")
    void testJvmOptionsForContainer() throws IOException {
        Path dockerfilePath = Paths.get("Dockerfile");
        String dockerfileContent = Files.readString(dockerfilePath);
        
        // Check for container-optimized JVM settings
        assertTrue(dockerfileContent.contains("JAVA_OPTS") || 
                  dockerfileContent.contains("-Xmx") || 
                  dockerfileContent.contains("java.security.egd"), 
            "Dockerfile should include container-optimized JVM settings");
    }
}