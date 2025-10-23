import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { ConfigStats as ConfigStatsType } from "@/types/configuration";
import { FileText, Sparkles, TrendingUp, Zap } from "lucide-react";

interface ConfigStatsProps {
  stats: ConfigStatsType;
}

export const ConfigStats = ({ stats }: ConfigStatsProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <h3 className="mb-6 text-lg font-semibold text-foreground">Configuration Statistics</h3>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <FileText className="h-4 w-4" />
            <span>Total Key Configs</span>
          </div>
          <p className="text-2xl font-bold text-foreground">{stats.totalKeyConfigs}</p>
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Sparkles className="h-4 w-4" />
            <span>Pattern Configs</span>
          </div>
          <p className="text-2xl font-bold text-foreground">{stats.totalPatternConfigs}</p>
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <TrendingUp className="h-4 w-4" />
            <span>Most Used Pattern</span>
          </div>
          <p className="font-mono text-lg font-bold text-foreground">{stats.mostUsedPattern}</p>
        </div>

        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Zap className="h-4 w-4" />
            <span>Avg Lookup Time</span>
          </div>
          <p className="text-2xl font-bold text-foreground">{stats.avgLookupTime}ms</p>
        </div>
      </div>

      <div className="mt-6 space-y-4 border-t border-border pt-6">
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Cache Hit Rate</span>
            <span className="font-medium text-foreground">{stats.cacheHitRate}%</span>
          </div>
          <Progress value={stats.cacheHitRate} className="h-2" />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg bg-accent/50 p-3">
            <p className="text-xs text-muted-foreground">Configuration Efficiency</p>
            <p className="mt-1 text-lg font-semibold text-foreground">
              {stats.cacheHitRate > 90 ? "Excellent" : stats.cacheHitRate > 75 ? "Good" : "Fair"}
            </p>
          </div>
          <div className="rounded-lg bg-accent/50 p-3">
            <p className="text-xs text-muted-foreground">Performance Rating</p>
            <p className="mt-1 text-lg font-semibold text-foreground">
              {stats.avgLookupTime < 5 ? "Fast" : stats.avgLookupTime < 10 ? "Normal" : "Slow"}
            </p>
          </div>
        </div>
      </div>
    </Card>
  );
};
