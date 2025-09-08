package dev.bnacar.distributedratelimiter.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import dev.bnacar.distributedratelimiter.TestcontainersConfiguration;
import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MetricsAndHealthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_ShouldReturnHealthStatus() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("status"));
    }

    @Test
    void metricsEndpoint_ShouldReturnInitialMetrics() {
        ResponseEntity<MetricsResponse> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/metrics", MetricsResponse.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        MetricsResponse metrics = response.getBody();
        assertTrue(metrics.getTotalAllowedRequests() >= 0);
        assertTrue(metrics.getTotalDeniedRequests() >= 0);
        assertNotNull(metrics.getKeyMetrics());
    }

    @Test
    void rateLimitingAndMetrics_ShouldTrackRequestsAccurately() {
        String key = "integration-test-user";
        
        // Clear any existing metrics first by getting baseline
        MetricsResponse baselineMetrics = restTemplate.getForEntity(
                "http://localhost:" + port + "/metrics", MetricsResponse.class).getBody();
        
        long baselineAllowed = baselineMetrics != null ? baselineMetrics.getTotalAllowedRequests() : 0;
        long baselineDenied = baselineMetrics != null ? baselineMetrics.getTotalDeniedRequests() : 0;
        
        // Make some rate limit requests
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // First request should be allowed
        RateLimitRequest allowedRequest = new RateLimitRequest(key, 5);
        HttpEntity<RateLimitRequest> allowedEntity = new HttpEntity<>(allowedRequest, headers);
        
        ResponseEntity<String> allowedResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/ratelimit/check", allowedEntity, String.class);
        
        assertEquals(200, allowedResponse.getStatusCode().value());
        assertTrue(allowedResponse.getBody().contains("\"allowed\":true"));
        
        // Make requests that will eventually be denied
        for (int i = 0; i < 3; i++) {
            RateLimitRequest request = new RateLimitRequest(key, 5);
            HttpEntity<RateLimitRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(
                    "http://localhost:" + port + "/api/ratelimit/check", entity, String.class);
        }
        
        // Check metrics
        ResponseEntity<MetricsResponse> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/metrics", MetricsResponse.class);
        
        assertEquals(200, metricsResponse.getStatusCode().value());
        MetricsResponse metrics = metricsResponse.getBody();
        assertNotNull(metrics);
        
        // Should have more total requests than baseline
        assertTrue(metrics.getTotalAllowedRequests() > baselineAllowed);
        
        // Should have metrics for our test key
        assertTrue(metrics.getKeyMetrics().containsKey(key));
        MetricsResponse.KeyMetrics keyMetrics = metrics.getKeyMetrics().get(key);
        assertTrue(keyMetrics.getAllowedRequests() > 0);
        assertTrue(keyMetrics.getLastAccessTime() > 0);
    }

    @Test
    void actuatorEndpoint_ShouldListAvailableEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator", String.class);
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("health"));
    }
}