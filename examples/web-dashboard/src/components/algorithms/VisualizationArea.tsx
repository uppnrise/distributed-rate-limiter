import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { AlgorithmType } from "@/types/algorithms";
import { Info } from "lucide-react";

interface VisualizationData {
  time: number;
  [key: string]: number;
}

interface VisualizationAreaProps {
  selectedAlgorithms: AlgorithmType[];
  data: VisualizationData[];
  lastEvents: Map<AlgorithmType, { allowed: boolean; reason: string }>;
}

const algorithmColors = {
  "token-bucket": "#3b82f6",
  "sliding-window": "#a855f7",
  "fixed-window": "#22c55e",
  "leaky-bucket": "#f97316",
};

const algorithmNames = {
  "token-bucket": "Token Bucket",
  "sliding-window": "Sliding Window",
  "fixed-window": "Fixed Window",
  "leaky-bucket": "Leaky Bucket",
};

export const VisualizationArea = ({
  selectedAlgorithms,
  data,
  lastEvents,
}: VisualizationAreaProps) => {
  if (selectedAlgorithms.length === 0) {
    return (
      <Card className="p-12 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50 text-center">
        <p className="text-muted-foreground">
          Select at least one algorithm to start visualization
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <div className="mb-6 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-foreground">Real-time Visualization</h3>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 text-xs">
              <div className="h-2 w-2 rounded-full bg-green-600" />
              <span className="text-muted-foreground">Allowed</span>
            </div>
            <div className="flex items-center gap-2 text-xs">
              <div className="h-2 w-2 rounded-full bg-red-600" />
              <span className="text-muted-foreground">Rejected</span>
            </div>
          </div>
        </div>

        <ResponsiveContainer width="100%" height={400}>
          <LineChart data={data}>
            <CartesianGrid strokeDasharray="3 3" className="stroke-muted" opacity={0.3} />
            <XAxis
              dataKey="time"
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
              label={{ value: "Time (s)", position: "insideBottom", offset: -5 }}
            />
            <YAxis
              className="text-muted-foreground"
              tick={{ fontSize: 12 }}
              label={{ value: "Tokens / Requests", angle: -90, position: "insideLeft" }}
            />
            <RechartsTooltip
              contentStyle={{
                backgroundColor: "hsl(var(--card))",
                border: "1px solid hsl(var(--border))",
                borderRadius: "8px",
              }}
            />
            <Legend />
            {selectedAlgorithms.map((algo) => (
              <Line
                key={algo}
                type="monotone"
                dataKey={algo}
                stroke={algorithmColors[algo]}
                strokeWidth={2}
                dot={{ r: 2 }}
                name={algorithmNames[algo]}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </Card>

      {/* Event Explanations */}
      {selectedAlgorithms.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2">
          {selectedAlgorithms.map((algo) => {
            const event = lastEvents.get(algo);
            if (!event) return null;

            return (
              <Card
                key={algo}
                className="p-4 shadow-card backdrop-blur-sm bg-gradient-to-br from-card to-card/50"
              >
                <div className="flex items-start gap-3">
                  <div
                    className="mt-1 h-8 w-1 rounded-full"
                    style={{ backgroundColor: algorithmColors[algo] }}
                  />
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <h4 className="font-semibold text-foreground">{algorithmNames[algo]}</h4>
                      <Badge variant={event.allowed ? "default" : "destructive"}>
                        {event.allowed ? "Allowed" : "Rejected"}
                      </Badge>
                    </div>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <p className="mt-2 flex items-start gap-2 text-sm text-muted-foreground">
                          <Info className="mt-0.5 h-4 w-4 flex-shrink-0" />
                          {event.reason}
                        </p>
                      </TooltipTrigger>
                      <TooltipContent className="max-w-xs">
                        <p>{event.reason}</p>
                      </TooltipContent>
                    </Tooltip>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};
