package dev.bnacar.distributedratelimiter.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.AdminLimitRequest;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
class AdminIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminWorkflow_CreateUpdateDeleteLimits_WorksCorrectly() throws Exception {
        String testKey = "integration-test-key";

        // 1. Initially, the key should not exist in admin view
        mockMvc.perform(get("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());

        // 2. Create a rate limit configuration for the key
        AdminLimitRequest createRequest = new AdminLimitRequest(5, 2, 30000L, RateLimitAlgorithm.TOKEN_BUCKET);
        mockMvc.perform(put("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(testKey))
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.refillRate").value(2));

        // 3. Verify the configuration is now visible
        mockMvc.perform(get("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(testKey))
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.refillRate").value(2));

        // 4. Use the rate limiter to create an active bucket
        RateLimitRequest rateLimitRequest = new RateLimitRequest(testKey, 1);
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rateLimitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        // 5. Check that the key now appears in the active keys list
        mockMvc.perform(get("/admin/keys")
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalKeys").value(1))
                .andExpect(jsonPath("$.keys[0].key").value(testKey))
                .andExpect(jsonPath("$.keys[0].capacity").value(5))
                .andExpect(jsonPath("$.keys[0].refillRate").value(2));

        // 6. Update the configuration
        AdminLimitRequest updateRequest = new AdminLimitRequest(20, 10, 60000L, RateLimitAlgorithm.SLIDING_WINDOW);
        mockMvc.perform(put("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.refillRate").value(10))
                .andExpect(jsonPath("$.algorithm").value("SLIDING_WINDOW"));

        // 7. Delete the configuration
        mockMvc.perform(delete("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Limits removed for key: " + testKey));

        // 8. Verify it's gone from the configuration (should fall back to default)
        mockMvc.perform(get("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(10)) // Default values
                .andExpect(jsonPath("$.refillRate").value(2));
    }

    @Test
    void adminAuthentication_WithWrongCredentials_ReturnUnauthorized() throws Exception {
        // Test with wrong password
        mockMvc.perform(get("/admin/keys")
                .with(httpBasic("admin", "wrongpassword")))
                .andExpect(status().isUnauthorized());

        // Test with wrong username
        mockMvc.perform(get("/admin/keys")
                .with(httpBasic("wronguser", "admin123")))
                .andExpect(status().isUnauthorized());

        // Test without credentials
        mockMvc.perform(get("/admin/keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminOperations_AffectRateLimiting_BehaviorChanges() throws Exception {
        String testKey = "rate-limit-test-key";

        // 1. Set a very restrictive rate limit (1 request per second)
        AdminLimitRequest restrictiveLimit = new AdminLimitRequest(1, 1, 60000L, RateLimitAlgorithm.TOKEN_BUCKET);
        mockMvc.perform(put("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restrictiveLimit)))
                .andExpect(status().isOk());

        // 2. Make the first request - should be allowed
        RateLimitRequest request = new RateLimitRequest(testKey, 1);
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        // 3. Make the second request immediately - should be denied
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.allowed").value(false));

        // 4. Update to a more permissive limit (10 requests)
        AdminLimitRequest permissiveLimit = new AdminLimitRequest(10, 5, 60000L, RateLimitAlgorithm.TOKEN_BUCKET);
        mockMvc.perform(put("/admin/limits/{key}", testKey)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(permissiveLimit)))
                .andExpect(status().isOk());

        // 5. Now requests should be allowed again with the new limit
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
    }
}