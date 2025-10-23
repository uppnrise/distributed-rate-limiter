import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { History, Star, Trash2 } from "lucide-react";
import { LoadTestResult } from "@/types/loadTesting";

interface HistoricalTestsProps {
  results: LoadTestResult[];
  onDelete: (id: string) => void;
  onFavorite: (id: string) => void;
}

export const HistoricalTests = ({
  results,
  onDelete,
  onFavorite,
}: HistoricalTestsProps) => {
  if (results.length === 0) {
    return null;
  }

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-6 flex items-center gap-2">
        <History className="h-5 w-5 text-primary" />
        <h3 className="text-lg font-semibold text-foreground">Historical Tests</h3>
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Test ID</TableHead>
              <TableHead>Target Key</TableHead>
              <TableHead>Duration</TableHead>
              <TableHead>Requests</TableHead>
              <TableHead>Success Rate</TableHead>
              <TableHead>Completed At</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {results.map((result) => (
              <TableRow key={result.id} className="hover:bg-accent/50">
                <TableCell className="font-mono text-sm">
                  {result.id.substring(0, 8)}...
                </TableCell>
                <TableCell className="font-mono">{result.config.targetKey}</TableCell>
                <TableCell>{result.duration}s</TableCell>
                <TableCell>{result.metrics.requestsSent.toLocaleString()}</TableCell>
                <TableCell>
                  <Badge
                    variant={result.metrics.successRate > 95 ? "default" : "destructive"}
                  >
                    {result.metrics.successRate.toFixed(1)}%
                  </Badge>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {new Date(result.completedAt).toLocaleString()}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onFavorite(result.id)}
                    >
                      <Star className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onDelete(result.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </Card>
  );
};
