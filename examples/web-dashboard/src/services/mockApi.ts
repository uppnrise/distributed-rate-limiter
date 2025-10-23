// Comprehensive Mock API Service for Rate Limiter Dashboard

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// Simulate realistic network delays
const randomDelay = () => delay(Math.random() * 150 + 50);

// Mock error simulation (5% failure rate)
const shouldSimulateError = () => Math.random() < 0.05;

export class MockApiService {
  // Dashboard API
  static async getDashboardMetrics() {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to fetch dashboard metrics');
    }
    
    return {
      activeKeys: Math.floor(Math.random() * 50) + 100,
      requestsPerSecond: Math.floor(Math.random() * 500) + 1000,
      successRate: Math.floor(Math.random() * 5) + 95,
      totalRequests: Math.floor(Math.random() * 1000000) + 5000000,
    };
  }

  static async getRecentActivity() {
    await randomDelay();
    const activities = [];
    const algorithms = ["Token Bucket", "Sliding Window", "Fixed Window", "Leaky Bucket"];
    const statuses: ("allowed" | "rejected")[] = ["allowed", "allowed", "allowed", "rejected"];
    
    for (let i = 0; i < 10; i++) {
      activities.push({
        id: Math.random().toString(36).substring(7),
        timestamp: new Date(Date.now() - i * 60000).toLocaleTimeString(),
        key: `rl_${Math.random().toString(36).substring(2, 10)}`,
        algorithm: algorithms[Math.floor(Math.random() * algorithms.length)],
        status: statuses[Math.floor(Math.random() * statuses.length)],
        tokensUsed: Math.floor(Math.random() * 50) + 1,
      });
    }
    
    return activities;
  }

  // Configuration API
  static async getConfiguration() {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to fetch configuration');
    }
    
    return {
      global: {
        capacity: 100,
        refillRate: 10,
        algorithm: "token-bucket",
      },
      keys: [],
      patterns: [],
    };
  }

  static async updateConfiguration(config: any) {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to update configuration');
    }
    return { success: true, config };
  }

  // Analytics API
  static async getAnalytics(timeRange: string) {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to fetch analytics data');
    }
    
    const dataPoints = timeRange === '1h' ? 12 : timeRange === '24h' ? 24 : 30;
    const usageData = [];
    
    for (let i = 0; i < dataPoints; i++) {
      usageData.push({
        timestamp: new Date(Date.now() - (dataPoints - i) * 3600000).toISOString(),
        requests: Math.floor(Math.random() * 5000) + 1000,
        successful: Math.floor(Math.random() * 4500) + 900,
        rejected: Math.floor(Math.random() * 500) + 50,
      });
    }
    
    return {
      overview: {
        totalRequests: Math.floor(Math.random() * 1000000) + 5000000,
        successRate: 95 + Math.random() * 4,
        peakRps: Math.floor(Math.random() * 2000) + 1000,
        avgResponseTime: Math.floor(Math.random() * 50) + 50,
      },
      usageData,
      algorithmDistribution: [
        { name: "Token Bucket", value: Math.floor(Math.random() * 30) + 30 },
        { name: "Sliding Window", value: Math.floor(Math.random() * 25) + 25 },
        { name: "Fixed Window", value: Math.floor(Math.random() * 20) + 20 },
        { name: "Leaky Bucket", value: Math.floor(Math.random() * 15) + 15 },
      ],
    };
  }

  // API Keys API
  static async getApiKeys() {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to fetch API keys');
    }
    
    // Return mock keys
    return [];
  }

  static async createApiKey(keyData: any) {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to create API key');
    }
    
    return {
      id: Math.random().toString(36).substring(7),
      ...keyData,
      key: `rl_${Array.from({ length: 32 }, () => Math.random().toString(36)[2]).join("")}`,
      createdAt: new Date().toISOString(),
      status: 'active',
    };
  }

  static async deleteApiKey(keyId: string) {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to delete API key');
    }
    return { success: true };
  }

  // Load Testing API
  static async runLoadTest(config: any) {
    await randomDelay();
    if (shouldSimulateError()) {
      throw new Error('Failed to start load test');
    }
    
    return {
      testId: Math.random().toString(36).substring(7),
      status: 'running',
      startTime: new Date().toISOString(),
    };
  }

  static async getLoadTestResults(testId: string) {
    await randomDelay();
    return {
      testId,
      status: 'completed',
      results: {
        totalRequests: Math.floor(Math.random() * 10000) + 5000,
        successfulRequests: Math.floor(Math.random() * 9000) + 4500,
        failedRequests: Math.floor(Math.random() * 500) + 100,
        avgResponseTime: Math.floor(Math.random() * 100) + 50,
      },
    };
  }
}

// WebSocket simulation for real-time updates
export class MockWebSocketService {
  private listeners: Set<(data: any) => void> = new Set();
  private intervalId: NodeJS.Timeout | null = null;

  connect() {
    console.log('MockWebSocket: Connected');
    
    // Simulate real-time updates every 3 seconds
    this.intervalId = setInterval(() => {
      const update = {
        type: 'metrics_update',
        data: {
          requestsPerSecond: Math.floor(Math.random() * 500) + 1000,
          successRate: Math.floor(Math.random() * 5) + 95,
          timestamp: new Date().toISOString(),
        },
      };
      
      this.listeners.forEach(listener => listener(update));
    }, 3000);
  }

  disconnect() {
    console.log('MockWebSocket: Disconnected');
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  subscribe(listener: (data: any) => void) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}
