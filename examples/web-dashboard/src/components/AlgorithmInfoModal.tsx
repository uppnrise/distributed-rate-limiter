import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";

interface AlgorithmInfo {
  name: string;
  description: string;
  howItWorks: string[];
  pros: string[];
  cons: string[];
  bestFor: string[];
  complexity: string;
}

const algorithmData: Record<string, AlgorithmInfo> = {
  "token-bucket": {
    name: "Token Bucket",
    description:
      "A token bucket algorithm uses a bucket that has a maximum capacity for tokens. Tokens are added at a fixed rate (refill rate). Each request consumes a token, and if the bucket is empty, the request is rejected.",
    howItWorks: [
      "The bucket starts with a certain capacity of tokens",
      "Tokens are added at a constant refill rate",
      "Each request consumes one or more tokens",
      "If tokens are available, the request is allowed and tokens are consumed",
      "If no tokens are available, the request is rejected",
    ],
    pros: [
      "Handles burst traffic well by allowing unused tokens to accumulate",
      "Simple to implement and understand",
      "Memory efficient - only needs to track token count and last refill time",
    ],
    cons: [
      "Can allow sudden bursts up to bucket capacity",
      "Requires careful tuning of capacity and refill rate",
    ],
    bestFor: [
      "APIs that need to handle occasional burst traffic",
      "Systems where short-term bursts are acceptable",
      "Applications with variable request patterns",
    ],
    complexity: "O(1) time and space",
  },
  "sliding-window": {
    name: "Sliding Window",
    description:
      "The sliding window algorithm tracks requests within a moving time window. It provides more precise rate limiting compared to fixed windows by considering the exact timing of requests.",
    howItWorks: [
      "Maintains a log of request timestamps",
      "When a new request arrives, removes timestamps older than the window",
      "Counts remaining timestamps in the window",
      "Allows request if count is below the limit",
      "Adds current request timestamp to the log",
    ],
    pros: [
      "More precise than fixed window - no boundary issues",
      "Smooth rate limiting without edge case bursts",
      "Fair distribution of requests over time",
    ],
    cons: [
      "Higher memory usage - stores timestamp for each request",
      "More computationally expensive than token bucket",
      "Needs periodic cleanup of old timestamps",
    ],
    bestFor: [
      "Applications requiring precise rate limiting",
      "Systems where fairness is critical",
      "APIs with strict SLA requirements",
    ],
    complexity: "O(n) time where n is the number of requests in window, O(n) space",
  },
  "fixed-window": {
    name: "Fixed Window",
    description:
      "The fixed window algorithm divides time into fixed intervals (windows). Each window has a quota, and requests are counted within each window. When the window expires, the counter resets.",
    howItWorks: [
      "Time is divided into fixed windows (e.g., 1 minute each)",
      "Each window has a request quota (e.g., 100 requests)",
      "A counter tracks requests in the current window",
      "When quota is reached, requests are rejected until the next window",
      "Counter resets at the start of each new window",
    ],
    pros: [
      "Very simple to implement",
      "Extremely memory efficient - only stores counter and window start time",
      "Fast - constant time operations",
    ],
    cons: [
      "Boundary problem - can allow 2x limit at window boundaries",
      "Less fair than sliding window",
      "Traffic spikes possible at window reset",
    ],
    bestFor: [
      "High-throughput systems where simplicity matters",
      "Applications with lenient rate limiting requirements",
      "Resource-constrained environments",
    ],
    complexity: "O(1) time and space",
  },
  "leaky-bucket": {
    name: "Leaky Bucket",
    description:
      "The leaky bucket algorithm processes requests at a constant rate, like water leaking from a bucket with a hole. Requests are queued and processed at a fixed rate, smoothing out bursts.",
    howItWorks: [
      "Requests are added to a queue (the bucket)",
      "The queue has a maximum capacity",
      "Requests are processed at a constant rate (the leak rate)",
      "If the queue is full, new requests are rejected",
      "The system processes requests from the queue continuously",
    ],
    pros: [
      "Smooths traffic - output rate is always constant",
      "Predictable resource usage",
      "Good for protecting downstream services",
    ],
    cons: [
      "Adds latency - requests may wait in queue",
      "Higher memory usage for queue",
      "More complex implementation",
      "Not suitable for applications requiring immediate responses",
    ],
    bestFor: [
      "Services that need to protect downstream resources",
      "Systems where consistent output rate is critical",
      "Batch processing systems",
    ],
    complexity: "O(1) time for operations, O(n) space for queue",
  },
};

interface AlgorithmInfoModalProps {
  algorithm: string | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function AlgorithmInfoModal({
  algorithm,
  open,
  onOpenChange,
}: AlgorithmInfoModalProps) {
  if (!algorithm || !algorithmData[algorithm]) return null;

  const info = algorithmData[algorithm];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {info.name}
            <Badge variant="outline">{info.complexity}</Badge>
          </DialogTitle>
          <DialogDescription>{info.description}</DialogDescription>
        </DialogHeader>

        <ScrollArea className="max-h-[calc(90vh-8rem)] pr-4">
          <div className="space-y-6">
            <div>
              <h4 className="font-semibold mb-3 text-foreground">How It Works</h4>
              <ol className="list-decimal list-inside space-y-2 text-sm text-muted-foreground">
                {info.howItWorks.map((step, index) => (
                  <li key={index}>{step}</li>
                ))}
              </ol>
            </div>

            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <h4 className="font-semibold mb-3 text-foreground flex items-center gap-2">
                  <span className="text-green-600">✓</span> Advantages
                </h4>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  {info.pros.map((pro, index) => (
                    <li key={index} className="flex gap-2">
                      <span className="text-green-600 flex-shrink-0">•</span>
                      <span>{pro}</span>
                    </li>
                  ))}
                </ul>
              </div>

              <div>
                <h4 className="font-semibold mb-3 text-foreground flex items-center gap-2">
                  <span className="text-red-600">✗</span> Disadvantages
                </h4>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  {info.cons.map((con, index) => (
                    <li key={index} className="flex gap-2">
                      <span className="text-red-600 flex-shrink-0">•</span>
                      <span>{con}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div>
              <h4 className="font-semibold mb-3 text-foreground">Best For</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                {info.bestFor.map((use, index) => (
                  <li key={index} className="flex gap-2">
                    <span className="text-primary flex-shrink-0">→</span>
                    <span>{use}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  );
}
