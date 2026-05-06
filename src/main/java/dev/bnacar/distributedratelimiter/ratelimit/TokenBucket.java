package dev.bnacar.distributedratelimiter.ratelimit;

public class TokenBucket implements RateLimiter {

    private final int capacity;
    private final int refillRate;
    private long lastRefillTime;
    private double currentTokens;


    public TokenBucket(int capacity, int refillRate) {
        this.currentTokens = capacity;
        this.capacity = capacity;
        this.lastRefillTime = System.currentTimeMillis();
        this.refillRate = refillRate;
    }

    public int getCurrentTokens() {
        refill();
        return (int) currentTokens;
    }

    public int getCapacity() {
        return capacity;
    }

    public long getLastRefillTime() {
        return lastRefillTime;
    }

    public int getRefillRate() {
        return refillRate;
    }

    public synchronized boolean tryConsume(int tokens) {
        refill();
        if (tokens <= 0 || tokens > currentTokens) {
            return false;
        }

        currentTokens -= tokens;
        return true;
    }

    private synchronized void refill() {
        if (refillRate <= 0 || currentTokens >= capacity) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLastRefill = currentTime - lastRefillTime;
        if (timeSinceLastRefill <= 0) {
            return;
        }

        double tokensToAdd = (timeSinceLastRefill / 1000.0) * refillRate;
        currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
        lastRefillTime = currentTime;
    }
}
