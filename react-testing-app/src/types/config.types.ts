export interface AppConfig {
  rateLimiterUrl: string;
  apiKey?: string;
  wsUrl?: string;
}

export interface TestScenario {
  id: string;
  name: string;
  description: string;
  config: {
    key: string;
    tokens: number;
    requestCount: number;
    intervalMs: number;
    concurrent?: boolean;
  };
}

export interface TestResult {
  scenario: string;
  startTime: string;
  endTime: string;
  totalRequests: number;
  allowedRequests: number;
  deniedRequests: number;
  averageResponseTime: number;
  successRate: number;
}