package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Strategy interface for rate limiter backends.
 * Allows pluggable storage backends (Redis, in-memory, etc.)
 */
public interface RateLimiterBackend {
    
    /**
     * Get or create a rate limiter for the given key and configuration.
     * 
     * @param key The rate limit key
     * @param config Rate limit configuration
     * @return RateLimiter instance
     */
    RateLimiter getRateLimiter(String key, RateLimitConfig config);
    
    /**
     * Check if this backend is available/healthy.
     * 
     * @return true if backend is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Clear all rate limiters (for testing/management).
     */
    void clear();
    
    /**
     * Get the number of active rate limiters.
     * 
     * @return Number of active rate limiters
     */
    int getActiveCount();
}