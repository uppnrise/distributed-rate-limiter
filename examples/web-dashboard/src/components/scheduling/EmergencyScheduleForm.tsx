import { useState } from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import { EmergencyScheduleRequest } from "@/types/scheduling";

interface EmergencyScheduleFormProps {
  onClose: () => void;
  onSuccess: () => void;
}

export const EmergencyScheduleForm = ({ onClose, onSuccess }: EmergencyScheduleFormProps) => {
  const [formData, setFormData] = useState<EmergencyScheduleRequest>({
    name: `emergency-${Date.now()}`,
    keyPattern: '*',
    duration: 'PT1H',
    capacity: 100,
    refillRate: 10,
    reason: '',
  });

  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await rateLimiterApi.createEmergencySchedule(formData);
      toast.success('Emergency schedule created successfully');
      onSuccess();
    } catch (error) {
      console.error('Failed to create emergency schedule:', error);
      toast.error('Failed to create emergency schedule');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (field: keyof EmergencyScheduleRequest, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const durationOptions = [
    { label: '15 minutes', value: 'PT15M' },
    { label: '30 minutes', value: 'PT30M' },
    { label: '1 hour', value: 'PT1H' },
    { label: '2 hours', value: 'PT2H' },
    { label: '6 hours', value: 'PT6H' },
    { label: '12 hours', value: 'PT12H' },
    { label: '24 hours', value: 'PT24H' },
  ];

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="text-destructive">Create Emergency Rate Limit</DialogTitle>
          <DialogDescription>
            Temporarily reduce rate limits for emergency situations (e.g., DDoS mitigation)
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="keyPattern">Key Pattern</Label>
            <Input
              id="keyPattern"
              value={formData.keyPattern}
              onChange={(e) => handleChange('keyPattern', e.target.value)}
              placeholder="* (all keys)"
              required
            />
            <p className="text-xs text-muted-foreground mt-1">
              Use * for all keys or specify a pattern like api:*
            </p>
          </div>

          <div>
            <Label htmlFor="duration">Duration</Label>
            <select
              id="duration"
              className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              value={formData.duration}
              onChange={(e) => handleChange('duration', e.target.value)}
            >
              {durationOptions.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            <p className="text-xs text-muted-foreground mt-1">
              How long this emergency limit should remain active
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="capacity">Capacity</Label>
              <Input
                id="capacity"
                type="number"
                value={formData.capacity}
                onChange={(e) => handleChange('capacity', parseInt(e.target.value))}
                required
              />
            </div>
            <div>
              <Label htmlFor="refillRate">Refill Rate</Label>
              <Input
                id="refillRate"
                type="number"
                value={formData.refillRate}
                onChange={(e) => handleChange('refillRate', parseInt(e.target.value))}
                required
              />
            </div>
          </div>

          <div>
            <Label htmlFor="reason">Reason (Optional)</Label>
            <Textarea
              id="reason"
              value={formData.reason}
              onChange={(e) => handleChange('reason', e.target.value)}
              placeholder="e.g., DDoS attack detected from IP range x.x.x.x"
              rows={3}
            />
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" variant="destructive" disabled={loading}>
              {loading ? 'Creating...' : 'Create Emergency Limit'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
