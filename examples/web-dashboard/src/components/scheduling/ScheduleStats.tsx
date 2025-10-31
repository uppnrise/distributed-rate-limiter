import { Card, CardContent } from "@/components/ui/card";
import { Calendar, Clock, AlertCircle, Activity } from "lucide-react";
import { ScheduleStats as ScheduleStatsType } from "@/types/scheduling";

interface ScheduleStatsProps {
  stats: ScheduleStatsType;
}

export const ScheduleStats = ({ stats }: ScheduleStatsProps) => {
  const statItems = [
    {
      label: "Total Schedules",
      value: stats.totalSchedules,
      icon: Calendar,
      color: "text-blue-500",
    },
    {
      label: "Active Now",
      value: stats.activeSchedules,
      icon: Activity,
      color: "text-green-500",
    },
    {
      label: "Recurring",
      value: stats.recurringSchedules,
      icon: Clock,
      color: "text-purple-500",
    },
    {
      label: "One-Time",
      value: stats.oneTimeSchedules,
      icon: Calendar,
      color: "text-blue-500",
    },
    {
      label: "Event-Driven",
      value: stats.eventDrivenSchedules,
      icon: AlertCircle,
      color: "text-orange-500",
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
      {statItems.map((item) => (
        <Card key={item.label}>
          <CardContent className="p-6">
            <div className="flex items-center justify-between space-x-4">
              <div className="flex-1">
                <p className="text-sm font-medium text-muted-foreground">
                  {item.label}
                </p>
                <p className="text-2xl font-bold">{item.value}</p>
              </div>
              <item.icon className={`h-8 w-8 ${item.color}`} />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};
