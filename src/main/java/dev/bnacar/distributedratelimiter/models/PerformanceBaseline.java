package dev.bnacar.distributedratelimiter.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Model representing a performance baseline for regression detection.
 */
public class PerformanceBaseline {
    
    @NotNull
    @JsonProperty("testName")
    private String testName;
    
    @NotNull
    @JsonProperty("averageResponseTime")
    private Double averageResponseTime;
    
    @NotNull
    @JsonProperty("maxResponseTime")
    private Double maxResponseTime;
    
    @NotNull
    @JsonProperty("throughputPerSecond")
    private Double throughputPerSecond;
    
    @NotNull
    @JsonProperty("successRate")
    private Double successRate;
    
    @NotNull
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("commitHash")
    private String commitHash;
    
    @JsonProperty("buildNumber")
    private String buildNumber;
    
    @JsonProperty("environment")
    private String environment;

    // Default constructor
    public PerformanceBaseline() {}

    // Constructor for creating baseline from test results
    public PerformanceBaseline(String testName, Double averageResponseTime, Double maxResponseTime,
                             Double throughputPerSecond, Double successRate) {
        this.testName = testName;
        this.averageResponseTime = averageResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.throughputPerSecond = throughputPerSecond;
        this.successRate = successRate;
        this.timestamp = LocalDateTime.now();
    }

    // Full constructor
    public PerformanceBaseline(String testName, Double averageResponseTime, Double maxResponseTime,
                             Double throughputPerSecond, Double successRate, String commitHash,
                             String buildNumber, String environment) {
        this(testName, averageResponseTime, maxResponseTime, throughputPerSecond, successRate);
        this.commitHash = commitHash;
        this.buildNumber = buildNumber;
        this.environment = environment;
    }

    // Getters and setters
    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(Double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public Double getMaxResponseTime() {
        return maxResponseTime;
    }

    public void setMaxResponseTime(Double maxResponseTime) {
        this.maxResponseTime = maxResponseTime;
    }

    public Double getThroughputPerSecond() {
        return throughputPerSecond;
    }

    public void setThroughputPerSecond(Double throughputPerSecond) {
        this.throughputPerSecond = throughputPerSecond;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public String toString() {
        return String.format(
            "PerformanceBaseline{testName='%s', avgResponseTime=%.2fms, maxResponseTime=%.2fms, " +
            "throughput=%.2f req/sec, successRate=%.2f%%, timestamp=%s, commit='%s'}",
            testName, averageResponseTime, maxResponseTime, throughputPerSecond, successRate, 
            timestamp, commitHash
        );
    }
}