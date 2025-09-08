package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisConnectionFailureTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private DistributedRateLimiterService distributedRateLimiterService;
    private RateLimiterBackend redisBackend;
    private RateLimiterBackend fallbackBackend;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create backends
        redisBackend = new RedisRateLimiterBackend(redisTemplate);
        fallbackBackend = new InMemoryRateLimiterBackend();
        
        // Create service with mocked Redis
        ConfigurationResolver resolver = new ConfigurationResolver(new RateLimiterConfiguration());
        distributedRateLimiterService = new DistributedRateLimiterService(resolver, redisBackend, fallbackBackend);
    }

    @Test
    void testRedisConnectionFailureDetection() {
        // Mock Redis ping to throw exception
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenThrow(new DataAccessException("Connection failed") {});
        
        assertFalse(redisBackend.isAvailable());
    }

    @Test
    void testFallbackToInMemoryWhenRedisUnavailable() {
        // Mock Redis operations to fail
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenThrow(new DataAccessException("Connection failed") {});
        when(redisTemplate.execute(any(), any(), any()))
            .thenThrow(new DataAccessException("Connection failed") {});
        
        String key = "test:fallback";
        
        // Service should fallback to in-memory and still work
        assertTrue(distributedRateLimiterService.isUsingFallback());
        assertTrue(distributedRateLimiterService.isAllowed(key, 5));
        assertTrue(distributedRateLimiterService.isAllowed(key, 5));
        assertFalse(distributedRateLimiterService.isAllowed(key, 1));
    }

    @Test
    void testRedisRecovery() {
        // Initially Redis is failing
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenThrow(new DataAccessException("Connection failed") {});
        
        String key = "test:recovery";
        
        // Should use fallback
        assertTrue(distributedRateLimiterService.isUsingFallback());
        assertTrue(distributedRateLimiterService.isAllowed(key, 3));
        
        // Now Redis recovers
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenReturn(null);
        when(redisTemplate.execute(any(), any(), any()))
            .thenReturn(java.util.Arrays.asList(1, 7, 10, 2, System.currentTimeMillis()));
        
        // Should switch back to Redis
        assertTrue(distributedRateLimiterService.isUsingRedis());
        assertFalse(distributedRateLimiterService.isUsingFallback());
    }

    @Test
    void testRedisTokenBucketFailsGracefully() {
        // Mock Redis script execution to fail
        when(redisTemplate.execute(any(), any(), any()))
            .thenThrow(new DataAccessException("Redis operation failed") {});
        
        RedisTokenBucket redisTokenBucket = new RedisTokenBucket("test:key", 10, 2, redisTemplate);
        
        // Should throw exception instead of returning incorrect result
        assertThrows(RuntimeException.class, () -> redisTokenBucket.tryConsume(5));
    }

    @Test
    void testInMemoryBackendAlwaysAvailable() {
        InMemoryRateLimiterBackend inMemoryBackend = new InMemoryRateLimiterBackend();
        assertTrue(inMemoryBackend.isAvailable());
        
        // Should work normally
        RateLimitConfig config = new RateLimitConfig(10, 2);
        RateLimiter limiter = inMemoryBackend.getRateLimiter("test:key", config);
        assertTrue(limiter.tryConsume(5));
        assertEquals(5, limiter.getCurrentTokens());
    }

    @Test
    void testFallbackBehaviorWithMultipleKeys() {
        // Mock Redis to fail
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
            .thenThrow(new DataAccessException("Connection failed") {});
        
        String key1 = "test:key1";
        String key2 = "test:key2";
        
        // Should use fallback for both keys
        assertTrue(distributedRateLimiterService.isUsingFallback());
        
        // Each key should have independent limits in fallback mode
        assertTrue(distributedRateLimiterService.isAllowed(key1, 10));
        assertTrue(distributedRateLimiterService.isAllowed(key2, 10));
        
        assertFalse(distributedRateLimiterService.isAllowed(key1, 1));
        assertFalse(distributedRateLimiterService.isAllowed(key2, 1));
    }

    @Test
    void testRedisBackendClearOperation() {
        // Mock Redis operations
        when(redisTemplate.keys(anyString())).thenReturn(java.util.Set.of("key1", "key2"));
        doNothing().when(redisTemplate).delete(any(java.util.Collection.class));
        
        redisBackend.clear();
        
        verify(redisTemplate).keys("rate_limit:*");
        verify(redisTemplate).delete(any(java.util.Collection.class));
    }

    @Test
    void testRedisBackendActiveCountOperation() {
        // Mock Redis operations
        when(redisTemplate.keys(anyString())).thenReturn(java.util.Set.of("key1", "key2", "key3"));
        
        assertEquals(3, redisBackend.getActiveCount());
        
        verify(redisTemplate).keys("rate_limit:*");
    }

    @Test
    void testRedisBackendHandlesExceptionsGracefully() {
        // Mock Redis operations to throw exceptions
        when(redisTemplate.keys(anyString())).thenThrow(new DataAccessException("Connection error") {});
        when(redisTemplate.delete(any(java.util.Collection.class))).thenThrow(new DataAccessException("Connection error") {});
        
        // Should not throw exceptions
        assertDoesNotThrow(() -> redisBackend.clear());
        assertEquals(0, redisBackend.getActiveCount());
    }
}