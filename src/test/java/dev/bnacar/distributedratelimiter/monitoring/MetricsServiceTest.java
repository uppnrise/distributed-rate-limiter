package dev.bnacar.distributedratelimiter.monitoring;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void onApplicationEvent_ContextClosed_ShouldStopFurtherHealthChecks() throws Exception {
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        metricsService.setRedisConnectionFactory(redisConnectionFactory);
        metricsService.initialize();

        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();
        metricsService.onApplicationEvent(new ContextClosedEvent(context));

        // Reset interactions recorded by any health check that may have already
        // run before the context-closed event was handled.
        clearInvocations(redisConnectionFactory);

        // Directly invoke the scheduled health check task: after the context
        // has closed, it must be a no-op and must not touch the (possibly
        // already-stopped) Redis connection factory.
        Method checkRedisHealth = MetricsService.class.getDeclaredMethod("checkRedisHealth");
        checkRedisHealth.setAccessible(true);
        checkRedisHealth.invoke(metricsService);

        verifyNoInteractions(redisConnectionFactory);

        context.close();
    }

    @Test
    void checkRedisHealth_WhenConnectionFactoryStopped_ShouldLogQuietlyAndStopPolling() throws Exception {
        // Simulates the scenario where the LettuceConnectionFactory reaches
        // its terminal STOPPED state (e.g. its backing container was torn
        // down in tests) without the application context itself closing.
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        when(redisConnectionFactory.getConnection())
                .thenThrow(new IllegalStateException(
                        "LettuceConnectionFactory has been STOPPED. Use start() to initialize it"));
        metricsService.setRedisConnectionFactory(redisConnectionFactory);
        metricsService.initialize();
        metricsService.setRedisConnected(true);

        Method checkRedisHealth = MetricsService.class.getDeclaredMethod("checkRedisHealth");
        checkRedisHealth.setAccessible(true);
        checkRedisHealth.invoke(metricsService);

        assertFalse(metricsService.isRedisConnected());

        // A subsequent invocation must be a no-op: the health-check scheduler
        // is expected to have stopped itself, so the factory must not be
        // queried again.
        clearInvocations(redisConnectionFactory);
        checkRedisHealth.invoke(metricsService);
        verifyNoInteractions(redisConnectionFactory);
    }
}