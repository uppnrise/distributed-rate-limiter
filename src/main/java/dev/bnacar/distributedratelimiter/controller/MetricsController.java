package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.MetricsResponse;
import dev.bnacar.distributedratelimiter.monitoring.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponse> getMetrics() {
        MetricsResponse metrics = metricsService.getMetrics();
        return ResponseEntity.ok(metrics);
    }
}