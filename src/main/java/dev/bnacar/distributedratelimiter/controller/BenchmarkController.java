package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.BenchmarkRequest;
import dev.bnacar.distributedratelimiter.models.BenchmarkResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark controller for measuring rate limiter performance.
 * Provides endpoints to test throughput under various load conditions.
 */
@RestController
@RequestMapping("/api/benchmark")
@Tag(name = "benchmark-controller", description = "Performance benchmarking and load testing utilities")
public class BenchmarkController {

    private final RateLimiterService rateLimiterService;
    private final ExecutorService benchmarkExecutor;

    @Autowired
    public BenchmarkController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
        this.benchmarkExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Benchmark-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Run a performance benchmark of the rate limiter.
     * Tests throughput under specified load conditions.
     */
    @PostMapping("/run")
    @Operation(summary = "Run performance benchmark",
               description = "Executes a performance benchmark with configurable load parameters to measure rate limiter throughput and latency")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", 
                    description = "Benchmark completed successfully",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = BenchmarkResponse.class),
                                     examples = @ExampleObject(value = "{\"totalRequests\":1000,\"successCount\":850,\"errorCount\":0,\"durationSeconds\":10.5,\"throughputPerSecond\":95.2,\"successRate\":85.0,\"concurrentThreads\":10,\"requestsPerThread\":100}"))),
        @ApiResponse(responseCode = "400", 
                    description = "Benchmark configuration invalid or benchmark failed")
    })
    public ResponseEntity<BenchmarkResponse> runBenchmark(
            @Parameter(description = "Benchmark configuration parameters", required = true,
                      content = @Content(examples = @ExampleObject(value = "{\"concurrentThreads\":10,\"requestsPerThread\":100,\"durationSeconds\":30,\"keyPrefix\":\"benchmark\",\"tokensPerRequest\":1,\"delayBetweenRequestsMs\":0}")))
            @Valid @RequestBody BenchmarkRequest request) {
        long startTime = System.nanoTime();
        
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        AtomicLong totalRequests = new AtomicLong(0);
        
        CountDownLatch latch = new CountDownLatch(request.getConcurrentThreads());
        
        // Launch concurrent workers
        for (int i = 0; i < request.getConcurrentThreads(); i++) {
            final int threadId = i;
            benchmarkExecutor.submit(() -> {
                try {
                    runWorkerThread(request, threadId, successCount, errorCount, totalRequests);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // Wait for all workers to complete or timeout
            boolean completed = latch.await(request.getDurationSeconds() + 10, TimeUnit.SECONDS);
            if (!completed) {
                return ResponseEntity.badRequest().body(
                    BenchmarkResponse.error("Benchmark timed out")
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.badRequest().body(
                BenchmarkResponse.error("Benchmark interrupted")
            );
        }
        
        long endTime = System.nanoTime();
        double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
        
        long total = totalRequests.get();
        long success = successCount.get();
        long errors = errorCount.get();
        
        double throughputPerSecond = total / durationSeconds;
        double successRate = total > 0 ? (double) success / total * 100.0 : 0.0;
        
        BenchmarkResponse response = new BenchmarkResponse(
            total,
            success,
            errors,
            durationSeconds,
            throughputPerSecond,
            successRate,
            request.getConcurrentThreads(),
            request.getRequestsPerThread()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple health check endpoint for the benchmark service.
     */
    @GetMapping("/health")
    @Operation(summary = "Benchmark service health check",
               description = "Check if the benchmark service is operational")
    @ApiResponse(responseCode = "200", 
                description = "Benchmark service is healthy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Benchmark service is healthy");
    }
    
    private void runWorkerThread(BenchmarkRequest request, int threadId, 
                                AtomicLong successCount, AtomicLong errorCount, 
                                AtomicLong totalRequests) {
        long requestsPerThread = request.getRequestsPerThread();
        String keyPrefix = request.getKeyPrefix() != null ? request.getKeyPrefix() : "benchmark";
        String key = keyPrefix + ":" + threadId;
        int tokensPerRequest = request.getTokensPerRequest();
        
        long startTime = System.currentTimeMillis();
        long durationMs = request.getDurationSeconds() * 1000L;
        
        for (long i = 0; i < requestsPerThread; i++) {
            // Check if we've exceeded the duration
            if (System.currentTimeMillis() - startTime > durationMs) {
                break;
            }
            
            try {
                boolean allowed = rateLimiterService.isAllowed(key, tokensPerRequest);
                totalRequests.incrementAndGet();
                
                if (allowed) {
                    successCount.incrementAndGet();
                } else {
                    // Rate limited, but not an error
                    // Don't increment error count for legitimate rate limiting
                }
                
                // Optional delay between requests
                if (request.getDelayBetweenRequestsMs() > 0) {
                    Thread.sleep(request.getDelayBetweenRequestsMs());
                }
                
            } catch (Exception e) {
                totalRequests.incrementAndGet();
                errorCount.incrementAndGet();
            }
        }
    }
}