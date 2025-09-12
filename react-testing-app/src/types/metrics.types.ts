export interface SystemMetrics {
  keyMetrics: Record<string, KeyMetric>;
  redisConnected: boolean;
  totalAllowedRequests: number;
  totalDeniedRequests: number;
}

export interface KeyMetric {
  requestCount: number;
  allowedCount: number;
  deniedCount: number;
  lastAccess: string;
}

export interface PerformanceMetrics {
  averageResponseTime: number;
  throughputPerSecond: number;
  successRate: number;
  errorRate: number;
  p95ResponseTime?: number;
  p99ResponseTime?: number;
}

export interface MetricsHistory {
  timestamp: string;
  metrics: PerformanceMetrics;
}