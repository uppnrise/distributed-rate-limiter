import { useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { CalendarIcon } from "lucide-react";
import { format } from "date-fns";
import { cn } from "@/lib/utils";
import { TimeRange } from "@/types/analytics";

interface TimeRangeSelectorProps {
  selectedRange: TimeRange;
  onRangeChange: (range: TimeRange) => void;
  customRange?: { from: Date; to: Date };
  onCustomRangeChange?: (range: { from: Date; to: Date }) => void;
}

const timeRangeOptions = [
  { label: "Last Hour", value: "1h" as TimeRange },
  { label: "24 Hours", value: "24h" as TimeRange },
  { label: "7 Days", value: "7d" as TimeRange },
  { label: "30 Days", value: "30d" as TimeRange },
  { label: "Custom Range", value: "custom" as TimeRange },
];

export const TimeRangeSelector = ({
  selectedRange,
  onRangeChange,
  customRange,
  onCustomRangeChange,
}: TimeRangeSelectorProps) => {
  const [dateRange, setDateRange] = useState<{ from: Date | undefined; to: Date | undefined }>({
    from: customRange?.from,
    to: customRange?.to,
  });

  const handleDateSelect = (range: { from: Date | undefined; to: Date | undefined } | undefined) => {
    if (range?.from && range?.to) {
      setDateRange({ from: range.from, to: range.to });
      onCustomRangeChange?.({ from: range.from, to: range.to });
      onRangeChange("custom");
    } else if (range) {
      setDateRange(range);
    }
  };

  return (
    <Card className="p-4 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-sm font-medium text-muted-foreground">Time Range:</span>
        
        {timeRangeOptions.map((option) => (
          <div key={option.value}>
            {option.value === "custom" ? (
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant={selectedRange === option.value ? "default" : "outline"}
                    className={cn(
                      "gap-2",
                      selectedRange === option.value && "shadow-lg"
                    )}
                  >
                    <CalendarIcon className="h-4 w-4" />
                    {dateRange.from && dateRange.to
                      ? `${format(dateRange.from, "MMM d")} - ${format(dateRange.to, "MMM d")}`
                      : option.label}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <Calendar
                    mode="range"
                    selected={dateRange}
                    onSelect={handleDateSelect}
                    numberOfMonths={2}
                    className="pointer-events-auto"
                  />
                </PopoverContent>
              </Popover>
            ) : (
              <Button
                variant={selectedRange === option.value ? "default" : "outline"}
                onClick={() => onRangeChange(option.value)}
                className={cn(
                  selectedRange === option.value && "shadow-lg"
                )}
              >
                {option.label}
              </Button>
            )}
          </div>
        ))}
      </div>
    </Card>
  );
};
