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
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Power, PowerOff, Trash2, Download } from "lucide-react";

interface BulkOperationsDialogProps {
  open: boolean;
  onClose: () => void;
  selectedCount: number;
  onActivate: () => void;
  onDeactivate: () => void;
  onDelete: () => void;
  onExport: () => void;
}

export const BulkOperationsDialog = ({
  open,
  onClose,
  selectedCount,
  onActivate,
  onDeactivate,
  onDelete,
  onExport,
}: BulkOperationsDialogProps) => {
  return (
    <AlertDialog open={open} onOpenChange={onClose}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Bulk Operations</AlertDialogTitle>
          <AlertDialogDescription>
            Perform actions on <Badge variant="secondary">{selectedCount}</Badge> selected API keys
          </AlertDialogDescription>
        </AlertDialogHeader>

        <div className="space-y-3 py-4">
          <Button
            onClick={() => {
              onActivate();
              onClose();
            }}
            variant="outline"
            className="w-full justify-start gap-2"
          >
            <Power className="h-4 w-4 text-green-600" />
            Activate Selected Keys
          </Button>

          <Button
            onClick={() => {
              onDeactivate();
              onClose();
            }}
            variant="outline"
            className="w-full justify-start gap-2"
          >
            <PowerOff className="h-4 w-4 text-gray-600" />
            Deactivate Selected Keys
          </Button>

          <Button
            onClick={() => {
              onExport();
              onClose();
            }}
            variant="outline"
            className="w-full justify-start gap-2"
          >
            <Download className="h-4 w-4 text-blue-600" />
            Export Selected Keys
          </Button>

          <Button
            onClick={() => {
              onDelete();
              onClose();
            }}
            variant="outline"
            className="w-full justify-start gap-2 text-destructive hover:text-destructive"
          >
            <Trash2 className="h-4 w-4" />
            Delete Selected Keys
          </Button>
        </div>

        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};
