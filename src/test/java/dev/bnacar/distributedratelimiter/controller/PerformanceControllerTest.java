package dev.bnacar.distributedratelimiter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult;
import dev.bnacar.distributedratelimiter.monitoring.PerformanceRegressionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for PerformanceController.
 */
@WebMvcTest(PerformanceController.class)
@Import(PerformanceControllerTest.TestConfig.class)
class PerformanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PerformanceRegressionService regressionService;

    @Autowired
    private SecurityConfiguration securityConfiguration;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PerformanceRegressionService regressionService() {
            return mock(PerformanceRegressionService.class);
        }

        @Bean
        public SecurityConfiguration securityConfiguration() {
            return mock(SecurityConfiguration.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Mock SecurityConfiguration to avoid NullPointerExceptions
        SecurityConfiguration.Headers mockHeaders = mock(SecurityConfiguration.Headers.class);
        when(mockHeaders.isEnabled()).thenReturn(false);
        when(securityConfiguration.getHeaders()).thenReturn(mockHeaders);
        when(securityConfiguration.getApiKeys()).thenReturn(null);
        when(securityConfiguration.getIp()).thenReturn(null);
    }

    @Test
    @DisplayName("Should store performance baseline successfully")
    void testStoreBaseline() throws Exception {
        // Given
        PerformanceBaseline baseline = new PerformanceBaseline(
            "test-scenario", 150.0, 300.0, 800.0, 95.5
        );
        baseline.setCommitHash("abc123");

        // When & Then
        mockMvc.perform(post("/api/performance/baseline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(baseline)))
                .andExpect(status().isOk())
                .andExpect(content().string("Performance baseline stored successfully"));

        verify(regressionService, times(1)).storeBaseline(any(PerformanceBaseline.class));
    }

    @Test
    @DisplayName("Should handle baseline storage failure")
    void testStoreBaselineFailure() throws Exception {
        // Given
        PerformanceBaseline baseline = new PerformanceBaseline(
            "test-scenario", 150.0, 300.0, 800.0, 95.5
        );
        
        doThrow(new RuntimeException("Storage failed")).when(regressionService).storeBaseline(any());

        // When & Then
        mockMvc.perform(post("/api/performance/baseline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(baseline)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Failed to store baseline: Storage failed"));
    }

    @Test
    @DisplayName("Should analyze performance regression")
    void testAnalyzeRegression() throws Exception {
        // Given
        PerformanceBaseline currentBaseline = new PerformanceBaseline(
            "test-scenario", 200.0, 400.0, 700.0, 93.0
        );
        
        PerformanceRegressionResult result = new PerformanceRegressionResult();
        result.setTestName("test-scenario");
        result.setHasRegression(true);
        
        when(regressionService.analyzeRegression(any(PerformanceBaseline.class), any()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/performance/regression/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(currentBaseline)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testName").value("test-scenario"))
                .andExpect(jsonPath("$.hasRegression").value(true));
    }

    @Test
    @DisplayName("Should get historical baselines")
    void testGetHistoricalBaselines() throws Exception {
        // Given
        String testName = "load-test";
        List<PerformanceBaseline> baselines = new ArrayList<>();
        baselines.add(new PerformanceBaseline(testName, 150.0, 300.0, 800.0, 95.5));
        baselines.add(new PerformanceBaseline(testName, 160.0, 320.0, 780.0, 94.8));
        
        when(regressionService.getPerformanceTrend(testName, 10)).thenReturn(baselines);

        // When & Then
        mockMvc.perform(get("/api/performance/baseline/{testName}", testName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].testName").value(testName))
                .andExpect(jsonPath("$[1].testName").value(testName));
    }

    @Test
    @DisplayName("Should get performance trend")
    void testGetPerformanceTrend() throws Exception {
        // Given
        String testName = "performance-test";
        int limit = 10;
        List<PerformanceBaseline> trend = new ArrayList<>();
        trend.add(new PerformanceBaseline(testName, 150.0, 300.0, 800.0, 95.5));
        
        when(regressionService.getPerformanceTrend(testName, limit)).thenReturn(trend);

        // When & Then
        mockMvc.perform(get("/api/performance/trend/{testName}", testName)
                .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].testName").value(testName));
    }

    @Test
    @DisplayName("Should handle IO exception in get baselines")
    void testGetBaselinesIOException() throws Exception {
        // Given
        String testName = "failing-test";
        when(regressionService.getPerformanceTrend(testName, 10))
            .thenThrow(new java.io.IOException("File not found"));

        // When & Then
        mockMvc.perform(get("/api/performance/baseline/{testName}", testName))
                .andExpect(status().isBadRequest());
    }
}