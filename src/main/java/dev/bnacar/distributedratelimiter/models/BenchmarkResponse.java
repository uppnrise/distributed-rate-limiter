package dev.bnacar.distributedratelimiter.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for benchmark operations.
 */
public class BenchmarkResponse {
    
    private final boolean success;
    private final String errorMessage;
    private final long totalRequests;
    private final long successfulRequests;
    private final long errorRequests;
    private final double durationSeconds;
    private final double throughputPerSecond;
    private final double successRate;
    private final int concurrentThreads;
    private final long requestsPerThread;
    
    // Constructor for successful benchmark
    @JsonCreator
    public BenchmarkResponse(
            @JsonProperty("totalRequests") long totalRequests, 
            @JsonProperty("successfulRequests") long successfulRequests, 
            @JsonProperty("errorRequests") long errorRequests,
            @JsonProperty("durationSeconds") double durationSeconds, 
            @JsonProperty("throughputPerSecond") double throughputPerSecond, 
            @JsonProperty("successRate") double successRate,
            @JsonProperty("concurrentThreads") int concurrentThreads, 
            @JsonProperty("requestsPerThread") long requestsPerThread) {
        this.success = true;
        this.errorMessage = null;
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.errorRequests = errorRequests;
        this.durationSeconds = durationSeconds;
        this.throughputPerSecond = throughputPerSecond;
        this.successRate = successRate;
        this.concurrentThreads = concurrentThreads;
        this.requestsPerThread = requestsPerThread;
    }
    
    // Constructor for error response
    private BenchmarkResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.totalRequests = 0;
        this.successfulRequests = 0;
        this.errorRequests = 0;
        this.durationSeconds = 0;
        this.throughputPerSecond = 0;
        this.successRate = 0;
        this.concurrentThreads = 0;
        this.requestsPerThread = 0;
    }
    
    public static BenchmarkResponse error(String errorMessage) {
        return new BenchmarkResponse(errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getTotalRequests() {
        return totalRequests;
    }
    
    public long getSuccessfulRequests() {
        return successfulRequests;
    }
    
    public long getErrorRequests() {
        return errorRequests;
    }
    
    public double getDurationSeconds() {
        return durationSeconds;
    }
    
    public double getThroughputPerSecond() {
        return throughputPerSecond;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public int getConcurrentThreads() {
        return concurrentThreads;
    }
    
    public long getRequestsPerThread() {
        return requestsPerThread;
    }
    
    /**
     * Check if the benchmark meets the performance target.
     */
    public boolean meetsPerformanceTarget(double targetThroughput) {
        return success && throughputPerSecond >= targetThroughput;
    }
    
    @Override
    public String toString() {
        if (!success) {
            return "BenchmarkResponse{success=false, error='" + errorMessage + "'}";
        }
        
        return String.format(
            "BenchmarkResponse{success=%s, totalRequests=%d, successfulRequests=%d, " +
            "errorRequests=%d, durationSeconds=%.2f, throughputPerSecond=%.2f, " +
            "successRate=%.2f%%, concurrentThreads=%d, requestsPerThread=%d}",
            success, totalRequests, successfulRequests, errorRequests, 
            durationSeconds, throughputPerSecond, successRate, concurrentThreads, requestsPerThread
        );
    }
}