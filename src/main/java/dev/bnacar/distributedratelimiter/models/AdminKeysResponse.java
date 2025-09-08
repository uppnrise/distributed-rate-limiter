package dev.bnacar.distributedratelimiter.models;

import java.util.List;

/**
 * Response model for listing all admin keys.
 */
public class AdminKeysResponse {
    private final List<AdminKeyStats> keys;
    private final int totalKeys;
    private final int activeKeys;

    public AdminKeysResponse(List<AdminKeyStats> keys, int totalKeys, int activeKeys) {
        this.keys = keys;
        this.totalKeys = totalKeys;
        this.activeKeys = activeKeys;
    }

    public List<AdminKeyStats> getKeys() {
        return keys;
    }

    public int getTotalKeys() {
        return totalKeys;
    }

    public int getActiveKeys() {
        return activeKeys;
    }
}