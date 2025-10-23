import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Slider } from "@/components/ui/slider";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { ChevronDown, Zap, Clock, Cpu } from "lucide-react";
import { LoadTestConfig } from "@/types/loadTesting";
import { useState } from "react";

interface AdvancedSettingsProps {
  config: LoadTestConfig;
  onChange: (config: LoadTestConfig) => void;
}

export const AdvancedSettings = ({ config, onChange }: AdvancedSettingsProps) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <Card className="shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <CollapsibleTrigger className={`flex w-full items-center justify-between hover:bg-accent/50 transition-all ${isOpen ? 'p-6' : 'p-4'}`}>
          <div className="flex-1 min-w-0">
            <h3 className={`font-semibold text-foreground ${isOpen ? 'text-lg mb-0' : 'text-base mb-2'}`}>
              Advanced Settings
            </h3>
            {!isOpen && (
              <div className="flex flex-wrap gap-2">
                <Badge variant="secondary" className="text-xs flex items-center gap-1.5">
                  <Zap className="h-3 w-3" />
                  {config.tokensPerRequest} token{config.tokensPerRequest !== 1 ? 's' : ''}
                </Badge>
                <Badge variant="secondary" className="text-xs flex items-center gap-1.5">
                  <Clock className="h-3 w-3" />
                  {config.timeout}ms
                </Badge>
                <Badge variant="secondary" className="text-xs flex items-center gap-1.5">
                  <Cpu className="h-3 w-3" />
                  {config.algorithmOverride || 'Default'}
                </Badge>
              </div>
            )}
          </div>
          <ChevronDown
            className={`h-5 w-5 text-muted-foreground transition-transform flex-shrink-0 ml-3 ${
              isOpen ? "rotate-180" : ""
            }`}
          />
        </CollapsibleTrigger>

        <CollapsibleContent>
          <div className="space-y-6 border-t border-border p-6">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>Tokens per Request</Label>
                <span className="text-sm font-medium text-foreground">
                  {config.tokensPerRequest}
                </span>
              </div>
              <Slider
                value={[config.tokensPerRequest]}
                onValueChange={([value]) =>
                  onChange({ ...config, tokensPerRequest: value })
                }
                min={1}
                max={10}
                step={1}
                className="py-4"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="algorithm-override">Algorithm Override</Label>
              <Select
                value={config.algorithmOverride || "none"}
                onValueChange={(value) =>
                  onChange({
                    ...config,
                    algorithmOverride: value === "none" ? undefined : value,
                  })
                }
              >
                <SelectTrigger id="algorithm-override">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None (use default)</SelectItem>
                  <SelectItem value="token-bucket">Token Bucket</SelectItem>
                  <SelectItem value="sliding-window">Sliding Window</SelectItem>
                  <SelectItem value="fixed-window">Fixed Window</SelectItem>
                  <SelectItem value="leaky-bucket">Leaky Bucket</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>Request Timeout (ms)</Label>
                <span className="text-sm font-medium text-foreground">
                  {config.timeout}
                </span>
              </div>
              <Slider
                value={[config.timeout]}
                onValueChange={([value]) => onChange({ ...config, timeout: value })}
                min={100}
                max={10000}
                step={100}
                className="py-4"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="custom-headers">Custom Headers (JSON)</Label>
              <Input
                id="custom-headers"
                placeholder='{"X-Custom-Header": "value"}'
                defaultValue={JSON.stringify(config.customHeaders || {})}
                onBlur={(e) => {
                  try {
                    const headers = JSON.parse(e.target.value || "{}");
                    onChange({ ...config, customHeaders: headers });
                  } catch {
                    // Invalid JSON, ignore
                  }
                }}
              />
            </div>
          </div>
        </CollapsibleContent>
      </Collapsible>
    </Card>
  );
};
