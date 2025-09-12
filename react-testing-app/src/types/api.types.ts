export interface RateLimitRequest {
  key: string;
  tokens: number;
  apiKey?: string;
}

export interface RateLimitResponse {
  key: string;
  tokensRequested: number;
  allowed: boolean;
  retryAfter?: number;
  remainingCapacity?: number;
}

export interface RateLimitConfig {
  capacity: number;
  refillRate: number;
  cleanupIntervalMs?: number;
}

export interface ConfigurationResponse {
  capacity: number;
  refillRate: number;
  cleanupIntervalMs: number;
  keyConfigs: Record<string, RateLimitConfig>;
  patternConfigs: Record<string, RateLimitConfig>;
}