package dev.bnacar.distributedratelimiter.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to validate API documentation completeness and accuracy.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.redis.host=localhost",
    "spring.redis.port=6379"
})
class ApiDocumentationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OpenAPI openAPI;

    @Test
    void testOpenApiSpecIsAvailable() {
        // Test that OpenAPI specification is accessible
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/v3/api-docs", 
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Distributed Rate Limiter API");
    }

    @Test
    void testSwaggerUiIsAvailable() {
        // Test that Swagger UI is accessible
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/swagger-ui/index.html", 
            String.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Swagger UI");
    }

    @Test
    void testApiInfoIsComplete() {
        // Test that OpenAPI info section has all required fields
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Distributed Rate Limiter API");
        assertThat(openAPI.getInfo().getDescription()).contains("distributed token bucket rate limiter");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("0.0.1-SNAPSHOT");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT License");
    }

    @Test
    void testRateLimitEndpointsAreDocumented() {
        // Test that main rate limit endpoints are documented
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            "http://localhost:" + port + "/v3/api-docs",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> apiDoc = response.getBody();
        assertThat(apiDoc).isNotNull();
        
        Map<String, Object> paths = requireStringKeyedMap(apiDoc.get("paths"), "paths");
        
        // Check that main endpoints are documented
        assertThat(paths).containsKey("/api/ratelimit/check");
        assertThat(paths).containsKey("/api/ratelimit/config");
        
        // Check that rate limit check endpoint has POST method
        Map<String, Object> rateLimitCheck = requireStringKeyedMap(
            paths.get("/api/ratelimit/check"),
            "rateLimitCheck");
        assertThat(rateLimitCheck).containsKey("post");
        
        // Check that config endpoint has GET method
        Map<String, Object> configEndpoint = requireStringKeyedMap(
            paths.get("/api/ratelimit/config"),
            "configEndpoint");
        assertThat(configEndpoint).containsKey("get");
    }

    @Test
    void testApiTagsArePresent() {
        // Test that API endpoints are properly tagged by checking the OpenAPI spec
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            "http://localhost:" + port + "/v3/api-docs",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> apiDoc = response.getBody();
        assertThat(apiDoc).isNotNull();
        
        Map<String, Object> paths = requireStringKeyedMap(apiDoc.get("paths"), "paths");
        
        // Check that endpoints have appropriate tags in their operations
        assertThat(paths).containsKey("/api/ratelimit/check");
        assertThat(paths).containsKey("/api/ratelimit/config");
    }

    private Map<String, Object> requireStringKeyedMap(Object value, String description) {
        assertThat(value).as(description).isInstanceOf(Map.class);
        Map<?, ?> raw = (Map<?, ?>) value;
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            assertThat(entry.getKey()).as(description + " key").isInstanceOf(String.class);
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }
}
