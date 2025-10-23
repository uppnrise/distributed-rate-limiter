import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Play, Square } from "lucide-react";
import { LoadTestMetrics } from "@/types/loadTesting";

interface TestExecutionProps {
  isRunning: boolean;
  progress: number;
  metrics: LoadTestMetrics;
  onStart: () => void;
  onStop: () => void;
}

export const TestExecution = ({
  isRunning,
  progress,
  metrics,
  onStart,
  onStop,
}: TestExecutionProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="space-y-6">
        <div className="text-center">
          {!isRunning ? (
            <Button size="lg" onClick={onStart} className="gap-2 px-12">
              <Play className="h-5 w-5" />
              Start Load Test
            </Button>
          ) : (
            <Button
              size="lg"
              onClick={onStop}
              variant="destructive"
              className="gap-2 px-12"
            >
              <Square className="h-5 w-5" />
              Stop Test
            </Button>
          )}
        </div>

        {isRunning && (
          <>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Test Progress</span>
                <span className="font-medium text-foreground">{progress}%</span>
              </div>
              <Progress value={progress} className="h-3" />
            </div>

            <div className="grid gap-4 md:grid-cols-4">
              <div className="rounded-lg bg-gradient-to-br from-blue-50 to-blue-100 p-4 dark:from-blue-950/30 dark:to-blue-900/20">
                <p className="text-sm text-muted-foreground">Requests Sent</p>
                <p className="mt-1 text-3xl font-bold text-foreground">
                  {metrics.requestsSent.toLocaleString()}
                </p>
              </div>

              <div className="rounded-lg bg-gradient-to-br from-green-50 to-green-100 p-4 dark:from-green-950/30 dark:to-green-900/20">
                <p className="text-sm text-muted-foreground">Successful</p>
                <p className="mt-1 text-3xl font-bold text-green-600">
                  {metrics.successful.toLocaleString()}
                </p>
              </div>

              <div className="rounded-lg bg-gradient-to-br from-red-50 to-red-100 p-4 dark:from-red-950/30 dark:to-red-900/20">
                <p className="text-sm text-muted-foreground">Rate Limited</p>
                <p className="mt-1 text-3xl font-bold text-red-600">
                  {metrics.rateLimited.toLocaleString()}
                </p>
              </div>

              <div className="rounded-lg bg-gradient-to-br from-gray-50 to-gray-100 p-4 dark:from-gray-950/30 dark:to-gray-900/20">
                <p className="text-sm text-muted-foreground">Failed</p>
                <p className="mt-1 text-3xl font-bold text-foreground">
                  {metrics.failed.toLocaleString()}
                </p>
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-3 border-t border-border pt-4">
              <div>
                <p className="text-sm text-muted-foreground">Current Rate</p>
                <p className="mt-1 text-xl font-semibold text-foreground">
                  {metrics.currentRate} req/s
                </p>
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Success Rate</p>
                <p className="mt-1 text-xl font-semibold text-foreground">
                  {metrics.successRate.toFixed(1)}%
                </p>
              </div>

              <div>
                <p className="text-sm text-muted-foreground">Avg Response</p>
                <p className="mt-1 text-xl font-semibold text-foreground">
                  {metrics.avgResponseTime.toFixed(1)}ms
                </p>
              </div>
            </div>
          </>
        )}
      </div>
    </Card>
  );
};
