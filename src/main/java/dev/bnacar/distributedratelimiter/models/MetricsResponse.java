package dev.bnacar.distributedratelimiter.models;

import java.util.Map;

public class MetricsResponse {
    private final Map<String, KeyMetrics> keyMetrics;
    private final boolean redisConnected;
    private final long totalAllowedRequests;
    private final long totalDeniedRequests;

    public MetricsResponse(Map<String, KeyMetrics> keyMetrics, boolean redisConnected, 
                          long totalAllowedRequests, long totalDeniedRequests) {
        this.keyMetrics = keyMetrics;
        this.redisConnected = redisConnected;
        this.totalAllowedRequests = totalAllowedRequests;
        this.totalDeniedRequests = totalDeniedRequests;
    }

    public Map<String, KeyMetrics> getKeyMetrics() {
        return keyMetrics;
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

        public KeyMetrics(long allowedRequests, long deniedRequests, long lastAccessTime) {
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