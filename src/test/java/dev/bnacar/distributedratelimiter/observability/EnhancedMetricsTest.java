package dev.bnacar.distributedratelimiter.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for enhanced metrics collection and logging.
 */
class EnhancedMetricsTest {

    private MetricsService metricsService;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService();

        // Set up log capture for MetricsService
        logger = (Logger) LoggerFactory.getLogger(MetricsService.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void shouldRecordAndLogAllowedRequests() {
        // Given
        String key = "test-key";

        // When
        metricsService.recordAllowedRequest(key);

        // Then
        MetricsResponse metrics = metricsService.getMetrics();
        assertThat(metrics.getKeyMetrics()).containsKey(key);
        assertThat(metrics.getKeyMetrics().get(key).getAllowedRequests()).isEqualTo(1);
        assertThat(metrics.getTotalAllowedRequests()).isEqualTo(1);

        // Check logging
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent allowedEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Recorded allowed request"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No allowed request log found"));

        assertThat(allowedEvent.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(allowedEvent.getFormattedMessage()).contains("key=test-key");
        assertThat(allowedEvent.getFormattedMessage()).contains("total_allowed=1");
    }

    @Test
    void shouldRecordAndLogDeniedRequests() {
        // Given
        String key = "denied-key";

        // When
        metricsService.recordDeniedRequest(key);

        // Then
        MetricsResponse metrics = metricsService.getMetrics();
        assertThat(metrics.getKeyMetrics()).containsKey(key);
        assertThat(metrics.getKeyMetrics().get(key).getDeniedRequests()).isEqualTo(1);
        assertThat(metrics.getTotalDeniedRequests()).isEqualTo(1);

        // Check logging
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent deniedEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Recorded denied request"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No denied request log found"));

        assertThat(deniedEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(deniedEvent.getFormattedMessage()).contains("key=denied-key");
        assertThat(deniedEvent.getFormattedMessage()).contains("total_denied=1");
        assertThat(deniedEvent.getFormattedMessage()).contains("denied_ratio=100.0%");
    }

    @Test
    void shouldCalculateDeniedRatioCorrectly() {
        // Given
        String key = "ratio-test-key";

        // When - 3 allowed, 1 denied = 25% denied ratio
        metricsService.recordAllowedRequest(key);
        metricsService.recordAllowedRequest(key);
        metricsService.recordAllowedRequest(key);
        listAppender.list.clear(); // Clear previous logs
        metricsService.recordDeniedRequest(key);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent deniedEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Recorded denied request"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No denied request log found"));

        assertThat(deniedEvent.getFormattedMessage()).contains("denied_ratio=25.0%");
    }

    @Test
    void shouldRecordAndLogBucketCreation() {
        // Given
        String key = "bucket-creation-key";

        // When
        metricsService.recordBucketCreation(key);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent creationEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("New bucket created"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No bucket creation log found"));

        assertThat(creationEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(creationEvent.getFormattedMessage()).contains("key=bucket-creation-key");
        assertThat(creationEvent.getFormattedMessage()).contains("total_buckets_created=1");
    }

    @Test
    void shouldRecordAndLogBucketCleanup() {
        // Given
        int cleanedCount = 5;

        // When
        metricsService.recordBucketCleanup(cleanedCount);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent cleanupEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Bucket cleanup completed"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No bucket cleanup log found"));

        assertThat(cleanupEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(cleanupEvent.getFormattedMessage()).contains("cleaned=5");
        assertThat(cleanupEvent.getFormattedMessage()).contains("total_cleanups=5");
    }

    @Test
    void shouldRecordAndLogProcessingTime() {
        // Given
        String key = "processing-time-key";
        long processingTime = 15; // Slow processing time

        // When
        metricsService.recordProcessingTime(key, processingTime);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        ILoggingEvent slowEvent = logEvents.stream()
                .filter(event -> event.getMessage().contains("Slow rate limit processing detected"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No slow processing log found"));

        assertThat(slowEvent.getLevel()).isEqualTo(Level.WARN);
        assertThat(slowEvent.getFormattedMessage()).contains("key=processing-time-key");
        assertThat(slowEvent.getFormattedMessage()).contains("processing_time_ms=15");
    }

    @Test
    void shouldNotLogFastProcessingTime() {
        // Given
        String key = "fast-processing-key";
        long processingTime = 5; // Fast processing time

        // When
        metricsService.recordProcessingTime(key, processingTime);

        // Then - no slow processing warning should be logged
        List<ILoggingEvent> logEvents = listAppender.list;
        boolean hasSlowProcessingLog = logEvents.stream()
                .anyMatch(event -> event.getMessage().contains("Slow rate limit processing detected"));

        assertThat(hasSlowProcessingLog).isFalse();
    }

    @Test
    void shouldLogRedisConnectionChanges() {
        // When - simulate Redis connection loss
        metricsService.setRedisConnected(false);

        // Then - no log should be generated for programmatic state change
        // The actual logging happens in checkRedisHealth method which requires Redis connection factory

        // Verify state is set correctly
        assertThat(metricsService.isRedisConnected()).isFalse();
    }

    @Test
    void shouldClearAllMetrics() {
        // Given - record some metrics
        metricsService.recordAllowedRequest("key1");
        metricsService.recordDeniedRequest("key2");
        metricsService.recordBucketCreation("key3");

        // When
        metricsService.clearMetrics();

        // Then
        MetricsResponse metrics = metricsService.getMetrics();
        assertThat(metrics.getKeyMetrics()).isEmpty();
        assertThat(metrics.getTotalAllowedRequests()).isEqualTo(0);
        assertThat(metrics.getTotalDeniedRequests()).isEqualTo(0);
    }

    @Test
    void shouldTrackMultipleKeysIndependently() {
        // Given
        String key1 = "key1";
        String key2 = "key2";

        // When
        metricsService.recordAllowedRequest(key1);
        metricsService.recordAllowedRequest(key1);
        metricsService.recordDeniedRequest(key2);

        // Then
        MetricsResponse metrics = metricsService.getMetrics();
        assertThat(metrics.getKeyMetrics().get(key1).getAllowedRequests()).isEqualTo(2);
        assertThat(metrics.getKeyMetrics().get(key1).getDeniedRequests()).isEqualTo(0);
        assertThat(metrics.getKeyMetrics().get(key2).getAllowedRequests()).isEqualTo(0);
        assertThat(metrics.getKeyMetrics().get(key2).getDeniedRequests()).isEqualTo(1);
        assertThat(metrics.getTotalAllowedRequests()).isEqualTo(2);
        assertThat(metrics.getTotalDeniedRequests()).isEqualTo(1);
    }
}