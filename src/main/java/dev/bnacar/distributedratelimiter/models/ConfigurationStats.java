package dev.bnacar.distributedratelimiter.models;

public class ConfigurationStats {
    private final int cacheSize;
    private final int bucketCount;
    private final int keyConfigCount;
    private final int patternConfigCount;

    public ConfigurationStats(int cacheSize, int bucketCount, int keyConfigCount, int patternConfigCount) {
        this.cacheSize = cacheSize;
        this.bucketCount = bucketCount;
        this.keyConfigCount = keyConfigCount;
        this.patternConfigCount = patternConfigCount;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public int getKeyConfigCount() {
        return keyConfigCount;
    }

    public int getPatternConfigCount() {
        return patternConfigCount;
    }
}