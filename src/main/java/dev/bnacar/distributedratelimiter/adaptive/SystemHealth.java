package dev.bnacar.distributedratelimiter.adaptive;

/**
 * System health metrics for adaptive rate limiting decisions
 */
public class SystemHealth {
    
    private double cpuUtilization;
    private double memoryUtilization;
    private double responseTimeP95;
    private double errorRate;
    private boolean redisHealthy;
    private boolean downstreamServicesHealthy;
    
    private SystemHealth(Builder builder) {
        this.cpuUtilization = builder.cpuUtilization;
        this.memoryUtilization = builder.memoryUtilization;
        this.responseTimeP95 = builder.responseTimeP95;
        this.errorRate = builder.errorRate;
        this.redisHealthy = builder.redisHealthy;
        this.downstreamServicesHealthy = builder.downstreamServicesHealthy;
    }
    
    public double getCpuUtilization() {
        return cpuUtilization;
    }
    
    public double getMemoryUtilization() {
        return memoryUtilization;
    }
    
    public double getResponseTimeP95() {
        return responseTimeP95;
    }
    
    public double getErrorRate() {
        return errorRate;
    }
    
    public boolean isRedisHealthy() {
        return redisHealthy;
    }
    
    public boolean isDownstreamServicesHealthy() {
        return downstreamServicesHealthy;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double cpuUtilization = 0.0;
        private double memoryUtilization = 0.0;
        private double responseTimeP95 = 0.0;
        private double errorRate = 0.0;
        private boolean redisHealthy = true;
        private boolean downstreamServicesHealthy = true;
        
        public Builder cpuUtilization(double cpuUtilization) {
            this.cpuUtilization = cpuUtilization;
            return this;
        }
        
        public Builder memoryUtilization(double memoryUtilization) {
            this.memoryUtilization = memoryUtilization;
            return this;
        }
        
        public Builder responseTimeP95(double responseTimeP95) {
            this.responseTimeP95 = responseTimeP95;
            return this;
        }
        
        public Builder errorRate(double errorRate) {
            this.errorRate = errorRate;
            return this;
        }
        
        public Builder redisHealthy(boolean redisHealthy) {
            this.redisHealthy = redisHealthy;
            return this;
        }
        
        public Builder downstreamServicesHealthy(boolean downstreamServicesHealthy) {
            this.downstreamServicesHealthy = downstreamServicesHealthy;
            return this;
        }
        
        public SystemHealth build() {
            return new SystemHealth(this);
        }
    }
}
