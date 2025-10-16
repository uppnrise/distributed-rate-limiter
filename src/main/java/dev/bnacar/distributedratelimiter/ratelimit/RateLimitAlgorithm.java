package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Enum representing different rate limiting algorithms.
 */
public enum RateLimitAlgorithm {
    /**
     * Token bucket algorithm - allows burst traffic up to bucket capacity,
     * with steady refill rate. Good for applications that need to handle
     * temporary traffic spikes.
     */
    TOKEN_BUCKET,
    
    /**
     * Sliding window algorithm - tracks requests within a time window,
     * providing more predictable rate limiting. Better for consistent
     * rate enforcement and handling sustained burst traffic.
     */
    SLIDING_WINDOW,
    
    /**
     * Fixed window algorithm - uses fixed time windows with counter reset
     * at window boundaries. Memory efficient and simple, ideal for basic
     * rate limiting with predictable reset times.
     */
    FIXED_WINDOW,
    
    /**
     * Leaky bucket algorithm - enforces constant output rate through request queuing.
     * Provides traffic shaping with predictable processing rates regardless of
     * input bursts. Ideal for downstream system protection and SLA compliance.
     */
    LEAKY_BUCKET
}