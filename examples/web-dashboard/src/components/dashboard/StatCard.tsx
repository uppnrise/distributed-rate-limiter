import { LucideIcon } from "lucide-react";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface StatCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend?: {
    value: string;
    isPositive: boolean;
  };
  children?: React.ReactNode;
}

export const StatCard = ({ title, value, icon: Icon, trend, children }: StatCardProps) => {
  return (
    <Card className="relative overflow-hidden p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="absolute right-0 top-0 h-32 w-32 translate-x-8 -translate-y-8 rounded-full bg-primary/5" />
      
      <div className="relative space-y-3">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <div className="rounded-lg bg-primary/10 p-2">
            <Icon className="h-4 w-4 text-primary" />
          </div>
        </div>
        
        <div>
          <p className="text-3xl font-bold text-foreground">{value}</p>
          {trend && (
            <p
              className={cn(
                "mt-1 text-sm font-medium",
                trend.isPositive ? "text-green-600" : "text-red-600"
              )}
            >
              {trend.value}
            </p>
          )}
        </div>
        
        {children && <div className="pt-2">{children}</div>}
      </div>
    </Card>
  );
};
