import {
  AlgorithmType,
  AlgorithmConfig,
  AlgorithmState,
  TrafficPattern,
} from "@/types/algorithms";

export class AlgorithmSimulator {
  private states: Map<AlgorithmType, AlgorithmState> = new Map();
  private config: AlgorithmConfig;

  constructor(config: AlgorithmConfig) {
    this.config = config;
  }

  initialize(algorithms: AlgorithmType[]) {
    this.states.clear();
    algorithms.forEach((algo) => {
      this.states.set(algo, {
        tokens: this.config.capacity,
        requestsInWindow: [],
        lastRefill: Date.now(),
      });
    });
  }

  updateConfig(config: AlgorithmConfig) {
    this.config = config;
    // Reset states with new config
    this.states.forEach((state) => {
      state.tokens = config.capacity;
      state.requestsInWindow = [];
    });
  }

  processRequest(algorithm: AlgorithmType, currentTime: number): { allowed: boolean; reason: string; tokens: number } {
    const state = this.states.get(algorithm);
    if (!state) {
      return { allowed: false, reason: "Algorithm not initialized", tokens: 0 };
    }

    switch (algorithm) {
      case "token-bucket":
        return this.processTokenBucket(state, currentTime);
      case "sliding-window":
        return this.processSlidingWindow(state, currentTime);
      case "fixed-window":
        return this.processFixedWindow(state, currentTime);
      case "leaky-bucket":
        return this.processLeakyBucket(state, currentTime);
      default:
        return { allowed: false, reason: "Unknown algorithm", tokens: 0 };
    }
  }

  private processTokenBucket(state: AlgorithmState, currentTime: number): { allowed: boolean; reason: string; tokens: number } {
    // Refill tokens based on time elapsed
    const timePassed = (currentTime - state.lastRefill) / 1000;
    const tokensToAdd = Math.floor(timePassed * this.config.refillRate);
    
    if (tokensToAdd > 0) {
      state.tokens = Math.min(this.config.capacity, state.tokens + tokensToAdd);
      state.lastRefill = currentTime;
    }

    if (state.tokens >= 1) {
      state.tokens -= 1;
      return {
        allowed: true,
        reason: `Request allowed. ${state.tokens} tokens remaining in bucket (capacity: ${this.config.capacity})`,
        tokens: state.tokens,
      };
    }

    return {
      allowed: false,
      reason: `Request rejected. No tokens available. Bucket refills at ${this.config.refillRate} tokens/sec`,
      tokens: state.tokens,
    };
  }

  private processSlidingWindow(state: AlgorithmState, currentTime: number): { allowed: boolean; reason: string; tokens: number } {
    const windowMs = this.config.timeWindow * 1000;
    const cutoffTime = currentTime - windowMs;
    
    // Remove old requests outside the window
    state.requestsInWindow = state.requestsInWindow.filter((time) => time > cutoffTime);

    if (state.requestsInWindow.length < this.config.capacity) {
      state.requestsInWindow.push(currentTime);
      return {
        allowed: true,
        reason: `Request allowed. ${state.requestsInWindow.length}/${this.config.capacity} requests in ${this.config.timeWindow}s window`,
        tokens: this.config.capacity - state.requestsInWindow.length,
      };
    }

    return {
      allowed: false,
      reason: `Request rejected. Limit of ${this.config.capacity} requests in ${this.config.timeWindow}s window reached`,
      tokens: 0,
    };
  }

  private processFixedWindow(state: AlgorithmState, currentTime: number): { allowed: boolean; reason: string; tokens: number } {
    const windowMs = this.config.timeWindow * 1000;
    const currentWindow = Math.floor(currentTime / windowMs);
    const lastWindow = state.requestsInWindow.length > 0 
      ? Math.floor(state.requestsInWindow[0] / windowMs)
      : currentWindow;

    // Reset counter if we're in a new window
    if (currentWindow > lastWindow) {
      state.requestsInWindow = [];
    }

    if (state.requestsInWindow.length < this.config.capacity) {
      state.requestsInWindow.push(currentTime);
      return {
        allowed: true,
        reason: `Request allowed. ${state.requestsInWindow.length}/${this.config.capacity} requests in current ${this.config.timeWindow}s window`,
        tokens: this.config.capacity - state.requestsInWindow.length,
      };
    }

    const timeUntilReset = this.config.timeWindow - ((currentTime % windowMs) / 1000);
    return {
      allowed: false,
      reason: `Request rejected. ${this.config.capacity} requests limit reached in current window. Resets in ${timeUntilReset.toFixed(1)}s`,
      tokens: 0,
    };
  }

  private processLeakyBucket(state: AlgorithmState, currentTime: number): { allowed: boolean; reason: string; tokens: number } {
    // Leak tokens based on time elapsed
    const timePassed = (currentTime - state.lastRefill) / 1000;
    const tokensToLeak = Math.floor(timePassed * this.config.refillRate);
    
    if (tokensToLeak > 0) {
      state.tokens = Math.max(0, state.tokens - tokensToLeak);
      state.lastRefill = currentTime;
    }

    if (state.tokens < this.config.capacity) {
      state.tokens += 1;
      return {
        allowed: true,
        reason: `Request queued. ${state.tokens}/${this.config.capacity} requests in bucket. Leaking at ${this.config.refillRate} req/sec`,
        tokens: state.tokens,
      };
    }

    return {
      allowed: false,
      reason: `Request rejected. Bucket full (${this.config.capacity}). Leaking at ${this.config.refillRate} requests/sec`,
      tokens: state.tokens,
    };
  }

  getState(algorithm: AlgorithmType): AlgorithmState | undefined {
    return this.states.get(algorithm);
  }
}

export const generateTrafficPattern = (pattern: TrafficPattern, time: number): number => {
  switch (pattern) {
    case "steady":
      return 1; // One request per tick
    case "bursty":
      return Math.random() > 0.7 ? Math.floor(Math.random() * 5) + 1 : 1;
    case "spike":
      return time % 20 === 0 ? 10 : 1;
    case "custom":
      return Math.random() > 0.5 ? 1 : 0;
    default:
      return 1;
  }
};
