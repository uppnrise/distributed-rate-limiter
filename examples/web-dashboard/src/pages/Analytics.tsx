import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Info } from "lucide-react";
import { TimeRangeSelector } from "@/components/analytics/TimeRangeSelector";
import { PerformanceOverview } from "@/components/analytics/PerformanceOverview";
import { UsageTrendsCharts } from "@/components/analytics/UsageTrendsCharts";
import { AlgorithmPerformanceTable } from "@/components/analytics/AlgorithmPerformanceTable";
import { TopKeysTable } from "@/components/analytics/TopKeysTable";
import { AlertsPanel } from "@/components/analytics/AlertsPanel";
import { ExportPanel } from "@/components/analytics/ExportPanel";
import { toast } from "sonner";
import { TimeRange } from "@/types/analytics";
import {
  generateMockPerformanceMetrics,
  generateMockTopKeys,
  generateMockAlgorithmPerformance,
  generateMockUsageData,
  generateMockAlgorithmUsage,
  generateMockAlerts,
} from "@/utils/analyticsData";

const Analytics = () => {
  const [selectedRange, setSelectedRange] = useState<TimeRange>("24h");
  const [customRange, setCustomRange] = useState<{ from: Date; to: Date }>();

  const metrics = generateMockPerformanceMetrics();
  const topKeys = generateMockTopKeys();
  const algorithmPerformance = generateMockAlgorithmPerformance();
  const usageData = generateMockUsageData(24);
  const algorithmUsage = generateMockAlgorithmUsage();
  const alerts = generateMockAlerts();

  const recommendation =
    "Based on your traffic patterns with frequent bursts, Token Bucket algorithm is optimal for your use case. It provides 5% better performance than alternatives.";

  const handleKeyClick = (key: string) => {
    toast.info(`Detailed analytics for ${key} coming soon`);
  };

  const handleExportCSV = () => {
    const csvData = topKeys
      .map(
        (key) =>
          `${key.key},${key.totalRequests},${key.rejectionRate.toFixed(2)},${key.algorithm},${key.lastActive}`
      )
      .join("\n");
    const blob = new Blob(
      [`Key,Total Requests,Rejection Rate,Algorithm,Last Active\n${csvData}`],
      { type: "text/csv" }
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `analytics-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success("CSV exported successfully");
  };

  const handleExportJSON = () => {
    const jsonData = {
      timeRange: selectedRange,
      metrics,
      topKeys,
      algorithmPerformance,
      usageData,
      algorithmUsage,
      alerts,
      exportedAt: new Date().toISOString(),
    };
    const blob = new Blob([JSON.stringify(jsonData, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `analytics-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success("JSON exported successfully");
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="text-3xl font-bold tracking-tight text-foreground">Analytics</h2>
        <p className="text-muted-foreground">
          Historical performance data and trends analysis
        </p>
      </div>

      {/* Demo Data Notice */}
      <Alert className="border-blue-500/50 bg-blue-500/10">
        <Info className="h-4 w-4 text-blue-600" />
        <AlertDescription className="text-blue-900 dark:text-blue-100">
          <strong>Demo Data:</strong> This page displays simulated analytics data for preview purposes. 
          Historical analytics features require a time-series database backend (InfluxDB, Prometheus, or TimescaleDB) 
          with data aggregation endpoints. See the{" "}
          <a 
            href="https://github.com/uppnrise/distributed-rate-limiter/blob/main/examples/web-dashboard/README.md#analytics-feature-roadmap" 
            target="_blank" 
            rel="noopener noreferrer"
            className="underline font-semibold hover:text-blue-700 dark:hover:text-blue-300"
          >
            Analytics Roadmap
          </a>
          {" "}for implementation details.
        </AlertDescription>
      </Alert>

      <TimeRangeSelector
        selectedRange={selectedRange}
        onRangeChange={setSelectedRange}
        customRange={customRange}
        onCustomRangeChange={setCustomRange}
      />

      <PerformanceOverview metrics={metrics} topKeys={topKeys} />

      <UsageTrendsCharts usageData={usageData} algorithmUsage={algorithmUsage} />

      <AlgorithmPerformanceTable
        performance={algorithmPerformance}
        recommendation={recommendation}
      />

      <TopKeysTable keys={topKeys} onKeyClick={handleKeyClick} />

      <div className="grid gap-6 lg:grid-cols-2">
        <AlertsPanel alerts={alerts} />
        <ExportPanel onExportCSV={handleExportCSV} onExportJSON={handleExportJSON} />
      </div>
    </div>
  );
};

export default Analytics;
