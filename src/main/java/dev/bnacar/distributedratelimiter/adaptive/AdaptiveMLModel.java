package dev.bnacar.distributedratelimiter.adaptive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptive ML Model for rate limit optimization
 * Phase 1: Rule-based decision making
 * Phase 2: Integration with TensorFlow/PyTorch models
 */
@Component
public class AdaptiveMLModel {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveMLModel.class);
    
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.7;
    private static final double MAX_ADJUSTMENT_FACTOR = 2.0;
    private static final double MIN_ADJUSTMENT_FACTOR = 0.5;
    
    /**
     * Predict adaptation decision based on multiple signals
     */
    public AdaptationDecision predict(TrafficPattern pattern, 
                                     SystemHealth health,
                                     UserBehavior behavior, 
                                     AnomalyScore anomaly,
                                     int currentCapacity,
                                     int currentRefillRate) {
        
        // Extract features from signals
        double[] features = extractFeatures(pattern, health, behavior, anomaly);
        
        // Generate decision using rule-based logic
        DecisionOutput output = makeRuleBasedDecision(features, health, anomaly, 
                                                      currentCapacity, currentRefillRate);
        
        Map<String, String> reasoning = generateReasoning(pattern, health, behavior, anomaly, output);
        
        return AdaptationDecision.builder()
            .shouldAdapt(output.shouldAdapt)
            .recommendedCapacity(output.capacity)
            .recommendedRefillRate(output.refillRate)
            .confidence(output.confidence)
            .reasoning(reasoning)
            .build();
    }
    
    /**
     * Extract numerical features from signals
     */
    private double[] extractFeatures(TrafficPattern pattern, 
                                    SystemHealth health,
                                    UserBehavior behavior, 
                                    AnomalyScore anomaly) {
        return new double[] {
            // System health features
            health.getCpuUtilization(),
            health.getMemoryUtilization(),
            health.getResponseTimeP95() / 1000.0, // Normalize to seconds
            health.getErrorRate(),
            health.isRedisHealthy() ? 1.0 : 0.0,
            
            // Traffic pattern features
            pattern.getConfidence(),
            getTrendScore(pattern.getTrend()),
            pattern.getVolatility().getCoefficientOfVariation(),
            
            // User behavior features
            behavior.getAverageRequestRate(),
            behavior.getBurstiness(),
            behavior.getAnomalyScore(),
            
            // Anomaly features
            anomaly.isAnomaly() ? 1.0 : 0.0,
            anomaly.getZScore(),
            anomaly.getConfidence()
        };
    }
    
    /**
     * Make rule-based decision (Phase 1)
     */
    private DecisionOutput makeRuleBasedDecision(double[] features, 
                                                 SystemHealth health, 
                                                 AnomalyScore anomaly,
                                                 int currentCapacity,
                                                 int currentRefillRate) {
        
        DecisionOutput output = new DecisionOutput();
        output.capacity = currentCapacity;
        output.refillRate = currentRefillRate;
        output.confidence = 0.7;
        output.shouldAdapt = false;
        
        // Rule 1: System under stress - reduce limits
        if (health.getCpuUtilization() > 0.8 || health.getResponseTimeP95() > 2000) {
            output.shouldAdapt = true;
            output.capacity = (int) (currentCapacity * 0.7);
            output.refillRate = (int) (currentRefillRate * 0.7);
            output.confidence = 0.85;
            output.reason = "System under stress";
            return output;
        }
        
        // Rule 2: Critical anomaly detected - reduce limits
        if (anomaly.isAnomaly() && "CRITICAL".equals(anomaly.getSeverity())) {
            output.shouldAdapt = true;
            output.capacity = (int) (currentCapacity * 0.6);
            output.refillRate = (int) (currentRefillRate * 0.6);
            output.confidence = 0.9;
            output.reason = "Critical anomaly detected";
            return output;
        }
        
        // Rule 3: High anomaly - moderate reduction
        if (anomaly.isAnomaly() && ("HIGH".equals(anomaly.getSeverity()) || "MEDIUM".equals(anomaly.getSeverity()))) {
            output.shouldAdapt = true;
            output.capacity = (int) (currentCapacity * 0.8);
            output.refillRate = (int) (currentRefillRate * 0.8);
            output.confidence = 0.75;
            output.reason = "Anomaly detected";
            return output;
        }
        
        // Rule 4: System has capacity and no anomalies - increase limits
        if (health.getCpuUtilization() < 0.3 && 
            health.getErrorRate() < 0.001 && 
            !anomaly.isAnomaly()) {
            output.shouldAdapt = true;
            output.capacity = (int) (currentCapacity * 1.3);
            output.refillRate = (int) (currentRefillRate * 1.3);
            output.confidence = 0.75;
            output.reason = "System has capacity";
            return output;
        }
        
        // Rule 5: Moderate capacity - small increase
        if (health.getCpuUtilization() < 0.5 && 
            health.getErrorRate() < 0.005 && 
            !anomaly.isAnomaly()) {
            output.shouldAdapt = true;
            output.capacity = (int) (currentCapacity * 1.1);
            output.refillRate = (int) (currentRefillRate * 1.1);
            output.confidence = 0.65;
            output.reason = "System stable with available capacity";
            return output;
        }
        
        // No adaptation needed
        output.reason = "System stable, no adaptation needed";
        return output;
    }
    
    /**
     * Generate human-readable reasoning
     */
    private Map<String, String> generateReasoning(TrafficPattern pattern, 
                                                  SystemHealth health,
                                                  UserBehavior behavior, 
                                                  AnomalyScore anomaly,
                                                  DecisionOutput output) {
        Map<String, String> reasoning = new HashMap<>();
        
        reasoning.put("decision", output.reason);
        
        // System metrics
        reasoning.put("systemMetrics", 
            String.format("CPU: %.1f%%, Memory: %.1f%%, Response Time P95: %.0fms, Error Rate: %.3f%%",
                         health.getCpuUtilization() * 100,
                         health.getMemoryUtilization() * 100,
                         health.getResponseTimeP95(),
                         health.getErrorRate() * 100));
        
        // Traffic pattern
        if (pattern.getTrend() != null) {
            reasoning.put("trafficTrend", 
                String.format("Direction: %s, Volatility: %s",
                             pattern.getTrend().getDirection(),
                             pattern.getVolatility().getVolatilityLevel()));
        }
        
        // Anomaly status
        if (anomaly.isAnomaly()) {
            reasoning.put("anomaly", 
                String.format("Detected: %s, Severity: %s, Type: %s",
                             anomaly.isAnomaly(),
                             anomaly.getSeverity(),
                             anomaly.getType()));
        } else {
            reasoning.put("anomaly", "No anomalies detected");
        }
        
        // User behavior
        reasoning.put("userBehavior",
            String.format("Avg Rate: %.2f req/s, Burstiness: %.2f, Session: %.0fs",
                         behavior.getAverageRequestRate(),
                         behavior.getBurstiness(),
                         behavior.getSessionDuration()));
        
        return reasoning;
    }
    
    /**
     * Convert trend to numerical score
     */
    private double getTrendScore(TrafficPattern.TrendInfo trend) {
        if (trend == null) {
            return 0.0;
        }
        
        switch (trend.getDirection()) {
            case "INCREASING":
                return 1.0;
            case "DECREASING":
                return -1.0;
            default:
                return 0.0;
        }
    }
    
    /**
     * Decision output holder
     */
    private static class DecisionOutput {
        boolean shouldAdapt;
        int capacity;
        int refillRate;
        double confidence;
        String reason;
    }
}
