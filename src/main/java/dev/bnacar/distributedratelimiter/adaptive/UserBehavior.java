package dev.bnacar.distributedratelimiter.adaptive;

/**
 * User behavior analysis for adaptive rate limiting
 */
public class UserBehavior {
    
    private double averageRequestRate;
    private double burstiness;
    private double sessionDuration;
    private TimeOfDayPattern timeOfDayPattern;
    private double anomalyScore;
    
    private UserBehavior(Builder builder) {
        this.averageRequestRate = builder.averageRequestRate;
        this.burstiness = builder.burstiness;
        this.sessionDuration = builder.sessionDuration;
        this.timeOfDayPattern = builder.timeOfDayPattern;
        this.anomalyScore = builder.anomalyScore;
    }
    
    public double getAverageRequestRate() {
        return averageRequestRate;
    }
    
    public double getBurstiness() {
        return burstiness;
    }
    
    public double getSessionDuration() {
        return sessionDuration;
    }
    
    public TimeOfDayPattern getTimeOfDayPattern() {
        return timeOfDayPattern;
    }
    
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double averageRequestRate;
        private double burstiness;
        private double sessionDuration;
        private TimeOfDayPattern timeOfDayPattern;
        private double anomalyScore;
        
        public Builder averageRequestRate(double averageRequestRate) {
            this.averageRequestRate = averageRequestRate;
            return this;
        }
        
        public Builder burstiness(double burstiness) {
            this.burstiness = burstiness;
            return this;
        }
        
        public Builder sessionDuration(double sessionDuration) {
            this.sessionDuration = sessionDuration;
            return this;
        }
        
        public Builder timeOfDayPattern(TimeOfDayPattern timeOfDayPattern) {
            this.timeOfDayPattern = timeOfDayPattern;
            return this;
        }
        
        public Builder anomalyScore(double anomalyScore) {
            this.anomalyScore = anomalyScore;
            return this;
        }
        
        public UserBehavior build() {
            return new UserBehavior(this);
        }
    }
    
    public static class TimeOfDayPattern {
        private int peakHour;
        private int offPeakHour;
        private double peakToOffPeakRatio;
        
        public TimeOfDayPattern(int peakHour, int offPeakHour, double peakToOffPeakRatio) {
            this.peakHour = peakHour;
            this.offPeakHour = offPeakHour;
            this.peakToOffPeakRatio = peakToOffPeakRatio;
        }
        
        public int getPeakHour() {
            return peakHour;
        }
        
        public int getOffPeakHour() {
            return offPeakHour;
        }
        
        public double getPeakToOffPeakRatio() {
            return peakToOffPeakRatio;
        }
    }
}
