package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.geo.GeographicConfigurationResolver;
import dev.bnacar.distributedratelimiter.geo.GeoLocationService;
import dev.bnacar.distributedratelimiter.models.GeoLocation;
import dev.bnacar.distributedratelimiter.models.GeographicRateLimitConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST controller for managing geographic rate limiting rules and debugging geographic location detection.
 */
@RestController
@RequestMapping("/api/ratelimit/geographic")
@Tag(name = "Geographic Rate Limiting", description = "Operations for geographic rate limiting configuration and debugging")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173", "http://127.0.0.1:3000", "http://[::1]:5173", "http://[::1]:3000"})
public class GeographicRateLimitController {
    private static final Logger logger = LoggerFactory.getLogger(GeographicRateLimitController.class);

    private final GeographicConfigurationResolver geographicConfigResolver;
    private final GeoLocationService geoLocationService;

    @Autowired
    public GeographicRateLimitController(GeographicConfigurationResolver geographicConfigResolver,
                                       GeoLocationService geoLocationService) {
        this.geographicConfigResolver = geographicConfigResolver;
        this.geoLocationService = geoLocationService;
    }

    @GetMapping("/rules")
    @Operation(summary = "Get all geographic rate limiting rules",
               description = "Returns all configured geographic rate limiting rules")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rules retrieved successfully")
    })
    public ResponseEntity<List<GeographicRateLimitConfig>> getAllRules() {
        List<GeographicRateLimitConfig> rules = geographicConfigResolver.getAllGeographicRules();
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/rules")
    @Operation(summary = "Add a new geographic rate limiting rule",
               description = "Creates a new geographic rate limiting rule")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule added successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rule configuration")
    })
    public ResponseEntity<String> addRule(@RequestBody GeographicRateLimitConfig rule) {
        try {
            geographicConfigResolver.addGeographicRule(rule);
            return ResponseEntity.ok("Geographic rule added successfully: " + rule.getName());
        } catch (Exception e) {
            logger.error("Failed to add geographic rule", e);
            return ResponseEntity.badRequest().body("Failed to add rule: " + e.getMessage());
        }
    }

    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Remove a geographic rate limiting rule",
               description = "Removes a geographic rate limiting rule by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule removed successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<String> removeRule(@Parameter(description = "Rule ID to remove") @PathVariable String ruleId) {
        boolean removed = geographicConfigResolver.removeGeographicRule(ruleId);
        if (removed) {
            return ResponseEntity.ok("Geographic rule removed successfully: " + ruleId);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/detect")
    @Operation(summary = "Detect geographic location for current request",
               description = "Detects and returns geographic location information for debugging purposes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Location detected successfully")
    })
    public ResponseEntity<Map<String, Object>> detectLocation(HttpServletRequest request,
                                                            @Parameter(description = "Override source IP for testing") @RequestParam(required = false) String sourceIP) {
        try {
            // Extract client IP
            String clientIP = sourceIP != null ? sourceIP : extractClientIP(request);
            
            // Extract geographic headers
            Map<String, String> headers = extractGeographicHeaders(request);
            
            // Detect location
            GeoLocation location = geoLocationService.detectLocation(clientIP, headers);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("sourceIP", clientIP);
            response.put("detectedHeaders", headers);
            response.put("geoLocation", location);
            
            if (location != null) {
                // Get applicable rules for this location
                List<GeographicRateLimitConfig> applicableRules = geographicConfigResolver.getRulesForLocation(location);
                response.put("applicableRules", applicableRules.size());
                response.put("ruleNames", applicableRules.stream().map(GeographicRateLimitConfig::getName).toList());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to detect geographic location", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to detect location: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get geographic rate limiting statistics",
               description = "Returns cache statistics and performance metrics for geographic rate limiting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("geoLocationCache", geoLocationService.getCacheStats());
        stats.put("geographicConfigCache", geographicConfigResolver.getCacheStats());
        stats.put("totalRules", geographicConfigResolver.getAllGeographicRules().size());
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear geographic caches",
               description = "Clears all geographic location and configuration caches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Caches cleared successfully")
    })
    public ResponseEntity<String> clearCaches() {
        geoLocationService.clearCache();
        geographicConfigResolver.clearCache();
        
        return ResponseEntity.ok("Geographic caches cleared successfully");
    }

    /**
     * Extract client IP address from request.
     */
    private String extractClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Extract geographic headers from HTTP request.
     */
    private Map<String, String> extractGeographicHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        // CloudFlare headers
        addHeaderIfPresent(headers, request, "CF-IPCountry");
        addHeaderIfPresent(headers, request, "CF-IPContinent");
        addHeaderIfPresent(headers, request, "CF-IPCity");
        addHeaderIfPresent(headers, request, "CF-Timezone");
        
        // AWS CloudFront headers
        addHeaderIfPresent(headers, request, "CloudFront-Viewer-Country");
        addHeaderIfPresent(headers, request, "CloudFront-Viewer-Country-Region");
        
        // Generic headers
        addHeaderIfPresent(headers, request, "X-Country-Code");
        addHeaderIfPresent(headers, request, "X-Region");
        addHeaderIfPresent(headers, request, "X-City");
        
        return headers;
    }

    /**
     * Add header to map if present in request.
     */
    private void addHeaderIfPresent(Map<String, String> headers, HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null && !headerValue.trim().isEmpty()) {
            headers.put(headerName, headerValue.trim());
        }
    }
}