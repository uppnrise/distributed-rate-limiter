import { useRef } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Download, Upload, FileJson, FileSpreadsheet } from "lucide-react";
import { toast } from "sonner";

interface BulkOperationsProps {
  onExportJSON: () => void;
  onImportJSON: (file: File) => void;
  onImportCSV: (file: File) => void;
}

export const BulkOperations = ({ onExportJSON, onImportJSON, onImportCSV }: BulkOperationsProps) => {
  const jsonInputRef = useRef<HTMLInputElement>(null);
  const csvInputRef = useRef<HTMLInputElement>(null);

  const handleJSONImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.type !== "application/json") {
        toast.error("Please upload a valid JSON file");
        return;
      }
      onImportJSON(file);
      if (jsonInputRef.current) jsonInputRef.current.value = "";
    }
  };

  const handleCSVImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.endsWith(".csv")) {
        toast.error("Please upload a valid CSV file");
        return;
      }
      onImportCSV(file);
      if (csvInputRef.current) csvInputRef.current.value = "";
    }
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <h3 className="mb-6 text-lg font-semibold text-foreground">Bulk Operations</h3>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <FileJson className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Export JSON</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Download all configurations as JSON
          </p>
          <Button onClick={onExportJSON} variant="outline" className="w-full gap-2">
            <Download className="h-4 w-4" />
            Export
          </Button>
        </div>

        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <FileJson className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Import JSON</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Upload JSON file to import configs
          </p>
          <input
            ref={jsonInputRef}
            type="file"
            accept=".json,application/json"
            onChange={handleJSONImport}
            className="hidden"
          />
          <Button
            onClick={() => jsonInputRef.current?.click()}
            variant="outline"
            className="w-full gap-2"
          >
            <Upload className="h-4 w-4" />
            Import
          </Button>
        </div>

        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Import CSV</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Bulk update via CSV upload
          </p>
          <input
            ref={csvInputRef}
            type="file"
            accept=".csv,text/csv"
            onChange={handleCSVImport}
            className="hidden"
          />
          <Button
            onClick={() => csvInputRef.current?.click()}
            variant="outline"
            className="w-full gap-2"
          >
            <Upload className="h-4 w-4" />
            Import
          </Button>
        </div>
      </div>

      <div className="mt-6 rounded-lg bg-muted/50 p-4">
        <h4 className="mb-2 font-medium text-foreground">Configuration Templates</h4>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm">
            E-commerce
          </Button>
          <Button variant="outline" size="sm">
            API Gateway
          </Button>
          <Button variant="outline" size="sm">
            SaaS Platform
          </Button>
          <Button variant="outline" size="sm">
            Mobile App
          </Button>
        </div>
      </div>
    </Card>
  );
};
