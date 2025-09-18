package dev.bnacar.distributedratelimiter.security;

import dev.bnacar.distributedratelimiter.config.SecurityConfiguration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.IOException;

@Component
public class SecurityFilter implements Filter {

    private final SecurityConfiguration securityConfiguration;

    @Autowired
    public SecurityFilter(SecurityConfiguration securityConfiguration) {
        this.securityConfiguration = securityConfiguration;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Allow CORS preflight requests to pass through
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Check request size limits
        if (!isRequestSizeValid(httpRequest)) {
            httpResponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            httpResponse.getWriter().write("{\"error\":\"Request size exceeds maximum allowed size\"}");
            httpResponse.setContentType("application/json");
            return;
        }

        // Add security headers
        if (securityConfiguration.getHeaders().isEnabled()) {
            addSecurityHeaders(httpResponse);
        }

        chain.doFilter(request, response);
    }

    private boolean isRequestSizeValid(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        if (contentLength == -1) {
            return true; // Content length not specified
        }

        try {
            DataSize maxSize = DataSize.parse(securityConfiguration.getMaxRequestSize());
            return contentLength <= maxSize.toBytes();
        } catch (Exception e) {
            // Default to 1MB if parsing fails
            return contentLength <= DataSize.ofMegabytes(1).toBytes();
        }
    }

    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }
}