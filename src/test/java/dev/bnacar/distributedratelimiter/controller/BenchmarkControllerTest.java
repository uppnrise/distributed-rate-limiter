package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.BenchmarkRequest;
import dev.bnacar.distributedratelimiter.models.BenchmarkResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the benchmark endpoint functionality.
 */
@WebMvcTest(BenchmarkController.class)
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    void testBenchmarkHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/benchmark/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Benchmark service is healthy"));
    }

    @Test
    void testBasicBenchmarkRun() throws Exception {
        // Mock the rate limiter service to allow some requests
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);
        
        BenchmarkRequest request = new BenchmarkRequest(2, 10L, 5, 1);
        request.setKeyPrefix("test_basic");

        String response = mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BenchmarkResponse benchmarkResponse = objectMapper.readValue(response, BenchmarkResponse.class);

        assertTrue(benchmarkResponse.isSuccess());
        assertTrue(benchmarkResponse.getTotalRequests() > 0);
        assertTrue(benchmarkResponse.getDurationSeconds() > 0);
        assertTrue(benchmarkResponse.getThroughputPerSecond() > 0);
        assertEquals(2, benchmarkResponse.getConcurrentThreads());
        assertEquals(10L, benchmarkResponse.getRequestsPerThread());
        assertNull(benchmarkResponse.getErrorMessage());
    }

    @Test
    void testBenchmarkAccuracy() throws Exception {
        // Mock the rate limiter service to allow all requests
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);
        
        // Test with known parameters to verify accuracy
        BenchmarkRequest request = new BenchmarkRequest(1, 5L, 2, 1);
        request.setKeyPrefix("test_accuracy");

        String response = mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BenchmarkResponse benchmarkResponse = objectMapper.readValue(response, BenchmarkResponse.class);

        assertTrue(benchmarkResponse.isSuccess());
        // With 1 thread and 5 requests, we should get exactly 5 total requests
        assertEquals(5L, benchmarkResponse.getTotalRequests());
        assertEquals(1, benchmarkResponse.getConcurrentThreads());
        assertEquals(5L, benchmarkResponse.getRequestsPerThread());
        
        // Duration should be reasonable (less than the max duration + buffer)
        assertTrue(benchmarkResponse.getDurationSeconds() <= 3.0, 
                  "Duration too long: " + benchmarkResponse.getDurationSeconds());
    }

    @Test
    void testHighConcurrencyBenchmark() throws Exception {
        // Mock alternating responses to simulate rate limiting
        when(rateLimiterService.isAllowed(anyString(), anyInt()))
                .thenReturn(true, true, false, true, false);
        
        BenchmarkRequest request = new BenchmarkRequest(5, 20L, 3, 1);
        request.setKeyPrefix("test_concurrency");

        String response = mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BenchmarkResponse benchmarkResponse = objectMapper.readValue(response, BenchmarkResponse.class);

        assertTrue(benchmarkResponse.isSuccess());
        assertEquals(5, benchmarkResponse.getConcurrentThreads());
        assertEquals(20L, benchmarkResponse.getRequestsPerThread());
        
        // With 5 threads * 20 requests, we should get up to 100 total requests
        assertTrue(benchmarkResponse.getTotalRequests() <= 100);
        assertTrue(benchmarkResponse.getTotalRequests() > 0);
        
        // Throughput should be reasonable for concurrent execution
        assertTrue(benchmarkResponse.getThroughputPerSecond() > 5, 
                  "Throughput too low: " + benchmarkResponse.getThroughputPerSecond());
    }

    @Test
    void testBenchmarkWithDelays() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);
        
        BenchmarkRequest request = new BenchmarkRequest(2, 3L, 3, 1);
        request.setKeyPrefix("test_delays");
        request.setDelayBetweenRequestsMs(100L); // 100ms delay between requests

        String response = mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BenchmarkResponse benchmarkResponse = objectMapper.readValue(response, BenchmarkResponse.class);

        assertTrue(benchmarkResponse.isSuccess());
        // With delays, the duration should be longer
        assertTrue(benchmarkResponse.getDurationSeconds() > 0.2, 
                  "Duration should account for delays: " + benchmarkResponse.getDurationSeconds());
    }

    @Test
    void testBenchmarkValidation() throws Exception {
        // Test with invalid request (negative values)
        BenchmarkRequest invalidRequest = new BenchmarkRequest(-1, 10L, 5, 1);

        mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPerformanceTarget() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt())).thenReturn(true);
        
        // Test the performance target checking functionality
        BenchmarkRequest request = new BenchmarkRequest(3, 50L, 3, 1);
        request.setKeyPrefix("test_performance");

        String response = mockMvc.perform(post("/api/benchmark/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BenchmarkResponse benchmarkResponse = objectMapper.readValue(response, BenchmarkResponse.class);

        assertTrue(benchmarkResponse.isSuccess());
        
        // Test the performance target method
        boolean meetsLowTarget = benchmarkResponse.meetsPerformanceTarget(10.0); // 10 req/sec
        assertTrue(meetsLowTarget, "Should meet low performance target");
        
        // The high target might not be met depending on system resources
        boolean meetsHighTarget = benchmarkResponse.meetsPerformanceTarget(1000.0); // 1000 req/sec
        // Don't assert this as it depends on system capabilities
    }
}