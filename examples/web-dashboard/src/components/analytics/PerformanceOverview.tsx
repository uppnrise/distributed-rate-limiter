import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { TrendingUp, TrendingDown, Activity, Award, Key } from "lucide-react";
import { PerformanceMetrics, TopKey } from "@/types/analytics";
import { cn } from "@/lib/utils";

interface PerformanceOverviewProps {
  metrics: PerformanceMetrics;
  topKeys: TopKey[];
}

export const PerformanceOverview = ({ metrics, topKeys }: PerformanceOverviewProps) => {
  const MetricCard = ({
    title,
    value,
    change,
    icon: Icon,
    format = (v: number) => v.toLocaleString(),
  }: {
    title: string;
    value: number;
    change: number;
    icon: any;
    format?: (v: number) => string;
  }) => (
    <Card className="relative overflow-hidden p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="absolute right-0 top-0 h-32 w-32 translate-x-8 -translate-y-8 rounded-full bg-primary/5" />
      
      <div className="relative space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <div className="rounded-lg bg-primary/10 p-2">
            <Icon className="h-4 w-4 text-primary" />
          </div>
        </div>
        
        <div>
          <p className="text-3xl font-bold text-foreground">{format(value)}</p>
          <div className="mt-2 flex items-center gap-2">
            {change >= 0 ? (
              <TrendingUp className="h-4 w-4 text-green-600" />
            ) : (
              <TrendingDown className="h-4 w-4 text-red-600" />
            )}
            <span
              className={cn(
                "text-sm font-medium",
                change >= 0 ? "text-green-600" : "text-red-600"
              )}
            >
              {change >= 0 ? "+" : ""}{change.toFixed(1)}%
            </span>
            <span className="text-xs text-muted-foreground">vs previous period</span>
          </div>
        </div>
      </div>
    </Card>
  );

  return (
    <div className="space-y-6">
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <MetricCard
          title="Total Requests"
          value={metrics.totalRequests}
          change={metrics.requestsChange}
          icon={Activity}
        />
        
        <MetricCard
          title="Average Success Rate"
          value={metrics.successRate}
          change={metrics.successRateChange}
          icon={Award}
          format={(v) => `${v.toFixed(1)}%`}
        />
        
        <MetricCard
          title="Peak RPS"
          value={metrics.peakRPS}
          change={metrics.peakRPSChange}
          icon={TrendingUp}
        />
      </div>

      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <div className="mb-4 flex items-center gap-2">
          <Key className="h-5 w-5 text-primary" />
          <h3 className="text-lg font-semibold text-foreground">Most Active Keys</h3>
        </div>
        
        <div className="space-y-3">
          {topKeys.slice(0, 5).map((key, index) => (
            <div
              key={key.key}
              className="flex items-center justify-between rounded-lg border border-border bg-background/50 p-3 transition-colors hover:bg-accent/50"
            >
              <div className="flex items-center gap-3">
                <Badge variant="outline" className="font-mono">
                  #{index + 1}
                </Badge>
                <div>
                  <p className="font-mono text-sm font-medium text-foreground">{key.key}</p>
                  <p className="text-xs text-muted-foreground">
                    {key.totalRequests.toLocaleString()} requests
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm font-medium text-foreground">{key.algorithm}</p>
                <p
                  className={cn(
                    "text-xs",
                    key.rejectionRate > 20 ? "text-red-600" : "text-green-600"
                  )}
                >
                  {key.rejectionRate.toFixed(1)}% rejected
                </p>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
};
