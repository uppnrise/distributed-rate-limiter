package dev.bnacar.distributedratelimiter.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class RedisRateLimiterConfigurationTest {

    @Test
    void rateLimiterRedisTemplateUsesJackson3Serializer() {
        RedisRateLimiterConfiguration configuration = new RedisRateLimiterConfiguration();

        RedisTemplate<String, Object> template =
            configuration.rateLimiterRedisTemplate(mock(RedisConnectionFactory.class));

        assertInstanceOf(GenericJacksonJsonRedisSerializer.class, template.getValueSerializer());
        assertInstanceOf(GenericJacksonJsonRedisSerializer.class, template.getHashValueSerializer());
    }
}
