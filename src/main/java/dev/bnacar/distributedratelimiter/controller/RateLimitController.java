package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    public static class RateLimitRequest {
        @NotBlank(message = "Key cannot be blank")
        private String key;
        
        @NotNull(message = "Tokens must be specified")
        @Min(value = 1, message = "Tokens must be at least 1")
        private Integer tokens = 1;

        public RateLimitRequest() {}

        public RateLimitRequest(String key, Integer tokens) {
            this.key = key;
            this.tokens = tokens;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public Integer getTokens() {
            return tokens;
        }

        public void setTokens(Integer tokens) {
            this.tokens = tokens;
        }
    }

    public static class RateLimitResponse {
        private final String key;
        private final int tokensRequested;
        private final boolean allowed;

        public RateLimitResponse(String key, int tokensRequested, boolean allowed) {
            this.key = key;
            this.tokensRequested = tokensRequested;
            this.allowed = allowed;
        }

        public String getKey() {
            return key;
        }

        public int getTokensRequested() {
            return tokensRequested;
        }

        public boolean isAllowed() {
            return allowed;
        }
    }
}