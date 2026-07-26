package dev.bnacar.distributedratelimiter.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.concurrent.Executor;

/**
 * Redis configuration for distributed rate limiting.
 * Provides RedisTemplate configuration with proper serializers and async support.
 */
@Configuration
@EnableAsync
public class RedisRateLimiterConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "ratelimiter.redis.enabled", havingValue = "true", matchIfMissing = true)
    public RedisTemplate<String, Object> rateLimiterRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use Jackson 3 JSON serialization with an explicit type allow-list.
        var typeValidator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("dev.bnacar.distributedratelimiter.")
            .allowIfSubType("java.lang.")
            .allowIfSubType("java.util.")
            .allowIfSubTypeIsArray()
            .build();
        var jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator)
            .build();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // Enable transaction support for better consistency
        template.setEnableTransactionSupport(true);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Thread pool executor for async rate limiter operations like cleanup tasks.
     */
    @Bean(name = "rateLimiterTaskExecutor")
    public Executor rateLimiterTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("RateLimiter-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
