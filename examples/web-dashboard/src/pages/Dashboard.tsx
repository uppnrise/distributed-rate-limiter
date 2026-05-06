import { useEffect, useRef, useState } from "react";
import { Card } from "@/components/ui/card";
import { Key, Activity, TrendingUp, PieChart } from "lucide-react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { StatCard } from "@/components/dashboard/StatCard";
import { ActivityFeed } from "@/components/dashboard/ActivityFeed";
import { AlgorithmCard } from "@/components/dashboard/AlgorithmCard";
import { AdaptiveStatusCard } from "@/components/dashboard/AdaptiveStatusCard";
import { DashboardLoadingSkeleton } from "@/components/LoadingState";
import { ApiHealthCheck } from "@/components/ApiHealthCheck";
import { useApp } from "@/contexts/AppContext";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import { useKeyboardShortcuts, dashboardShortcuts } from "@/hooks/useKeyboardShortcuts";

interface TimeSeriesData {
  time: string;
  allowed: number;
  rejected: number;
  total: number;
}

interface ActivityEvent {
  id: string;
  timestamp: string;
  key: string;
  algorithm: string;
  status: "allowed" | "rejected" | "current";
  description: string;
}

interface AlgorithmMetric {
  name: string;
  activeKeys: number;
  avgResponseTime: number | null;
  successRate: number | null;
}

const buildSnapshotActivities = (
  keyMetrics: Record<string, { allowedRequests: number; deniedRequests: number; lastAccessTime: number }>
): ActivityEvent[] =>
  Object.entries(keyMetrics)
    .filter(([, metric]) => metric.allowedRequests > 0 || metric.deniedRequests > 0)
    .map(([key, metric]) => ({
      id: `${key}-snapshot-${metric.lastAccessTime}`,
      timestamp: new Date(metric.lastAccessTime).toISOString(),
      key,
      algorithm: "TOKEN_BUCKET",
      status: "current" as const,
      description: `${metric.allowedRequests} allowed • ${metric.deniedRequests} rejected`,
    }))
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    .slice(0, 20);

const Dashboard = () => {
  const { realtimeMetrics, isConnected } = useApp();
  const [loading, setLoading] = useState(true);
  const [activeKeys, setActiveKeys] = useState(0);
  const [requestsPerSecond, setRequestsPerSecond] = useState(0);
  const [successRate, setSuccessRate] = useState(100);
  const [algorithmMetrics, setAlgorithmMetrics] = useState<AlgorithmMetric[]>([]);
  const [activities, setActivities] = useState<ActivityEvent[]>([]);
  const [timeSeriesData, setTimeSeriesData] = useState<TimeSeriesData[]>([]);
  const previousMetricsRef = useRef<{ allowed: number; denied: number; timestamp: number } | null>(null);
  const previousKeyMetricsRef = useRef<Record<string, { allowedRequests: number; deniedRequests: number }>>({});
  const metricsBaselineInitializedRef = useRef(false);

  // Enable keyboard shortcuts
  useKeyboardShortcuts(dashboardShortcuts);

  // Initial data load
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        // Fetch initial metrics and active keys
        const [metricsData, keysData] = await Promise.all([
          rateLimiterApi.getMetrics(),
          rateLimiterApi.getActiveKeys()
        ]);
        
        // Initialize time series with current data
        const now = new Date();
        const totalAllowed = metricsData.totalAllowedRequests;
        const totalDenied = metricsData.totalDeniedRequests;
        
        setTimeSeriesData([{
          time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          allowed: totalAllowed,
          rejected: totalDenied,
          total: totalAllowed + totalDenied,
        }]);
        setActivities(buildSnapshotActivities(metricsData.keyMetrics));

        previousMetricsRef.current = {
          allowed: totalAllowed,
          denied: totalDenied,
          timestamp: Date.now(),
        };
        previousKeyMetricsRef.current = Object.fromEntries(
          Object.entries(metricsData.keyMetrics).map(([key, value]) => [
            key,
            {
              allowedRequests: value.allowedRequests,
              deniedRequests: value.deniedRequests,
            },
          ])
        );
        metricsBaselineInitializedRef.current = true;
        setLoading(false);
      } catch (error) {
        console.error('Failed to load dashboard data:', error);
        setLoading(false);
      }
    };
    loadInitialData();
  }, []);

  // Update metrics from real-time polling (AppContext polls /metrics every 5s)
  useEffect(() => {
    if (!realtimeMetrics) return;

    if (!metricsBaselineInitializedRef.current) {
      previousMetricsRef.current = {
        allowed: realtimeMetrics.totalAllowedRequests,
        denied: realtimeMetrics.totalDeniedRequests,
        timestamp: Date.now(),
      };
      previousKeyMetricsRef.current = Object.fromEntries(
        Object.entries(realtimeMetrics.keyMetrics).map(([key, value]) => [
          key,
          {
            allowedRequests: value.allowedRequests,
            deniedRequests: value.deniedRequests,
          },
        ])
      );
      metricsBaselineInitializedRef.current = true;
      return;
    }
    
    const totalRequests = realtimeMetrics.totalAllowedRequests + realtimeMetrics.totalDeniedRequests;
    const currentSuccessRate = totalRequests > 0 
      ? Math.round((realtimeMetrics.totalAllowedRequests / totalRequests) * 100) 
      : 100;
    
    const currentActiveKeys = Object.keys(realtimeMetrics.keyMetrics).length;
    
    // Calculate requests per second
    const previousMetrics = previousMetricsRef.current;
    if (previousMetrics) {
      const timeDiff = (Date.now() - previousMetrics.timestamp) / 1000; // seconds
      const allowedDiff = realtimeMetrics.totalAllowedRequests - previousMetrics.allowed;
      const deniedDiff = realtimeMetrics.totalDeniedRequests - previousMetrics.denied;
      const totalDiff = allowedDiff + deniedDiff;
      
      if (timeDiff > 0) {
        setRequestsPerSecond(Math.round(totalDiff / timeDiff));
      }
    }
    
    setActiveKeys(currentActiveKeys);
    setSuccessRate(currentSuccessRate);
    
    // Update time series data
    const now = new Date();
    setTimeSeriesData((prev) => {
      const newPoint = {
        time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        allowed: realtimeMetrics.totalAllowedRequests,
        rejected: realtimeMetrics.totalDeniedRequests,
        total: totalRequests,
      };
      
      const updated = [...prev, newPoint].slice(-10); // Keep last 10 data points
      return updated;
    });
    
    // Generate activity events from per-poll key metric deltas
    const previousKeyMetrics = previousKeyMetricsRef.current;
    const newActivities: ActivityEvent[] = [];
    Object.entries(realtimeMetrics.keyMetrics).forEach(([key, metric]) => {
      const previousKeyMetric = previousKeyMetrics[key] ?? { allowedRequests: 0, deniedRequests: 0 };
      const allowedDiff = Math.max(0, metric.allowedRequests - previousKeyMetric.allowedRequests);
      const deniedDiff = Math.max(0, metric.deniedRequests - previousKeyMetric.deniedRequests);
      const eventTimestamp = new Date(metric.lastAccessTime).toISOString();

      if (allowedDiff > 0) {
        newActivities.push({
          id: `${key}-allowed-${metric.lastAccessTime}-${allowedDiff}`,
          timestamp: eventTimestamp,
          key: key,
          algorithm: "TOKEN_BUCKET", // Default, will be enriched from admin/keys
          status: "allowed",
          description: `${allowedDiff} request${allowedDiff === 1 ? "" : "s"}`,
        });
      }

      if (deniedDiff > 0) {
        newActivities.push({
          id: `${key}-rejected-${metric.lastAccessTime}-${deniedDiff}`,
          timestamp: eventTimestamp,
          key: key,
          algorithm: "TOKEN_BUCKET", // Default, will be enriched from admin/keys
          status: "rejected",
          description: `${deniedDiff} request${deniedDiff === 1 ? "" : "s"}`,
        });
      }
    });

    if (newActivities.length > 0) {
      setActivities((prev) =>
        [...newActivities, ...prev]
          .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
          .slice(0, 20)
      );
    }

    previousMetricsRef.current = {
      allowed: realtimeMetrics.totalAllowedRequests,
      denied: realtimeMetrics.totalDeniedRequests,
      timestamp: Date.now(),
    };
    previousKeyMetricsRef.current = Object.fromEntries(
      Object.entries(realtimeMetrics.keyMetrics).map(([key, value]) => [
        key,
        {
          allowedRequests: value.allowedRequests,
          deniedRequests: value.deniedRequests,
        },
      ])
    );
    
  }, [realtimeMetrics]);

  // Fetch and update algorithm distribution from admin keys
  useEffect(() => {
    const fetchAlgorithmMetrics = async () => {
      try {
        const keysData = await rateLimiterApi.getActiveKeys();
        
        // Group by algorithm
        const algorithmGroups: Record<string, { count: number; totalRequests: number; successCount: number }> = {};
        
        keysData.keys.forEach(key => {
          const algo = key.algorithm || 'TOKEN_BUCKET';
          if (!algorithmGroups[algo]) {
            algorithmGroups[algo] = { count: 0, totalRequests: 0, successCount: 0 };
          }
          algorithmGroups[algo].count++;
        });
        
        // Convert to algorithm metrics format
        const metrics: AlgorithmMetric[] = Object.entries(algorithmGroups).map(([name, data]) => ({
          name,
          activeKeys: data.count,
          avgResponseTime: null,
          successRate: null,
        }));
        
        setAlgorithmMetrics(metrics);
        
      } catch (error) {
        console.error('Failed to fetch algorithm metrics:', error);
      }
    };
    
    // Fetch immediately and then every 30 seconds
    fetchAlgorithmMetrics();
    const interval = setInterval(fetchAlgorithmMetrics, 30000);
    
    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return <DashboardLoadingSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight text-foreground">Dashboard</h2>
        <p className="text-muted-foreground">
          Real-time monitoring of your rate limiter performance
        </p>
      </div>

      <ApiHealthCheck />

      {/* Top Stats Row */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Active Keys"
          value={activeKeys}
          icon={Key}
          trend={isConnected ? { value: "Live", isPositive: true } : undefined}
        />
        
        <StatCard
          title="Requests/Second"
          value={requestsPerSecond}
          icon={Activity}
          trend={isConnected ? { value: "Live", isPositive: true } : undefined}
        />
        
        <StatCard
          title="Success Rate"
          value={`${successRate}%`}
          icon={TrendingUp}
          trend={successRate >= 95 ? { value: "Healthy", isPositive: true } : { value: "Warning", isPositive: false }}
        />
        
        <StatCard
          title="Algorithms"
          value={algorithmMetrics.length}
          icon={PieChart}
        />
      </div>

      {/* Real-time Metrics Chart */}
      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-semibold text-foreground">Requests Over Time</h3>
            <p className="text-sm text-muted-foreground">Last 10 minutes • Updates every 5 seconds</p>
          </div>
          <div className="flex items-center gap-4 text-sm">
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-green-600" />
              <span className="text-muted-foreground">Allowed</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-red-600" />
              <span className="text-muted-foreground">Rejected</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-full bg-primary" />
              <span className="text-muted-foreground">Total</span>
            </div>
          </div>
        </div>
        
        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={timeSeriesData}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
            <XAxis 
              dataKey="time" 
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
            />
            <YAxis 
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
              label={{ value: 'Requests/Second', angle: -90, position: 'insideLeft' }}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: "8px",
              }}
            />
            <Legend />
            <Line
              type="monotone"
              dataKey="allowed"
              stroke="#16a34a"
              strokeWidth={2}
              dot={{ r: 3 }}
              name="Allowed"
            />
            <Line
              type="monotone"
              dataKey="rejected"
              stroke="#dc2626"
              strokeWidth={2}
              dot={{ r: 3 }}
              name="Rejected"
            />
            <Line
              type="monotone"
              dataKey="total"
              stroke="hsl(var(--primary))"
              strokeWidth={2}
              dot={{ r: 3 }}
              name="Total"
            />
          </LineChart>
        </ResponsiveContainer>
      </Card>

      {/* Algorithm Performance Cards */}
      <div>
        <h3 className="mb-4 text-xl font-semibold text-foreground">Algorithm Performance</h3>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {algorithmMetrics.map((algo) => (
            <AlgorithmCard
              key={algo.name}
              name={algo.name}
              activeKeys={algo.activeKeys}
              avgResponseTime={algo.avgResponseTime}
              successRate={algo.successRate}
            />
          ))}
        </div>
      </div>

      {/* Bottom Section: Activity Feed and Adaptive Status */}
      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <ActivityFeed events={activities} />
        </div>
        <div>
          <AdaptiveStatusCard />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
