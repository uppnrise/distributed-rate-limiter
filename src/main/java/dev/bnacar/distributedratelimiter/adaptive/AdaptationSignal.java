package dev.bnacar.distributedratelimiter.adaptive;

/**
 * Signal indicating whether system can handle more/less load
 */
public enum AdaptationSignal {
    REDUCE_LIMITS,     // System under stress, reduce limits
    MAINTAIN_LIMITS,   // System stable, maintain current limits
    INCREASE_LIMITS    // System has capacity, can increase limits
}
