import { useState, useEffect, useRef } from "react";
import { TestConfigPanel } from "@/components/loadtest/TestConfigPanel";
import { AdvancedSettings } from "@/components/loadtest/AdvancedSettings";
import { TestExecution } from "@/components/loadtest/TestExecution";
import { LiveResults } from "@/components/loadtest/LiveResults";
import { TestResultsSummary } from "@/components/loadtest/TestResultsSummary";
import { HistoricalTests } from "@/components/loadtest/HistoricalTests";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import { toast } from "sonner";
import {
  LoadTestConfig,
  LoadTestMetrics,
  LoadTestResult,
  TimeSeriesPoint,
} from "@/types/loadTesting";

const LoadTesting = () => {
  const [config, setConfig] = useState<LoadTestConfig>({
    targetKey: "rl_prod_user123",
    requestRate: 100,
    duration: 30,
    concurrency: 10,
    pattern: "constant",
    tokensPerRequest: 1,
    timeout: 5000,
  });

  const [isRunning, setIsRunning] = useState(false);
  const [progress, setProgress] = useState(0);
  const [metrics, setMetrics] = useState<LoadTestMetrics>({
    requestsSent: 0,
    successful: 0,
    failed: 0,
    rateLimited: 0,
    avgResponseTime: 0,
    p50ResponseTime: 0,
    p95ResponseTime: 0,
    p99ResponseTime: 0,
    successRate: 100,
    currentRate: 0,
  });
  const [timeSeriesData, setTimeSeriesData] = useState<TimeSeriesPoint[]>([]);
  const [currentResult, setCurrentResult] = useState<LoadTestResult | null>(null);
  const [historicalResults, setHistoricalResults] = useState<LoadTestResult[]>([]);

  const abortControllerRef = useRef<AbortController | null>(null);
  const intervalRef = useRef<number | null>(null);
  const startTimeRef = useRef<number>(0);

  const handleStart = async () => {
    if (!config.targetKey) {
      toast.error("Please enter a target key");
      return;
    }

    setIsRunning(true);
    setCurrentResult(null);
    setTimeSeriesData([]);
    setProgress(0);
    startTimeRef.current = Date.now();
    
    // Reset metrics
    setMetrics({
      requestsSent: 0,
      successful: 0,
      failed: 0,
      rateLimited: 0,
      avgResponseTime: 0,
      p50ResponseTime: 0,
      p95ResponseTime: 0,
      p99ResponseTime: 0,
      successRate: 100,
      currentRate: 0,
    });
    
    toast.success("Starting load test on backend...");

    // Create abort controller for cancellation
    abortControllerRef.current = new AbortController();

    // Start progress simulation
    intervalRef.current = window.setInterval(() => {
      const elapsed = (Date.now() - startTimeRef.current) / 1000;
      const progressPercent = Math.min((elapsed / config.duration) * 100, 100);
      setProgress(progressPercent);
      
      if (progressPercent >= 100) {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
        }
      }
    }, 100);

    try {
      // Call real backend API
      const response = await rateLimiterApi.runLoadTest({
        concurrentThreads: config.concurrency,
        requestsPerThread: Math.ceil(config.requestRate * config.duration / config.concurrency),
        durationSeconds: config.duration,
        tokensPerRequest: config.tokensPerRequest,
        delayBetweenRequestsMs: config.pattern === 'spike' ? 0 : Math.floor(1000 / config.requestRate),
        keyPrefix: config.targetKey,
      });

      // Convert backend response to frontend metrics
      const backendMetrics: LoadTestMetrics = {
        requestsSent: response.totalRequests,
        successful: response.successfulRequests,
        failed: response.errorRequests,
        rateLimited: response.totalRequests - response.successfulRequests - response.errorRequests,
        avgResponseTime: 0, // Backend doesn't track response time yet
        p50ResponseTime: 0,
        p95ResponseTime: 0,
        p99ResponseTime: 0,
        successRate: response.successRate,
        currentRate: response.throughputPerSecond,
      };

      setMetrics(backendMetrics);
      
      // Create time series data point
      const timePoint: TimeSeriesPoint = {
        timestamp: Date.now(),
        requestsPerSecond: response.throughputPerSecond,
        successRate: response.successRate,
        avgResponseTime: 0, // Backend doesn't track response time yet
        p95ResponseTime: 0,
      };
      
      setTimeSeriesData([timePoint]);
      setProgress(100);
      
      handleComplete(backendMetrics, [timePoint]);
      toast.success(`Load test completed: ${response.successfulRequests}/${response.totalRequests} requests succeeded`);
      
    } catch (error) {
      console.error('Load test failed:', error);
      toast.error(`Load test failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
      setIsRunning(false);
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }
  };

  const handleStop = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    setIsRunning(false);
    toast.info("Load test stopped (note: backend test may still be running)");
  };

  const handleComplete = (finalMetrics: LoadTestMetrics, finalTimeSeriesData: TimeSeriesPoint[]) => {
    setIsRunning(false);
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }

    const result: LoadTestResult = {
      id: Math.random().toString(36).substring(7),
      config,
      metrics: finalMetrics,
      startedAt: new Date(startTimeRef.current).toISOString(),
      completedAt: new Date().toISOString(),
      duration: config.duration,
      timeSeriesData: finalTimeSeriesData,
    };

    setCurrentResult(result);
    setHistoricalResults((prev) => [result, ...prev].slice(0, 10));
  };

  const handleSaveConfig = () => {
    localStorage.setItem(
      `loadtest-config-${Date.now()}`,
      JSON.stringify(config)
    );
    toast.success("Configuration saved");
  };

  const handleDeleteHistorical = (id: string) => {
    setHistoricalResults((prev) => prev.filter((r) => r.id !== id));
    toast.success("Test result deleted");
  };

  const handleFavorite = (id: string) => {
    toast.info("Favorite feature coming soon");
  };

  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="text-3xl font-bold tracking-tight text-foreground">
          Load Testing
        </h2>
        <p className="text-muted-foreground">
          Simulate traffic patterns and test rate limiter performance
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <TestConfigPanel config={config} onChange={setConfig} />
        <AdvancedSettings config={config} onChange={setConfig} />
      </div>

      <TestExecution
        isRunning={isRunning}
        progress={progress}
        metrics={metrics}
        onStart={handleStart}
        onStop={handleStop}
      />

      {(isRunning || timeSeriesData.length > 0) && (
        <LiveResults data={timeSeriesData} />
      )}

      {currentResult && (
        <TestResultsSummary
          result={currentResult}
          onSaveConfig={handleSaveConfig}
        />
      )}

      <HistoricalTests
        results={historicalResults}
        onDelete={handleDeleteHistorical}
        onFavorite={handleFavorite}
      />
    </div>
  );
};

export default LoadTesting;
