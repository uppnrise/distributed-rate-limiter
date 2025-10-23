import {
  LoadTestConfig,
  LoadTestMetrics,
  TimeSeriesPoint,
  RequestPattern,
} from "@/types/loadTesting";

export class LoadTestSimulator {
  private config: LoadTestConfig;
  private startTime: number = 0;
  private elapsedTime: number = 0;
  private isRunning: boolean = false;
  private metrics: LoadTestMetrics = {
    requestsSent: 0,
    successful: 0,
    failed: 0,
    rateLimited: 0,
    avgResponseTime: 0,
    p50ResponseTime: 0,
    p95ResponseTime: 0,
    p99ResponseTime: 0,
    successRate: 100,
    currentRate: 0,
  };
  private timeSeriesData: TimeSeriesPoint[] = [];
  private responseTimes: number[] = [];

  constructor(config: LoadTestConfig) {
    this.config = config;
  }

  start() {
    this.isRunning = true;
    this.startTime = Date.now();
    this.resetMetrics();
  }

  stop() {
    this.isRunning = false;
  }

  private resetMetrics() {
    this.metrics = {
      requestsSent: 0,
      successful: 0,
      failed: 0,
      rateLimited: 0,
      avgResponseTime: 0,
      p50ResponseTime: 0,
      p95ResponseTime: 0,
      p99ResponseTime: 0,
      successRate: 100,
      currentRate: 0,
    };
    this.timeSeriesData = [];
    this.responseTimes = [];
    this.elapsedTime = 0;
  }

  update(): { metrics: LoadTestMetrics; timeSeriesData: TimeSeriesPoint[] } {
    if (!this.isRunning) {
      return { metrics: this.metrics, timeSeriesData: this.timeSeriesData };
    }

    this.elapsedTime = (Date.now() - this.startTime) / 1000;
    const currentRate = this.calculateCurrentRate();

    // Simulate requests based on current rate
    const requestsThisTick = Math.floor(currentRate / 10); // Update every 100ms
    
    for (let i = 0; i < requestsThisTick; i++) {
      this.simulateRequest();
    }

    this.metrics.currentRate = currentRate;
    this.updatePercentiles();

    // Add time series data point
    if (this.metrics.requestsSent % 10 === 0) {
      this.timeSeriesData.push({
        timestamp: Date.now() - this.startTime,
        requestsPerSecond: currentRate,
        successRate: this.metrics.successRate,
        avgResponseTime: this.metrics.avgResponseTime,
        p95ResponseTime: this.metrics.p95ResponseTime,
      });

      // Keep last 60 points
      if (this.timeSeriesData.length > 60) {
        this.timeSeriesData.shift();
      }
    }

    return { metrics: this.metrics, timeSeriesData: this.timeSeriesData };
  }

  private calculateCurrentRate(): number {
    const progress = this.elapsedTime / this.config.duration;

    switch (this.config.pattern) {
      case "constant":
        return this.config.requestRate;

      case "ramp-up":
        return this.config.requestRate * Math.min(progress * 2, 1);

      case "spike":
        // Spike at 50% duration
        if (progress > 0.45 && progress < 0.55) {
          return this.config.requestRate * 3;
        }
        return this.config.requestRate;

      case "step-load":
        // Step increases every 25%
        if (progress < 0.25) return this.config.requestRate * 0.25;
        if (progress < 0.5) return this.config.requestRate * 0.5;
        if (progress < 0.75) return this.config.requestRate * 0.75;
        return this.config.requestRate;

      default:
        return this.config.requestRate;
    }
  }

  private simulateRequest() {
    this.metrics.requestsSent++;

    // Simulate rate limiting (increase rejection as load increases)
    const rejectionProbability = Math.min(
      (this.metrics.currentRate / this.config.requestRate) * 0.3,
      0.5
    );

    const isRateLimited = Math.random() < rejectionProbability;
    const isFailed = Math.random() < 0.02; // 2% failure rate

    if (isFailed) {
      this.metrics.failed++;
    } else if (isRateLimited) {
      this.metrics.rateLimited++;
    } else {
      this.metrics.successful++;
    }

    // Simulate response time (increases with load)
    const baseResponseTime = 50;
    const loadFactor = this.metrics.currentRate / 100;
    const responseTime =
      baseResponseTime + Math.random() * 100 * loadFactor + Math.random() * 50;
    
    this.responseTimes.push(responseTime);

    // Calculate averages
    this.metrics.avgResponseTime =
      this.responseTimes.reduce((a, b) => a + b, 0) / this.responseTimes.length;

    this.metrics.successRate =
      (this.metrics.successful / this.metrics.requestsSent) * 100;
  }

  private updatePercentiles() {
    if (this.responseTimes.length === 0) return;

    const sorted = [...this.responseTimes].sort((a, b) => a - b);
    const p50Index = Math.floor(sorted.length * 0.5);
    const p95Index = Math.floor(sorted.length * 0.95);
    const p99Index = Math.floor(sorted.length * 0.99);

    this.metrics.p50ResponseTime = sorted[p50Index] || 0;
    this.metrics.p95ResponseTime = sorted[p95Index] || 0;
    this.metrics.p99ResponseTime = sorted[p99Index] || 0;
  }

  getProgress(): number {
    return Math.min((this.elapsedTime / this.config.duration) * 100, 100);
  }

  isComplete(): boolean {
    return this.elapsedTime >= this.config.duration;
  }

  getMetrics(): LoadTestMetrics {
    return this.metrics;
  }

  getTimeSeriesData(): TimeSeriesPoint[] {
    return this.timeSeriesData;
  }
}
