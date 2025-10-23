import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ArrowDown, Search, CheckCircle2 } from "lucide-react";

export const HierarchyVisualization = () => {
  const [testKey, setTestKey] = useState("");
  const [matchResult, setMatchResult] = useState<{
    level: string;
    config: string;
    priority: number;
  } | null>(null);

  const handleTest = () => {
    // Simulate configuration lookup
    if (testKey.includes("premium")) {
      setMatchResult({
        level: "Pattern Match",
        config: "premium:* (capacity: 100, rate: 20)",
        priority: 2,
      });
    } else if (testKey.includes("user")) {
      setMatchResult({
        level: "Pattern Match",
        config: "user:* (capacity: 50, rate: 10)",
        priority: 2,
      });
    } else if (testKey) {
      setMatchResult({
        level: "Global Default",
        config: "Default (capacity: 10, rate: 5)",
        priority: 3,
      });
    }
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <h3 className="mb-6 text-lg font-semibold text-foreground">
        Configuration Hierarchy
      </h3>

      <div className="space-y-8">
        {/* Hierarchy Diagram */}
        <div className="flex flex-col items-center gap-4">
          <div className="w-full max-w-md">
            <div className="relative rounded-lg border-2 border-green-600 bg-green-50 p-4 dark:bg-green-950/20">
              <Badge className="absolute -top-3 left-3 bg-green-600">Priority 1</Badge>
              <h4 className="font-semibold text-foreground">Per-Key Configuration</h4>
              <p className="text-sm text-muted-foreground">Exact key match (highest priority)</p>
            </div>
          </div>

          <ArrowDown className="h-6 w-6 text-muted-foreground" />

          <div className="w-full max-w-md">
            <div className="relative rounded-lg border-2 border-blue-600 bg-blue-50 p-4 dark:bg-blue-950/20">
              <Badge className="absolute -top-3 left-3 bg-blue-600">Priority 2</Badge>
              <h4 className="font-semibold text-foreground">Pattern-Based Configuration</h4>
              <p className="text-sm text-muted-foreground">Wildcard pattern match</p>
            </div>
          </div>

          <ArrowDown className="h-6 w-6 text-muted-foreground" />

          <div className="w-full max-w-md">
            <div className="relative rounded-lg border-2 border-gray-600 bg-gray-50 p-4 dark:bg-gray-950/20">
              <Badge className="absolute -top-3 left-3 bg-gray-600">Priority 3</Badge>
              <h4 className="font-semibold text-foreground">Global Default</h4>
              <p className="text-sm text-muted-foreground">Fallback configuration</p>
            </div>
          </div>
        </div>

        {/* Test Configuration Tool */}
        <div className="border-t border-border pt-6">
          <h4 className="mb-4 font-semibold text-foreground">Test Configuration Lookup</h4>
          <div className="space-y-4">
            <div className="flex gap-3">
              <div className="flex-1">
                <Label htmlFor="test-key" className="sr-only">Test Key</Label>
                <Input
                  id="test-key"
                  placeholder="Enter a key to test (e.g., user:123, premium:gold)"
                  value={testKey}
                  onChange={(e) => setTestKey(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleTest()}
                />
              </div>
              <Button onClick={handleTest} className="gap-2">
                <Search className="h-4 w-4" />
                Test
              </Button>
            </div>

            {matchResult && (
              <div className="rounded-lg border border-border bg-accent/50 p-4 animate-fade-in">
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="mt-0.5 h-5 w-5 flex-shrink-0 text-green-600" />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-foreground">{matchResult.level}</p>
                      <Badge variant="outline">Priority {matchResult.priority}</Badge>
                    </div>
                    <p className="mt-1 font-mono text-sm text-muted-foreground">
                      {matchResult.config}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};
