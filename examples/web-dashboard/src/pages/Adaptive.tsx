import { useState, useEffect, useCallback } from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Progress } from "@/components/ui/progress";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Brain,
  Activity,
  Shield,
  Zap,
  Settings,
  RefreshCw,
  AlertTriangle,
  CheckCircle,
  Info,
  TrendingUp,
  TrendingDown,
  Minus,
  Edit3,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import type { AdaptiveStatus, AdaptiveConfig, AdaptiveOverrideRequest } from "@/types/adaptive";
import { useApp } from "@/contexts/AppContext";

const Adaptive = () => {
  const { isConnected } = useApp();
  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState<AdaptiveConfig | null>(null);
  const [adaptiveStatuses, setAdaptiveStatuses] = useState<AdaptiveStatus[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [overrideDialogOpen, setOverrideDialogOpen] = useState(false);
  const [overrideForm, setOverrideForm] = useState<AdaptiveOverrideRequest>({
    capacity: 100,
    refillRate: 10,
    reason: "",
  });

  const loadData = useCallback(async () => {
    try {
      const [configData, statusesData] = await Promise.all([
        rateLimiterApi.getAdaptiveConfig(),
        rateLimiterApi.getAdaptiveStatusForAllKeys(),
      ]);
      setConfig(configData);
      setAdaptiveStatuses(statusesData);
    } catch (error) {
      console.error("Failed to load adaptive data:", error);
      toast.error("Failed to load adaptive rate limiting data");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    // Auto-refresh every 30 seconds
    const interval = setInterval(loadData, 30000);
    return () => clearInterval(interval);
  }, [loadData]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
    toast.success("Data refreshed");
  };

  const handleSetOverride = async () => {
    if (!selectedKey) return;
    
    try {
      await rateLimiterApi.setAdaptiveOverride(selectedKey, overrideForm);
      toast.success(`Override set for ${selectedKey}`);
      setOverrideDialogOpen(false);
      setSelectedKey(null);
      setOverrideForm({ capacity: 100, refillRate: 10, reason: "" });
      await loadData();
    } catch (error) {
      console.error("Failed to set override:", error);
      toast.error("Failed to set override");
    }
  };

  const handleRemoveOverride = async (key: string) => {
    try {
      await rateLimiterApi.removeAdaptiveOverride(key);
      toast.success(`Override removed for ${key}`);
      await loadData();
    } catch (error) {
      console.error("Failed to remove override:", error);
      toast.error("Failed to remove override");
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

  const getModeIcon = (mode: string) => {
    switch (mode) {
      case "ADAPTIVE":
        return <Brain className="h-4 w-4" />;
      case "LEARNING":
        return <Activity className="h-4 w-4" />;
      case "OVERRIDE":
        return <Shield className="h-4 w-4" />;
      default:
        return <Settings className="h-4 w-4" />;
    }
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return "text-green-600";
    if (confidence >= 0.6) return "text-yellow-600";
    return "text-red-600";
  };

  const formatEvaluationInterval = (ms: number) => {
    const minutes = Math.floor(ms / 60000);
    if (minutes >= 60) {
      return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
    }
    return `${minutes}m`;
  };

  if (loading) {
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center justify-center h-64">
          <div className="flex items-center gap-2">
            <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            <span className="text-muted-foreground">Loading adaptive rate limiting data...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight text-foreground flex items-center gap-3">
            <Brain className="h-8 w-8 text-primary" />
            Adaptive Rate Limiting
          </h2>
          <p className="text-muted-foreground">
            ML-driven automatic rate limit optimization based on traffic patterns and system metrics
          </p>
        </div>
        <Button
          onClick={handleRefresh}
          variant="outline"
          disabled={refreshing}
          className="gap-2"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      {/* Status Banner */}
      {config && (
        <Alert className={config.enabled ? "border-green-500/50 bg-green-500/10" : "border-yellow-500/50 bg-yellow-500/10"}>
          {config.enabled ? (
            <CheckCircle className="h-4 w-4 text-green-600" />
          ) : (
            <AlertTriangle className="h-4 w-4 text-yellow-600" />
          )}
          <AlertDescription className={config.enabled ? "text-green-900 dark:text-green-100" : "text-yellow-900 dark:text-yellow-100"}>
            {config.enabled ? (
              <>
                <strong>Adaptive Rate Limiting is ENABLED.</strong> The system is actively adjusting rate limits
                based on traffic patterns and system metrics. Evaluations occur every{" "}
                {formatEvaluationInterval(config.evaluationIntervalMs)}.
              </>
            ) : (
              <>
                <strong>Adaptive Rate Limiting is DISABLED.</strong> Set{" "}
                <code className="bg-yellow-200/50 dark:bg-yellow-800/50 px-1 rounded">
                  ratelimiter.adaptive.enabled=true
                </code>{" "}
                in application.properties to enable ML-driven rate limiting.
              </>
            )}
          </AlertDescription>
        </Alert>
      )}

      {/* Configuration Overview */}
      {config && (
        <Card className="p-6">
          <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <Settings className="h-5 w-5" />
            Configuration
          </h3>
          <div className="grid gap-4 md:grid-cols-3 lg:grid-cols-6">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Status</Label>
              <div className="flex items-center gap-2">
                <Badge variant={config.enabled ? "default" : "secondary"}>
                  {config.enabled ? "Enabled" : "Disabled"}
                </Badge>
              </div>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Evaluation Interval</Label>
              <p className="font-medium">{formatEvaluationInterval(config.evaluationIntervalMs)}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Min Confidence</Label>
              <p className="font-medium">{(config.minConfidenceThreshold * 100).toFixed(0)}%</p>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Max Adjustment</Label>
              <p className="font-medium">{config.maxAdjustmentFactor}x</p>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Min Capacity</Label>
              <p className="font-medium">{config.minCapacity.toLocaleString()}</p>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Max Capacity</Label>
              <p className="font-medium">{config.maxCapacity.toLocaleString()}</p>
            </div>
          </div>
        </Card>
      )}

      {/* Stats Overview */}
      <div className="grid gap-6 md:grid-cols-4">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Total Keys</p>
              <p className="text-3xl font-bold">{adaptiveStatuses.length}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
              <Zap className="h-6 w-6 text-primary" />
            </div>
          </div>
        </Card>
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Adaptive Mode</p>
              <p className="text-3xl font-bold">
                {adaptiveStatuses.filter(s => s.adaptiveStatus.mode === "ADAPTIVE").length}
              </p>
            </div>
            <div className="h-12 w-12 rounded-full bg-green-500/10 flex items-center justify-center">
              <Brain className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </Card>
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Learning Mode</p>
              <p className="text-3xl font-bold">
                {adaptiveStatuses.filter(s => s.adaptiveStatus.mode === "LEARNING").length}
              </p>
            </div>
            <div className="h-12 w-12 rounded-full bg-blue-500/10 flex items-center justify-center">
              <Activity className="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </Card>
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground">Manual Overrides</p>
              <p className="text-3xl font-bold">
                {adaptiveStatuses.filter(s => s.adaptiveStatus.mode === "OVERRIDE").length}
              </p>
            </div>
            <div className="h-12 w-12 rounded-full bg-orange-500/10 flex items-center justify-center">
              <Shield className="h-6 w-6 text-orange-600" />
            </div>
          </div>
        </Card>
      </div>

      {/* Keys Table */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold flex items-center gap-2">
            <Activity className="h-5 w-5" />
            Adaptive Keys Status
          </h3>
          {!isConnected && (
            <Badge variant="destructive" className="gap-1">
              <AlertTriangle className="h-3 w-3" />
              Disconnected
            </Badge>
          )}
        </div>

        {adaptiveStatuses.length === 0 ? (
          <div className="text-center py-12">
            <Info className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground mb-2">No adaptive keys found</p>
            <p className="text-sm text-muted-foreground">
              Make some rate limit requests to see adaptive status for keys.
            </p>
          </div>
        ) : (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Key</TableHead>
                  <TableHead>Mode</TableHead>
                  <TableHead>Confidence</TableHead>
                  <TableHead>Current Limits</TableHead>
                  <TableHead>Reasoning</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {adaptiveStatuses.map((status) => (
                  <TableRow key={status.key}>
                    <TableCell className="font-mono text-sm">{status.key}</TableCell>
                    <TableCell>
                      <Badge className={`gap-1 ${getModeColor(status.adaptiveStatus.mode)}`}>
                        {getModeIcon(status.adaptiveStatus.mode)}
                        {status.adaptiveStatus.mode}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Progress
                          value={status.adaptiveStatus.confidence * 100}
                          className="w-16 h-2"
                        />
                        <span className={`text-sm font-medium ${getConfidenceColor(status.adaptiveStatus.confidence)}`}>
                          {(status.adaptiveStatus.confidence * 100).toFixed(0)}%
                        </span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        <div>Capacity: <span className="font-medium">{status.currentLimits.capacity}</span></div>
                        <div>Refill: <span className="font-medium">{status.currentLimits.refillRate}/s</span></div>
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs">
                      <div className="text-sm text-muted-foreground truncate">
                        {Object.entries(status.adaptiveStatus.reasoning || {}).map(([key, value]) => (
                          <div key={key} className="truncate" title={`${key}: ${value}`}>
                            <span className="font-medium">{key}:</span> {value}
                          </div>
                        )).slice(0, 2)}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        {status.adaptiveStatus.mode === "OVERRIDE" ? (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleRemoveOverride(status.key)}
                            className="gap-1"
                          >
                            <X className="h-3 w-3" />
                            Remove Override
                          </Button>
                        ) : (
                          <Dialog open={overrideDialogOpen && selectedKey === status.key} onOpenChange={(open) => {
                            setOverrideDialogOpen(open);
                            if (open) {
                              setSelectedKey(status.key);
                              setOverrideForm({
                                capacity: status.currentLimits.capacity,
                                refillRate: status.currentLimits.refillRate,
                                reason: "",
                              });
                            } else {
                              setSelectedKey(null);
                            }
                          }}>
                            <DialogTrigger asChild>
                              <Button size="sm" variant="outline" className="gap-1">
                                <Edit3 className="h-3 w-3" />
                                Override
                              </Button>
                            </DialogTrigger>
                            <DialogContent>
                              <DialogHeader>
                                <DialogTitle>Set Manual Override</DialogTitle>
                                <DialogDescription>
                                  Override adaptive limits for key: <code className="bg-muted px-1 rounded">{status.key}</code>
                                </DialogDescription>
                              </DialogHeader>
                              <div className="grid gap-4 py-4">
                                <div className="grid gap-2">
                                  <Label htmlFor="capacity">Capacity</Label>
                                  <Input
                                    id="capacity"
                                    type="number"
                                    value={overrideForm.capacity}
                                    onChange={(e) => setOverrideForm({ ...overrideForm, capacity: parseInt(e.target.value, 10) || 0 })}
                                  />
                                </div>
                                <div className="grid gap-2">
                                  <Label htmlFor="refillRate">Refill Rate (per second)</Label>
                                  <Input
                                    id="refillRate"
                                    type="number"
                                    value={overrideForm.refillRate}
                                    onChange={(e) => setOverrideForm({ ...overrideForm, refillRate: parseInt(e.target.value, 10) || 0 })}
                                  />
                                </div>
                                <div className="grid gap-2">
                                  <Label htmlFor="reason">Reason</Label>
                                  <Textarea
                                    id="reason"
                                    placeholder="Enter reason for override..."
                                    value={overrideForm.reason}
                                    onChange={(e) => setOverrideForm({ ...overrideForm, reason: e.target.value })}
                                  />
                                </div>
                              </div>
                              <DialogFooter>
                                <Button variant="outline" onClick={() => setOverrideDialogOpen(false)}>
                                  Cancel
                                </Button>
                                <Button onClick={handleSetOverride} disabled={!overrideForm.reason}>
                                  Set Override
                                </Button>
                              </DialogFooter>
                            </DialogContent>
                          </Dialog>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </Card>

      {/* Help Section */}
      <Card className="p-6 bg-gradient-to-br from-card to-muted/30">
        <h3 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Info className="h-5 w-5" />
          Understanding Adaptive Modes
        </h3>
        <div className="grid gap-4 md:grid-cols-4">
          <div className="flex items-start gap-3">
            <div className="h-8 w-8 rounded-full bg-gray-500/20 flex items-center justify-center flex-shrink-0">
              <Settings className="h-4 w-4 text-gray-500" />
            </div>
            <div>
              <p className="font-medium">STATIC</p>
              <p className="text-sm text-muted-foreground">Using default static configuration</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="h-8 w-8 rounded-full bg-blue-500/20 flex items-center justify-center flex-shrink-0">
              <Activity className="h-4 w-4 text-blue-500" />
            </div>
            <div>
              <p className="font-medium">LEARNING</p>
              <p className="text-sm text-muted-foreground">Collecting traffic patterns and system metrics</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="h-8 w-8 rounded-full bg-green-500/20 flex items-center justify-center flex-shrink-0">
              <Brain className="h-4 w-4 text-green-500" />
            </div>
            <div>
              <p className="font-medium">ADAPTIVE</p>
              <p className="text-sm text-muted-foreground">Actively adjusting limits based on ML analysis</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <div className="h-8 w-8 rounded-full bg-orange-500/20 flex items-center justify-center flex-shrink-0">
              <Shield className="h-4 w-4 text-orange-500" />
            </div>
            <div>
              <p className="font-medium">OVERRIDE</p>
              <p className="text-sm text-muted-foreground">Manual override by administrator</p>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default Adaptive;
