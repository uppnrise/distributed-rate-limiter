package dev.bnacar.distributedratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Explicit, stateless security filter chain for the rate limiter's JSON REST API.
 *
 * <p>Adding {@code spring-boot-starter-security} and declaring this bean serves two
 * purposes: it establishes a single, auditable place for the application's HTTP
 * security posture, and it makes the CSRF decision below an explicit, intentional
 * choice rather than the "protection missing" state static analysis tools such as
 * Snyk Code flag on plain Spring MVC controllers.</p>
 *
 * <p><b>Why CSRF protection is disabled:</b> CSRF attacks work by having a
 * victim's browser automatically attach an <em>ambient credential</em> (a
 * session cookie) to a forged cross-site request. This service never issues or
 * consumes session/authentication cookies - {@link dev.bnacar.distributedratelimiter.security.ApiKeyService}
 * validates an explicit {@code apiKey} supplied by the caller in the request
 * payload/headers on every call, and {@link SessionCreationPolicy#STATELESS} below
 * ensures Spring Security itself never creates an {@code HttpSession} either. With
 * no ambient credential for a forged request to piggyback on, there is nothing for
 * CSRF protection to defend (see the OWASP CSRF Prevention Cheat Sheet and the
 * Spring Security reference docs, both of which state CSRF protection is only
 * required for browser clients relying on cookie-based authentication).</p>
 */
@Configuration
public class ApiSecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }
}
