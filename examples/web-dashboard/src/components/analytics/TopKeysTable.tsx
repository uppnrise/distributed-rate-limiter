import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Search, TrendingUp } from "lucide-react";
import { TopKey } from "@/types/analytics";
import { cn } from "@/lib/utils";

interface TopKeysTableProps {
  keys: TopKey[];
  onKeyClick?: (key: string) => void;
}

export const TopKeysTable = ({ keys, onKeyClick }: TopKeysTableProps) => {
  const [searchTerm, setSearchTerm] = useState("");

  const filteredKeys = keys.filter((key) =>
    key.key.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <TrendingUp className="h-5 w-5 text-primary" />
          <h3 className="text-lg font-semibold text-foreground">Top Keys Analysis</h3>
        </div>
        <div className="relative w-full sm:w-64">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search keys..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
      </div>

      <div className="rounded-lg border border-border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Key</TableHead>
              <TableHead className="text-right">Total Requests</TableHead>
              <TableHead className="text-right">Rejection Rate</TableHead>
              <TableHead>Algorithm Used</TableHead>
              <TableHead>Last Active</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredKeys.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No keys found
                </TableCell>
              </TableRow>
            ) : (
              filteredKeys.map((key) => (
                <TableRow key={key.key} className="hover:bg-accent/50">
                  <TableCell className="font-mono font-medium">{key.key}</TableCell>
                  <TableCell className="text-right">
                    {key.totalRequests.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    <span
                      className={cn(
                        "font-medium",
                        key.rejectionRate > 20
                          ? "text-red-600"
                          : key.rejectionRate > 10
                          ? "text-yellow-600"
                          : "text-green-600"
                      )}
                    >
                      {key.rejectionRate.toFixed(1)}%
                    </span>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline">{key.algorithm}</Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(key.lastActive).toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onKeyClick?.(key.key)}
                    >
                      View Details
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </Card>
  );
};
