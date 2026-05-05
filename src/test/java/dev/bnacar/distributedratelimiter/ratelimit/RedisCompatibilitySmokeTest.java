package dev.bnacar.distributedratelimiter.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import dev.bnacar.distributedratelimiter.RedisTestContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "ratelimiter.redis.enabled=true",
        "ratelimiter.capacity=5",
        "ratelimiter.refillRate=1"
})
class RedisCompatibilitySmokeTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            RedisTestContainerFactory.newRedisContainer(RedisTestContainerFactory.LEGACY_COMPATIBILITY_IMAGE);

    @LocalServerPort
    private int port;

    @Autowired
    private DistributedRateLimiterService distributedRateLimiterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        distributedRateLimiterService.clearAllBuckets();
        redisTemplate.delete("compat:token");
        redisTemplate.delete("leaky_bucket:compat:leaky:queue");
        redisTemplate.delete("leaky_bucket:compat:leaky:meta");
    }

    @Test
    void tokenBucketWorksWithRedis7() {
        RedisTokenBucket bucket = new RedisTokenBucket("compat:token", 5, 1, redisTemplate);

        assertTrue(bucket.tryConsume(3));
        assertThat(bucket.getCurrentTokens()).isEqualTo(2);
        assertFalse(bucket.tryConsume(3));
    }

    @Test
    void leakyBucketWorksWithRedis7() {
        RedisLeakyBucket bucket = new RedisLeakyBucket("compat:leaky", 5, 2.0, 5000, redisTemplate);

        try {
            assertTrue(bucket.tryConsume(2));
            assertThat(bucket.getQueueSize()).isEqualTo(2);

            RedisLeakyBucket anotherInstance = new RedisLeakyBucket("compat:leaky", 5, 2.0, 5000, redisTemplate);
            assertThat(anotherInstance.getQueueSize()).isEqualTo(2);
            assertThat(anotherInstance.getCurrentTokens()).isEqualTo(3);
        } finally {
            bucket.clearQueue();
        }
    }

    @Test
    void distributedServiceSharesStateWithRedis7() {
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(5);
        config.setRefillRate(1);

        ConfigurationResolver resolver = new ConfigurationResolver(config);
        DistributedRateLimiterService instance1 = new DistributedRateLimiterService(resolver, redisTemplate);
        DistributedRateLimiterService instance2 = new DistributedRateLimiterService(resolver, redisTemplate);

        assertTrue(instance1.isAllowed("compat:distributed", 3));
        assertTrue(instance2.isAllowed("compat:distributed", 2));
        assertFalse(instance1.isAllowed("compat:distributed", 1));
    }

    @Test
    void httpEndpointsWorkWithRedis7() {
        ResponseEntity<Map> rateLimitResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/ratelimit/check",
                Map.of("key", "compat:http", "tokens", 1, "apiKey", "api-key-1"),
                Map.class
        );
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                Map.class
        );

        assertThat(rateLimitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rateLimitResponse.getBody()).isNotNull();
        assertThat(rateLimitResponse.getBody()).containsKey("allowed");

        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).isNotNull();
        assertThat(healthResponse.getBody().get("status")).isEqualTo("UP");
    }
}
