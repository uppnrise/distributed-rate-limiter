package dev.bnacar.distributedratelimiter.models;

public class RateLimitResponse {
    private final String key;
    private final int tokensRequested;
    private final boolean allowed;
    private final AdaptiveInfo adaptiveInfo;

    public RateLimitResponse(String key, int tokensRequested, boolean allowed) {
        this(key, tokensRequested, allowed, null);
    }

    public RateLimitResponse(String key, int tokensRequested, boolean allowed, AdaptiveInfo adaptiveInfo) {
        this.key = key;
        this.tokensRequested = tokensRequested;
        this.allowed = allowed;
        this.adaptiveInfo = adaptiveInfo;
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

    public AdaptiveInfo getAdaptiveInfo() {
        return adaptiveInfo;
    }
}