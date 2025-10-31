package dev.bnacar.distributedratelimiter.schedule;

/**
 * Configuration for gradual transitions between rate limits.
 */
public class TransitionConfig {
    private int rampUpMinutes;
    private int rampDownMinutes;
    
    public TransitionConfig() {
        this.rampUpMinutes = 0;
        this.rampDownMinutes = 0;
    }
    
    public TransitionConfig(int rampUpMinutes, int rampDownMinutes) {
        this.rampUpMinutes = rampUpMinutes;
        this.rampDownMinutes = rampDownMinutes;
    }
    
    public int getRampUpMinutes() {
        return rampUpMinutes;
    }
    
    public void setRampUpMinutes(int rampUpMinutes) {
        this.rampUpMinutes = rampUpMinutes;
    }
    
    public int getRampDownMinutes() {
        return rampDownMinutes;
    }
    
    public void setRampDownMinutes(int rampDownMinutes) {
        this.rampDownMinutes = rampDownMinutes;
    }
    
    @Override
    public String toString() {
        return "TransitionConfig{" +
                "rampUpMinutes=" + rampUpMinutes +
                ", rampDownMinutes=" + rampDownMinutes +
                '}';
    }
}
