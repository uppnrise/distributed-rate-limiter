export type KeyStatus = "active" | "inactive" | "expired";

export interface ApiKey {
  id: string;
  name: string;
  key: string;
  description?: string;
  status: KeyStatus;
  createdAt: string;
  lastUsed?: string;
  expiresAt?: string;
  rateLimit: {
    capacity: number;
    refillRate: number;
    algorithm: string;
  };
  usageStats: {
    totalRequests: number;
    successfulRequests: number;
    rateLimitedRequests: number;
  };
  ipWhitelist?: string[];
  ipBlacklist?: string[];
}

export interface ApiKeyCreateInput {
  name: string;
  description?: string;
  expiresAt?: Date;
  useDefaultLimits: boolean;
  customLimits?: {
    capacity: number;
    refillRate: number;
    algorithm: string;
  };
  ipWhitelist?: string[];
}

export interface KeyAccessLog {
  id: string;
  timestamp: string;
  ipAddress: string;
  endpoint: string;
  statusCode: number;
  responseTime: number;
}
