package dev.bnacar.distributedratelimiter.models;

import java.time.Instant;

/**
 * Adaptive information included in rate limit check responses
 */
public class AdaptiveInfo {
    
    private OriginalLimits originalLimits;
    private CurrentLimits currentLimits;
    private String adaptationReason;
    private Instant adjustmentTimestamp;
    private String nextEvaluationIn;
    
    public AdaptiveInfo() {}
    
    public AdaptiveInfo(OriginalLimits originalLimits, CurrentLimits currentLimits, 
                       String adaptationReason, Instant adjustmentTimestamp, String nextEvaluationIn) {
        this.originalLimits = originalLimits;
        this.currentLimits = currentLimits;
        this.adaptationReason = adaptationReason;
        this.adjustmentTimestamp = adjustmentTimestamp;
        this.nextEvaluationIn = nextEvaluationIn;
    }
    
    // Getters and Setters
    public OriginalLimits getOriginalLimits() {
        return originalLimits;
    }
    
    public void setOriginalLimits(OriginalLimits originalLimits) {
        this.originalLimits = originalLimits;
    }
    
    public CurrentLimits getCurrentLimits() {
        return currentLimits;
    }
    
    public void setCurrentLimits(CurrentLimits currentLimits) {
        this.currentLimits = currentLimits;
    }
    
    public String getAdaptationReason() {
        return adaptationReason;
    }
    
    public void setAdaptationReason(String adaptationReason) {
        this.adaptationReason = adaptationReason;
    }
    
    public Instant getAdjustmentTimestamp() {
        return adjustmentTimestamp;
    }
    
    public void setAdjustmentTimestamp(Instant adjustmentTimestamp) {
        this.adjustmentTimestamp = adjustmentTimestamp;
    }
    
    public String getNextEvaluationIn() {
        return nextEvaluationIn;
    }
    
    public void setNextEvaluationIn(String nextEvaluationIn) {
        this.nextEvaluationIn = nextEvaluationIn;
    }
    
    public static class OriginalLimits {
        private int capacity;
        private int refillRate;
        
        public OriginalLimits() {}
        
        public OriginalLimits(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
        
        public int getRefillRate() {
            return refillRate;
        }
        
        public void setRefillRate(int refillRate) {
            this.refillRate = refillRate;
        }
    }
    
    public static class CurrentLimits {
        private int capacity;
        private int refillRate;
        
        public CurrentLimits() {}
        
        public CurrentLimits(int capacity, int refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
        
        public int getRefillRate() {
            return refillRate;
        }
        
        public void setRefillRate(int refillRate) {
            this.refillRate = refillRate;
        }
    }
}
