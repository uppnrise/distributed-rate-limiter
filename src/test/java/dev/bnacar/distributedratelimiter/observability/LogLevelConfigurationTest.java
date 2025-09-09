package dev.bnacar.distributedratelimiter.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for log level configuration and dynamic changes.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.dev.bnacar.distributedratelimiter.ratelimit=DEBUG",
    "logging.level.dev.bnacar.distributedratelimiter.monitoring=INFO"
})
class LogLevelConfigurationTest {

    @Test
    void shouldRespectConfiguredLogLevels() {
        // Given
        Logger rateLimiterLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService");
        Logger monitoringLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.monitoring.MetricsService");
        Logger observabilityLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.observability.CorrelationIdFilter");

        // Then - check effective log levels (using isEnabled methods since getLevel() might be null due to inheritance)
        assertThat(rateLimiterLogger.isDebugEnabled()).isTrue();
        assertThat(monitoringLogger.isInfoEnabled()).isTrue();
        assertThat(monitoringLogger.isDebugEnabled()).isFalse(); // Should be false since level is INFO
        
        // Observability logger should inherit from parent or default
        assertThat(observabilityLogger.isDebugEnabled()).isTrue();
    }

    @Test
    void shouldAllowDynamicLogLevelChanges() {
        // Given
        Logger logger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.test");
        
        // When - change log level programmatically
        logger.setLevel(Level.WARN);
        
        // Then
        assertThat(logger.getLevel()).isEqualTo(Level.WARN);
        assertThat(logger.isInfoEnabled()).isFalse();
        assertThat(logger.isWarnEnabled()).isTrue();
        
        // When - change to debug
        logger.setLevel(Level.DEBUG);
        
        // Then
        assertThat(logger.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(logger.isDebugEnabled()).isTrue();
    }

    @Test
    void shouldHaveCorrectLoggerHierarchy() {
        // Given
        Logger rootAppLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter");
        Logger rateLimiterLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.ratelimit");
        Logger serviceLogger = (Logger) LoggerFactory.getLogger("dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService");

        // Then - check logger hierarchy is properly set up
        assertThat(serviceLogger.getName()).startsWith(rateLimiterLogger.getName());
        assertThat(rateLimiterLogger.getName()).startsWith(rootAppLogger.getName());
        
        // Check effective levels are inherited properly
        assertThat(serviceLogger.isDebugEnabled()).isTrue();
        assertThat(rateLimiterLogger.isDebugEnabled()).isTrue();
    }
}