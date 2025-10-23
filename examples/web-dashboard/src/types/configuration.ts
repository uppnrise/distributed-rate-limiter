export type ConfigAlgorithm = "token-bucket" | "sliding-window" | "fixed-window" | "leaky-bucket";

export interface GlobalConfig {
  defaultCapacity: number;
  defaultRefillRate: number;
  cleanupInterval: number;
  algorithm: ConfigAlgorithm;
}

export interface KeyConfig {
  id: string;
  keyName: string;
  capacity: number;
  refillRate: number;
  algorithm: ConfigAlgorithm;
  createdAt: string;
  updatedAt: string;
}

export interface PatternConfig {
  id: string;
  pattern: string;
  capacity: number;
  refillRate: number;
  algorithm: ConfigAlgorithm;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConfigStats {
  totalKeyConfigs: number;
  totalPatternConfigs: number;
  mostUsedPattern: string;
  cacheHitRate: number;
  avgLookupTime: number;
}
