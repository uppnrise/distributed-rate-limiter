import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Star } from "lucide-react";
import { AlgorithmPerformance } from "@/types/analytics";

interface AlgorithmPerformanceTableProps {
  performance: AlgorithmPerformance[];
  recommendation?: string;
}

export const AlgorithmPerformanceTable = ({
  performance,
  recommendation,
}: AlgorithmPerformanceTableProps) => {
  const renderStars = (rating: number) => {
    return (
      <div className="flex gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            className={`h-4 w-4 ${
              star <= rating
                ? "fill-yellow-500 text-yellow-500"
                : "text-muted-foreground"
            }`}
          />
        ))}
      </div>
    );
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-foreground">
          Algorithm Performance Analysis
        </h3>
        {recommendation && (
          <div className="mt-3 rounded-lg bg-primary/10 p-3">
            <p className="text-sm font-medium text-primary">💡 Recommendation</p>
            <p className="mt-1 text-sm text-muted-foreground">{recommendation}</p>
          </div>
        )}
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Algorithm</TableHead>
              <TableHead className="text-right">Requests Handled</TableHead>
              <TableHead className="text-right">Success Rate</TableHead>
              <TableHead className="text-right">Avg Response Time</TableHead>
              <TableHead className="text-right">Memory Usage</TableHead>
              <TableHead className="text-right">Efficiency</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {performance.map((algo) => (
              <TableRow key={algo.algorithm} className="hover:bg-accent/50">
                <TableCell className="font-medium">{algo.algorithm}</TableCell>
                <TableCell className="text-right">
                  {algo.requestsHandled.toLocaleString()}
                </TableCell>
                <TableCell className="text-right">
                  <Badge
                    variant={algo.successRate > 95 ? "default" : "secondary"}
                    className="font-mono"
                  >
                    {algo.successRate.toFixed(1)}%
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  {algo.avgResponseTime.toFixed(1)}ms
                </TableCell>
                <TableCell className="text-right">{algo.memoryUsage}MB</TableCell>
                <TableCell className="text-right">
                  {renderStars(algo.efficiency)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </Card>
  );
};
