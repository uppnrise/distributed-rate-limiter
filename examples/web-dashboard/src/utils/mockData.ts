export const generateRealtimeData = (previousData: number) => {
  const variation = (Math.random() - 0.5) * 20;
  return Math.max(0, previousData + variation);
};

export const generateMockMetrics = () => {
  return {
    activeKeys: Math.floor(Math.random() * 50) + 100,
    requestsPerSecond: Math.floor(Math.random() * 500) + 1000,
    successRate: Math.floor(Math.random() * 5) + 95,
  };
};

export const generateActivityEvent = () => {
  const algorithms = ["Token Bucket", "Sliding Window", "Fixed Window", "Leaky Bucket"];
  const statuses: ("allowed" | "rejected")[] = ["allowed", "allowed", "allowed", "rejected"];
  const now = new Date();
  
  return {
    id: Math.random().toString(36).substring(7),
    timestamp: now.toLocaleTimeString(),
    key: `rl_${Math.random().toString(36).substring(2, 10)}`,
    algorithm: algorithms[Math.floor(Math.random() * algorithms.length)],
    status: statuses[Math.floor(Math.random() * statuses.length)],
    tokensUsed: Math.floor(Math.random() * 50) + 1,
  };
};

export const generateAlgorithmMetrics = () => {
  return [
    {
      name: "Token Bucket",
      activeKeys: Math.floor(Math.random() * 20) + 30,
      avgResponseTime: Math.floor(Math.random() * 50) + 50,
      successRate: Math.floor(Math.random() * 5) + 95,
    },
    {
      name: "Sliding Window",
      activeKeys: Math.floor(Math.random() * 20) + 25,
      avgResponseTime: Math.floor(Math.random() * 50) + 80,
      successRate: Math.floor(Math.random() * 10) + 85,
    },
    {
      name: "Fixed Window",
      activeKeys: Math.floor(Math.random() * 15) + 20,
      avgResponseTime: Math.floor(Math.random() * 30) + 40,
      successRate: Math.floor(Math.random() * 5) + 95,
    },
    {
      name: "Leaky Bucket",
      activeKeys: Math.floor(Math.random() * 15) + 15,
      avgResponseTime: Math.floor(Math.random() * 40) + 70,
      successRate: Math.floor(Math.random() * 10) + 88,
    },
  ];
};
