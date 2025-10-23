import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Search, Copy, Eye, Edit, Trash2, MoreVertical, Check } from "lucide-react";
import { ApiKey, KeyStatus } from "@/types/apiKeys";
import { toast } from "sonner";
import { cn } from "@/lib/utils";

interface KeysTableProps {
  keys: ApiKey[];
  selectedKeys: string[];
  onSelectionChange: (ids: string[]) => void;
  onView: (key: ApiKey) => void;
  onEdit: (key: ApiKey) => void;
  onDelete: (id: string) => void;
}

const maskApiKey = (key: string) => {
  return `${key.substring(0, 8)}...${key.substring(key.length - 4)}`;
};

const statusColors: Record<KeyStatus, string> = {
  active: "bg-green-600",
  inactive: "bg-gray-600",
  expired: "bg-red-600",
};

export const KeysTable = ({
  keys,
  selectedKeys,
  onSelectionChange,
  onView,
  onEdit,
  onDelete,
}: KeysTableProps) => {
  const [searchTerm, setSearchTerm] = useState("");
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const filteredKeys = keys.filter(
    (key) =>
      key.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      key.key.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleCopy = async (key: string, keyId: string) => {
    try {
      await navigator.clipboard.writeText(key);
      setCopiedKey(keyId);
      toast.success("API key copied to clipboard");
      setTimeout(() => setCopiedKey(null), 2000);
    } catch (error) {
      toast.error("Failed to copy API key");
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      onSelectionChange(filteredKeys.map((k) => k.id));
    } else {
      onSelectionChange([]);
    }
  };

  const handleSelectKey = (keyId: string, checked: boolean) => {
    if (checked) {
      onSelectionChange([...selectedKeys, keyId]);
    } else {
      onSelectionChange(selectedKeys.filter((id) => id !== keyId));
    }
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-6 flex items-center justify-between">
        <h3 className="text-lg font-semibold text-foreground">API Keys</h3>
        <div className="relative w-64">
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
              <TableHead className="w-12">
                <Checkbox
                  checked={
                    filteredKeys.length > 0 && selectedKeys.length === filteredKeys.length
                  }
                  onCheckedChange={handleSelectAll}
                />
              </TableHead>
              <TableHead>Key Name</TableHead>
              <TableHead>API Key</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Created</TableHead>
              <TableHead>Last Used</TableHead>
              <TableHead>Rate Limit</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredKeys.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} className="text-center text-muted-foreground">
                  No API keys found
                </TableCell>
              </TableRow>
            ) : (
              filteredKeys.map((key) => (
                <TableRow key={key.id} className="hover:bg-accent/50">
                  <TableCell>
                    <Checkbox
                      checked={selectedKeys.includes(key.id)}
                      onCheckedChange={(checked) =>
                        handleSelectKey(key.id, checked as boolean)
                      }
                    />
                  </TableCell>
                  <TableCell className="font-medium">{key.name}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <code className="rounded bg-muted px-2 py-1 text-sm">
                        {maskApiKey(key.key)}
                      </code>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        onClick={() => handleCopy(key.key, key.id)}
                      >
                        {copiedKey === key.id ? (
                          <Check className="h-4 w-4 text-green-600" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </Button>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant="outline"
                      className={cn("border-0 text-white", statusColors[key.status])}
                    >
                      {key.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {new Date(key.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {key.lastUsed ? new Date(key.lastUsed).toLocaleDateString() : "Never"}
                  </TableCell>
                  <TableCell className="text-sm">
                    {key.rateLimit.capacity} / {key.rateLimit.refillRate}s
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => onView(key)}>
                          <Eye className="mr-2 h-4 w-4" />
                          View Details
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => onEdit(key)}>
                          <Edit className="mr-2 h-4 w-4" />
                          Edit
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => onDelete(key.id)}
                          className="text-destructive"
                        >
                          <Trash2 className="mr-2 h-4 w-4" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
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
