package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.AdminLimitRequest;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"ratelimiter.geographic.enabled=false"})
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBasicAuthHeader() {
        String credentials = "admin:admin123";
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encoded;
    }

    @Test
    void getKeyLimits_WithValidKey_ReturnsDefaultLimits() throws Exception {
        // When & Then - should return default configuration for any key
        mockMvc.perform(get("/admin/limits/test-key")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("test-key"))
                .andExpect(jsonPath("$.capacity").value(10)) // Default values
                .andExpect(jsonPath("$.refillRate").value(2))
                .andExpect(jsonPath("$.algorithm").value("TOKEN_BUCKET"));
    }

    @Test
    void getKeyLimits_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/limits/test-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateKeyLimits_WithValidRequest_UpdatesLimits() throws Exception {
        // Given
        String key = "update-test-key";
        AdminLimitRequest request = new AdminLimitRequest(20, 10, 120000L, RateLimitAlgorithm.SLIDING_WINDOW);

        // When & Then
        mockMvc.perform(put("/admin/limits/{key}", key)
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.refillRate").value(10))
                .andExpect(jsonPath("$.cleanupIntervalMs").value(120000))
                .andExpect(jsonPath("$.algorithm").value("SLIDING_WINDOW"));
    }

    @Test
    void updateKeyLimits_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        String key = "test-key";
        AdminLimitRequest request = new AdminLimitRequest(-1, -1, 100L, null); // Invalid values

        // When & Then
        mockMvc.perform(put("/admin/limits/{key}", key)
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeKeyLimits_WithExistingKey_RemovesLimits() throws Exception {
        // Given - first create a configuration
        String key = "delete-test-key";
        AdminLimitRequest request = new AdminLimitRequest(5, 2, 30000L, RateLimitAlgorithm.TOKEN_BUCKET);
        
        mockMvc.perform(put("/admin/limits/{key}", key)
                .header("Authorization", getBasicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then - delete it
        mockMvc.perform(delete("/admin/limits/{key}", key)
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(content().string("Limits removed for key: " + key));
    }

    @Test
    void getAllKeys_EmptyState_ReturnsEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/keys")
                .header("Authorization", getBasicAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalKeys").value(0))
                .andExpect(jsonPath("$.activeKeys").value(0))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isEmpty());
    }

    @Test
    void getAllKeys_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAuthentication_WithWrongCredentials_ReturnsUnauthorized() throws Exception {
        String wrongCredentials = Base64.getEncoder().encodeToString("admin:wrongpassword".getBytes());
        
        // Test with wrong password
        mockMvc.perform(get("/admin/keys")
                .header("Authorization", "Basic " + wrongCredentials))
                .andExpect(status().isUnauthorized());

        // Test with wrong username  
        String wrongUser = Base64.getEncoder().encodeToString("wronguser:admin123".getBytes());
        mockMvc.perform(get("/admin/keys")
                .header("Authorization", "Basic " + wrongUser))
                .andExpect(status().isUnauthorized());
    }
}