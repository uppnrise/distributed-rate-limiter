package dev.bnacar.distributedratelimiter.models;

import dev.bnacar.distributedratelimiter.ratelimit.RateLimitAlgorithm;
import dev.bnacar.distributedratelimiter.ratelimit.CompositeRateLimitConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RateLimitRequest {
    @NotBlank(message = "Key cannot be blank")
    private String key;
    
    @NotNull(message = "Tokens must be specified")
    @Min(value = 1, message = "Tokens must be at least 1")
    private Integer tokens = 1;
    
    private String apiKey;
    
    private RateLimitAlgorithm algorithm;
    
    private CompositeRateLimitConfig compositeConfig;

    public RateLimitRequest() {}

    public RateLimitRequest(String key, Integer tokens) {
        this.key = key;
        this.tokens = tokens;
    }

    public RateLimitRequest(String key, Integer tokens, String apiKey) {
        this.key = key;
        this.tokens = tokens;
        this.apiKey = apiKey;
    }
    
    public RateLimitRequest(String key, Integer tokens, RateLimitAlgorithm algorithm, CompositeRateLimitConfig compositeConfig) {
        this.key = key;
        this.tokens = tokens;
        this.algorithm = algorithm;
        this.compositeConfig = compositeConfig;
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

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }
    
    public CompositeRateLimitConfig getCompositeConfig() {
        return compositeConfig;
    }
    
    public void setCompositeConfig(CompositeRateLimitConfig compositeConfig) {
        this.compositeConfig = compositeConfig;
    }
}