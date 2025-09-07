package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.RateLimitRequest;
import dev.bnacar.distributedratelimiter.models.RateLimitResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    @Autowired
    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(@Valid @RequestBody RateLimitRequest request) {
        boolean allowed = rateLimiterService.isAllowed(request.getKey(), request.getTokens());
        
        RateLimitResponse response = new RateLimitResponse(request.getKey(), request.getTokens(), allowed);
        
        if (allowed) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }
}