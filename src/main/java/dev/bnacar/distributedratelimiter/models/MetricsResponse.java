package dev.bnacar.distributedratelimiter.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;

public class MetricsResponse {
    private final Map<String, KeyMetrics> keyMetrics;
    private final boolean redisConnected;
    private final long totalAllowedRequests;
    private final long totalDeniedRequests;

    @JsonCreator
    public MetricsResponse(@JsonProperty("keyMetrics") Map<String, KeyMetrics> keyMetrics, 
                          @JsonProperty("redisConnected") boolean redisConnected, 
                          @JsonProperty("totalAllowedRequests") long totalAllowedRequests, 
                          @JsonProperty("totalDeniedRequests") long totalDeniedRequests) {
        this.keyMetrics = keyMetrics != null ? new HashMap<>(keyMetrics) : new HashMap<>();
        this.redisConnected = redisConnected;
        this.totalAllowedRequests = totalAllowedRequests;
        this.totalDeniedRequests = totalDeniedRequests;
    }

    public Map<String, KeyMetrics> getKeyMetrics() {
        return new HashMap<>(keyMetrics);
    }

    public boolean isRedisConnected() {
        return redisConnected;
    }

    public long getTotalAllowedRequests() {
        return totalAllowedRequests;
    }

    public long getTotalDeniedRequests() {
        return totalDeniedRequests;
    }

    public static class KeyMetrics {
        private final long allowedRequests;
        private final long deniedRequests;
        private final long lastAccessTime;

        @JsonCreator
        public KeyMetrics(@JsonProperty("allowedRequests") long allowedRequests, 
                         @JsonProperty("deniedRequests") long deniedRequests, 
                         @JsonProperty("lastAccessTime") long lastAccessTime) {
            this.allowedRequests = allowedRequests;
            this.deniedRequests = deniedRequests;
            this.lastAccessTime = lastAccessTime;
        }

        public long getAllowedRequests() {
            return allowedRequests;
        }

        public long getDeniedRequests() {
            return deniedRequests;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}