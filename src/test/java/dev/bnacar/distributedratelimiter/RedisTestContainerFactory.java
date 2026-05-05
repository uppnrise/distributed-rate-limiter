package dev.bnacar.distributedratelimiter;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class RedisTestContainerFactory {

    public static final String DEFAULT_REDIS_IMAGE = "redis:8-alpine";
    public static final String LEGACY_COMPATIBILITY_IMAGE = "redis:7-alpine";

    private static final String REDIS_IMAGE_PROPERTY = "test.redis.image";

    private RedisTestContainerFactory() {
    }

    public static GenericContainer<?> newRedisContainer() {
        return newRedisContainer(System.getProperty(REDIS_IMAGE_PROPERTY, DEFAULT_REDIS_IMAGE));
    }

    public static GenericContainer<?> newRedisContainer(String imageName) {
        return new GenericContainer<>(DockerImageName.parse(imageName))
                .withExposedPorts(6379)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2));
    }
}
