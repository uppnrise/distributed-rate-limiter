package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.models.RateLimitResponse;
import dev.bnacar.distributedratelimiter.models.CompositeRateLimitResponse;
import dev.bnacar.distributedratelimiter.models.GeographicRateLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimiterService;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.geo.GeographicRateLimitService;
import dev.bnacar.distributedratelimiter.security.ApiKeyService;
import dev.bnacar.distributedratelimiter.security.IpAddressExtractor;
import dev.bnacar.distributedratelimiter.security.IpSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ratelimit")
@Tag(name = "Rate Limit", description = "Rate limiting operations for token bucket algorithm")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class RateLimitController {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitController.class);

    private final RateLimiterService rateLimiterService;
    private final CompositeRateLimiterService compositeRateLimiterService;
    private final GeographicRateLimitService geographicRateLimitService;
    private final ApiKeyService apiKeyService;
    private final IpSecurityService ipSecurityService;
    private final IpAddressExtractor ipAddressExtractor;
    
    @Value("${ratelimiter.geographic.enabled:false}")
    private boolean geographicRateLimitingEnabled;

    public RateLimitController(RateLimiterService rateLimiterService,
                              CompositeRateLimiterService compositeRateLimiterService,
                              @org.springframework.beans.factory.annotation.Autowired(required = false) GeographicRateLimitService geographicRateLimitService,
                              ApiKeyService apiKeyService,
                              IpSecurityService ipSecurityService,
                              IpAddressExtractor ipAddressExtractor) {
        this.rateLimiterService = rateLimiterService;
        this.compositeRateLimiterService = compositeRateLimiterService;
        this.geographicRateLimitService = geographicRateLimitService;
        this.apiKeyService = apiKeyService;
        this.ipSecurityService = ipSecurityService;
        this.ipAddressExtractor = ipAddressExtractor;
    }

    @PostMapping("/check")
    @Operation(summary = "Check rate limit for a key",
               description = "Checks if a request is allowed based on the configured rate limits for the given key. Supports geographic rate limiting when enabled.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Request allowed",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = RateLimitResponse.class),
                                     examples = @ExampleObject(value = "{\"key\":\"user:123\",\"tokensRequested\":1,\"allowed\":true}"))),
        @ApiResponse(responseCode = "429", 
                    description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = RateLimitResponse.class),
                                     examples = @ExampleObject(value = "{\"key\":\"user:123\",\"tokensRequested\":1,\"allowed\":false}"))),
        @ApiResponse(responseCode = "401", 
                    description = "Invalid API key",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = RateLimitResponse.class))),
        @ApiResponse(responseCode = "403", 
                    description = "IP address not allowed",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = RateLimitResponse.class)))
    })
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @Parameter(description = "Rate limit request containing key, tokens, optional API key, and optional client info for geographic rate limiting",
                      required = true,
                      content = @Content(examples = @ExampleObject(value = "{\"key\":\"user:123\",\"tokens\":1,\"apiKey\":\"your-api-key\",\"clientInfo\":{\"sourceIP\":\"192.168.1.1\",\"countryCode\":\"US\"}}")))
            @Valid @RequestBody RateLimitRequest request,
            HttpServletRequest httpRequest) {
        
        // Extract client IP address
        String clientIp = ipAddressExtractor.getClientIpAddress(httpRequest);
        
        // Check IP whitelist/blacklist
        if (!ipSecurityService.isIpAllowed(clientIp)) {
            RateLimitResponse response = new RateLimitResponse(request.getKey(), request.getTokens(), false);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        
        // Validate API key if provided or required
        if (!apiKeyService.isValidApiKey(request.getApiKey())) {
            RateLimitResponse response = new RateLimitResponse(request.getKey(), request.getTokens(), false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        // Create IP-based rate limiting key
        String effectiveKey = ipSecurityService.createIpBasedKey(request.getKey(), clientIp);
        
        // Extract headers for geographic detection
        Map<String, String> headers = extractGeographicHeaders(httpRequest);
        
        // Try geographic rate limiting first if enabled and service is available
        if (geographicRateLimitingEnabled && geographicRateLimitService != null) {
            try {
                GeographicRateLimitResponse geoResponse = geographicRateLimitService.checkGeographicRateLimit(
                    request, clientIp, headers
                );
                
                // Return geographic response if geographic rules were applied
                if (geoResponse.getGeoLocation() != null && 
                    !"NO_GEOGRAPHIC_RULES_APPLY".equals(geoResponse.getFallbackReason())) {
                    
                    if (geoResponse.isAllowed()) {
                        return ResponseEntity.ok(geoResponse);
                    } else {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(geoResponse);
                    }
                }
                // If no geographic rules apply, fall through to standard processing
            } catch (Exception e) {
                // Log error but continue with standard rate limiting
                logger.warn("Geographic rate limiting failed for key: {}, falling back to standard rate limiting", 
                          request.getKey(), e);
            }
        }
        
        // Check if this is a composite rate limiting request
        if (request.getAlgorithm() == RateLimitAlgorithm.COMPOSITE || request.getCompositeConfig() != null) {
            // Handle composite rate limiting
            CompositeRateLimitResponse compositeResponse = compositeRateLimiterService.checkCompositeRateLimit(
                effectiveKey, request.getTokens(), request.getCompositeConfig()
            );
            
            if (compositeResponse.isAllowed()) {
                return ResponseEntity.ok(compositeResponse);
            } else {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(compositeResponse);
            }
        } else {
            // Standard single-algorithm rate limiting
            boolean allowed = rateLimiterService.isAllowed(effectiveKey, request.getTokens());
            
            RateLimitResponse response = new RateLimitResponse(request.getKey(), request.getTokens(), allowed);
            
            if (allowed) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
            }
        }
    }
    
    /**
     * Extract geographic headers from HTTP request for location detection.
     */
    private Map<String, String> extractGeographicHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        // CloudFlare headers
        addHeaderIfPresent(headers, request, "CF-IPCountry");
        addHeaderIfPresent(headers, request, "CF-IPContinent");
        addHeaderIfPresent(headers, request, "CF-IPCity");
        addHeaderIfPresent(headers, request, "CF-Timezone");
        
        // AWS CloudFront headers
        addHeaderIfPresent(headers, request, "CloudFront-Viewer-Country");
        addHeaderIfPresent(headers, request, "CloudFront-Viewer-Country-Region");
        
        // Azure CDN headers
        addHeaderIfPresent(headers, request, "X-MS-Country-Code");
        addHeaderIfPresent(headers, request, "X-Country-Code");
        
        // Generic geographic headers
        addHeaderIfPresent(headers, request, "X-Country");
        addHeaderIfPresent(headers, request, "X-Region");
        addHeaderIfPresent(headers, request, "X-City");
        addHeaderIfPresent(headers, request, "X-Timezone");
        addHeaderIfPresent(headers, request, "X-GeoIP-Country");
        
        return headers;
    }
    
    /**
     * Add header to map if present in request.
     */
    private void addHeaderIfPresent(Map<String, String> headers, HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            headers.put(headerName, headerValue.trim());
        }
    }
}