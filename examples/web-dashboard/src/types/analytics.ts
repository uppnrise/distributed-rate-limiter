export type TimeRange = "1h" | "24h" | "7d" | "30d" | "custom";

export interface PerformanceMetrics {
  totalRequests: number;
  requestsChange: number;
  successRate: number;
  successRateChange: number;
  peakRPS: number;
  peakRPSChange: number;
}

export interface TopKey {
  key: string;
  totalRequests: number;
  rejectionRate: number;
  algorithm: string;
  lastActive: string;
}

export interface AlgorithmPerformance {
  algorithm: string;
  requestsHandled: number;
  successRate: number;
  avgResponseTime: number;
  memoryUsage: number;
  efficiency: number;
}

export interface UsageDataPoint {
  timestamp: string;
  requests: number;
  successful: number;
  rejected: number;
}

export interface AlgorithmUsage {
  algorithm: string;
  value: number;
  color: string;
}

export interface Alert {
  id: string;
  type: "spike" | "degradation" | "config" | "anomaly";
  message: string;
  timestamp: string;
  severity: "low" | "medium" | "high";
}
