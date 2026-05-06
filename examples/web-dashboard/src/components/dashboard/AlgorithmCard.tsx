import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

interface AlgorithmCardProps {
  name: string;
  activeKeys: number;
  avgResponseTime: number | null;
  successRate: number | null;
}

export const AlgorithmCard = ({
  name,
  activeKeys,
  avgResponseTime,
  successRate,
}: AlgorithmCardProps) => {
  const hasObservedPerformance = successRate !== null;

  const getPerformanceColor = (rate: number) => {
    if (rate >= 95) return "text-green-600";
    if (rate >= 85) return "text-yellow-600";
    return "text-red-600";
  };

  const getPerformanceBadge = (rate: number) => {
    if (rate >= 95) return "Excellent";
    if (rate >= 85) return "Good";
    return "Poor";
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50 transition-all hover:shadow-xl">
      <div className="space-y-4">
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-lg font-semibold text-foreground">{name}</h3>
            <p className="text-sm text-muted-foreground">{activeKeys} active keys</p>
          </div>
          <Badge
            variant="outline"
            className={cn(
              "font-medium",
              !hasObservedPerformance && "border-border text-muted-foreground",
              hasObservedPerformance && successRate >= 95 && "border-green-600 text-green-600",
              hasObservedPerformance && successRate >= 85 && successRate < 95 && "border-yellow-600 text-yellow-600",
              hasObservedPerformance && successRate < 85 && "border-red-600 text-red-600"
            )}
          >
            {hasObservedPerformance ? getPerformanceBadge(successRate) : "Observed"}
          </Badge>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Avg Response</span>
            <span className="font-medium text-foreground">
              {avgResponseTime !== null ? `${avgResponseTime.toFixed(1)}ms` : "Not available"}
            </span>
          </div>
          
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Success Rate</span>
            {hasObservedPerformance ? (
              <span className={cn("font-bold", getPerformanceColor(successRate))}>
                {successRate.toFixed(1)}%
              </span>
            ) : (
              <span className="font-medium text-muted-foreground">Not available</span>
            )}
          </div>

          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div
              className={cn(
                "h-full rounded-full transition-all",
                !hasObservedPerformance && "bg-muted-foreground/30",
                hasObservedPerformance && successRate >= 95 && "bg-green-600",
                hasObservedPerformance && successRate >= 85 && successRate < 95 && "bg-yellow-600",
                hasObservedPerformance && successRate < 85 && "bg-red-600"
              )}
              style={{ width: `${hasObservedPerformance ? successRate : 0}%` }}
            />
          </div>
        </div>
      </div>
    </Card>
  );
};
