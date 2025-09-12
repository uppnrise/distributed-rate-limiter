package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "metrics-controller", description = "System metrics and monitoring data")
public class MetricsController {

    private final MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics",
               description = "Retrieves comprehensive system metrics including rate limiter performance and operational statistics")
    @ApiResponse(responseCode = "200", 
                description = "Metrics retrieved successfully",
                content = @Content(mediaType = "application/json",
                                 schema = @Schema(implementation = MetricsResponse.class)))
    public ResponseEntity<MetricsResponse> getMetrics() {
        MetricsResponse metrics = metricsService.getMetrics();
        return ResponseEntity.ok(metrics);
    }
}