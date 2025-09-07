package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateLimitController.class)
public class RateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    public void testSuccessfulRateLimitCheck() throws Exception {
        when(rateLimiterService.isAllowed("user1", 5)).thenReturn(true);
        
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
    public void testRateLimitExceeded() throws Exception {
        when(rateLimiterService.isAllowed("user2", 1)).thenReturn(false);
        
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
    public void testRequestExceedsBucketCapacity() throws Exception {
        when(rateLimiterService.isAllowed("user3", 15)).thenReturn(false);
        
        RateLimitRequest request = new RateLimitRequest("user3", 15);
        
        mockMvc.perform(post("/api/ratelimit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.key").value("user3"))
                .andExpect(jsonPath("$.tokensRequested").value(15))
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
        when(rateLimiterService.isAllowed("user7", 1)).thenReturn(true);
        
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