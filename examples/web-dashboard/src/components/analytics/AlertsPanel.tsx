import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { AlertTriangle, Bell, Settings, TrendingUp } from "lucide-react";
import { Alert } from "@/types/analytics";
import { cn } from "@/lib/utils";

interface AlertsPanelProps {
  alerts: Alert[];
}

const alertIcons = {
  spike: TrendingUp,
  degradation: AlertTriangle,
  config: Settings,
  anomaly: Bell,
};

const severityColors = {
  low: "border-blue-600 bg-blue-50 dark:bg-blue-950/20",
  medium: "border-yellow-600 bg-yellow-50 dark:bg-yellow-950/20",
  high: "border-red-600 bg-red-50 dark:bg-red-950/20",
};

export const AlertsPanel = ({ alerts }: AlertsPanelProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-4 flex items-center gap-2">
        <Bell className="h-5 w-5 text-primary" />
        <h3 className="text-lg font-semibold text-foreground">Alerts & Anomalies</h3>
        <Badge variant="secondary" className="ml-auto">
          {alerts.filter((a) => a.severity === "high").length} High Priority
        </Badge>
      </div>

      <ScrollArea className="h-[400px] pr-4">
        <div className="space-y-3">
          {alerts.map((alert) => {
            const Icon = alertIcons[alert.type];
            return (
              <div
                key={alert.id}
                className={cn(
                  "rounded-lg border-l-4 p-4 transition-all hover:shadow-md",
                  severityColors[alert.severity]
                )}
              >
                <div className="flex items-start gap-3">
                  <Icon className="mt-0.5 h-5 w-5 flex-shrink-0 text-foreground" />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          alert.severity === "high"
                            ? "destructive"
                            : alert.severity === "medium"
                            ? "default"
                            : "secondary"
                        }
                        className="text-xs"
                      >
                        {alert.severity.toUpperCase()}
                      </Badge>
                      <span className="text-xs text-muted-foreground">
                        {new Date(alert.timestamp).toLocaleString()}
                      </span>
                    </div>
                    <p className="mt-2 text-sm font-medium text-foreground">
                      {alert.message}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </ScrollArea>
    </Card>
  );
};
