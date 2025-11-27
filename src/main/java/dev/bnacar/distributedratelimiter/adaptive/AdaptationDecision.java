package dev.bnacar.distributedratelimiter.adaptive;

import java.time.Instant;
import java.util.Map;

/**
 * Decision on whether and how to adapt rate limits for a key
 */
public class AdaptationDecision {
    
    private boolean shouldAdapt;
    private int recommendedCapacity;
    private int recommendedRefillRate;
    private double confidence;
    private Map<String, String> reasoning;
    private Instant timestamp;
    
    private AdaptationDecision(Builder builder) {
        this.shouldAdapt = builder.shouldAdapt;
        this.recommendedCapacity = builder.recommendedCapacity;
        this.recommendedRefillRate = builder.recommendedRefillRate;
        this.confidence = builder.confidence;
        this.reasoning = builder.reasoning;
        this.timestamp = Instant.now();
    }
    
    public boolean shouldAdapt() {
        return shouldAdapt;
    }
    
    public int getRecommendedCapacity() {
        return recommendedCapacity;
    }
    
    public int getRecommendedRefillRate() {
        return recommendedRefillRate;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public Map<String, String> getReasoning() {
        return reasoning;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean shouldAdapt;
        private int recommendedCapacity;
        private int recommendedRefillRate;
        private double confidence;
        private Map<String, String> reasoning;
        
        public Builder shouldAdapt(boolean shouldAdapt) {
            this.shouldAdapt = shouldAdapt;
            return this;
        }
        
        public Builder recommendedCapacity(int recommendedCapacity) {
            this.recommendedCapacity = recommendedCapacity;
            return this;
        }
        
        public Builder recommendedRefillRate(int recommendedRefillRate) {
            this.recommendedRefillRate = recommendedRefillRate;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder reasoning(Map<String, String> reasoning) {
            this.reasoning = reasoning;
            return this;
        }
        
        public AdaptationDecision build() {
            return new AdaptationDecision(this);
        }
    }
}
