import { Card } from "@/components/ui/card";
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { UsageDataPoint, AlgorithmUsage } from "@/types/analytics";

interface UsageTrendsChartsProps {
  usageData: UsageDataPoint[];
  algorithmUsage: AlgorithmUsage[];
}

export const UsageTrendsCharts = ({ usageData, algorithmUsage }: UsageTrendsChartsProps) => {
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <h3 className="mb-4 text-lg font-semibold text-foreground">Request Volume Over Time</h3>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={usageData}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
            <XAxis
              dataKey="timestamp"
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
            />
            <YAxis className="text-muted-foreground" tick={{ fontSize: 12 }} />
            <Tooltip
              contentStyle={{
                backgroundColor: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: "8px",
              }}
            />
            <Area
              type="monotone"
              dataKey="requests"
              stroke="hsl(var(--primary))"
              fill="hsl(var(--primary))"
              fillOpacity={0.6}
              name="Total Requests"
            />
          </AreaChart>
        </ResponsiveContainer>
      </Card>

      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <h3 className="mb-4 text-lg font-semibold text-foreground">
          Success vs Rejection Rates
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={usageData}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
            <XAxis
              dataKey="timestamp"
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
            />
            <YAxis className="text-muted-foreground" tick={{ fontSize: 12 }} />
            <Tooltip
              contentStyle={{
                backgroundColor: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: "8px",
              }}
            />
            <Legend />
            <Bar dataKey="successful" stackId="a" fill="#22c55e" name="Successful" />
            <Bar dataKey="rejected" stackId="a" fill="#ef4444" name="Rejected" />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <h3 className="mb-4 text-lg font-semibold text-foreground">
          Algorithm Usage Distribution
        </h3>
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={algorithmUsage}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              paddingAngle={5}
              dataKey="value"
              label={({ algorithm, value }) => `${algorithm}: ${value}%`}
            >
              {algorithmUsage.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                backgroundColor: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: "8px",
              }}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="mt-4 grid grid-cols-2 gap-3">
          {algorithmUsage.map((item) => (
            <div key={item.algorithm} className="flex items-center gap-2">
              <div
                className="h-3 w-3 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              <span className="text-sm text-muted-foreground">{item.algorithm}</span>
            </div>
          ))}
        </div>
      </Card>

      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <h3 className="mb-4 text-lg font-semibold text-foreground">
          Performance Trends
        </h3>
        <div className="space-y-4">
          <div className="rounded-lg border border-border bg-background/50 p-4">
            <p className="text-sm text-muted-foreground">Average Request Duration</p>
            <p className="mt-2 text-2xl font-bold text-foreground">127ms</p>
            <p className="mt-1 text-xs text-green-600">-5.3% from last period</p>
          </div>

          <div className="rounded-lg border border-border bg-background/50 p-4">
            <p className="text-sm text-muted-foreground">Rate Limit Efficiency</p>
            <p className="mt-2 text-2xl font-bold text-foreground">94.2%</p>
            <p className="mt-1 text-xs text-green-600">+2.1% from last period</p>
          </div>

          <div className="rounded-lg border border-border bg-background/50 p-4">
            <p className="text-sm text-muted-foreground">System Uptime</p>
            <p className="mt-2 text-2xl font-bold text-foreground">99.98%</p>
            <p className="mt-1 text-xs text-green-600">+0.01% from last period</p>
          </div>
        </div>
      </Card>
    </div>
  );
};
