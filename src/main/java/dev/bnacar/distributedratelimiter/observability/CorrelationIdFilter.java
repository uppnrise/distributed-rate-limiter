package dev.bnacar.distributedratelimiter.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds correlation ID to every HTTP request for distributed tracing.
 * The correlation ID is extracted from headers or generated if not present.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String SPAN_ID_HEADER = "X-Span-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String correlationId = extractOrGenerateCorrelationId(request);
        String traceId = extractOrGenerateTraceId(request);
        String spanId = generateSpanId();
        
        try {
            // Set MDC for this request
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            MDC.put(SPAN_ID_MDC_KEY, spanId);
            
            // Add headers to response for downstream services
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(SPAN_ID_HEADER, spanId);
            
            logger.debug("Request started with correlation_id={}, trace_id={}, span_id={}, method={}, uri={}",
                    correlationId, traceId, spanId, request.getMethod(), request.getRequestURI());
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Clear MDC after request processing
            MDC.clear();
            logger.debug("Request completed with correlation_id={}", correlationId);
        }
    }
    
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
    
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }
    
    private String generateSpanId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}