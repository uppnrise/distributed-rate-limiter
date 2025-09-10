package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult;
import dev.bnacar.distributedratelimiter.monitoring.PerformanceRegressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * REST controller for performance monitoring and regression detection.
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceRegressionService regressionService;

    @Autowired
    public PerformanceController(PerformanceRegressionService regressionService) {
        this.regressionService = regressionService;
    }

    /**
     * Store a new performance baseline.
     */
    @PostMapping("/baseline")
    public ResponseEntity<String> storeBaseline(@Valid @RequestBody PerformanceBaseline baseline) {
        try {
            regressionService.storeBaseline(baseline);
            return ResponseEntity.ok("Performance baseline stored successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to store baseline: " + e.getMessage());
        }
    }

    /**
     * Analyze performance regression for a new baseline.
     */
    @PostMapping("/regression/analyze")
    public ResponseEntity<PerformanceRegressionResult> analyzeRegression(
            @Valid @RequestBody PerformanceBaseline currentBaseline,
            @RequestParam(required = false) Double responseTimeThreshold,
            @RequestParam(required = false) Double throughputThreshold,
            @RequestParam(required = false) Double successRateThreshold) {
        
        try {
            PerformanceRegressionResult.RegressionThresholds customThresholds = null;
            if (responseTimeThreshold != null || throughputThreshold != null || successRateThreshold != null) {
                customThresholds = new PerformanceRegressionResult.RegressionThresholds(
                    responseTimeThreshold != null ? responseTimeThreshold : 20.0,
                    throughputThreshold != null ? throughputThreshold : 15.0,
                    successRateThreshold != null ? successRateThreshold : 5.0
                );
            }
            
            PerformanceRegressionResult result = regressionService.analyzeRegression(currentBaseline, customThresholds);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Store baseline and analyze regression in one call.
     */
    @PostMapping("/baseline/store-and-analyze")
    public ResponseEntity<PerformanceRegressionResult> storeAndAnalyze(
            @Valid @RequestBody PerformanceBaseline baseline) {
        try {
            // First analyze regression before storing
            PerformanceRegressionResult result = regressionService.analyzeRegression(baseline, null);
            
            // Then store the baseline for future comparisons
            regressionService.storeBaseline(baseline);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get historical performance baselines for a test.
     */
    @GetMapping("/baseline/{testName}")
    public ResponseEntity<List<PerformanceBaseline>> getBaselines(
            @PathVariable String testName,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PerformanceBaseline> baselines = regressionService.getPerformanceTrend(testName, limit);
            return ResponseEntity.ok(baselines);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get performance trend for a specific test.
     */
    @GetMapping("/trend/{testName}")
    public ResponseEntity<List<PerformanceBaseline>> getPerformanceTrend(
            @PathVariable String testName,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<PerformanceBaseline> trend = regressionService.getPerformanceTrend(testName, limit);
            return ResponseEntity.ok(trend);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Health check for performance monitoring.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Performance monitoring service is healthy");
    }
}