export type AlgorithmType = "token-bucket" | "sliding-window" | "fixed-window" | "leaky-bucket";

export type TrafficPattern = "steady" | "bursty" | "spike" | "custom";

export interface AlgorithmConfig {
  capacity: number;
  refillRate: number;
  timeWindow: number;
}

export interface SimulationRequest {
  id: number;
  timestamp: number;
  allowed: boolean;
  algorithm: AlgorithmType;
  reason: string;
}

export interface AlgorithmState {
  tokens: number;
  requestsInWindow: number[];
  lastRefill: number;
}

export interface AlgorithmStats {
  totalRequests: number;
  rejectionRate: number;
  avgResponseTime: number;
  burstEfficiency: number;
}
