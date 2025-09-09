package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Redis connection pool behavior and configuration.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.data.redis.lettuce.pool.enabled=true",
    "spring.data.redis.lettuce.pool.max-active=10",
    "spring.data.redis.lettuce.pool.max-idle=5",
    "spring.data.redis.lettuce.pool.min-idle=2"
})
class RedisConnectionPoolTest {

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4.1-alpine")
            .withExposedPorts(6379);

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Test
    void testConnectionPoolConfiguration() {
        assertNotNull(connectionFactory);
        assertTrue(connectionFactory instanceof LettuceConnectionFactory);
        
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
        // Verify that the connection factory is properly configured
        assertNotNull(lettuceFactory.getClientConfiguration());
    }

    @Test
    void testConnectionPoolPerformance() throws InterruptedException {
        // Test that multiple concurrent connections can be obtained efficiently
        long startTime = System.currentTimeMillis();
        
        // Simulate multiple concurrent requests for connections
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    try (var connection = connectionFactory.getConnection()) {
                        connection.ping();
                        Thread.sleep(1); // Small delay to simulate work
                    } catch (Exception e) {
                        fail("Connection pool test failed: " + e.getMessage());
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // With proper connection pooling, this should complete quickly
        // 200 operations (20 threads * 10 operations) should complete in under 5 seconds
        assertTrue(duration < 5000, "Connection pool operations took too long: " + duration + "ms");
    }

    @Test
    void testConnectionReuseAndHealth() {
        // Test that connections are properly reused and healthy
        for (int i = 0; i < 100; i++) {
            try (var connection = connectionFactory.getConnection()) {
                connection.ping();
                
                // Basic Redis operation to ensure connection is working
                connection.set("test:pool:key".getBytes(), ("value" + i).getBytes());
                byte[] retrieved = connection.get("test:pool:key".getBytes());
                assertNotNull(retrieved);
                assertEquals("value" + i, new String(retrieved));
            }
        }
    }
}