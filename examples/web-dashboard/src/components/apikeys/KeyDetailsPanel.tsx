import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { Edit, Trash2, RefreshCw } from "lucide-react";
import { ApiKey, KeyAccessLog } from "@/types/apiKeys";

interface KeyDetailsPanelProps {
  keyData: ApiKey | null;
  accessLogs: KeyAccessLog[];
  open: boolean;
  onClose: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onRegenerate: () => void;
}

export const KeyDetailsPanel = ({
  keyData,
  accessLogs,
  open,
  onClose,
  onEdit,
  onDelete,
  onRegenerate,
}: KeyDetailsPanelProps) => {
  if (!keyData) return null;

  const usageData = [
    { time: "00:00", requests: 120 },
    { time: "04:00", requests: 80 },
    { time: "08:00", requests: 200 },
    { time: "12:00", requests: 350 },
    { time: "16:00", requests: 280 },
    { time: "20:00", requests: 150 },
  ];

  return (
    <Sheet open={open} onOpenChange={onClose}>
      <SheetContent className="w-full sm:max-w-2xl">
        <SheetHeader>
          <SheetTitle>{keyData.name}</SheetTitle>
          <SheetDescription>Complete API key information and statistics</SheetDescription>
        </SheetHeader>

        <ScrollArea className="h-[calc(100vh-120px)] pr-4">
          <div className="space-y-6 pt-6">
            {/* Key Information */}
            <div className="space-y-4">
              <h3 className="font-semibold">Key Information</h3>
              <div className="grid gap-3">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Status</span>
                  <Badge>{keyData.status}</Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Created</span>
                  <span className="text-sm">{new Date(keyData.createdAt).toLocaleString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Last Used</span>
                  <span className="text-sm">
                    {keyData.lastUsed ? new Date(keyData.lastUsed).toLocaleString() : "Never"}
                  </span>
                </div>
                {keyData.expiresAt && (
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">Expires</span>
                    <span className="text-sm">{new Date(keyData.expiresAt).toLocaleString()}</span>
                  </div>
                )}
              </div>
            </div>

            <Separator />

            {/* Usage Statistics */}
            <div className="space-y-4">
              <h3 className="font-semibold">Usage Statistics</h3>
              <div className="grid gap-4 md:grid-cols-3">
                <div className="rounded-lg border border-border bg-background/50 p-3">
                  <p className="text-xs text-muted-foreground">Total Requests</p>
                  <p className="text-2xl font-bold">
                    {keyData.usageStats.totalRequests.toLocaleString()}
                  </p>
                </div>
                <div className="rounded-lg border border-border bg-background/50 p-3">
                  <p className="text-xs text-muted-foreground">Successful</p>
                  <p className="text-2xl font-bold text-green-600">
                    {keyData.usageStats.successfulRequests.toLocaleString()}
                  </p>
                </div>
                <div className="rounded-lg border border-border bg-background/50 p-3">
                  <p className="text-xs text-muted-foreground">Rate Limited</p>
                  <p className="text-2xl font-bold text-red-600">
                    {keyData.usageStats.rateLimitedRequests.toLocaleString()}
                  </p>
                </div>
              </div>

              <div className="rounded-lg border border-border p-4">
                <h4 className="mb-3 text-sm font-medium">Usage Over Time</h4>
                <ResponsiveContainer width="100%" height={200}>
                  <LineChart data={usageData}>
                    <CartesianGrid strokeDasharray="3 3" opacity={0.3} />
                    <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} />
                    <Tooltip />
                    <Line
                      type="monotone"
                      dataKey="requests"
                      stroke="hsl(var(--primary))"
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>

            <Separator />

            {/* Rate Limiting Configuration */}
            <div className="space-y-4">
              <h3 className="font-semibold">Rate Limiting Configuration</h3>
              <div className="grid gap-3">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Algorithm</span>
                  <Badge variant="outline">{keyData.rateLimit.algorithm}</Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Capacity</span>
                  <span className="text-sm font-medium">{keyData.rateLimit.capacity}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">Refill Rate</span>
                  <span className="text-sm font-medium">
                    {keyData.rateLimit.refillRate} tokens/sec
                  </span>
                </div>
              </div>
            </div>

            <Separator />

            {/* Access Logs */}
            <div className="space-y-4">
              <h3 className="font-semibold">Recent Access Logs</h3>
              <div className="space-y-2">
                {accessLogs.slice(0, 10).map((log) => (
                  <div
                    key={log.id}
                    className="flex items-center justify-between rounded-lg border border-border bg-background/50 p-3 text-sm"
                  >
                    <div>
                      <p className="font-medium">{log.endpoint}</p>
                      <p className="text-xs text-muted-foreground">
                        {new Date(log.timestamp).toLocaleString()} • {log.ipAddress}
                      </p>
                    </div>
                    <div className="text-right">
                      <Badge
                        variant={log.statusCode < 300 ? "default" : "destructive"}
                        className="font-mono text-xs"
                      >
                        {log.statusCode}
                      </Badge>
                      <p className="text-xs text-muted-foreground">{log.responseTime}ms</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Actions */}
            <div className="flex gap-2 pt-4">
              <Button onClick={onEdit} variant="outline" className="gap-2">
                <Edit className="h-4 w-4" />
                Edit
              </Button>
              <Button onClick={onRegenerate} variant="outline" className="gap-2">
                <RefreshCw className="h-4 w-4" />
                Regenerate
              </Button>
              <Button onClick={onDelete} variant="destructive" className="gap-2">
                <Trash2 className="h-4 w-4" />
                Delete
              </Button>
            </div>
          </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  );
};
