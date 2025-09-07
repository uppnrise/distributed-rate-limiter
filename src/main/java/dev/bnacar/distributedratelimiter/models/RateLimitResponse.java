package dev.bnacar.distributedratelimiter.models;

public class RateLimitResponse {
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