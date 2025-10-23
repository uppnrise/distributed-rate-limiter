import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Key, Plus, Settings } from "lucide-react";
import { ApiKey } from "@/types/apiKeys";

interface KeysOverviewProps {
  keys: ApiKey[];
  onCreateNew: () => void;
  onBulkOperations: () => void;
}

export const KeysOverview = ({ keys, onCreateNew, onBulkOperations }: KeysOverviewProps) => {
  const totalKeys = keys.length;
  const activeKeys = keys.filter((k) => k.status === "active").length;
  const expiredKeys = keys.filter((k) => k.status === "expired").length;
  const usageThisMonth = keys.reduce((sum, k) => sum + k.usageStats.totalRequests, 0);

  const summaryCards = [
    { label: "Total Keys", value: totalKeys, color: "text-primary" },
    { label: "Active Keys", value: activeKeys, color: "text-green-600" },
    { label: "Expired Keys", value: expiredKeys, color: "text-red-600" },
    { label: "Usage This Month", value: usageThisMonth.toLocaleString(), color: "text-blue-600" },
  ];

  return (
    <div className="space-y-6">
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
        {summaryCards.map((card) => (
          <Card
            key={card.label}
            className="relative overflow-hidden p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50"
          >
            <div className="absolute right-0 top-0 h-24 w-24 translate-x-6 -translate-y-6 rounded-full bg-primary/5" />
            <div className="relative">
              <p className="text-sm font-medium text-muted-foreground">{card.label}</p>
              <p className={`mt-2 text-3xl font-bold ${card.color}`}>{card.value}</p>
            </div>
          </Card>
        ))}
      </div>

      <div className="flex gap-3">
        <Button onClick={onCreateNew} className="gap-2">
          <Plus className="h-4 w-4" />
          Generate New Key
        </Button>
        <Button onClick={onBulkOperations} variant="outline" className="gap-2">
          <Settings className="h-4 w-4" />
          Bulk Operations
        </Button>
      </div>
    </div>
  );
};
