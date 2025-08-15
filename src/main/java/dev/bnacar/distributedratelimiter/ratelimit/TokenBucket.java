package dev.bnacar.distributedratelimiter.ratelimit;

public class TokenBucket {

    private final int capacity;
    private final int refillRate;
    private long lastRefillTime;
    private int currentTokens;


    public TokenBucket(int capacity, int refillRate) {
        this.currentTokens = capacity;
        this.capacity = capacity;
        this.lastRefillTime = System.currentTimeMillis();
        this.refillRate = refillRate;
    }

    public int getCurrentTokens() {
        return currentTokens;
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

    public boolean tryConsume(int tokens) {
        if (tokens <= 0 || tokens > currentTokens) {
            return false;
        }

        currentTokens -= tokens;

        return true;
    }

    public long refill() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRefill = currentTime - lastRefillTime;

        int tokensToAdd = (int) (timeSinceLastRefill / 1000 * refillRate);
        currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
        lastRefillTime = currentTime;
        return lastRefillTime;
    }
}
