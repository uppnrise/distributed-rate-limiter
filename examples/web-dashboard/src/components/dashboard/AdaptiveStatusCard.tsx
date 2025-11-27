import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Brain, Activity, Shield, Settings, ChevronRight, Loader2 } from "lucide-react";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import type { AdaptiveConfig, AdaptiveStatus } from "@/types/adaptive";

export const AdaptiveStatusCard = () => {
  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState<AdaptiveConfig | null>(null);
  const [statuses, setStatuses] = useState<AdaptiveStatus[]>([]);

  useEffect(() => {
    const loadData = async () => {
      try {
        const [configData, statusesData] = await Promise.all([
          rateLimiterApi.getAdaptiveConfig(),
          rateLimiterApi.getAdaptiveStatusForAllKeys(),
        ]);
        setConfig(configData);
        setStatuses(statusesData);
      } catch (error) {
        console.error("Failed to load adaptive status:", error);
      } finally {
        setLoading(false);
      }
    };

    loadData();
    const interval = setInterval(loadData, 30000);
    return () => clearInterval(interval);
  }, []);

  const getModeIcon = (mode: string) => {
    switch (mode) {
      case "ADAPTIVE":
        return <Brain className="h-3 w-3" />;
      case "LEARNING":
        return <Activity className="h-3 w-3" />;
      case "OVERRIDE":
        return <Shield className="h-3 w-3" />;
      default:
        return <Settings className="h-3 w-3" />;
    }
  };

  const getModeColor = (mode: string) => {
    switch (mode) {
      case "ADAPTIVE":
        return "bg-green-500";
      case "LEARNING":
        return "bg-blue-500";
      case "OVERRIDE":
        return "bg-orange-500";
      default:
        return "bg-gray-500";
    }
  };

  if (loading) {
    return (
      <Card className="p-6">
        <div className="flex items-center justify-center h-24">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      </Card>
    );
  }

  const adaptiveCount = statuses.filter(s => s.adaptiveStatus.mode === "ADAPTIVE").length;
  const learningCount = statuses.filter(s => s.adaptiveStatus.mode === "LEARNING").length;
  const overrideCount = statuses.filter(s => s.adaptiveStatus.mode === "OVERRIDE").length;
  const avgConfidence = statuses.length > 0 
    ? statuses.reduce((sum, s) => sum + s.adaptiveStatus.confidence, 0) / statuses.length 
    : 0;

  return (
    <Card className="p-6 bg-gradient-to-br from-card to-primary/5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
            <Brain className="h-4 w-4 text-primary" />
          </div>
          <div>
            <h3 className="font-semibold">Adaptive Rate Limiting</h3>
            <p className="text-xs text-muted-foreground">ML-driven optimization</p>
          </div>
        </div>
        <Badge variant={config?.enabled ? "default" : "secondary"}>
          {config?.enabled ? "Enabled" : "Disabled"}
        </Badge>
      </div>

      <div className="space-y-4">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <div className="flex items-center justify-center gap-1 mb-1">
              <Brain className="h-3 w-3 text-green-500" />
              <span className="text-lg font-bold">{adaptiveCount}</span>
            </div>
            <p className="text-xs text-muted-foreground">Adaptive</p>
          </div>
          <div>
            <div className="flex items-center justify-center gap-1 mb-1">
              <Activity className="h-3 w-3 text-blue-500" />
              <span className="text-lg font-bold">{learningCount}</span>
            </div>
            <p className="text-xs text-muted-foreground">Learning</p>
          </div>
          <div>
            <div className="flex items-center justify-center gap-1 mb-1">
              <Shield className="h-3 w-3 text-orange-500" />
              <span className="text-lg font-bold">{overrideCount}</span>
            </div>
            <p className="text-xs text-muted-foreground">Override</p>
          </div>
        </div>

        <div className="space-y-1">
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Avg Confidence</span>
            <span className="font-medium">{(avgConfidence * 100).toFixed(0)}%</span>
          </div>
          <Progress value={avgConfidence * 100} className="h-2" />
        </div>

        {statuses.length > 0 && (
          <div className="space-y-2">
            <p className="text-xs text-muted-foreground font-medium">Recent Keys</p>
            <div className="flex flex-wrap gap-1">
              {statuses.slice(0, 5).map((status) => (
                <Badge 
                  key={status.key} 
                  variant="outline" 
                  className={`text-xs gap-1 ${getModeColor(status.adaptiveStatus.mode)} text-white`}
                >
                  {getModeIcon(status.adaptiveStatus.mode)}
                  {status.key.length > 15 ? status.key.substring(0, 15) + "..." : status.key}
                </Badge>
              ))}
            </div>
          </div>
        )}

        <Link to="/adaptive">
          <Button variant="outline" className="w-full gap-2">
            View Details
            <ChevronRight className="h-4 w-4" />
          </Button>
        </Link>
      </div>
    </Card>
  );
};
