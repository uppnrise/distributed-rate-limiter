package dev.bnacar.distributedratelimiter.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple admin authentication configuration using interceptors.
 */
@Configuration
public class AdminAuthConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/admin/**");
    }

    /**
     * Simple HTTP Basic Auth interceptor for admin endpoints.
     */
    private static class AdminAuthInterceptor implements HandlerInterceptor {
        
        private static final String ADMIN_USERNAME = "admin";
        private static final String ADMIN_PASSWORD = "admin123";

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
                throws Exception {
            
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Basic ")) {
                sendUnauthorized(response);
                return false;
            }

            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                
                if (parts.length == 2 && ADMIN_USERNAME.equals(parts[0]) && ADMIN_PASSWORD.equals(parts[1])) {
                    return true;
                }
            } catch (Exception e) {
                // Invalid base64 or other parsing error
            }

            sendUnauthorized(response);
            return false;
        }

        private void sendUnauthorized(HttpServletResponse response) throws Exception {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Admin\"");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Admin credentials required\"}");
            response.setContentType("application/json");
        }
    }
}