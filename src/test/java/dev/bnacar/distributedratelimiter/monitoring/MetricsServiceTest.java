package dev.bnacar.distributedratelimiter.monitoring;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();
    }

    @Test
    void recordAllowedRequest_ShouldIncrementCounters() {
        String key = "user1";
        
        metricsService.recordAllowedRequest(key);
        metricsService.recordAllowedRequest(key);
        
        MetricsResponse metrics = metricsService.getMetrics();
        
        assertEquals(2, metrics.getTotalAllowedRequests());
        assertEquals(0, metrics.getTotalDeniedRequests());
        assertTrue(metrics.getKeyMetrics().containsKey(key));
        assertEquals(2, metrics.getKeyMetrics().get(key).getAllowedRequests());
        assertEquals(0, metrics.getKeyMetrics().get(key).getDeniedRequests());
    }

    @Test
    void recordDeniedRequest_ShouldIncrementCounters() {
        String key = "user1";
        
        metricsService.recordDeniedRequest(key);
        metricsService.recordDeniedRequest(key);
        metricsService.recordDeniedRequest(key);
        
        MetricsResponse metrics = metricsService.getMetrics();
        
        assertEquals(0, metrics.getTotalAllowedRequests());
        assertEquals(3, metrics.getTotalDeniedRequests());
        assertTrue(metrics.getKeyMetrics().containsKey(key));
        assertEquals(0, metrics.getKeyMetrics().get(key).getAllowedRequests());
        assertEquals(3, metrics.getKeyMetrics().get(key).getDeniedRequests());
    }

    @Test
    void recordMixedRequests_ShouldTrackBothTypes() {
        String key1 = "user1";
        String key2 = "user2";
        
        metricsService.recordAllowedRequest(key1);
        metricsService.recordAllowedRequest(key1);
        metricsService.recordDeniedRequest(key1);
        
        metricsService.recordAllowedRequest(key2);
        metricsService.recordDeniedRequest(key2);
        metricsService.recordDeniedRequest(key2);
        
        MetricsResponse metrics = metricsService.getMetrics();
        
        assertEquals(3, metrics.getTotalAllowedRequests());
        assertEquals(3, metrics.getTotalDeniedRequests());
        
        assertEquals(2, metrics.getKeyMetrics().get(key1).getAllowedRequests());
        assertEquals(1, metrics.getKeyMetrics().get(key1).getDeniedRequests());
        
        assertEquals(1, metrics.getKeyMetrics().get(key2).getAllowedRequests());
        assertEquals(2, metrics.getKeyMetrics().get(key2).getDeniedRequests());
    }

    @Test
    void setRedisConnected_ShouldUpdateConnectionStatus() {
        assertFalse(metricsService.isRedisConnected());
        
        metricsService.setRedisConnected(true);
        assertTrue(metricsService.isRedisConnected());
        
        MetricsResponse metrics = metricsService.getMetrics();
        assertTrue(metrics.isRedisConnected());
        
        metricsService.setRedisConnected(false);
        assertFalse(metricsService.isRedisConnected());
        
        metrics = metricsService.getMetrics();
        assertFalse(metrics.isRedisConnected());
    }

    @Test
    void clearMetrics_ShouldResetAllCounters() {
        String key = "user1";
        
        metricsService.recordAllowedRequest(key);
        metricsService.recordDeniedRequest(key);
        metricsService.setRedisConnected(true);
        
        MetricsResponse metrics = metricsService.getMetrics();
        assertEquals(1, metrics.getTotalAllowedRequests());
        assertEquals(1, metrics.getTotalDeniedRequests());
        assertTrue(metrics.isRedisConnected());
        
        metricsService.clearMetrics();
        
        metrics = metricsService.getMetrics();
        assertEquals(0, metrics.getTotalAllowedRequests());
        assertEquals(0, metrics.getTotalDeniedRequests());
        assertTrue(metrics.getKeyMetrics().isEmpty());
        // Redis connection status should not be affected by clearMetrics
        assertTrue(metrics.isRedisConnected());
    }

    @Test
    void getMetrics_InitialState_ShouldReturnEmptyMetrics() {
        MetricsResponse metrics = metricsService.getMetrics();
        
        assertEquals(0, metrics.getTotalAllowedRequests());
        assertEquals(0, metrics.getTotalDeniedRequests());
        assertFalse(metrics.isRedisConnected());
        assertTrue(metrics.getKeyMetrics().isEmpty());
    }

    @Test
    void lastAccessTime_ShouldBeUpdatedOnRequests() {
        String key = "user1";
        long beforeTime = System.currentTimeMillis();
        
        metricsService.recordAllowedRequest(key);
        
        MetricsResponse metrics = metricsService.getMetrics();
        long afterTime = System.currentTimeMillis();
        
        long lastAccessTime = metrics.getKeyMetrics().get(key).getLastAccessTime();
        assertTrue(lastAccessTime >= beforeTime);
        assertTrue(lastAccessTime <= afterTime);
    }
}