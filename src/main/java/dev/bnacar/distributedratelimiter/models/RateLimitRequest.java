package dev.bnacar.distributedratelimiter.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RateLimitRequest {
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