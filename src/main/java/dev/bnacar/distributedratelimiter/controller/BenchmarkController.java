package dev.bnacar.distributedratelimiter.controller;

import dev.bnacar.distributedratelimiter.models.BenchmarkRequest;
import dev.bnacar.distributedratelimiter.models.BenchmarkResponse;
import dev.bnacar.distributedratelimiter.ratelimit.RateLimiterService;
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
    public ResponseEntity<BenchmarkResponse> runBenchmark(@Valid @RequestBody BenchmarkRequest request) {
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