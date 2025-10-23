import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Calendar } from "@/components/ui/calendar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { CalendarIcon, Copy, Check } from "lucide-react";
import { format } from "date-fns";
import { cn } from "@/lib/utils";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { ApiKeyCreateInput } from "@/types/apiKeys";
import { toast } from "sonner";

const createKeySchema = z.object({
  name: z.string().min(1, "Key name is required").max(100),
  description: z.string().max(500).optional(),
  expiresAt: z.date().optional(),
  useDefaultLimits: z.boolean(),
  capacity: z.number().min(1).max(10000).optional(),
  refillRate: z.number().min(1).max(1000).optional(),
  algorithm: z.string().optional(),
});

interface CreateKeyModalProps {
  open: boolean;
  onClose: () => void;
  onCreate: (input: ApiKeyCreateInput) => void;
}

export const CreateKeyModal = ({ open, onClose, onCreate }: CreateKeyModalProps) => {
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const form = useForm<z.infer<typeof createKeySchema>>({
    resolver: zodResolver(createKeySchema),
    defaultValues: {
      name: "",
      description: "",
      useDefaultLimits: true,
      capacity: 10,
      refillRate: 5,
      algorithm: "token-bucket",
    },
  });

  const useDefaultLimits = form.watch("useDefaultLimits");

  const handleGenerate = () => {
    const key = `rl_${Array.from({ length: 32 }, () =>
      Math.random().toString(36)[2]
    ).join("")}`;
    setGeneratedKey(key);
  };

  const handleCopy = async () => {
    if (generatedKey) {
      await navigator.clipboard.writeText(generatedKey);
      setCopied(true);
      toast.success("API key copied to clipboard");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const onSubmit = (data: z.infer<typeof createKeySchema>) => {
    if (!generatedKey) {
      toast.error("Please generate an API key first");
      return;
    }

    const input: ApiKeyCreateInput = {
      name: data.name,
      description: data.description,
      expiresAt: data.expiresAt,
      useDefaultLimits: data.useDefaultLimits,
      customLimits: data.useDefaultLimits
        ? undefined
        : {
            capacity: data.capacity!,
            refillRate: data.refillRate!,
            algorithm: data.algorithm!,
          },
    };

    onCreate(input);
    form.reset();
    setGeneratedKey(null);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create New API Key</DialogTitle>
          <DialogDescription>
            Generate a new API key for accessing your rate limiter
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Key Name *</FormLabel>
                  <FormControl>
                    <Input placeholder="Production API Key" {...field} />
                  </FormControl>
                  <FormDescription>A friendly name for this API key</FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Description</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Used for production environment..."
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="expiresAt"
              render={({ field }) => (
                <FormItem className="flex flex-col">
                  <FormLabel>Expiration Date (Optional)</FormLabel>
                  <Popover>
                    <PopoverTrigger asChild>
                      <FormControl>
                        <Button
                          variant="outline"
                          className={cn(
                            "w-full justify-start text-left font-normal",
                            !field.value && "text-muted-foreground"
                          )}
                        >
                          <CalendarIcon className="mr-2 h-4 w-4" />
                          {field.value ? format(field.value, "PPP") : "No expiration"}
                        </Button>
                      </FormControl>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <Calendar
                        mode="single"
                        selected={field.value}
                        onSelect={field.onChange}
                        disabled={(date) => date < new Date()}
                        className="pointer-events-auto"
                      />
                    </PopoverContent>
                  </Popover>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="useDefaultLimits"
              render={({ field }) => (
                <FormItem className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <FormLabel>Use Default Rate Limits</FormLabel>
                    <FormDescription>
                      Use global default rate limiting configuration
                    </FormDescription>
                  </div>
                  <FormControl>
                    <Switch checked={field.value} onCheckedChange={field.onChange} />
                  </FormControl>
                </FormItem>
              )}
            />

            {!useDefaultLimits && (
              <div className="space-y-4 rounded-lg border border-border p-4">
                <h4 className="font-medium">Custom Rate Limits</h4>

                <div className="grid gap-4 md:grid-cols-2">
                  <FormField
                    control={form.control}
                    name="capacity"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Capacity</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            {...field}
                            onChange={(e) => field.onChange(parseInt(e.target.value))}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="refillRate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Refill Rate</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            {...field}
                            onChange={(e) => field.onChange(parseInt(e.target.value))}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <FormField
                  control={form.control}
                  name="algorithm"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Algorithm</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="token-bucket">Token Bucket</SelectItem>
                          <SelectItem value="sliding-window">Sliding Window</SelectItem>
                          <SelectItem value="fixed-window">Fixed Window</SelectItem>
                          <SelectItem value="leaky-bucket">Leaky Bucket</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            )}

            <div className="space-y-4 rounded-lg bg-muted/50 p-4">
              <div className="flex items-center justify-between">
                <h4 className="font-medium">Generated API Key</h4>
                {!generatedKey && (
                  <Button type="button" onClick={handleGenerate} variant="outline" size="sm">
                    Generate Key
                  </Button>
                )}
              </div>

              {generatedKey && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2 rounded-lg border border-border bg-background p-3">
                    <code className="flex-1 font-mono text-sm">{generatedKey}</code>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={handleCopy}
                    >
                      {copied ? (
                        <Check className="h-4 w-4 text-green-600" />
                      ) : (
                        <Copy className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                  <p className="text-xs text-destructive">
                    ⚠️ Save this key securely. It won't be shown again.
                  </p>
                </div>
              )}
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={!generatedKey}>
                Create API Key
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};
