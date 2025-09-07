package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    @Autowired
    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/check")
    public ResponseEntity<RateLimitResponse> checkRateLimit(
            @RequestParam String key,
            @RequestParam(defaultValue = "1") int tokens) {
        
        boolean allowed = rateLimiterService.isAllowed(key, tokens);
        
        return ResponseEntity.ok(new RateLimitResponse(key, tokens, allowed));
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