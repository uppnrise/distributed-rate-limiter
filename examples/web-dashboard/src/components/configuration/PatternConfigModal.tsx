import { useEffect } from "react";
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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { PatternConfig } from "@/types/configuration";
import { Badge } from "@/components/ui/badge";

const patternConfigSchema = z.object({
  pattern: z.string()
    .min(1, "Pattern is required")
    .max(100)
    .regex(/^[a-zA-Z0-9:_*-]+$/, "Pattern can only contain letters, numbers, :, _, *, -"),
  description: z.string().min(1, "Description is required").max(200),
  capacity: z.number().min(1, "Capacity must be at least 1").max(10000),
  refillRate: z.number().min(1, "Refill rate must be at least 1").max(1000),
  algorithm: z.enum(["token-bucket", "sliding-window", "fixed-window", "leaky-bucket"]),
});

type PatternConfigFormData = z.infer<typeof patternConfigSchema>;

interface PatternConfigModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (config: Omit<PatternConfig, "id" | "createdAt" | "updatedAt">) => void;
  initialData?: PatternConfig;
  title: string;
}

const patternExamples = [
  { pattern: "user:*", matches: "user:123, user:456" },
  { pattern: "api:*", matches: "api:prod, api:dev" },
  { pattern: "premium:*", matches: "premium:gold, premium:platinum" },
];

export const PatternConfigModal = ({
  open,
  onClose,
  onSave,
  initialData,
  title,
}: PatternConfigModalProps) => {
  const form = useForm<PatternConfigFormData>({
    resolver: zodResolver(patternConfigSchema),
    defaultValues: {
      pattern: "",
      description: "",
      capacity: 10,
      refillRate: 5,
      algorithm: "token-bucket",
    },
  });

  useEffect(() => {
    if (initialData) {
      form.reset({
        pattern: initialData.pattern,
        description: initialData.description,
        capacity: initialData.capacity,
        refillRate: initialData.refillRate,
        algorithm: initialData.algorithm,
      });
    } else {
      form.reset({
        pattern: "",
        description: "",
        capacity: 10,
        refillRate: 5,
        algorithm: "token-bucket" as const,
      });
    }
  }, [initialData, form, open]);

  const onSubmit = (data: PatternConfigFormData) => {
    onSave(data as Omit<PatternConfig, "id" | "createdAt" | "updatedAt">);
    form.reset();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            Configure rate limiting settings for keys matching a pattern
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="pattern"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Pattern</FormLabel>
                  <FormControl>
                    <Input placeholder="user:*" {...field} />
                  </FormControl>
                  <FormDescription>
                    Use * as wildcard. Examples:
                    <div className="mt-2 space-y-1">
                      {patternExamples.map((ex) => (
                        <div key={ex.pattern} className="flex items-center gap-2 text-xs">
                          <Badge variant="outline" className="font-mono">{ex.pattern}</Badge>
                          <span className="text-muted-foreground">matches {ex.matches}</span>
                        </div>
                      ))}
                    </div>
                  </FormDescription>
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
                      placeholder="Configuration for all user keys"
                      {...field}
                    />
                  </FormControl>
                  <FormDescription>
                    Brief description of this pattern
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

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
                        <SelectValue placeholder="Select an algorithm" />
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

            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit">Save Pattern</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};
