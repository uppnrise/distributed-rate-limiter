package dev.bnacar.distributedratelimiter.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request model for benchmark operations.
 */
public class BenchmarkRequest {
    
    @NotNull
    @Min(1)
    private Integer concurrentThreads = 10;
    
    @NotNull
    @Min(1)
    private Long requestsPerThread = 100L;
    
    @NotNull
    @Min(1)
    private Integer durationSeconds = 30;
    
    @NotNull
    @Min(1)
    private Integer tokensPerRequest = 1;
    
    @Min(0)
    private Long delayBetweenRequestsMs = 0L;
    
    private String keyPrefix = "benchmark";
    
    public BenchmarkRequest() {}
    
    public BenchmarkRequest(Integer concurrentThreads, Long requestsPerThread, 
                          Integer durationSeconds, Integer tokensPerRequest) {
        this.concurrentThreads = concurrentThreads;
        this.requestsPerThread = requestsPerThread;
        this.durationSeconds = durationSeconds;
        this.tokensPerRequest = tokensPerRequest;
    }
    
    public Integer getConcurrentThreads() {
        return concurrentThreads;
    }
    
    public void setConcurrentThreads(Integer concurrentThreads) {
        this.concurrentThreads = concurrentThreads;
    }
    
    public Long getRequestsPerThread() {
        return requestsPerThread;
    }
    
    public void setRequestsPerThread(Long requestsPerThread) {
        this.requestsPerThread = requestsPerThread;
    }
    
    public Integer getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    
    public Integer getTokensPerRequest() {
        return tokensPerRequest;
    }
    
    public void setTokensPerRequest(Integer tokensPerRequest) {
        this.tokensPerRequest = tokensPerRequest;
    }
    
    public Long getDelayBetweenRequestsMs() {
        return delayBetweenRequestsMs;
    }
    
    public void setDelayBetweenRequestsMs(Long delayBetweenRequestsMs) {
        this.delayBetweenRequestsMs = delayBetweenRequestsMs;
    }
    
    public String getKeyPrefix() {
        return keyPrefix;
    }
    
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}