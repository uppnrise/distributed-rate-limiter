package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
public class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    private MetricsResponse metricsResponse;

    @BeforeEach
    void setUp() {
        Map<String, MetricsResponse.KeyMetrics> keyMetrics = new HashMap<>();
        keyMetrics.put("user1", new MetricsResponse.KeyMetrics(5, 2, System.currentTimeMillis()));
        keyMetrics.put("user2", new MetricsResponse.KeyMetrics(3, 1, System.currentTimeMillis()));

        metricsResponse = new MetricsResponse(keyMetrics, true, 8, 3);
    }

    @Test
    void getMetrics_ShouldReturnMetricsResponse() throws Exception {
        when(metricsService.getMetrics()).thenReturn(metricsResponse);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalAllowedRequests").value(8))
                .andExpect(jsonPath("$.totalDeniedRequests").value(3))
                .andExpect(jsonPath("$.redisConnected").value(true))
                .andExpect(jsonPath("$.keyMetrics.user1.allowedRequests").value(5))
                .andExpect(jsonPath("$.keyMetrics.user1.deniedRequests").value(2))
                .andExpect(jsonPath("$.keyMetrics.user2.allowedRequests").value(3))
                .andExpect(jsonPath("$.keyMetrics.user2.deniedRequests").value(1));
    }

    @Test
    void getMetrics_WithEmptyMetrics_ShouldReturnEmptyKeyMetrics() throws Exception {
        MetricsResponse emptyResponse = new MetricsResponse(new HashMap<>(), false, 0, 0);
        when(metricsService.getMetrics()).thenReturn(emptyResponse);

        mockMvc.perform(get("/metrics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.totalAllowedRequests").value(0))
                .andExpect(jsonPath("$.totalDeniedRequests").value(0))
                .andExpect(jsonPath("$.redisConnected").value(false))
                .andExpect(jsonPath("$.keyMetrics").isEmpty());
    }
}