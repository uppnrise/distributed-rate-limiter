package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.DefaultConfigRequest;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for dynamic configuration management.
 */
@WebMvcTest(RateLimitConfigController.class)
public class RateLimitConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RateLimiterConfiguration configuration;

    @MockBean
    private ConfigurationResolver configurationResolver;

    @MockBean
    private RateLimiterService rateLimiterService;

    @MockBean
    private SecurityConfiguration securityConfiguration;

    @BeforeEach
    void setUp() {
        // Mock SecurityConfiguration to avoid NullPointerExceptions
        SecurityConfiguration.Headers headers = new SecurityConfiguration.Headers();
        headers.setEnabled(true);
        when(securityConfiguration.getHeaders()).thenReturn(headers);
        when(securityConfiguration.getMaxRequestSize()).thenReturn("1MB");
    }

    @Test
    void test_shouldUpdateKeyConfiguration() throws Exception {
        RateLimiterConfiguration.KeyConfig keyConfig = new RateLimiterConfiguration.KeyConfig();
        keyConfig.setCapacity(25);
        keyConfig.setRefillRate(5);

        mockMvc.perform(post("/api/ratelimit/config/keys/test_key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(keyConfig)))
                .andExpect(status().isOk())
                .andExpect(content().string("Configuration updated for key: test_key"));
    }

    @Test
    void test_shouldUpdatePatternConfiguration() throws Exception {
        RateLimiterConfiguration.KeyConfig patternConfig = new RateLimiterConfiguration.KeyConfig();
        patternConfig.setCapacity(50);
        patternConfig.setRefillRate(20);

        mockMvc.perform(post("/api/ratelimit/config/patterns/api:*")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patternConfig)))
                .andExpect(status().isOk())
                .andExpect(content().string("Configuration updated for pattern: api:*"));
    }

    @Test
    void test_shouldUpdateDefaultConfiguration() throws Exception {
        DefaultConfigRequest request = new DefaultConfigRequest();
        request.setCapacity(15);
        request.setRefillRate(3);

        mockMvc.perform(post("/api/ratelimit/config/default")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Default configuration updated"));
    }

    @Test
    void test_shouldRemoveKeyConfiguration() throws Exception {
        mockMvc.perform(delete("/api/ratelimit/config/keys/test_remove"))
                .andExpect(status().isOk())
                .andExpect(content().string("Configuration removed for key: test_remove"));
    }

    @Test
    void test_shouldReloadConfiguration() throws Exception {
        mockMvc.perform(post("/api/ratelimit/config/reload"))
                .andExpect(status().isOk())
                .andExpect(content().string("Configuration reloaded successfully"));
    }
}