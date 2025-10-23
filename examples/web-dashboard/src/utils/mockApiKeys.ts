import { ApiKey, KeyAccessLog } from "@/types/apiKeys";

export const generateMockApiKeys = (): ApiKey[] => {
  const algorithms = ["token-bucket", "sliding-window", "fixed-window", "leaky-bucket"];
  const statuses: ApiKey["status"][] = ["active", "active", "active", "inactive", "expired"];

  return Array.from({ length: 12 }, (_, i) => ({
    id: Math.random().toString(36).substring(7),
    name: `${["Production", "Development", "Testing", "Staging"][i % 4]} API Key ${
      Math.floor(i / 4) + 1
    }`,
    key: `rl_${Array.from({ length: 32 }, () => Math.random().toString(36)[2]).join("")}`,
    description: i % 3 === 0 ? "Primary API key for production environment" : undefined,
    status: statuses[i % statuses.length],
    createdAt: new Date(Date.now() - Math.random() * 30 * 86400000).toISOString(),
    lastUsed:
      i % 4 === 0 ? undefined : new Date(Date.now() - Math.random() * 7 * 86400000).toISOString(),
    expiresAt:
      i % 5 === 0
        ? new Date(Date.now() + Math.random() * 180 * 86400000).toISOString()
        : undefined,
    rateLimit: {
      capacity: [10, 50, 100, 200][i % 4],
      refillRate: [5, 10, 20, 40][i % 4],
      algorithm: algorithms[i % algorithms.length],
    },
    usageStats: {
      totalRequests: Math.floor(Math.random() * 100000) + 10000,
      successfulRequests: Math.floor(Math.random() * 90000) + 9000,
      rateLimitedRequests: Math.floor(Math.random() * 5000) + 500,
    },
    ipWhitelist: i % 3 === 0 ? ["192.168.1.0/24", "10.0.0.0/16"] : undefined,
    ipBlacklist: i % 7 === 0 ? ["123.45.67.89"] : undefined,
  }));
};

export const generateMockAccessLogs = (): KeyAccessLog[] => {
  const endpoints = ["/api/v1/rate-limit", "/api/v1/check", "/api/v1/stats", "/api/v1/config"];
  const statusCodes = [200, 200, 200, 429, 500];

  return Array.from({ length: 50 }, (_, i) => ({
    id: Math.random().toString(36).substring(7),
    timestamp: new Date(Date.now() - i * 300000).toISOString(),
    ipAddress: `${Math.floor(Math.random() * 255)}.${Math.floor(
      Math.random() * 255
    )}.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
    endpoint: endpoints[Math.floor(Math.random() * endpoints.length)],
    statusCode: statusCodes[Math.floor(Math.random() * statusCodes.length)],
    responseTime: Math.floor(Math.random() * 200) + 50,
  }));
};
