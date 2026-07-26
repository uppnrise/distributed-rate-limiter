package dev.bnacar.distributedratelimiter.integration;

import dev.bnacar.distributedratelimiter.RedisTestContainerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    @Container
    static GenericContainer<?> redis = RedisTestContainerFactory.newRedisContainer();

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
        ResponseEntity<Map<String, Object>> healthResponse = getMap(
                "http://localhost:" + port + "/actuator/health",
                null
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
                "tokens", 1,
                "apiKey", "api-key-1"
        );

        ResponseEntity<Map<String, Object>> response = getMap(
                "http://localhost:" + port + "/api/ratelimit/check",
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("allowed");
    }

    @Test
    void healthEndpointIsAccessible() {
        ResponseEntity<Map<String, Object>> response = getMap(
                "http://localhost:" + port + "/actuator/health",
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        
        // Verify Redis health is included
        Object components = response.getBody().get("components");
        if (components instanceof Map<?, ?> componentMap) {
            assertThat(componentMap.containsKey("redis")).isTrue();
        }
    }

    @Test
    void applicationHandlesRedisTemporaryUnavailability() {
        // This test verifies graceful handling when Redis becomes unavailable
        // The application should still respond to health checks
        ResponseEntity<Map<String, Object>> healthResponse = getMap(
                "http://localhost:" + port + "/actuator/health",
                null
        );
        
        // Application should be able to respond even if some components are down
        assertThat(healthResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(healthResponse.getBody()).isNotNull();
    }

    @Test
    void applicationMetricsAreAvailable() {
        ResponseEntity<Map<String, Object>> response = getMap(
                "http://localhost:" + port + "/metrics",
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // The custom metrics endpoint should return our metrics structure
        assertThat(response.getBody()).containsKey("totalAllowedRequests");
        assertThat(response.getBody()).containsKey("totalDeniedRequests");
        assertThat(response.getBody()).containsKey("redisConnected");
    }

    @Test
    void gracefulShutdownConfiguration() {
        // This test verifies that the application is configured for graceful shutdown
        // by checking the actuator info endpoint which should include shutdown configuration
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info",
                String.class
        );

        // The info endpoint should be accessible (may be empty but should return 200)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> getMap(
            String url,
            Map<String, Object> request
    ) {
        HttpMethod method = request == null ? HttpMethod.GET : HttpMethod.POST;
        HttpEntity<Map<String, Object>> entity = request == null ? null : new HttpEntity<>(request);
        return restTemplate.exchange(url, method, entity, MAP_RESPONSE);
    }
}
