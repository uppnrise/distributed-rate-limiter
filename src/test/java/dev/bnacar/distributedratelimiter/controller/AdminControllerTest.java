package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.AdminLimitRequest;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitConfig;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private RateLimiterConfiguration configuration;

    @MockBean
    private ConfigurationResolver configurationResolver;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getKeyLimits_WithValidKey_ReturnsLimits() throws Exception {
        // Given
        String key = "test-key";
        RateLimitConfig config = new RateLimitConfig(10, 5, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        when(rateLimiterService.getKeyConfiguration(key)).thenReturn(config);

        // When & Then
        mockMvc.perform(get("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.refillRate").value(5))
                .andExpect(jsonPath("$.cleanupIntervalMs").value(60000))
                .andExpect(jsonPath("$.algorithm").value("TOKEN_BUCKET"));
    }

    @Test
    void getKeyLimits_WithInvalidKey_ReturnsNotFound() throws Exception {
        // Given
        String key = "non-existent-key";
        when(rateLimiterService.getKeyConfiguration(key)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
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
        String key = "test-key";
        AdminLimitRequest request = new AdminLimitRequest(20, 10, 120000L, RateLimitAlgorithm.SLIDING_WINDOW);
        
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        when(configuration.getKeys()).thenReturn(keys);
        
        RateLimitConfig updatedConfig = new RateLimitConfig(20, 10, 120000, RateLimitAlgorithm.SLIDING_WINDOW);
        when(rateLimiterService.getKeyConfiguration(key)).thenReturn(updatedConfig);

        // When & Then
        mockMvc.perform(put("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.refillRate").value(10))
                .andExpect(jsonPath("$.cleanupIntervalMs").value(120000))
                .andExpect(jsonPath("$.algorithm").value("SLIDING_WINDOW"));

        verify(rateLimiterService).removeKey(key);
        verify(configurationResolver).clearCache();
    }

    @Test
    void updateKeyLimits_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        String key = "test-key";
        AdminLimitRequest request = new AdminLimitRequest(-1, -1, 100L, null); // Invalid values

        // When & Then
        mockMvc.perform(put("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeKeyLimits_WithExistingKey_RemovesLimits() throws Exception {
        // Given
        String key = "test-key";
        Map<String, RateLimiterConfiguration.KeyConfig> keys = new HashMap<>();
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keys.put(key, keyConfig);
        when(configuration.getKeys()).thenReturn(keys);
        when(rateLimiterService.removeKey(key)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string("Limits removed for key: " + key));

        verify(rateLimiterService).removeKey(key);
        verify(configurationResolver).clearCache();
    }

    @Test
    void removeKeyLimits_WithNonExistentKey_ReturnsNotFound() throws Exception {
        // Given
        String key = "non-existent-key";
        when(configuration.getKeys()).thenReturn(new HashMap<>());
        when(rateLimiterService.removeKey(key)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/admin/limits/{key}", key)
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllKeys_WithActiveKeys_ReturnsKeyStatistics() throws Exception {
        // Given
        RateLimitConfig config = new RateLimitConfig(10, 5, 60000, RateLimitAlgorithm.TOKEN_BUCKET);
        RateLimiterService.BucketHolder holder = new RateLimiterService.BucketHolder(null, config);
        
        Map<String, RateLimiterService.BucketHolder> bucketHolders = new HashMap<>();
        bucketHolders.put("key1", holder);
        bucketHolders.put("key2", holder);
        
        when(rateLimiterService.getBucketHolders()).thenReturn(bucketHolders);

        // When & Then
        mockMvc.perform(get("/admin/keys")
                .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalKeys").value(2))
                .andExpect(jsonPath("$.activeKeys").value(2))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].capacity").value(10))
                .andExpect(jsonPath("$.keys[0].refillRate").value(5));
    }

    @Test
    void getAllKeys_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoints_WithWrongRole_ReturnsForbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/keys"))
                .andExpect(status().isForbidden());
    }
}