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
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Plus, Edit, Trash2, Search } from "lucide-react";
import { KeyConfig } from "@/types/configuration";
import { KeyConfigModal } from "./KeyConfigModal";

interface KeyConfigTableProps {
  configs: KeyConfig[];
  onAdd: (config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => void;
  onEdit: (id: string, config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => void;
  onDelete: (id: string) => void;
}

const algorithmNames = {
  "token-bucket": "Token Bucket",
  "sliding-window": "Sliding Window",
  "fixed-window": "Fixed Window",
  "leaky-bucket": "Leaky Bucket",
};

export const KeyConfigTable = ({ configs, onAdd, onEdit, onDelete }: KeyConfigTableProps) => {
  const [searchTerm, setSearchTerm] = useState("");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingConfig, setEditingConfig] = useState<KeyConfig | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const filteredConfigs = configs.filter((config) =>
    config.keyName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleAdd = (config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => {
    onAdd(config);
    setIsModalOpen(false);
  };

  const handleEdit = (config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => {
    if (editingConfig) {
      onEdit(editingConfig.id, config);
      setEditingConfig(null);
      setIsModalOpen(false);
    }
  };

  const handleDelete = () => {
    if (deletingId) {
      onDelete(deletingId);
      setDeletingId(null);
    }
  };

  return (
    <>
      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <h3 className="text-lg font-semibold text-foreground">Per-Key Configuration</h3>
          <div className="flex gap-3">
            <div className="relative flex-1 sm:w-64">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search keys..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9"
              />
            </div>
            <Button
              onClick={() => {
                setEditingConfig(null);
                setIsModalOpen(true);
              }}
              className="gap-2"
            >
              <Plus className="h-4 w-4" />
              Add Key
            </Button>
          </div>
        </div>

        <div className="rounded-lg border border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Key Name</TableHead>
                <TableHead>Capacity</TableHead>
                <TableHead>Refill Rate</TableHead>
                <TableHead>Algorithm</TableHead>
                <TableHead>Updated</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredConfigs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    No configurations found
                  </TableCell>
                </TableRow>
              ) : (
                filteredConfigs.map((config) => (
                  <TableRow key={config.id} className="hover:bg-accent/50">
                    <TableCell className="font-mono font-medium">{config.keyName}</TableCell>
                    <TableCell>{config.capacity}</TableCell>
                    <TableCell>{config.refillRate}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{algorithmNames[config.algorithm]}</Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(config.updatedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => {
                            setEditingConfig(config);
                            setIsModalOpen(true);
                          }}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => setDeletingId(config.id)}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </Card>

      <KeyConfigModal
        open={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingConfig(null);
        }}
        onSave={editingConfig ? handleEdit : handleAdd}
        initialData={editingConfig || undefined}
        title={editingConfig ? "Edit Key Configuration" : "Add Key Configuration"}
      />

      <AlertDialog open={!!deletingId} onOpenChange={() => setDeletingId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Configuration</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this key configuration? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
};
