import axios from 'axios';
import type { AxiosInstance, AxiosResponse } from 'axios';
import type { 
  RateLimitRequest, 
  RateLimitResponse, 
  RateLimitConfig, 
  ConfigurationResponse 
} from '../types/api.types';
import type { SystemMetrics } from '../types/metrics.types';

class ApiClient {
  private api: AxiosInstance;

  constructor(baseURL: string = 'http://localhost:8080', apiKey?: string) {
    this.api = axios.create({
      baseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        ...(apiKey && { 'X-API-Key': apiKey }),
      },
    });

    // Add response interceptor for better error handling
    this.api.interceptors.response.use(
      (response: AxiosResponse) => response,
      (error) => {
        console.error('API Error:', error.response?.data || error.message);
        return Promise.reject(error);
      }
    );
  }

  // Rate limiting operations
  async checkRateLimit(request: RateLimitRequest): Promise<RateLimitResponse> {
    const response = await this.api.post<RateLimitResponse>('/api/ratelimit/check', request);
    return response.data;
  }

  // Configuration management
  async getConfiguration(): Promise<ConfigurationResponse> {
    const response = await this.api.get<ConfigurationResponse>('/api/ratelimit/config');
    return response.data;
  }

  async setDefaultConfig(config: RateLimitConfig): Promise<void> {
    await this.api.post('/api/ratelimit/config/default', config);
  }

  async setKeyConfig(key: string, config: RateLimitConfig): Promise<void> {
    await this.api.post(`/api/ratelimit/config/keys/${encodeURIComponent(key)}`, config);
  }

  async reloadConfiguration(): Promise<void> {
    await this.api.post('/api/ratelimit/config/reload');
  }

  // Metrics and monitoring
  async getMetrics(): Promise<SystemMetrics> {
    const response = await this.api.get<SystemMetrics>('/metrics');
    return response.data;
  }

  async getHealth(): Promise<{ status: string }> {
    const response = await this.api.get('/actuator/health');
    return response.data;
  }

  // Administrative operations
  async clearAllBuckets(): Promise<void> {
    await this.api.delete('/api/admin/buckets');
  }

  async resetMetrics(): Promise<void> {
    await this.api.delete('/api/admin/metrics');
  }
}

// Export a default instance
const apiClient = new ApiClient(
  import.meta.env.VITE_RATE_LIMITER_URL || 'http://localhost:8080',
  import.meta.env.VITE_API_KEY
);

export { ApiClient };
export default apiClient;