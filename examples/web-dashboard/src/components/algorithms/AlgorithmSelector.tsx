import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { Database, BarChart3, Clock, Droplet, Info } from "lucide-react";
import { AlgorithmType } from "@/types/algorithms";
import { cn } from "@/lib/utils";
import { useState } from "react";
import { AlgorithmInfoModal } from "@/components/AlgorithmInfoModal";

interface AlgorithmSelectorProps {
  selected: AlgorithmType[];
  onToggle: (algorithm: AlgorithmType) => void;
}

const algorithms = [
  {
    type: "token-bucket" as AlgorithmType,
    name: "Token Bucket",
    icon: Database,
    description: "Allows bursts while maintaining average rate",
    color: "text-blue-600",
  },
  {
    type: "sliding-window" as AlgorithmType,
    name: "Sliding Window",
    icon: BarChart3,
    description: "Smooth rate limiting with rolling window",
    color: "text-purple-600",
  },
  {
    type: "fixed-window" as AlgorithmType,
    name: "Fixed Window",
    icon: Clock,
    description: "Simple counter reset at fixed intervals",
    color: "text-green-600",
  },
  {
    type: "leaky-bucket" as AlgorithmType,
    name: "Leaky Bucket",
    icon: Droplet,
    description: "Smooths traffic at constant output rate",
    color: "text-orange-600",
  },
];

export const AlgorithmSelector = ({ selected, onToggle }: AlgorithmSelectorProps) => {
  const [infoModalOpen, setInfoModalOpen] = useState(false);
  const [selectedAlgorithm, setSelectedAlgorithm] = useState<string | null>(null);

  const handleInfoClick = (algorithm: AlgorithmType, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedAlgorithm(algorithm);
    setInfoModalOpen(true);
  };

  return (
    <>
      <Card className="p-6 shadow-elevated backdrop-blur-sm bg-gradient-to-br from-card to-card/50">
        <h3 className="mb-4 text-lg font-semibold text-foreground">Select Algorithms</h3>
        
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {algorithms.map((algo) => {
            const isSelected = selected.includes(algo.type);
            const Icon = algo.icon;
            
            return (
              <Tooltip key={algo.type}>
                <TooltipTrigger asChild>
                  <div className="relative">
                    <Button
                      variant={isSelected ? "default" : "outline"}
                      className={cn(
                        "h-full w-full flex-col items-start gap-3 p-4 transition-all hover:scale-105 overflow-hidden",
                        isSelected && "shadow-lg"
                      )}
                      onClick={() => onToggle(algo.type)}
                    >
                      <div className="flex w-full items-center justify-between flex-shrink-0">
                        <Icon className={cn("h-6 w-6 flex-shrink-0", isSelected ? "text-primary-foreground" : algo.color)} />
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <div
                            role="button"
                            tabIndex={0}
                            onClick={(e) => handleInfoClick(algo.type, e)}
                            onKeyDown={(e) => {
                              if (e.key === "Enter" || e.key === " ") {
                                e.preventDefault();
                                handleInfoClick(algo.type, e as any);
                              }
                            }}
                            className="rounded p-1 hover:bg-background/20 transition-colors flex-shrink-0 cursor-pointer"
                            aria-label={`Learn more about ${algo.name}`}
                          >
                            <Info className="h-4 w-4" />
                          </div>
                          {isSelected && <Badge variant="secondary" className="text-xs whitespace-nowrap">Active</Badge>}
                        </div>
                      </div>
                      
                      <div className="text-left w-full min-w-0">
                        <p className={cn(
                          "text-sm font-semibold mb-1",
                          isSelected ? "text-primary-foreground" : "text-foreground"
                        )}>
                          {algo.name}
                        </p>
                        <p className={cn(
                          "text-xs leading-relaxed break-words whitespace-normal overflow-wrap-anywhere",
                          isSelected ? "text-primary-foreground/80" : "text-muted-foreground"
                        )}>
                          {algo.description}
                        </p>
                      </div>
                    </Button>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p className="max-w-xs">{algo.description}</p>
                </TooltipContent>
              </Tooltip>
            );
          })}
        </div>
      </Card>

      <AlgorithmInfoModal
        algorithm={selectedAlgorithm}
        open={infoModalOpen}
        onOpenChange={setInfoModalOpen}
      />
    </>
  );
};
