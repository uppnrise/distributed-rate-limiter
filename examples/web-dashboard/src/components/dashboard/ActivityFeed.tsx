import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Clock } from "lucide-react";

interface ActivityEvent {
  id: string;
  timestamp: string;
  key: string;
  algorithm: string;
  status: "allowed" | "rejected";
  tokensUsed: number;
}

interface ActivityFeedProps {
  events: ActivityEvent[];
}

export const ActivityFeed = ({ events }: ActivityFeedProps) => {
  return (
    <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="mb-4 flex items-center gap-2">
        <Clock className="h-5 w-5 text-primary" />
        <h3 className="text-lg font-semibold text-foreground">Recent Activity</h3>
      </div>
      
      <ScrollArea className="h-[400px] pr-4">
        <div className="space-y-3">
          {events.map((event) => (
            <div
              key={event.id}
              className="flex items-center justify-between rounded-lg border border-border bg-background/50 p-3 transition-colors hover:bg-accent/50"
            >
              <div className="flex-1 space-y-1">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm font-medium text-foreground">
                    {event.key}
                  </span>
                  <Badge
                    variant={event.status === "allowed" ? "default" : "destructive"}
                    className="text-xs"
                  >
                    {event.status}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground">
                  {event.algorithm} • {event.tokensUsed} tokens
                </p>
              </div>
              <span className="text-xs text-muted-foreground">{event.timestamp}</span>
            </div>
          ))}
        </div>
      </ScrollArea>
    </Card>
  );
};
