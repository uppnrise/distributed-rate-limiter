package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private SecurityConfiguration securityConfiguration;
    private SecurityFilter securityFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        securityConfiguration = new SecurityConfiguration();
        securityFilter = new SecurityFilter(securityConfiguration);
    }

    @Test
    public void testValidRequestSizePasses() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(1024L); // 1KB
        securityConfiguration.setMaxRequestSize("1MB");

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testRequestSizeExceedsLimit() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(2 * 1024 * 1024L); // 2MB
        securityConfiguration.setMaxRequestSize("1MB");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        
        assertTrue(stringWriter.toString().contains("Request size exceeds maximum allowed size"));
    }

    @Test
    public void testUnknownContentLengthPasses() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(-1L); // Unknown content length

        securityFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testSecurityHeadersAdded() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(100L);
        securityConfiguration.getHeaders().setEnabled(true);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setHeader("X-Content-Type-Options", "nosniff");
        verify(response).setHeader("X-Frame-Options", "DENY");
        verify(response).setHeader("X-XSS-Protection", "1; mode=block");
        verify(response).setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        verify(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setHeader("Expires", "0");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testSecurityHeadersDisabled() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(100L);
        securityConfiguration.getHeaders().setEnabled(false);

        securityFilter.doFilter(request, response, filterChain);

        verify(response, never()).setHeader(eq("X-Content-Type-Options"), any());
        verify(response, never()).setHeader(eq("X-Frame-Options"), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testInvalidMaxSizeConfigurationDefaultsTo1MB() throws IOException, ServletException {
        when(request.getContentLengthLong()).thenReturn(2 * 1024 * 1024L); // 2MB
        securityConfiguration.setMaxRequestSize("invalid-size");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        securityFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        verify(filterChain, never()).doFilter(request, response);
    }
}