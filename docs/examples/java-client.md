# Java/Spring Boot Integration

This example demonstrates how to integrate the Distributed Rate Limiter into a Java Spring Boot application.

## Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## Rate Limiter Client

```java
package com.example.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RateLimiterClient {

    private final WebClient webClient;
    private final String rateLimiterUrl;
    private final String apiKey;

    public RateLimiterClient(@Value("${ratelimiter.url:http://localhost:8080}") String url,
                           @Value("${ratelimiter.apikey:}") String apiKey) {
        this.rateLimiterUrl = url;
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl(url)
            .build();
    }

    public Mono<RateLimitResult> checkRateLimit(String key, int tokens) {
        RateLimitRequest request = new RateLimitRequest(key, tokens, apiKey);
        
        return webClient.post()
            .uri("/api/ratelimit/check")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, 
                response -> Mono.just(new RuntimeException("Client error: " + response.statusCode())))
            .onStatus(HttpStatus::is5xxServerError,
                response -> Mono.just(new RuntimeException("Server error: " + response.statusCode())))
            .bodyToMono(RateLimitResponse.class)
            .map(response -> new RateLimitResult(response.isAllowed(), response.getKey()))
            .onErrorReturn(new RateLimitResult(false, key));
    }

    // Request/Response DTOs
    public static class RateLimitRequest {
        private String key;
        private Integer tokens;
        private String apiKey;

        public RateLimitRequest(String key, Integer tokens, String apiKey) {
            this.key = key;
            this.tokens = tokens;
            this.apiKey = apiKey;
        }

        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public Integer getTokens() { return tokens; }
        public void setTokens(Integer tokens) { this.tokens = tokens; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class RateLimitResponse {
        private String key;
        private int tokensRequested;
        private boolean allowed;

        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public int getTokensRequested() { return tokensRequested; }
        public void setTokensRequested(int tokensRequested) { this.tokensRequested = tokensRequested; }
        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final String key;

        public RateLimitResult(boolean allowed, String key) {
            this.allowed = allowed;
            this.key = key;
        }

        public boolean isAllowed() { return allowed; }
        public String getKey() { return key; }
    }
}
```

## Controller with Rate Limiting

```java
package com.example.controller;

import com.example.client.RateLimiterClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private RateLimiterClient rateLimiterClient;

    @GetMapping("/user/{userId}/profile")
    public Mono<ResponseEntity<UserProfile>> getUserProfile(@PathVariable String userId) {
        String rateLimitKey = "user:" + userId;
        
        return rateLimiterClient.checkRateLimit(rateLimitKey, 1)
            .flatMap(result -> {
                if (!result.isAllowed()) {
                    return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new UserProfile("Rate limit exceeded", null)));
                }
                
                // Process the actual request
                return processUserProfileRequest(userId)
                    .map(profile -> ResponseEntity.ok(profile));
            });
    }

    private Mono<UserProfile> processUserProfileRequest(String userId) {
        // Your business logic here
        return Mono.just(new UserProfile("John Doe", userId));
    }

    public static class UserProfile {
        private String name;
        private String userId;

        public UserProfile(String name, String userId) {
            this.name = name;
            this.userId = userId;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
```

## Configuration

Add to your `application.properties`:

```properties
# Rate limiter configuration
ratelimiter.url=http://localhost:8080
ratelimiter.apikey=your-api-key

# Optional: Configure timeouts
spring.webflux.client.connect-timeout=5000
spring.webflux.client.read-timeout=10000
```

## Algorithm Configuration

Configure different algorithms for different use cases:

```java
@Service
public class RateLimiterConfigurationService {
    
    private final WebClient webClient;
    
    public RateLimiterConfigurationService(@Value("${ratelimiter.url:http://localhost:8080}") String url) {
        this.webClient = WebClient.builder().baseUrl(url).build();
    }
    
    @PostConstruct
    public void configureAlgorithms() {
        // Token Bucket for user APIs - allows bursts
        configurePattern("user:*", new AlgorithmConfig(50, 10, "TOKEN_BUCKET"));
        
        // Sliding Window for critical APIs - precise control
        configurePattern("api:critical:*", new AlgorithmConfig(100, 20, "SLIDING_WINDOW"));
        
        // Fixed Window for bulk APIs - memory efficient
        configurePattern("bulk:*", new AlgorithmConfig(1000, 100, "FIXED_WINDOW"));
    }
    
    private void configurePattern(String pattern, AlgorithmConfig config) {
        webClient.post()
            .uri("/api/ratelimit/config/patterns/{pattern}", pattern)
            .bodyValue(config)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> System.err.println("Failed to configure pattern " + pattern + ": " + e.getMessage()))
            .subscribe();
    }
    
    public static class AlgorithmConfig {
        private int capacity;
        private int refillRate;
        private String algorithm;
        
        public AlgorithmConfig(int capacity, int refillRate, String algorithm) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.algorithm = algorithm;
        }
        
        // Getters and setters
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public int getRefillRate() { return refillRate; }
        public void setRefillRate(int refillRate) { this.refillRate = refillRate; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }
}
```

### Algorithm Selection Guidelines

```java
// Choose algorithm based on use case
public String selectAlgorithm(ApiEndpoint endpoint) {
    switch (endpoint.getType()) {
        case USER_FACING:
            return "TOKEN_BUCKET";    // Allows bursts, better UX
        case CRITICAL_API:
            return "SLIDING_WINDOW";  // Precise rate control
        case BULK_OPERATION:
            return "FIXED_WINDOW";    // Memory efficient
        default:
            return "TOKEN_BUCKET";    // Safe default
    }
}
```

## Usage with Resilience

For production use, consider adding circuit breakers and retries:

```java
@Service
public class ResilientRateLimiterClient {
    
    private final CircuitBreaker circuitBreaker;
    private final RateLimiterClient rateLimiterClient;
    
    public ResilientRateLimiterClient(RateLimiterClient rateLimiterClient) {
        this.rateLimiterClient = rateLimiterClient;
        this.circuitBreaker = CircuitBreaker.ofDefaults("rateLimiter");
        
        // Configure circuit breaker to fail open on rate limiter issues
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                System.out.println("Circuit breaker state transition: " + event));
    }
    
    public Mono<Boolean> isAllowed(String key, int tokens) {
        return Mono.fromSupplier(() -> 
            circuitBreaker.executeSupplier(() -> 
                rateLimiterClient.checkRateLimit(key, tokens)
                    .map(RateLimiterClient.RateLimitResult::isAllowed)
                    .block()))
            .onErrorReturn(true); // Fail open: allow requests if rate limiter is down
    }
}
```