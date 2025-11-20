package dev.bnacar.distributedratelimiter.adaptive;

import java.util.List;

/**
 * Traffic pattern analysis for a key
 */
public class TrafficPattern {
    
    private String key;
    private SeasonalityInfo seasonality;
    private TrendInfo trend;
    private VolatilityMetrics volatility;
    private List<TrafficPrediction> predictedNext24h;
    private double confidence;
    
    private TrafficPattern(Builder builder) {
        this.key = builder.key;
        this.seasonality = builder.seasonality;
        this.trend = builder.trend;
        this.volatility = builder.volatility;
        this.predictedNext24h = builder.predictedNext24h;
        this.confidence = builder.confidence;
    }
    
    public String getKey() {
        return key;
    }
    
    public SeasonalityInfo getSeasonality() {
        return seasonality;
    }
    
    public TrendInfo getTrend() {
        return trend;
    }
    
    public VolatilityMetrics getVolatility() {
        return volatility;
    }
    
    public List<TrafficPrediction> getPredictedNext24h() {
        return predictedNext24h;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String key;
        private SeasonalityInfo seasonality;
        private TrendInfo trend;
        private VolatilityMetrics volatility;
        private List<TrafficPrediction> predictedNext24h;
        private double confidence;
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder seasonality(SeasonalityInfo seasonality) {
            this.seasonality = seasonality;
            return this;
        }
        
        public Builder trend(TrendInfo trend) {
            this.trend = trend;
            return this;
        }
        
        public Builder volatility(VolatilityMetrics volatility) {
            this.volatility = volatility;
            return this;
        }
        
        public Builder predictedNext24h(List<TrafficPrediction> predictedNext24h) {
            this.predictedNext24h = predictedNext24h;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public TrafficPattern build() {
            return new TrafficPattern(this);
        }
    }
    
    public static class SeasonalityInfo {
        private boolean detected;
        private String pattern; // DAILY, WEEKLY, MONTHLY
        private double strength;
        
        public SeasonalityInfo(boolean detected, String pattern, double strength) {
            this.detected = detected;
            this.pattern = pattern;
            this.strength = strength;
        }
        
        public boolean isDetected() {
            return detected;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public double getStrength() {
            return strength;
        }
    }
    
    public static class TrendInfo {
        private String direction; // INCREASING, DECREASING, STABLE
        private double slope;
        private double confidence;
        
        public TrendInfo(String direction, double slope, double confidence) {
            this.direction = direction;
            this.slope = slope;
            this.confidence = confidence;
        }
        
        public String getDirection() {
            return direction;
        }
        
        public double getSlope() {
            return slope;
        }
        
        public double getConfidence() {
            return confidence;
        }
    }
    
    public static class VolatilityMetrics {
        private double standardDeviation;
        private double coefficientOfVariation;
        private String volatilityLevel; // LOW, MEDIUM, HIGH
        
        public VolatilityMetrics(double standardDeviation, double coefficientOfVariation, String volatilityLevel) {
            this.standardDeviation = standardDeviation;
            this.coefficientOfVariation = coefficientOfVariation;
            this.volatilityLevel = volatilityLevel;
        }
        
        public double getStandardDeviation() {
            return standardDeviation;
        }
        
        public double getCoefficientOfVariation() {
            return coefficientOfVariation;
        }
        
        public String getVolatilityLevel() {
            return volatilityLevel;
        }
    }
    
    public static class TrafficPrediction {
        private long timestamp;
        private double predictedRate;
        private double confidence;
        
        public TrafficPrediction(long timestamp, double predictedRate, double confidence) {
            this.timestamp = timestamp;
            this.predictedRate = predictedRate;
            this.confidence = confidence;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public double getPredictedRate() {
            return predictedRate;
        }
        
        public double getConfidence() {
            return confidence;
        }
    }
}
