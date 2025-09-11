package dev.bnacar.distributedratelimiter.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for correlation ID filter functionality.
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter correlationIdFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        correlationIdFilter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();

        // Set up log capture
        logger = (Logger) LoggerFactory.getLogger(CorrelationIdFilter.class);
        logger.setLevel(Level.DEBUG);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void shouldGenerateCorrelationIdWhenNotPresent() throws ServletException, IOException {
        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).matches("[0-9a-f-]{36}"); // UUID format
    }

    @Test
    void shouldUseExistingCorrelationIdFromHeader() throws ServletException, IOException {
        // Given
        String existingCorrelationId = "test-correlation-id-123";
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        String responseCorrelationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseCorrelationId).isEqualTo(existingCorrelationId);
    }

    @Test
    void shouldSetAllTracingHeaders() throws ServletException, IOException {
        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNotNull();
        assertThat(response.getHeader(CorrelationIdFilter.TRACE_ID_HEADER)).isNotNull();
        assertThat(response.getHeader(CorrelationIdFilter.SPAN_ID_HEADER)).isNotNull();
        
        // Span ID should be 8 characters
        assertThat(response.getHeader(CorrelationIdFilter.SPAN_ID_HEADER)).hasSize(8);
    }

    @Test
    void shouldLogRequestStartAndCompletion() throws ServletException, IOException {
        // Given
        request.setMethod("GET");
        request.setRequestURI("/api/ratelimit");

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertThat(logEvents).hasSize(2);
        
        ILoggingEvent startEvent = logEvents.get(0);
        assertThat(startEvent.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(startEvent.getFormattedMessage()).contains("Request started with correlation_id");
        assertThat(startEvent.getFormattedMessage()).contains("method=GET");
        assertThat(startEvent.getFormattedMessage()).contains("uri=/api/ratelimit");

        ILoggingEvent endEvent = logEvents.get(1);
        assertThat(endEvent.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(endEvent.getFormattedMessage()).contains("Request completed with correlation_id");
    }

    @Test
    void shouldClearMDCAfterRequest() throws ServletException, IOException {
        // Given
        MDC.put("test-key", "test-value");

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then - MDC should be cleared except for our test key from before the filter
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.SPAN_ID_MDC_KEY)).isNull();
        assertThat(MDC.get("test-key")).isNull(); // Filter clears all MDC
    }

    @Test
    void shouldHandleEmptyCorrelationIdHeader() throws ServletException, IOException {
        // Given
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "");

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then - should generate new ID since header is empty
        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).isNotEmpty();
        assertThat(correlationId).matches("[0-9a-f-]{36}"); // UUID format
    }

    @Test
    void shouldUseExistingTraceIdFromHeader() throws ServletException, IOException {
        // Given
        String existingTraceId = "existing-trace-id-456";
        request.addHeader(CorrelationIdFilter.TRACE_ID_HEADER, existingTraceId);

        // When
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Then
        String responseTraceId = response.getHeader(CorrelationIdFilter.TRACE_ID_HEADER);
        assertThat(responseTraceId).isEqualTo(existingTraceId);
    }
}