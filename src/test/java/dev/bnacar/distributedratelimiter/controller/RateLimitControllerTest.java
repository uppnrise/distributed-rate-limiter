package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.security.ApiKeyService;
import dev.bnacar.distributedratelimiter.security.IpAddressExtractor;
import dev.bnacar.distributedratelimiter.security.IpSecurityService;
import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.BeforeEach;

@WebMvcTest(controllers = RateLimitController.class)
@Import(RateLimitControllerTest.TestConfig.class)
public class RateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimiterService compositeRateLimiterService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private IpSecurityService ipSecurityService;

    @Autowired
    private IpAddressExtractor ipAddressExtractor;

    @Autowired
    private SecurityConfiguration securityConfiguration;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RateLimiterService rateLimiterService() {
            return mock(RateLimiterService.class);
        }

        @Bean
        public dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimiterService compositeRateLimiterService() {
            return mock(dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimiterService.class);
        }

        @Bean
        public ApiKeyService apiKeyService() {
            return mock(ApiKeyService.class);
        }

        @Bean
        public IpSecurityService ipSecurityService() {
            return mock(IpSecurityService.class);
        }

        @Bean
        public IpAddressExtractor ipAddressExtractor() {
            return mock(IpAddressExtractor.class);
        }

        @Bean
        public SecurityConfiguration securityConfiguration() {
            return mock(SecurityConfiguration.class);
        }
        
        @Bean
        public dev.bnacar.distributedratelimiter.geo.GeographicRateLimitService geographicRateLimitService() {
            return mock(dev.bnacar.distributedratelimiter.geo.GeographicRateLimitService.class);
        }
        
        @Bean
        public dev.bnacar.distributedratelimiter.adaptive.AdaptiveRateLimitEngine adaptiveRateLimitEngine() {
            return mock(dev.bnacar.distributedratelimiter.adaptive.AdaptiveRateLimitEngine.class);
        }
    }

    @BeforeEach
    public void setUp() {
        // Mock SecurityConfiguration to avoid NullPointerExceptions
        SecurityConfiguration.Headers headers = new SecurityConfiguration.Headers();
        headers.setEnabled(true);
        when(securityConfiguration.getHeaders()).thenReturn(headers);
        when(securityConfiguration.getMaxRequestSize()).thenReturn("1MB");
    }

    @Test
    public void testSuccessfulRateLimitCheck() throws Exception {
        // Mock all security services to allow the request
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("127.0.0.1");
        when(ipSecurityService.isIpAllowed("127.0.0.1")).thenReturn(true);
        when(apiKeyService.isValidApiKey(null)).thenReturn(true);
        when(ipSecurityService.createIpBasedKey("user1", "127.0.0.1")).thenReturn("ip:127.0.0.1:user1");
        when(rateLimiterService.isAllowed("ip:127.0.0.1:user1", 5)).thenReturn(true);
        
        RateLimitRequest request = new RateLimitRequest("user1", 5);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value("user1"))
                .andExpect(jsonPath("$.tokensRequested").value(5))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    public void testSuccessfulRateLimitCheckWithApiKey() throws Exception {
        // Mock all security services to allow the request with API key
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("127.0.0.1");
        when(ipSecurityService.isIpAllowed("127.0.0.1")).thenReturn(true);
        when(apiKeyService.isValidApiKey("valid-api-key")).thenReturn(true);
        when(ipSecurityService.createIpBasedKey("user1", "127.0.0.1")).thenReturn("ip:127.0.0.1:user1");
        when(rateLimiterService.isAllowed("ip:127.0.0.1:user1", 5)).thenReturn(true);
        
        RateLimitRequest request = new RateLimitRequest("user1", 5, "valid-api-key");
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value("user1"))
                .andExpect(jsonPath("$.tokensRequested").value(5))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    public void testInvalidApiKey() throws Exception {
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("127.0.0.1");
        when(ipSecurityService.isIpAllowed("127.0.0.1")).thenReturn(true);
        when(apiKeyService.isValidApiKey("invalid-api-key")).thenReturn(false);
        
        RateLimitRequest request = new RateLimitRequest("user1", 5, "invalid-api-key");
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value("user1"))
                .andExpect(jsonPath("$.tokensRequested").value(5))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    public void testIpAddressBlocked() throws Exception {
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("192.168.1.100");
        when(ipSecurityService.isIpAllowed("192.168.1.100")).thenReturn(false);
        
        RateLimitRequest request = new RateLimitRequest("user1", 5);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value("user1"))
                .andExpect(jsonPath("$.tokensRequested").value(5))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    public void testRateLimitExceeded() throws Exception {
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("127.0.0.1");
        when(ipSecurityService.isIpAllowed("127.0.0.1")).thenReturn(true);
        when(apiKeyService.isValidApiKey(null)).thenReturn(true);
        when(ipSecurityService.createIpBasedKey("user2", "127.0.0.1")).thenReturn("ip:127.0.0.1:user2");
        when(rateLimiterService.isAllowed("ip:127.0.0.1:user2", 1)).thenReturn(false);
        
        RateLimitRequest request = new RateLimitRequest("user2", 1);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.key").value("user2"))
                .andExpect(jsonPath("$.tokensRequested").value(1))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    public void testInvalidRequestBlankKey() throws Exception {
        RateLimitRequest request = new RateLimitRequest("", 5);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testInvalidRequestZeroTokens() throws Exception {
        RateLimitRequest request = new RateLimitRequest("user4", 0);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDefaultTokensValue() throws Exception {
        when(ipAddressExtractor.getClientIpAddress(any())).thenReturn("127.0.0.1");
        when(ipSecurityService.isIpAllowed("127.0.0.1")).thenReturn(true);
        when(apiKeyService.isValidApiKey(null)).thenReturn(true);
        when(ipSecurityService.createIpBasedKey("user7", "127.0.0.1")).thenReturn("ip:127.0.0.1:user7");
        when(rateLimiterService.isAllowed("ip:127.0.0.1:user7", 1)).thenReturn(true);
        
        // Test that tokens defaults to 1 when not specified
        String requestJson = "{\"key\":\"user7\"}";
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("user7"))
                .andExpect(jsonPath("$.tokensRequested").value(1))
                .andExpect(jsonPath("$.allowed").value(true));
    }
}