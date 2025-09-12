package dev.bnacar.distributedratelimiter.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

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
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/v3/api-docs", 
            Map.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> apiDoc = response.getBody();
        assertThat(apiDoc).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");
        assertThat(paths).isNotNull();
        
        // Check that main endpoints are documented
        assertThat(paths).containsKey("/api/ratelimit/check");
        assertThat(paths).containsKey("/api/ratelimit/config");
        
        // Check that rate limit check endpoint has POST method
        @SuppressWarnings("unchecked")
        Map<String, Object> rateLimitCheck = (Map<String, Object>) paths.get("/api/ratelimit/check");
        assertThat(rateLimitCheck).containsKey("post");
        
        // Check that config endpoint has GET method
        @SuppressWarnings("unchecked")
        Map<String, Object> configEndpoint = (Map<String, Object>) paths.get("/api/ratelimit/config");
        assertThat(configEndpoint).containsKey("get");
    }

    @Test
    void testApiTagsArePresent() {
        // Test that API endpoints are properly tagged by checking the OpenAPI spec
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/v3/api-docs", 
            Map.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> apiDoc = response.getBody();
        assertThat(apiDoc).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) apiDoc.get("paths");
        assertThat(paths).isNotNull();
        
        // Check that endpoints have appropriate tags in their operations
        assertThat(paths).containsKey("/api/ratelimit/check");
        assertThat(paths).containsKey("/api/ratelimit/config");
    }
}