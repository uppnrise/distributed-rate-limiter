package dev.bnacar.distributedratelimiter.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.ArrayList;

/**
 * Model representing the result of a performance regression analysis.
 */
public class PerformanceRegressionResult {
    
    @JsonProperty("hasRegression")
    private boolean hasRegression;
    
    @JsonProperty("testName")
    private String testName;
    
    @JsonProperty("currentBaseline")
    private PerformanceBaseline currentBaseline;
    
    @JsonProperty("previousBaseline")
    private PerformanceBaseline previousBaseline;
    
    @JsonProperty("regressionDetails")
    private List<String> regressionDetails;
    
    @JsonProperty("regressionSeverity")
    private RegressionSeverity regressionSeverity;
    
    @JsonProperty("thresholds")
    private RegressionThresholds thresholds;

    public enum RegressionSeverity {
        NONE,
        MINOR,
        MODERATE,
        MAJOR,
        CRITICAL
    }

    public static class RegressionThresholds {
        @JsonProperty("responseTimeThreshold")
        private double responseTimeThreshold = 20.0; // 20% increase
        
        @JsonProperty("throughputThreshold")
        private double throughputThreshold = 15.0; // 15% decrease
        
        @JsonProperty("successRateThreshold")
        private double successRateThreshold = 5.0; // 5% decrease

        public RegressionThresholds() {}

        public RegressionThresholds(double responseTimeThreshold, double throughputThreshold, double successRateThreshold) {
            this.responseTimeThreshold = responseTimeThreshold;
            this.throughputThreshold = throughputThreshold;
            this.successRateThreshold = successRateThreshold;
        }

        // Getters and setters
        public double getResponseTimeThreshold() {
            return responseTimeThreshold;
        }

        public void setResponseTimeThreshold(double responseTimeThreshold) {
            this.responseTimeThreshold = responseTimeThreshold;
        }

        public double getThroughputThreshold() {
            return throughputThreshold;
        }

        public void setThroughputThreshold(double throughputThreshold) {
            this.throughputThreshold = throughputThreshold;
        }

        public double getSuccessRateThreshold() {
            return successRateThreshold;
        }

        public void setSuccessRateThreshold(double successRateThreshold) {
            this.successRateThreshold = successRateThreshold;
        }
    }

    // Constructors
    public PerformanceRegressionResult() {}

    public PerformanceRegressionResult(String testName, PerformanceBaseline currentBaseline, 
                                     PerformanceBaseline previousBaseline) {
        this.testName = testName;
        this.currentBaseline = currentBaseline;
        this.previousBaseline = previousBaseline;
        this.thresholds = new RegressionThresholds();
    }

    // Getters and setters
    public boolean isHasRegression() {
        return hasRegression;
    }

    public void setHasRegression(boolean hasRegression) {
        this.hasRegression = hasRegression;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public PerformanceBaseline getCurrentBaseline() {
        return currentBaseline;
    }

    public void setCurrentBaseline(PerformanceBaseline currentBaseline) {
        this.currentBaseline = currentBaseline;
    }

    public PerformanceBaseline getPreviousBaseline() {
        return previousBaseline;
    }

    public void setPreviousBaseline(PerformanceBaseline previousBaseline) {
        this.previousBaseline = previousBaseline;
    }

    public List<String> getRegressionDetails() {
        return regressionDetails != null ? new ArrayList<>(regressionDetails) : new ArrayList<>();
    }

    public void setRegressionDetails(List<String> regressionDetails) {
        this.regressionDetails = regressionDetails != null ? new ArrayList<>(regressionDetails) : new ArrayList<>();
    }

    public RegressionSeverity getRegressionSeverity() {
        return regressionSeverity;
    }

    public void setRegressionSeverity(RegressionSeverity regressionSeverity) {
        this.regressionSeverity = regressionSeverity;
    }

    public RegressionThresholds getThresholds() {
        return thresholds;
    }

    public void setThresholds(RegressionThresholds thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String toString() {
        return String.format(
            "PerformanceRegressionResult{hasRegression=%s, testName='%s', severity=%s, details=%s}",
            hasRegression, testName, regressionSeverity, regressionDetails
        );
    }
}