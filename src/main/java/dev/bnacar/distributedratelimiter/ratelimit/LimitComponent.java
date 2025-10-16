package dev.bnacar.distributedratelimiter.ratelimit;

/**
 * Represents a single rate limiting component within a composite rate limiter.
 */
public class LimitComponent {
    private final String name;
    private final RateLimiter rateLimiter;
    private final double weight;
    private final int priority;
    private final String scope; // USER, TENANT, GLOBAL, etc.
    
    public LimitComponent(String name, RateLimiter rateLimiter, double weight, int priority) {
        this(name, rateLimiter, weight, priority, null);
    }
    
    public LimitComponent(String name, RateLimiter rateLimiter, double weight, int priority, String scope) {
        this.name = name;
        this.rateLimiter = rateLimiter;
        this.weight = weight;
        this.priority = priority;
        this.scope = scope;
    }
    
    /**
     * Check if the component allows the requested tokens.
     */
    public boolean tryConsume(int tokens) {
        return rateLimiter.tryConsume(tokens);
    }
    
    /**
     * Get current available tokens for this component.
     */
    public int getCurrentTokens() {
        return rateLimiter.getCurrentTokens();
    }
    
    public String getName() {
        return name;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getScope() {
        return scope;
    }
    
    @Override
    public String toString() {
        return "LimitComponent{" +
                "name='" + name + '\'' +
                ", weight=" + weight +
                ", priority=" + priority +
                ", scope='" + scope + '\'' +
                ", currentTokens=" + getCurrentTokens() +
                '}';
    }
}