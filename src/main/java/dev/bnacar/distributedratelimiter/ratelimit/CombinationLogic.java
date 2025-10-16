package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Defines how multiple rate limit components are combined in composite rate limiting.
 */
public enum CombinationLogic {
    /**
     * All components must pass (AND operation).
     * Request is allowed only if all rate limiters allow it.
     */
    ALL_MUST_PASS,
    
    /**
     * Any component can pass (OR operation).
     * Request is allowed if at least one rate limiter allows it.
     */
    ANY_CAN_PASS,
    
    /**
     * Weighted average of component results.
     * Uses component weights to calculate overall score.
     */
    WEIGHTED_AVERAGE,
    
    /**
     * Hierarchical evaluation with AND logic.
     * Checks limits in order of scope hierarchy (user -> tenant -> global).
     */
    HIERARCHICAL_AND,
    
    /**
     * Priority-based evaluation.
     * Checks highest priority components first, fails fast on denial.
     */
    PRIORITY_BASED
}