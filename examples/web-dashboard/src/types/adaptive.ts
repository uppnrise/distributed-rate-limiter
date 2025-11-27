// Types for Adaptive Rate Limiting API responses

export interface CurrentLimits {
  capacity: number;
  refillRate: number;
}

export interface RecommendedLimits {
  capacity: number;
  refillRate: number;
}

export interface AdaptiveStatusInfo {
  mode: 'STATIC' | 'ADAPTIVE' | 'LEARNING' | 'OVERRIDE';
  confidence: number;
  recommendedLimits: RecommendedLimits;
  reasoning: Record<string, string>;
}

export interface AdaptiveStatus {
  key: string;
  currentLimits: CurrentLimits;
  adaptiveStatus: AdaptiveStatusInfo;
  timestamp: string;
}

export interface AdaptiveConfig {
  enabled: boolean;
  evaluationIntervalMs: number;
  minConfidenceThreshold: number;
  maxAdjustmentFactor: number;
  minCapacity: number;
  maxCapacity: number;
}

export interface AdaptiveOverrideRequest {
  capacity: number;
  refillRate: number;
  reason: string;
}

export interface AdaptiveInfo {
  originalLimits: CurrentLimits;
  currentLimits: CurrentLimits;
  adaptationReason: string;
  adjustmentTimestamp: string;
  nextEvaluationIn: string;
}

// Enhanced rate limit response with adaptive info
export interface EnhancedRateLimitCheckResponse {
  key: string;
  tokensRequested: number;
  allowed: boolean;
  adaptiveInfo?: AdaptiveInfo;
}

// Adaptive key summary for dashboard
export interface AdaptiveKeySummary {
  key: string;
  mode: string;
  confidence: number;
  currentCapacity: number;
  originalCapacity: number;
  adaptationReason: string;
  lastUpdate: string;
}
