// Real API Service for Distributed Rate Limiter Backend

const API_BASE_URL = 'http://localhost:8080';
const ADMIN_CREDENTIALS = btoa('admin:changeme');

interface RateLimitCheckRequest {
  key: string;
  tokens: number;
  apiKey?: string;
}

interface RateLimitCheckResponse {
  key: string;
  tokensRequested: number;
  allowed: boolean;
}

interface RateLimiterConfig {
  capacity: number;
  refillRate: number;
  cleanupIntervalMs: number;
  algorithm: string;
  keys: Record<string, { capacity: number; refillRate: number; algorithm?: string }>;
  patterns: Record<string, { capacity: number; refillRate: number }>;
}

interface KeyConfig {
  capacity: number;
  refillRate: number;
  algorithm?: string;
}

interface PatternConfig {
  capacity: number;
  refillRate: number;
}

interface ActiveKey {
  key: string;
  capacity: number;
  algorithm: string;
  lastAccessTime: number;
  active: boolean;
}

interface AdminKeysResponse {
  keys: ActiveKey[];
  totalKeys: number;
}

interface LoadTestRequest {
  requests: number;
  concurrency: number;
  key: string;
  durationSeconds: number;
}

interface LoadTestResponse {
  totalRequests: number;
  successCount: number;
  errorCount: number;
  throughputPerSecond: number;
  successRate: number;
  durationSeconds: number;
}

interface KeyMetric {
  requestCount: number;
  allowedCount: number;
  deniedCount: number;
  lastAccess: string;
}

interface SystemMetrics {
  keyMetrics: Record<string, KeyMetric>;
  totalAllowedRequests: number;
  totalDeniedRequests: number;
  redisConnected: boolean;
}

interface HealthResponse {
  status: string;
  components: {
    redis: { status: string };
    rateLimiter: { status: string };
  };
}

interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

class RateLimiterApiService {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${API_BASE_URL}${endpoint}`;
    
    try {
      const response = await fetch(url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
      });

      if (!response.ok) {
        if (response.status === 429) {
          const error: ErrorResponse = await response.json();
          throw new Error(error.message || 'Rate limit exceeded');
        }
        
        if (response.headers.get('content-type')?.includes('application/json')) {
          const error: ErrorResponse = await response.json();
          throw new Error(error.message || `HTTP ${response.status}`);
        }
        
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      return response.json();
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('Network error occurred');
    }
  }

  private async adminRequest<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    return this.request<T>(endpoint, {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Basic ${ADMIN_CREDENTIALS}`,
      },
    });
  }

  // ============ RATE LIMITING ============
  
  async checkRateLimit(request: RateLimitCheckRequest): Promise<RateLimitCheckResponse> {
    return this.request<RateLimitCheckResponse>('/api/ratelimit/check', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ============ CONFIGURATION ============
  
  async getConfig(): Promise<RateLimiterConfig> {
    return this.request<RateLimiterConfig>('/api/ratelimit/config');
  }

  async updateKeyConfig(key: string, config: KeyConfig): Promise<void> {
    await this.request(`/api/ratelimit/config/keys/${encodeURIComponent(key)}`, {
      method: 'POST',
      body: JSON.stringify(config),
    });
  }

  async updatePatternConfig(pattern: string, config: PatternConfig): Promise<void> {
    await this.request(`/api/ratelimit/config/patterns/${encodeURIComponent(pattern)}`, {
      method: 'POST',
      body: JSON.stringify(config),
    });
  }

  async deleteKeyConfig(key: string): Promise<void> {
    await this.request(`/api/ratelimit/config/keys/${encodeURIComponent(key)}`, {
      method: 'DELETE',
    });
  }

  async deletePatternConfig(pattern: string): Promise<void> {
    await this.request(`/api/ratelimit/config/patterns/${encodeURIComponent(pattern)}`, {
      method: 'DELETE',
    });
  }

  // ============ ADMIN - ACTIVE KEYS ============
  
  async getActiveKeys(): Promise<AdminKeysResponse> {
    return this.adminRequest<AdminKeysResponse>('/admin/keys');
  }

  // ============ LOAD TESTING ============
  
  async runLoadTest(request: LoadTestRequest): Promise<LoadTestResponse> {
    return this.request<LoadTestResponse>('/api/benchmark/load-test', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  // ============ METRICS ============
  
  async getMetrics(): Promise<SystemMetrics> {
    return this.request<SystemMetrics>('/metrics');
  }

  // ============ HEALTH CHECK ============
  
  async healthCheck(): Promise<HealthResponse> {
    return this.request<HealthResponse>('/actuator/health');
  }
}

export const rateLimiterApi = new RateLimiterApiService();

// Export types for use in components
export type {
  RateLimitCheckRequest,
  RateLimitCheckResponse,
  RateLimiterConfig,
  KeyConfig,
  PatternConfig,
  ActiveKey,
  AdminKeysResponse,
  LoadTestRequest,
  LoadTestResponse,
  KeyMetric,
  SystemMetrics,
  HealthResponse,
  ErrorResponse,
};
