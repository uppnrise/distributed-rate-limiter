import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Play, Square, Zap } from "lucide-react";
import { TrafficPattern } from "@/types/algorithms";

interface TrafficSimulationProps {
  isRunning: boolean;
  pattern: TrafficPattern;
  speed: number;
  onPatternChange: (pattern: TrafficPattern) => void;
  onSpeedChange: (speed: number) => void;
  onStart: () => void;
  onStop: () => void;
}

export const TrafficSimulation = ({
  isRunning,
  pattern,
  speed,
  onPatternChange,
  onSpeedChange,
  onStart,
  onStop,
}: TrafficSimulationProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-4 flex items-center gap-2">
        <Zap className="h-5 w-5 text-primary" />
        <h3 className="text-lg font-semibold text-foreground">Traffic Simulation</h3>
      </div>
      
      <div className="space-y-6">
        <div className="space-y-3">
          <Label>Traffic Pattern</Label>
          <Select value={pattern} onValueChange={(value) => onPatternChange(value as TrafficPattern)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="steady">Steady - Consistent rate</SelectItem>
              <SelectItem value="bursty">Bursty - Irregular spikes</SelectItem>
              <SelectItem value="spike">Spike - Sudden high load</SelectItem>
              <SelectItem value="custom">Custom - Manual control</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>Simulation Speed</Label>
            <span className="text-sm font-medium text-foreground">{speed}x</span>
          </div>
          <Slider
            value={[speed]}
            onValueChange={([value]) => onSpeedChange(value)}
            min={0.5}
            max={5}
            step={0.5}
            className="py-4"
          />
        </div>

        <div className="flex gap-3">
          {!isRunning ? (
            <Button onClick={onStart} className="flex-1 gap-2">
              <Play className="h-4 w-4" />
              Start Simulation
            </Button>
          ) : (
            <Button onClick={onStop} variant="destructive" className="flex-1 gap-2">
              <Square className="h-4 w-4" />
              Stop Simulation
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
};
