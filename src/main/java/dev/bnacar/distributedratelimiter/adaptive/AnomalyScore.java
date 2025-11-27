package dev.bnacar.distributedratelimiter.adaptive;

/**
 * Anomaly detection score for traffic patterns
 */
public class AnomalyScore {
    
    private boolean isAnomaly;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String type; // SPIKE, DROP, UNUSUAL_PATTERN, SUSTAINED_HIGH, SUSTAINED_LOW
    private double confidence;
    private double zScore;
    
    private AnomalyScore(Builder builder) {
        this.isAnomaly = builder.isAnomaly;
        this.severity = builder.severity;
        this.type = builder.type;
        this.confidence = builder.confidence;
        this.zScore = builder.zScore;
    }
    
    public boolean isAnomaly() {
        return isAnomaly;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public String getType() {
        return type;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public double getZScore() {
        return zScore;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean isAnomaly;
        private String severity;
        private String type;
        private double confidence;
        private double zScore;
        
        public Builder isAnomaly(boolean isAnomaly) {
            this.isAnomaly = isAnomaly;
            return this;
        }
        
        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder zScore(double zScore) {
            this.zScore = zScore;
            return this;
        }
        
        public AnomalyScore build() {
            return new AnomalyScore(this);
        }
    }
}
