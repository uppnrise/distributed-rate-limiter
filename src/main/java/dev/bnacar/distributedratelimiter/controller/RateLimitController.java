package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.models.RateLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ratelimit")
@Tag(name = "Rate Limit", description = "Rate limiting operations for token bucket algorithm")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class RateLimitController {

    private final RateLimiterService rateLimiterService;
    private final ApiKeyService apiKeyService;
    private final IpSecurityService ipSecurityService;
    private final IpAddressExtractor ipAddressExtractor;

    @Autowired
    public RateLimitController(RateLimiterService rateLimiterService,
                              ApiKeyService apiKeyService,
                              IpSecurityService ipSecurityService,
                              IpAddressExtractor ipAddressExtractor) {
        this.rateLimiterService = rateLimiterService;
        this.apiKeyService = apiKeyService;
        this.ipSecurityService = ipSecurityService;
        this.ipAddressExtractor = ipAddressExtractor;
    }

    @PostMapping("/check")
    @Operation(summary = "Check rate limit for a key",
               description = "Checks if a request is allowed based on the configured rate limits for the given key")
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
            @Parameter(description = "Rate limit request containing key, tokens, and optional API key",
                      required = true,
                      content = @Content(examples = @ExampleObject(value = "{\"key\":\"user:123\",\"tokens\":1,\"apiKey\":\"your-api-key\"}")))
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
        
        // Check rate limit
        boolean allowed = rateLimiterService.isAllowed(effectiveKey, request.getTokens());
        
        RateLimitResponse response = new RateLimitResponse(request.getKey(), request.getTokens(), allowed);
        
        if (allowed) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }
}