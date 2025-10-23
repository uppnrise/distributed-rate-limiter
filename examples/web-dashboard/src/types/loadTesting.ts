export type RequestPattern = "constant" | "ramp-up" | "spike" | "step-load";

export interface LoadTestConfig {
  targetKey: string;
  requestRate: number;
  duration: number;
  concurrency: number;
  pattern: RequestPattern;
  tokensPerRequest: number;
  algorithmOverride?: string;
  customHeaders?: Record<string, string>;
  timeout: number;
}

export interface LoadTestMetrics {
  requestsSent: number;
  successful: number;
  failed: number;
  rateLimited: number;
  avgResponseTime: number;
  p50ResponseTime: number;
  p95ResponseTime: number;
  p99ResponseTime: number;
  successRate: number;
  currentRate: number;
}

export interface LoadTestResult {
  id: string;
  config: LoadTestConfig;
  metrics: LoadTestMetrics;
  startedAt: string;
  completedAt: string;
  duration: number;
  timeSeriesData: TimeSeriesPoint[];
}

export interface TimeSeriesPoint {
  timestamp: number;
  requestsPerSecond: number;
  successRate: number;
  avgResponseTime: number;
  p95ResponseTime: number;
}
