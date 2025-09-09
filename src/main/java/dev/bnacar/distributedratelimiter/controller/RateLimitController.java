package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.models.RateLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import dev.bnacar.distributedratelimiter.security.ApiKeyService;
import dev.bnacar.distributedratelimiter.security.IpAddressExtractor;
import dev.bnacar.distributedratelimiter.security.IpSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ratelimit")
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
    public ResponseEntity<RateLimitResponse> checkRateLimit(@Valid @RequestBody RateLimitRequest request,
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