package dev.bnacar.distributedratelimiter.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import dev.bnacar.distributedratelimiter.ratelimit.ConfigurationResolver;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterConfiguration;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for structured logging in rate limiting operations.
 */
@ExtendWith(MockitoExtension.class)
class StructuredLoggingTest {

    @Mock
    private MetricsService metricsService;

    private RateLimiterService rateLimiterService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Set up log capture for RateLimiterService
        logger = (Logger) LoggerFactory.getLogger(RateLimiterService.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        // Create service with small limits for testing
        RateLimiterConfiguration config = new RateLimiterConfiguration();
        config.setCapacity(2);
        config.setRefillRate(1);
        config.setCleanupIntervalMs(10000);

        ConfigurationResolver resolver = new ConfigurationResolver(config);
        rateLimiterService = new RateLimiterService(resolver, config, metricsService);
    }

    @Test
    void shouldLogRateLimitViolationWithStructuredData() {
        // Given - exhaust the bucket
        String key = "test-key";
        rateLimiterService.isAllowed(key, 2); // Use all tokens
        listAppender.list.clear(); // Clear setup logs

        // When - request should be denied
        boolean allowed = rateLimiterService.isAllowed(key, 1);

        // Then
        assertThat(allowed).isFalse();

        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).isNotEmpty();

        // Find the rate limit violation log
        ILoggingEvent violationEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Rate limit VIOLATED"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No rate limit violation log found"));

        assertThat(violationEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(violationEvent.getFormattedMessage()).contains("key=test-key");
        assertThat(violationEvent.getFormattedMessage()).contains("tokens_requested=1");
        assertThat(violationEvent.getFormattedMessage()).contains("capacity=2");
        assertThat(violationEvent.getFormattedMessage()).contains("processing_time_ms=");
    }

    @Test
    void shouldLogSuccessfulRequestWithMetrics() {
        // Given
        String key = "success-key";

        // When
        boolean allowed = rateLimiterService.isAllowed(key, 1);

        // Then
        assertThat(allowed).isTrue();

        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Find the successful request log
        ILoggingEvent successEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Rate limit ALLOWED"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No successful request log found"));

        assertThat(successEvent.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(successEvent.getFormattedMessage()).contains("key=success-key");
        assertThat(successEvent.getFormattedMessage()).contains("tokens_requested=1");
        assertThat(successEvent.getFormattedMessage()).contains("remaining_tokens=1");
        assertThat(successEvent.getFormattedMessage()).contains("processing_time_ms=");
    }

    @Test
    void shouldLogBucketCreation() {
        // Given
        String key = "new-bucket-key";

        // When
        rateLimiterService.isAllowed(key, 1);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Find the bucket creation log
        ILoggingEvent creationEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Created new bucket for key"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No bucket creation log found"));

        assertThat(creationEvent.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(creationEvent.getFormattedMessage()).contains("key=new-bucket-key");
        assertThat(creationEvent.getFormattedMessage()).contains("capacity=2");
        assertThat(creationEvent.getFormattedMessage()).contains("refillRate=1");
        assertThat(creationEvent.getFormattedMessage()).contains("algorithm=TOKEN_BUCKET");
    }

    @Test
    void shouldHandleInvalidTokenRequest() {
        // Given
        String key = "invalid-key";

        // When
        boolean allowed = rateLimiterService.isAllowed(key, 0);

        // Then
        assertThat(allowed).isFalse();

        List<ILoggingEvent> logEvents = listAppender.list;
        
        // Find the invalid request log
        ILoggingEvent invalidEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Invalid token request"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No invalid request log found"));

        assertThat(invalidEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(invalidEvent.getFormattedMessage()).contains("key=invalid-key");
        assertThat(invalidEvent.getFormattedMessage()).contains("tokens=0");
    }

    @Test
    void shouldIncludeMDCInViolationLogs() {
        // Given - set up MDC context
        MDC.put("test-context", "test-value");
        String key = "mdc-test-key";
        rateLimiterService.isAllowed(key, 2); // Exhaust bucket
        listAppender.list.clear();

        // When - trigger violation
        rateLimiterService.isAllowed(key, 1);

        // Then - check that violation details are captured
        List<ILoggingEvent> logEvents = listAppender.list;
        
        ILoggingEvent detailsEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Rate limit violation details captured"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No violation details log found"));

        assertThat(detailsEvent.getLevel()).isEqualTo(Level.INFO);
        
        // Check that our original MDC context is preserved
        assertThat(MDC.get("test-context")).isEqualTo("test-value");
        
        // Clean up
        MDC.clear();
    }

    @Test
    void shouldLogProcessingTimeForAllRequests() {
        // Given
        String key = "timing-test-key";

        // When
        rateLimiterService.isAllowed(key, 1);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        
        // All rate limit decision logs should include processing time
        long eventsWithProcessingTime = logEvents.stream()
                .filter(event -> event.getMessage().contains("Rate limit ALLOWED") || 
                               event.getMessage().contains("Rate limit VIOLATED"))
                .filter(event -> event.getFormattedMessage().contains("processing_time_ms="))
                .count();

        assertThat(eventsWithProcessingTime).isGreaterThan(0);
    }
}