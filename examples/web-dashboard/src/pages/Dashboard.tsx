import { useEffect, useState } from "react";
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
import { DashboardLoadingSkeleton } from "@/components/LoadingState";
import { ApiHealthCheck } from "@/components/ApiHealthCheck";
import { useApp } from "@/contexts/AppContext";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import {
  generateRealtimeData,
  generateMockMetrics,
  generateActivityEvent,
  generateAlgorithmMetrics,
} from "@/utils/mockData";
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
  status: "allowed" | "rejected";
  tokensUsed: number;
}

const Dashboard = () => {
  const { realtimeMetrics } = useApp();
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState(() => generateMockMetrics());
  const [algorithmMetrics, setAlgorithmMetrics] = useState(() => generateAlgorithmMetrics());
  const [activities, setActivities] = useState<ActivityEvent[]>([]);
  const [timeSeriesData, setTimeSeriesData] = useState<TimeSeriesData[]>(() => {
    const now = new Date();
    return Array.from({ length: 10 }, (_, i) => {
      const time = new Date(now.getTime() - (9 - i) * 60000);
      const allowed = Math.floor(Math.random() * 800) + 400;
      const rejected = Math.floor(Math.random() * 200) + 50;
      return {
        time: time.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        allowed,
        rejected,
        total: allowed + rejected,
      };
    });
  });

  // Enable keyboard shortcuts
  useKeyboardShortcuts(dashboardShortcuts);

  // Initial data load
  useEffect(() => {
    const loadInitialData = async () => {
      try {
        await rateLimiterApi.getMetrics();
        setLoading(false);
      } catch (error) {
        console.error('Failed to load dashboard data:', error);
        setLoading(false);
      }
    };
    loadInitialData();
  }, []);

  // Use realtime metrics from API polling
  useEffect(() => {
    if (realtimeMetrics) {
      const totalRequests = realtimeMetrics.totalAllowedRequests + realtimeMetrics.totalDeniedRequests;
      const successRate = totalRequests > 0 
        ? Math.round((realtimeMetrics.totalAllowedRequests / totalRequests) * 100) 
        : 100;
      
      const activeKeys = Object.keys(realtimeMetrics.keyMetrics).length;
      
      setMetrics(prev => ({
        ...prev,
        activeKeys,
        successRate,
        totalRequests: realtimeMetrics.totalAllowedRequests + realtimeMetrics.totalDeniedRequests,
      }));
    }
  }, [realtimeMetrics]);

  // Update real-time data every 5 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      // Update metrics
      setMetrics(generateMockMetrics());
      
      // Update algorithm metrics
      setAlgorithmMetrics(generateAlgorithmMetrics());
      
      // Add new activity
      setActivities((prev) => {
        const newActivity = generateActivityEvent();
        return [newActivity, ...prev].slice(0, 20);
      });
      
      // Update time series data
      setTimeSeriesData((prev) => {
        const newData = [...prev];
        const lastData = newData[newData.length - 1];
        const now = new Date();
        
        newData.push({
          time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          allowed: Math.floor(generateRealtimeData(lastData.allowed)),
          rejected: Math.floor(generateRealtimeData(lastData.rejected)),
          total: 0,
        });
        
        newData[newData.length - 1].total =
          newData[newData.length - 1].allowed + newData[newData.length - 1].rejected;
        
        return newData.slice(-10);
      });
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  // Initialize activities
  useEffect(() => {
    const initialActivities = Array.from({ length: 10 }, () => generateActivityEvent());
    setActivities(initialActivities);
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
          value={metrics.activeKeys}
          icon={Key}
          trend={{ value: "+12.5%", isPositive: true }}
        />
        
        <StatCard
          title="Requests/Second"
          value={metrics.requestsPerSecond}
          icon={Activity}
          trend={{ value: "+8.3%", isPositive: true }}
        />
        
        <StatCard
          title="Success Rate"
          value={`${metrics.successRate}%`}
          icon={TrendingUp}
          trend={{ value: "-0.5%", isPositive: false }}
        />
        
        <StatCard
          title="Algorithms"
          value={4}
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

      {/* Recent Activity Feed */}
      <ActivityFeed events={activities} />
    </div>
  );
};

export default Dashboard;
