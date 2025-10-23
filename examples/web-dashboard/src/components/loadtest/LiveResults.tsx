import { Card } from "@/components/ui/card";
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
import { TimeSeriesPoint } from "@/types/loadTesting";
import { Activity } from "lucide-react";

interface LiveResultsProps {
  data: TimeSeriesPoint[];
}

export const LiveResults = ({ data }: LiveResultsProps) => {
  if (data.length === 0) {
    return null;
  }

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50 animate-fade-in">
      <div className="mb-6 flex items-center gap-2">
        <Activity className="h-5 w-5 text-primary animate-pulse" />
        <h3 className="text-lg font-semibold text-foreground">Live Results</h3>
      </div>

      <div className="space-y-6">
        <div>
          <h4 className="mb-4 text-sm font-medium text-muted-foreground">
            Requests Per Second
          </h4>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
              <XAxis
                dataKey="timestamp"
                className="text-muted-foreground"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `${Math.floor(value / 1000)}s`}
              />
              <YAxis className="text-muted-foreground" tick={{ fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  backgroundColor: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: "8px",
                }}
              />
              <Line
                type="monotone"
                dataKey="requestsPerSecond"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                dot={{ r: 2 }}
                name="Requests/s"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div>
          <h4 className="mb-4 text-sm font-medium text-muted-foreground">Success Rate</h4>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
              <XAxis
                dataKey="timestamp"
                className="text-muted-foreground"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `${Math.floor(value / 1000)}s`}
              />
              <YAxis
                className="text-muted-foreground"
                tick={{ fontSize: 12 }}
                domain={[0, 100]}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "hsl(var(--card))",
                  border: "1px solid hsl(var(--border))",
                  borderRadius: "8px",
                }}
              />
              <Line
                type="monotone"
                dataKey="successRate"
                stroke="#22c55e"
                strokeWidth={2}
                dot={{ r: 2 }}
                name="Success %"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div>
          <h4 className="mb-4 text-sm font-medium text-muted-foreground">
            Response Times
          </h4>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
              <XAxis
                dataKey="timestamp"
                className="text-muted-foreground"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `${Math.floor(value / 1000)}s`}
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
              <Line
                type="monotone"
                dataKey="avgResponseTime"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ r: 2 }}
                name="P50 (ms)"
              />
              <Line
                type="monotone"
                dataKey="p95ResponseTime"
                stroke="#f97316"
                strokeWidth={2}
                dot={{ r: 2 }}
                name="P95 (ms)"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>
    </Card>
  );
};
