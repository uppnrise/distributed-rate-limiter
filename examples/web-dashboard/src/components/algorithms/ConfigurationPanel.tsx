import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Slider } from "@/components/ui/slider";
import { Button } from "@/components/ui/button";
import { Settings } from "lucide-react";
import { AlgorithmConfig } from "@/types/algorithms";

interface ConfigurationPanelProps {
  config: AlgorithmConfig;
  onChange: (config: AlgorithmConfig) => void;
  onApply: () => void;
}

export const ConfigurationPanel = ({ config, onChange, onApply }: ConfigurationPanelProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-4 flex items-center gap-2">
        <Settings className="h-5 w-5 text-primary" />
        <h3 className="text-lg font-semibold text-foreground">Configuration</h3>
      </div>
      
      <div className="space-y-6">
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>Capacity / Limit</Label>
            <span className="text-sm font-medium text-foreground">{config.capacity}</span>
          </div>
          <Slider
            value={[config.capacity]}
            onValueChange={([value]) => onChange({ ...config, capacity: value })}
            min={1}
            max={100}
            step={1}
            className="py-4"
          />
          <p className="text-xs text-muted-foreground">Maximum tokens or requests allowed</p>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>Refill Rate / Window Size</Label>
            <span className="text-sm font-medium text-foreground">{config.refillRate}</span>
          </div>
          <Slider
            value={[config.refillRate]}
            onValueChange={([value]) => onChange({ ...config, refillRate: value })}
            min={1}
            max={50}
            step={1}
            className="py-4"
          />
          <p className="text-xs text-muted-foreground">Tokens per second or window size</p>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <Label>Time Window (seconds)</Label>
            <span className="text-sm font-medium text-foreground">{config.timeWindow}</span>
          </div>
          <Slider
            value={[config.timeWindow]}
            onValueChange={([value]) => onChange({ ...config, timeWindow: value })}
            min={1}
            max={60}
            step={1}
            className="py-4"
          />
          <p className="text-xs text-muted-foreground">Duration of the time window</p>
        </div>

        <Button onClick={onApply} className="w-full">
          Apply Configuration
        </Button>
      </div>
    </Card>
  );
};
