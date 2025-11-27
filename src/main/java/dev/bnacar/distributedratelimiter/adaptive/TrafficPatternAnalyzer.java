package dev.bnacar.distributedratelimiter.adaptive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Analyzes traffic patterns for adaptive rate limiting
 * Phase 1: Simple statistical analysis
 * Phase 2: Advanced time series forecasting
 */
@Component
public class TrafficPatternAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(TrafficPatternAnalyzer.class);
    
    // Store traffic data points for analysis
    private final Map<String, List<TrafficDataPoint>> trafficHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 10000; // Limit history per key
    
    /**
     * Record a traffic event for pattern analysis
     */
    public void recordTrafficEvent(String key, int tokensRequested, boolean allowed) {
        List<TrafficDataPoint> history = trafficHistory.computeIfAbsent(key, k -> new ArrayList<>());
        
        synchronized (history) {
            history.add(new TrafficDataPoint(Instant.now().toEpochMilli(), tokensRequested, allowed));
            
            // Limit history size to prevent memory issues
            if (history.size() > MAX_HISTORY_SIZE) {
                history.remove(0);
            }
        }
    }
    
    /**
     * Analyze traffic pattern for a key
     */
    public TrafficPattern analyzePattern(String key) {
        List<TrafficDataPoint> history = getTrafficHistory(key, Duration.ofDays(7));
        
        if (history.isEmpty()) {
            logger.debug("No traffic history for key: {}", key);
            return createDefaultPattern(key);
        }
        
        // Analyze patterns
        TrafficPattern.SeasonalityInfo seasonality = detectSeasonality(history);
        TrafficPattern.TrendInfo trend = analyzeTrend(history);
        TrafficPattern.VolatilityMetrics volatility = calculateVolatility(history);
        List<TrafficPattern.TrafficPrediction> predictions = predictNextDay(history);
        double confidence = calculateConfidence(history);
        
        return TrafficPattern.builder()
            .key(key)
            .seasonality(seasonality)
            .trend(trend)
            .volatility(volatility)
            .predictedNext24h(predictions)
            .confidence(confidence)
            .build();
    }
    
    /**
     * Get traffic history for a key within a duration
     */
    private List<TrafficDataPoint> getTrafficHistory(String key, Duration duration) {
        List<TrafficDataPoint> history = trafficHistory.get(key);
        if (history == null) {
            return new ArrayList<>();
        }
        
        long cutoffTime = Instant.now().minus(duration).toEpochMilli();
        List<TrafficDataPoint> filtered = new ArrayList<>();
        
        synchronized (history) {
            for (TrafficDataPoint point : history) {
                if (point.timestamp >= cutoffTime) {
                    filtered.add(point);
                }
            }
        }
        
        return filtered;
    }
    
    /**
     * Detect seasonality patterns (simplified version)
     */
    private TrafficPattern.SeasonalityInfo detectSeasonality(List<TrafficDataPoint> history) {
        // Phase 1: Simple heuristic - check if there's a daily pattern
        // Phase 2: Use FFT or autocorrelation for proper seasonality detection
        boolean detected = history.size() > 24; // Need enough data
        String pattern = detected ? "DAILY" : "NONE";
        double strength = detected ? 0.5 : 0.0;
        
        return new TrafficPattern.SeasonalityInfo(detected, pattern, strength);
    }
    
    /**
     * Analyze trend direction (simplified version)
     */
    private TrafficPattern.TrendInfo analyzeTrend(List<TrafficDataPoint> history) {
        if (history.size() < 10) {
            return new TrafficPattern.TrendInfo("STABLE", 0.0, 0.5);
        }
        
        // Simple trend: compare first half to second half
        int midpoint = history.size() / 2;
        double firstHalfAvg = history.subList(0, midpoint).stream()
            .mapToDouble(dp -> dp.tokensRequested)
            .average()
            .orElse(0.0);
        
        double secondHalfAvg = history.subList(midpoint, history.size()).stream()
            .mapToDouble(dp -> dp.tokensRequested)
            .average()
            .orElse(0.0);
        
        double slope = (secondHalfAvg - firstHalfAvg) / firstHalfAvg;
        String direction;
        
        if (slope > 0.1) {
            direction = "INCREASING";
        } else if (slope < -0.1) {
            direction = "DECREASING";
        } else {
            direction = "STABLE";
        }
        
        return new TrafficPattern.TrendInfo(direction, slope, 0.7);
    }
    
    /**
     * Calculate volatility metrics
     */
    private TrafficPattern.VolatilityMetrics calculateVolatility(List<TrafficDataPoint> history) {
        if (history.isEmpty()) {
            return new TrafficPattern.VolatilityMetrics(0.0, 0.0, "LOW");
        }
        
        double mean = history.stream()
            .mapToDouble(dp -> dp.tokensRequested)
            .average()
            .orElse(0.0);
        
        double variance = history.stream()
            .mapToDouble(dp -> Math.pow(dp.tokensRequested - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 0.0;
        
        String level;
        if (cv < 0.5) {
            level = "LOW";
        } else if (cv < 1.5) {
            level = "MEDIUM";
        } else {
            level = "HIGH";
        }
        
        return new TrafficPattern.VolatilityMetrics(stdDev, cv, level);
    }
    
    /**
     * Predict traffic for next 24 hours (simplified version)
     */
    private List<TrafficPattern.TrafficPrediction> predictNextDay(List<TrafficDataPoint> history) {
        List<TrafficPattern.TrafficPrediction> predictions = new ArrayList<>();
        
        if (history.isEmpty()) {
            return predictions;
        }
        
        // Simple prediction: use recent average
        double recentAvg = history.stream()
            .mapToDouble(dp -> dp.tokensRequested)
            .average()
            .orElse(0.0);
        
        long now = Instant.now().toEpochMilli();
        long hourMs = 3600000;
        
        // Predict next 24 hours (one prediction per hour)
        for (int i = 1; i <= 24; i++) {
            predictions.add(new TrafficPattern.TrafficPrediction(
                now + (i * hourMs),
                recentAvg,
                0.6 // Confidence
            ));
        }
        
        return predictions;
    }
    
    /**
     * Calculate confidence in the analysis
     */
    private double calculateConfidence(List<TrafficDataPoint> history) {
        // Confidence increases with more data
        int size = history.size();
        if (size < 10) return 0.3;
        if (size < 100) return 0.5;
        if (size < 1000) return 0.7;
        return 0.85;
    }
    
    /**
     * Create a default pattern when no history exists
     */
    private TrafficPattern createDefaultPattern(String key) {
        return TrafficPattern.builder()
            .key(key)
            .seasonality(new TrafficPattern.SeasonalityInfo(false, "NONE", 0.0))
            .trend(new TrafficPattern.TrendInfo("STABLE", 0.0, 0.3))
            .volatility(new TrafficPattern.VolatilityMetrics(0.0, 0.0, "LOW"))
            .predictedNext24h(new ArrayList<>())
            .confidence(0.3)
            .build();
    }
    
    /**
     * Clear history for a key
     */
    public void clearHistory(String key) {
        trafficHistory.remove(key);
    }
    
    /**
     * Data point for traffic history
     */
    public static class TrafficDataPoint {
        final long timestamp;
        final int tokensRequested;
        final boolean allowed;
        
        public TrafficDataPoint(long timestamp, int tokensRequested, boolean allowed) {
            this.timestamp = timestamp;
            this.tokensRequested = tokensRequested;
            this.allowed = allowed;
        }
    }
}
