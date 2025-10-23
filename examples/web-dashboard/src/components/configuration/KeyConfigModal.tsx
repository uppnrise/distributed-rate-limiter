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
import { Label } from "@/components/ui/label";
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
import { KeyConfig } from "@/types/configuration";

const keyConfigSchema = z.object({
  keyName: z.string().min(1, "Key name is required").max(100),
  capacity: z.number().min(1, "Capacity must be at least 1").max(10000),
  refillRate: z.number().min(1, "Refill rate must be at least 1").max(1000),
  algorithm: z.enum(["token-bucket", "sliding-window", "fixed-window", "leaky-bucket"]),
});

type KeyConfigFormData = z.infer<typeof keyConfigSchema>;

interface KeyConfigModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => void;
  initialData?: KeyConfig;
  title: string;
}

export const KeyConfigModal = ({
  open,
  onClose,
  onSave,
  initialData,
  title,
}: KeyConfigModalProps) => {
  const form = useForm<KeyConfigFormData>({
    resolver: zodResolver(keyConfigSchema),
    defaultValues: {
      keyName: "",
      capacity: 10,
      refillRate: 5,
      algorithm: "token-bucket",
    },
  });

  useEffect(() => {
    if (initialData) {
      form.reset({
        keyName: initialData.keyName,
        capacity: initialData.capacity,
        refillRate: initialData.refillRate,
        algorithm: initialData.algorithm,
      });
    } else {
      form.reset({
        keyName: "",
        capacity: 10,
        refillRate: 5,
        algorithm: "token-bucket" as const,
      });
    }
  }, [initialData, form, open]);

  const onSubmit = (data: KeyConfigFormData) => {
    onSave(data as Omit<KeyConfig, "id" | "createdAt" | "updatedAt">);
    form.reset();
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            Configure rate limiting settings for a specific API key
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="keyName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Key Name</FormLabel>
                  <FormControl>
                    <Input placeholder="rl_prod_user123" {...field} />
                  </FormControl>
                  <FormDescription>
                    Unique identifier for this API key
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

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
                  <FormDescription>
                    Maximum requests or tokens allowed
                  </FormDescription>
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
                  <FormDescription>
                    Tokens per second or window size
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

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
                  <FormDescription>
                    Rate limiting algorithm to use
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit">Save Configuration</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
};
