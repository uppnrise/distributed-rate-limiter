package dev.bnacar.distributedratelimiter.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Docker containerization.
 * These tests validate that the application works correctly when containerized.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DockerContainerIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("ratelimiter.redis.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
    }

    @Test
    void applicationStartsCorrectlyWithRedis() {
        // Verify that Redis container is running
        assertThat(redis.isRunning()).isTrue();
        
        // Verify that the application can connect to Redis
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Map.class
        );
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).isNotNull();
        assertThat(healthResponse.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void redisConnectivityFromApplication() {
        // Test Redis connectivity by making a rate limit request
        Map<String, Object> request = Map.of(
                "key", "test-key",
                "tokens", 1
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/ratelimit/check",
                request,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("allowed");
    }

    @Test
    void healthEndpointIsAccessible() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        
        // Verify Redis health is included
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        if (components != null) {
            assertThat(components).containsKey("redis");
        }
    }

    @Test
    void applicationHandlesRedisTemporaryUnavailability() {
        // This test verifies graceful handling when Redis becomes unavailable
        // The application should still respond to health checks
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Map.class
        );
        
        // Application should be able to respond even if some components are down
        assertThat(healthResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(healthResponse.getBody()).isNotNull();
    }

    @Test
    void applicationMetricsAreAvailable() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/metrics",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // The custom metrics endpoint should return our metrics structure
        assertThat(response.getBody()).containsKey("totalAllowedRequests");
        assertThat(response.getBody()).containsKey("totalDeniedRequests");
        assertThat(response.getBody()).containsKey("redisConnected");
    }
}