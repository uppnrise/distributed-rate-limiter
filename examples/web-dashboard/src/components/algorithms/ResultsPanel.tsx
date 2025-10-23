import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { AlgorithmType, AlgorithmStats } from "@/types/algorithms";
import { TrendingUp, TrendingDown } from "lucide-react";
import { cn } from "@/lib/utils";

interface ResultsPanelProps {
  stats: Map<AlgorithmType, AlgorithmStats>;
}

const algorithmNames = {
  "token-bucket": "Token Bucket",
  "sliding-window": "Sliding Window",
  "fixed-window": "Fixed Window",
  "leaky-bucket": "Leaky Bucket",
};

const algorithmColors = {
  "token-bucket": "border-blue-600",
  "sliding-window": "border-purple-600",
  "fixed-window": "border-green-600",
  "leaky-bucket": "border-orange-600",
};

export const ResultsPanel = ({ stats }: ResultsPanelProps) => {
  if (stats.size === 0) {
    return null;
  }

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <h3 className="mb-6 text-lg font-semibold text-foreground">Performance Statistics</h3>
      
      <div className="space-y-6">
        {Array.from(stats.entries()).map(([algo, stat]) => (
          <div
            key={algo}
            className={cn(
              "rounded-lg border-l-4 bg-background/50 p-4 transition-all hover:bg-accent/50",
              algorithmColors[algo]
            )}
          >
            <h4 className="mb-4 font-semibold text-foreground">{algorithmNames[algo]}</h4>
            
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Total Requests</span>
                  <span className="font-medium text-foreground">{stat.totalRequests}</span>
                </div>
                
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Rejection Rate</span>
                  <div className="flex items-center gap-1">
                    <span
                      className={cn(
                        "font-medium",
                        stat.rejectionRate > 50 ? "text-red-600" : "text-green-600"
                      )}
                    >
                      {stat.rejectionRate.toFixed(1)}%
                    </span>
                    {stat.rejectionRate > 50 ? (
                      <TrendingUp className="h-3 w-3 text-red-600" />
                    ) : (
                      <TrendingDown className="h-3 w-3 text-green-600" />
                    )}
                  </div>
                </div>
                <Progress
                  value={stat.rejectionRate}
                  className="h-2"
                />
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Avg Response Time</span>
                  <span className="font-medium text-foreground">{stat.avgResponseTime}ms</span>
                </div>
                
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">Burst Efficiency</span>
                  <span
                    className={cn(
                      "font-medium",
                      stat.burstEfficiency > 80 ? "text-green-600" : "text-yellow-600"
                    )}
                  >
                    {stat.burstEfficiency.toFixed(1)}%
                  </span>
                </div>
                <Progress
                  value={stat.burstEfficiency}
                  className="h-2"
                />
              </div>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};
