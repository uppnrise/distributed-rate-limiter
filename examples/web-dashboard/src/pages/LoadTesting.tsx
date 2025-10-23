import { useState, useEffect, useRef } from "react";
import { TestConfigPanel } from "@/components/loadtest/TestConfigPanel";
import { AdvancedSettings } from "@/components/loadtest/AdvancedSettings";
import { TestExecution } from "@/components/loadtest/TestExecution";
import { LiveResults } from "@/components/loadtest/LiveResults";
import { TestResultsSummary } from "@/components/loadtest/TestResultsSummary";
import { HistoricalTests } from "@/components/loadtest/HistoricalTests";
import { LoadTestSimulator } from "@/utils/loadTestSimulator";
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

  const simulatorRef = useRef<LoadTestSimulator | null>(null);
  const intervalRef = useRef<number | null>(null);

  const handleStart = () => {
    if (!config.targetKey) {
      toast.error("Please enter a target key");
      return;
    }

    simulatorRef.current = new LoadTestSimulator(config);
    simulatorRef.current.start();
    setIsRunning(true);
    setCurrentResult(null);
    setTimeSeriesData([]);
    
    toast.success("Load test started");

    // Update every 100ms
    intervalRef.current = window.setInterval(() => {
      if (!simulatorRef.current) return;

      const { metrics: newMetrics, timeSeriesData: newData } =
        simulatorRef.current.update();
      setMetrics(newMetrics);
      setTimeSeriesData(newData);
      setProgress(simulatorRef.current.getProgress());

      if (simulatorRef.current.isComplete()) {
        handleComplete();
      }
    }, 100);
  };

  const handleStop = () => {
    if (simulatorRef.current) {
      simulatorRef.current.stop();
    }
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    handleComplete();
    toast.info("Load test stopped");
  };

  const handleComplete = () => {
    setIsRunning(false);
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }

    const result: LoadTestResult = {
      id: Math.random().toString(36).substring(7),
      config,
      metrics,
      startedAt: new Date(Date.now() - config.duration * 1000).toISOString(),
      completedAt: new Date().toISOString(),
      duration: config.duration,
      timeSeriesData,
    };

    setCurrentResult(result);
    setHistoricalResults((prev) => [result, ...prev].slice(0, 10));
    toast.success("Load test completed");
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
