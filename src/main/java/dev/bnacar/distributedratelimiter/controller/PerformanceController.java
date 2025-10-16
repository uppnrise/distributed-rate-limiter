package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.PerformanceBaseline;
import dev.bnacar.distributedratelimiter.models.PerformanceRegressionResult;
import dev.bnacar.distributedratelimiter.monitoring.PerformanceRegressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * REST controller for performance monitoring and regression detection.
 */
@RestController
@RequestMapping("/api/performance")
@Tag(name = "performance-controller", description = "Performance monitoring and regression analysis for rate limiter operations")
public class PerformanceController {

    private final PerformanceRegressionService regressionService;

    public PerformanceController(PerformanceRegressionService regressionService) {
        this.regressionService = regressionService;
    }

    /**
     * Store a new performance baseline.
     */
    @PostMapping("/baseline")
    @Operation(summary = "Store performance baseline",
               description = "Records a new performance baseline for future regression analysis")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Baseline stored successfully"),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid baseline data")
    })
    public ResponseEntity<String> storeBaseline(
            @Parameter(description = "Performance baseline data", required = true)
            @Valid @RequestBody PerformanceBaseline baseline) {
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
    @Operation(summary = "Analyze performance regression",
               description = "Compares current performance against historical baselines to detect regressions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Regression analysis completed",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = PerformanceRegressionResult.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid performance data or analysis failed")
    })
    public ResponseEntity<PerformanceRegressionResult> analyzeRegression(
            @Parameter(description = "Current performance baseline to analyze", required = true)
            @Valid @RequestBody PerformanceBaseline currentBaseline,
            @Parameter(description = "Custom response time regression threshold (percentage)", example = "20.0")
            @RequestParam(value = "responseTimeThreshold", required = false) Double responseTimeThreshold,
            @Parameter(description = "Custom throughput regression threshold (percentage)", example = "15.0")
            @RequestParam(value = "throughputThreshold", required = false) Double throughputThreshold,
            @Parameter(description = "Custom success rate regression threshold (percentage)", example = "5.0")
            @RequestParam(value = "successRateThreshold", required = false) Double successRateThreshold) {
        
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
    @Operation(summary = "Store baseline and analyze regression",
               description = "Performs regression analysis and then stores the baseline for future comparisons")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Analysis and storage completed",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = PerformanceRegressionResult.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Invalid performance data or operation failed")
    })
    public ResponseEntity<PerformanceRegressionResult> storeAndAnalyze(
            @Parameter(description = "Performance baseline to analyze and store", required = true)
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
    @Operation(summary = "Get performance baselines for a test",
               description = "Retrieves historical performance baseline data for the specified test")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Baselines retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = PerformanceBaseline.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Failed to retrieve baselines")
    })
    public ResponseEntity<List<PerformanceBaseline>> getBaselines(
            @Parameter(description = "Name of the test", required = true, example = "rate-limiter-load-test")
            @PathVariable("testName") String testName,
            @Parameter(description = "Maximum number of baselines to return", example = "10")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
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
    @Operation(summary = "Get performance trend for a test",
               description = "Retrieves performance trend data showing how metrics have changed over time")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Trend data retrieved successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = PerformanceBaseline.class))),
        @ApiResponse(responseCode = "400", 
                    description = "Failed to retrieve trend data")
    })
    public ResponseEntity<List<PerformanceBaseline>> getPerformanceTrend(
            @Parameter(description = "Name of the test", required = true, example = "rate-limiter-load-test")
            @PathVariable("testName") String testName,
            @Parameter(description = "Maximum number of data points to return", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
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
    @Operation(summary = "Performance monitoring health check",
               description = "Check if the performance monitoring service is operational")
    @ApiResponse(responseCode = "200", 
                description = "Performance monitoring service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Performance monitoring service is healthy");
    }
}