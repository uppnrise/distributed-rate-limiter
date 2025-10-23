import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Download, FileJson, FileSpreadsheet, Calendar } from "lucide-react";
import { toast } from "sonner";

interface ExportPanelProps {
  onExportCSV: () => void;
  onExportJSON: () => void;
}

export const ExportPanel = ({ onExportCSV, onExportJSON }: ExportPanelProps) => {
  const handleScheduleReport = () => {
    toast.info("Automated reporting feature coming soon");
  };

  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <h3 className="mb-6 text-lg font-semibold text-foreground">Export & Reporting</h3>

      <div className="grid gap-4 md:grid-cols-3">
        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Export CSV</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Download analytics data as CSV
          </p>
          <Button onClick={onExportCSV} variant="outline" className="w-full gap-2">
            <Download className="h-4 w-4" />
            Export CSV
          </Button>
        </div>

        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <FileJson className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Export JSON</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Download raw analytics data
          </p>
          <Button onClick={onExportJSON} variant="outline" className="w-full gap-2">
            <Download className="h-4 w-4" />
            Export JSON
          </Button>
        </div>

        <div className="rounded-lg border border-border bg-background/50 p-4">
          <div className="mb-3 flex items-center gap-2">
            <Calendar className="h-5 w-5 text-primary" />
            <h4 className="font-medium text-foreground">Schedule Reports</h4>
          </div>
          <p className="mb-4 text-sm text-muted-foreground">
            Automated email reports
          </p>
          <Button
            onClick={handleScheduleReport}
            variant="outline"
            className="w-full gap-2"
          >
            <Calendar className="h-4 w-4" />
            Schedule
          </Button>
        </div>
      </div>
    </Card>
  );
};
