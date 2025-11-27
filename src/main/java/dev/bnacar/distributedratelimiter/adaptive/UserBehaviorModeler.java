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
 * Models user behavior patterns for adaptive rate limiting
 */
@Component
public class UserBehaviorModeler {
    
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorModeler.class);
    
    // Store request events for behavior analysis
    private final Map<String, List<RequestEvent>> requestHistory = new ConcurrentHashMap<>();
    private static final int MAX_EVENTS_PER_KEY = 5000;
    
    /**
     * Record a request event
     */
    public void recordRequest(String key, int tokensRequested, boolean allowed) {
        List<RequestEvent> events = requestHistory.computeIfAbsent(key, k -> new ArrayList<>());
        
        synchronized (events) {
            events.add(new RequestEvent(Instant.now(), tokensRequested, allowed));
            
            // Limit history to prevent memory issues
            if (events.size() > MAX_EVENTS_PER_KEY) {
                events.remove(0);
            }
        }
    }
    
    /**
     * Get current user behavior for a key
     */
    public UserBehavior getCurrentBehavior(String key) {
        List<RequestEvent> recentRequests = getRecentRequests(key, Duration.ofHours(4));
        
        if (recentRequests.isEmpty()) {
            return createDefaultBehavior();
        }
        
        return UserBehavior.builder()
            .averageRequestRate(calculateAverageRate(recentRequests))
            .burstiness(calculateBurstiness(recentRequests))
            .sessionDuration(calculateSessionDuration(recentRequests))
            .timeOfDayPattern(analyzeTimePattern(recentRequests))
            .anomalyScore(detectBehaviorAnomalies(key, recentRequests))
            .build();
    }
    
    /**
     * Get recent requests within a duration
     */
    private List<RequestEvent> getRecentRequests(String key, Duration duration) {
        List<RequestEvent> events = requestHistory.get(key);
        if (events == null) {
            return new ArrayList<>();
        }
        
        Instant cutoff = Instant.now().minus(duration);
        List<RequestEvent> recent = new ArrayList<>();
        
        synchronized (events) {
            for (RequestEvent event : events) {
                if (event.timestamp.isAfter(cutoff)) {
                    recent.add(event);
                }
            }
        }
        
        return recent;
    }
    
    /**
     * Calculate average request rate (requests per second)
     */
    private double calculateAverageRate(List<RequestEvent> requests) {
        if (requests.size() < 2) {
            return 0.0;
        }
        
        long durationMs = requests.get(requests.size() - 1).timestamp.toEpochMilli() 
                        - requests.get(0).timestamp.toEpochMilli();
        
        if (durationMs <= 0) {
            return 0.0;
        }
        
        return (double) requests.size() / (durationMs / 1000.0);
    }
    
    /**
     * Calculate burstiness (variance/mean ratio)
     */
    private double calculateBurstiness(List<RequestEvent> requests) {
        if (requests.size() < 10) {
            return 0.0;
        }
        
        // Calculate inter-arrival times
        List<Long> interArrivalTimes = new ArrayList<>();
        for (int i = 1; i < requests.size(); i++) {
            long interval = requests.get(i).timestamp.toEpochMilli() 
                          - requests.get(i - 1).timestamp.toEpochMilli();
            interArrivalTimes.add(interval);
        }
        
        double mean = interArrivalTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        if (mean == 0) {
            return 0.0;
        }
        
        double variance = interArrivalTimes.stream()
            .mapToDouble(time -> Math.pow(time - mean, 2))
            .average()
            .orElse(0.0);
        
        return variance / mean;
    }
    
    /**
     * Calculate session duration in seconds
     */
    private double calculateSessionDuration(List<RequestEvent> requests) {
        if (requests.size() < 2) {
            return 0.0;
        }
        
        long durationMs = requests.get(requests.size() - 1).timestamp.toEpochMilli()
                        - requests.get(0).timestamp.toEpochMilli();
        
        return durationMs / 1000.0;
    }
    
    /**
     * Analyze time-of-day pattern
     */
    private UserBehavior.TimeOfDayPattern analyzeTimePattern(List<RequestEvent> requests) {
        if (requests.isEmpty()) {
            return new UserBehavior.TimeOfDayPattern(12, 0, 1.0);
        }
        
        // Count requests by hour
        int[] hourCounts = new int[24];
        for (RequestEvent event : requests) {
            int hour = event.timestamp.atZone(java.time.ZoneId.systemDefault()).getHour();
            hourCounts[hour]++;
        }
        
        // Find peak and off-peak hours
        int peakHour = 0;
        int offPeakHour = 0;
        int maxCount = 0;
        int minCount = Integer.MAX_VALUE;
        
        for (int hour = 0; hour < 24; hour++) {
            if (hourCounts[hour] > maxCount) {
                maxCount = hourCounts[hour];
                peakHour = hour;
            }
            if (hourCounts[hour] < minCount && hourCounts[hour] > 0) {
                minCount = hourCounts[hour];
                offPeakHour = hour;
            }
        }
        
        double ratio = minCount > 0 ? (double) maxCount / minCount : 1.0;
        
        return new UserBehavior.TimeOfDayPattern(peakHour, offPeakHour, ratio);
    }
    
    /**
     * Detect behavior anomalies (simplified)
     */
    private double detectBehaviorAnomalies(String key, List<RequestEvent> requests) {
        if (requests.size() < 10) {
            return 0.0;
        }
        
        // Calculate recent vs historical average
        List<RequestEvent> allHistory = requestHistory.get(key);
        if (allHistory == null || allHistory.size() < 100) {
            return 0.0;
        }
        
        double recentRate = calculateAverageRate(requests);
        double historicalRate = calculateAverageRate(allHistory);
        
        if (historicalRate == 0) {
            return 0.0;
        }
        
        double deviation = Math.abs(recentRate - historicalRate) / historicalRate;
        return Math.min(deviation, 1.0);
    }
    
    /**
     * Create default behavior when no history exists
     */
    private UserBehavior createDefaultBehavior() {
        return UserBehavior.builder()
            .averageRequestRate(0.0)
            .burstiness(0.0)
            .sessionDuration(0.0)
            .timeOfDayPattern(new UserBehavior.TimeOfDayPattern(12, 0, 1.0))
            .anomalyScore(0.0)
            .build();
    }
    
    /**
     * Clear history for a key
     */
    public void clearHistory(String key) {
        requestHistory.remove(key);
    }
    
    /**
     * Request event for behavior tracking
     */
    public static class RequestEvent {
        final Instant timestamp;
        final int tokensRequested;
        final boolean allowed;
        
        public RequestEvent(Instant timestamp, int tokensRequested, boolean allowed) {
            this.timestamp = timestamp;
            this.tokensRequested = tokensRequested;
            this.allowed = allowed;
        }
    }
}
