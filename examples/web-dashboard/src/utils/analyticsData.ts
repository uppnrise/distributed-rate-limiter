import {
  PerformanceMetrics,
  TopKey,
  AlgorithmPerformance,
  UsageDataPoint,
  AlgorithmUsage,
  Alert,
} from "@/types/analytics";

export const generateMockPerformanceMetrics = (): PerformanceMetrics => ({
  totalRequests: Math.floor(Math.random() * 1000000) + 500000,
  requestsChange: (Math.random() - 0.3) * 30,
  successRate: 95 + Math.random() * 4,
  successRateChange: (Math.random() - 0.5) * 5,
  peakRPS: Math.floor(Math.random() * 5000) + 1000,
  peakRPSChange: (Math.random() - 0.3) * 20,
});

export const generateMockTopKeys = (): TopKey[] => {
  const algorithms = ["Token Bucket", "Sliding Window", "Fixed Window", "Leaky Bucket"];
  return Array.from({ length: 15 }, (_, i) => ({
    key: `rl_${["prod", "dev", "test"][Math.floor(Math.random() * 3)]}_${Math.random()
      .toString(36)
      .substring(7)}`,
    totalRequests: Math.floor(Math.random() * 100000) + 10000,
    rejectionRate: Math.random() * 30,
    algorithm: algorithms[Math.floor(Math.random() * algorithms.length)],
    lastActive: new Date(Date.now() - Math.random() * 86400000).toISOString(),
  })).sort((a, b) => b.totalRequests - a.totalRequests);
};

export const generateMockAlgorithmPerformance = (): AlgorithmPerformance[] => [
  {
    algorithm: "Token Bucket",
    requestsHandled: Math.floor(Math.random() * 500000) + 200000,
    successRate: 96 + Math.random() * 3,
    avgResponseTime: 80 + Math.random() * 40,
    memoryUsage: 120 + Math.floor(Math.random() * 50),
    efficiency: 5,
  },
  {
    algorithm: "Sliding Window",
    requestsHandled: Math.floor(Math.random() * 400000) + 150000,
    successRate: 94 + Math.random() * 4,
    avgResponseTime: 90 + Math.random() * 50,
    memoryUsage: 150 + Math.floor(Math.random() * 60),
    efficiency: 4,
  },
  {
    algorithm: "Fixed Window",
    requestsHandled: Math.floor(Math.random() * 300000) + 100000,
    successRate: 93 + Math.random() * 5,
    avgResponseTime: 70 + Math.random() * 30,
    memoryUsage: 100 + Math.floor(Math.random() * 40),
    efficiency: 4,
  },
  {
    algorithm: "Leaky Bucket",
    requestsHandled: Math.floor(Math.random() * 350000) + 120000,
    successRate: 95 + Math.random() * 3,
    avgResponseTime: 85 + Math.random() * 45,
    memoryUsage: 130 + Math.floor(Math.random() * 45),
    efficiency: 4,
  },
];

export const generateMockUsageData = (points: number = 24): UsageDataPoint[] => {
  const now = new Date();
  return Array.from({ length: points }, (_, i) => {
    const timestamp = new Date(now.getTime() - (points - i - 1) * 3600000);
    const requests = Math.floor(Math.random() * 50000) + 20000;
    const successful = Math.floor(requests * (0.9 + Math.random() * 0.08));
    return {
      timestamp: timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      requests,
      successful,
      rejected: requests - successful,
    };
  });
};

export const generateMockAlgorithmUsage = (): AlgorithmUsage[] => [
  { algorithm: "Token Bucket", value: 40, color: "#3b82f6" },
  { algorithm: "Sliding Window", value: 30, color: "#a855f7" },
  { algorithm: "Fixed Window", value: 20, color: "#22c55e" },
  { algorithm: "Leaky Bucket", value: 10, color: "#f97316" },
];

export const generateMockAlerts = (): Alert[] => {
  const types: Alert["type"][] = ["spike", "degradation", "config", "anomaly"];
  const severities: Alert["severity"][] = ["low", "medium", "high"];
  const messages = [
    "Rate limiting spike detected on user:* pattern keys",
    "Performance degradation detected in Token Bucket algorithm",
    "Configuration change: Global default capacity updated to 50",
    "Anomaly detected: Unusual traffic pattern from 192.168.1.0/24",
    "Rate limit exceeded threshold on premium:* keys",
    "System memory usage approaching 80%",
    "New API key added: rl_prod_api_new_key",
    "Burst traffic detected from mobile clients",
  ];

  return Array.from({ length: 8 }, (_, i) => ({
    id: Math.random().toString(36).substring(7),
    type: types[Math.floor(Math.random() * types.length)],
    message: messages[i],
    timestamp: new Date(Date.now() - Math.random() * 86400000).toISOString(),
    severity: severities[Math.floor(Math.random() * severities.length)],
  })).sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
};
