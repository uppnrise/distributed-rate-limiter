package dev.bnacar.distributedratelimiter.adaptive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects anomalies in traffic patterns using statistical methods
 */
@Component
public class AnomalyDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetector.class);
    
    // Store baseline statistics for anomaly detection
    private final Map<String, TrafficStats> baselineStats = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> recentRates = new ConcurrentHashMap<>();
    
    private static final int BASELINE_WINDOW = 1000; // Number of data points for baseline
    private static final double Z_SCORE_THRESHOLD = 3.0; // 3-sigma rule
    
    /**
     * Record traffic rate for anomaly detection
     */
    public void recordTrafficRate(String key, double rate) {
        List<Double> rates = recentRates.computeIfAbsent(key, k -> new ArrayList<>());
        
        synchronized (rates) {
            rates.add(rate);
            
            // Keep only recent data
            if (rates.size() > BASELINE_WINDOW) {
                rates.remove(0);
            }
            
            // Update baseline if we have enough data
            if (rates.size() >= 100) {
                updateBaseline(key, rates);
            }
        }
    }
    
    /**
     * Detect anomalies for a key
     */
    public AnomalyScore detectAnomalies(String key) {
        TrafficStats current = getCurrentStats(key);
        TrafficStats baseline = getBaselineStats(key);
        
        if (baseline == null || current == null) {
            return AnomalyScore.builder()
                .isAnomaly(false)
                .severity("NONE")
                .type("NONE")
                .confidence(0.0)
                .zScore(0.0)
                .build();
        }
        
        double zScore = calculateZScore(current, baseline);
        boolean isAnomaly = Math.abs(zScore) > Z_SCORE_THRESHOLD;
        
        String severity = calculateSeverity(zScore);
        String type = classifyAnomalyType(current, baseline, zScore);
        double confidence = Math.min(Math.abs(zScore) / Z_SCORE_THRESHOLD, 1.0);
        
        if (isAnomaly) {
            logger.warn("Anomaly detected for key {}: type={}, severity={}, z-score={}", 
                       key, type, severity, zScore);
        }
        
        return AnomalyScore.builder()
            .isAnomaly(isAnomaly)
            .severity(severity)
            .type(type)
            .confidence(confidence)
            .zScore(zScore)
            .build();
    }
    
    /**
     * Get current traffic statistics
     */
    private TrafficStats getCurrentStats(String key) {
        List<Double> rates = recentRates.get(key);
        if (rates == null || rates.isEmpty()) {
            return null;
        }
        
        synchronized (rates) {
            // Use last 10 data points for current stats
            int startIndex = Math.max(0, rates.size() - 10);
            List<Double> recent = rates.subList(startIndex, rates.size());
            
            double mean = recent.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(recent, mean);
            
            return new TrafficStats(mean, stdDev);
        }
    }
    
    /**
     * Get baseline traffic statistics
     */
    private TrafficStats getBaselineStats(String key) {
        return baselineStats.get(key);
    }
    
    /**
     * Update baseline statistics
     */
    private void updateBaseline(String key, List<Double> rates) {
        synchronized (rates) {
            double mean = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(rates, mean);
            
            baselineStats.put(key, new TrafficStats(mean, stdDev));
        }
    }
    
    /**
     * Calculate z-score for anomaly detection
     */
    private double calculateZScore(TrafficStats current, TrafficStats baseline) {
        if (baseline.stdDev == 0) {
            return 0.0;
        }
        
        return (current.mean - baseline.mean) / baseline.stdDev;
    }
    
    /**
     * Calculate standard deviation
     */
    private double calculateStdDev(List<Double> values, double mean) {
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    /**
     * Calculate severity based on z-score
     */
    private String calculateSeverity(double zScore) {
        double absZ = Math.abs(zScore);
        
        if (absZ < Z_SCORE_THRESHOLD) {
            return "NONE";
        } else if (absZ < 4.0) {
            return "LOW";
        } else if (absZ < 5.0) {
            return "MEDIUM";
        } else if (absZ < 6.0) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }
    
    /**
     * Classify type of anomaly
     */
    private String classifyAnomalyType(TrafficStats current, TrafficStats baseline, double zScore) {
        if (Math.abs(zScore) < Z_SCORE_THRESHOLD) {
            return "NONE";
        }
        
        // Positive z-score means current rate is higher than baseline
        if (zScore > Z_SCORE_THRESHOLD) {
            if (zScore > 5.0) {
                return "SPIKE";
            } else {
                return "SUSTAINED_HIGH";
            }
        } else {
            // Negative z-score means current rate is lower than baseline
            if (zScore < -5.0) {
                return "DROP";
            } else {
                return "SUSTAINED_LOW";
            }
        }
    }
    
    /**
     * Clear baseline for a key
     */
    public void clearBaseline(String key) {
        baselineStats.remove(key);
        recentRates.remove(key);
    }
    
    /**
     * Traffic statistics holder
     */
    private static class TrafficStats {
        final double mean;
        final double stdDev;
        
        TrafficStats(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }
}
