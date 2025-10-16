package dev.bnacar.distributedratelimiter.monitoring;

import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult.RegressionSeverity;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult.RegressionThresholds;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for detecting performance regressions by comparing current results
 * against historical performance baselines.
 */
@Service
public class PerformanceRegressionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceRegressionService.class);
    
    private final ObjectMapper objectMapper;
    private final String baselineStoragePath;
    private final RegressionThresholds defaultThresholds;
    
    public PerformanceRegressionService(@Value("${performance.baseline.storage.path:./target/performance-baselines.json}") String baselineStoragePath) {
        this.baselineStoragePath = baselineStoragePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.defaultThresholds = new RegressionThresholds();
    }
    
    /**
     * Store a new performance baseline.
     */
    public void storeBaseline(PerformanceBaseline baseline) {
        try {
            List<PerformanceBaseline> baselines = loadBaselines();
            baselines.add(baseline);
            
            // Keep only the latest 50 baselines per test to avoid file growth
            Map<String, List<PerformanceBaseline>> byTestName = baselines.stream()
                .collect(Collectors.groupingBy(PerformanceBaseline::getTestName));
            
            List<PerformanceBaseline> trimmedBaselines = new ArrayList<>();
            for (Map.Entry<String, List<PerformanceBaseline>> entry : byTestName.entrySet()) {
                List<PerformanceBaseline> testBaselines = entry.getValue();
                testBaselines.sort(Comparator.comparing(PerformanceBaseline::getTimestamp).reversed());
                trimmedBaselines.addAll(testBaselines.stream().limit(50).collect(Collectors.toList()));
            }
            
            saveBaselines(trimmedBaselines);
            logger.info("Stored performance baseline for test: {}", baseline.getTestName());
            
        } catch (IOException e) {
            logger.error("Failed to store performance baseline", e);
            throw new RuntimeException("Failed to store performance baseline", e);
        }
    }
    
    /**
     * Analyze current performance against historical baselines.
     */
    public PerformanceRegressionResult analyzeRegression(PerformanceBaseline currentBaseline, 
                                                       RegressionThresholds customThresholds) {
        RegressionThresholds thresholds = customThresholds != null ? customThresholds : defaultThresholds;
        
        try {
            List<PerformanceBaseline> historicalBaselines = getHistoricalBaselines(currentBaseline.getTestName());
            
            if (historicalBaselines.isEmpty()) {
                logger.info("No historical baselines found for test: {}", currentBaseline.getTestName());
                PerformanceRegressionResult result = new PerformanceRegressionResult(
                    currentBaseline.getTestName(), currentBaseline, null);
                result.setHasRegression(false);
                result.setRegressionSeverity(RegressionSeverity.NONE);
                result.setRegressionDetails(List.of("No historical data available for comparison"));
                result.setThresholds(thresholds);
                return result;
            }
            
            // Use the most recent baseline for comparison
            PerformanceBaseline previousBaseline = historicalBaselines.get(0);
            
            PerformanceRegressionResult result = new PerformanceRegressionResult(
                currentBaseline.getTestName(), currentBaseline, previousBaseline);
            result.setThresholds(thresholds);
            
            List<String> regressionDetails = new ArrayList<>();
            boolean hasRegression = false;
            RegressionSeverity maxSeverity = RegressionSeverity.NONE;
            
            // Analyze response time regression
            double responseTimeIncrease = calculatePercentageIncrease(
                previousBaseline.getAverageResponseTime(), currentBaseline.getAverageResponseTime());
            
            if (responseTimeIncrease > thresholds.getResponseTimeThreshold()) {
                hasRegression = true;
                String detail = String.format("Response time increased by %.1f%% (%.2fms -> %.2fms)", 
                    responseTimeIncrease, previousBaseline.getAverageResponseTime(), currentBaseline.getAverageResponseTime());
                regressionDetails.add(detail);
                maxSeverity = getWorstSeverity(maxSeverity, getSeverityFromIncrease(responseTimeIncrease, thresholds.getResponseTimeThreshold()));
            }
            
            // Analyze throughput regression
            double throughputDecrease = calculatePercentageDecrease(
                previousBaseline.getThroughputPerSecond(), currentBaseline.getThroughputPerSecond());
            
            if (throughputDecrease > thresholds.getThroughputThreshold()) {
                hasRegression = true;
                String detail = String.format("Throughput decreased by %.1f%% (%.2f -> %.2f req/sec)", 
                    throughputDecrease, previousBaseline.getThroughputPerSecond(), currentBaseline.getThroughputPerSecond());
                regressionDetails.add(detail);
                maxSeverity = getWorstSeverity(maxSeverity, getSeverityFromIncrease(throughputDecrease, thresholds.getThroughputThreshold()));
            }
            
            // Analyze success rate regression
            double successRateDecrease = calculatePercentageDecrease(
                previousBaseline.getSuccessRate(), currentBaseline.getSuccessRate());
            
            if (successRateDecrease > thresholds.getSuccessRateThreshold()) {
                hasRegression = true;
                String detail = String.format("Success rate decreased by %.1f%% (%.2f%% -> %.2f%%)", 
                    successRateDecrease, previousBaseline.getSuccessRate(), currentBaseline.getSuccessRate());
                regressionDetails.add(detail);
                maxSeverity = getWorstSeverity(maxSeverity, getSeverityFromIncrease(successRateDecrease, thresholds.getSuccessRateThreshold()));
            }
            
            result.setHasRegression(hasRegression);
            result.setRegressionDetails(regressionDetails);
            result.setRegressionSeverity(maxSeverity);
            
            if (hasRegression) {
                logger.warn("Performance regression detected for test {}: {}", 
                    currentBaseline.getTestName(), regressionDetails);
            } else {
                logger.info("No performance regression detected for test: {}", currentBaseline.getTestName());
            }
            
            return result;
            
        } catch (IOException e) {
            logger.error("Failed to analyze performance regression", e);
            throw new RuntimeException("Failed to analyze performance regression", e);
        }
    }
    
    /**
     * Get historical baselines for a specific test.
     */
    public List<PerformanceBaseline> getHistoricalBaselines(String testName) throws IOException {
        List<PerformanceBaseline> allBaselines = loadBaselines();
        return allBaselines.stream()
            .filter(baseline -> testName.equals(baseline.getTestName()))
            .sorted(Comparator.comparing(PerformanceBaseline::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get performance trend for a specific test.
     */
    public List<PerformanceBaseline> getPerformanceTrend(String testName, int limit) throws IOException {
        return getHistoricalBaselines(testName).stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    private List<PerformanceBaseline> loadBaselines() throws IOException {
        File file = new File(baselineStoragePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        return objectMapper.readValue(file, new TypeReference<List<PerformanceBaseline>>() {});
    }
    
    private void saveBaselines(List<PerformanceBaseline> baselines) throws IOException {
        File file = new File(baselineStoragePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
        }
        objectMapper.writeValue(file, baselines);
    }
    
    private double calculatePercentageIncrease(double oldValue, double newValue) {
        if (oldValue == 0) return 0;
        return ((newValue - oldValue) / oldValue) * 100;
    }
    
    private double calculatePercentageDecrease(double oldValue, double newValue) {
        if (oldValue == 0) return 0;
        return ((oldValue - newValue) / oldValue) * 100;
    }
    
    private RegressionSeverity getSeverityFromIncrease(double increase, double threshold) {
        if (increase < threshold * 1.5) return RegressionSeverity.MINOR;
        if (increase < threshold * 2.0) return RegressionSeverity.MODERATE;
        if (increase < threshold * 3.0) return RegressionSeverity.MAJOR;
        return RegressionSeverity.CRITICAL;
    }
    
    private RegressionSeverity getWorstSeverity(RegressionSeverity current, RegressionSeverity candidate) {
        return current.ordinal() > candidate.ordinal() ? current : candidate;
    }
}