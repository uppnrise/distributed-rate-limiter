import { useState, useEffect } from "react";
import { Calendar, Clock, Plus, Power, PowerOff, Trash2, Edit, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import { rateLimiterApi, ScheduleResponse } from "@/services/rateLimiterApi";
import { ScheduleType } from "@/types/scheduling";
import { ScheduleForm } from "@/components/scheduling/ScheduleForm";
import { EmergencyScheduleForm } from "@/components/scheduling/EmergencyScheduleForm";
import { ScheduleStats } from "@/components/scheduling/ScheduleStats";

const Scheduling = () => {
  const [loading, setLoading] = useState(true);
  const [schedules, setSchedules] = useState<ScheduleResponse[]>([]);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEmergencyDialog, setShowEmergencyDialog] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<ScheduleResponse | null>(null);

  const loadSchedules = async () => {
    try {
      const data = await rateLimiterApi.getSchedules();
      setSchedules(data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to load schedules:', error);
      toast.error('Failed to load schedules from backend');
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSchedules();
  }, []);

  const handleCreateSchedule = () => {
    setEditingSchedule(null);
    setShowCreateDialog(true);
  };

  const handleEditSchedule = (schedule: ScheduleResponse) => {
    setEditingSchedule(schedule);
    setShowCreateDialog(true);
  };

  const handleDeleteSchedule = async (name: string) => {
    if (!confirm(`Are you sure you want to delete schedule "${name}"?`)) {
      return;
    }

    try {
      await rateLimiterApi.deleteSchedule(name);
      toast.success(`Schedule "${name}" deleted successfully`);
      loadSchedules();
    } catch (error) {
      console.error('Failed to delete schedule:', error);
      toast.error('Failed to delete schedule');
    }
  };

  const handleToggleSchedule = async (schedule: ScheduleResponse) => {
    try {
      if (schedule.enabled) {
        await rateLimiterApi.deactivateSchedule(schedule.name);
        toast.success(`Schedule "${schedule.name}" deactivated`);
      } else {
        await rateLimiterApi.activateSchedule(schedule.name);
        toast.success(`Schedule "${schedule.name}" activated`);
      }
      loadSchedules();
    } catch (error) {
      console.error('Failed to toggle schedule:', error);
      toast.error('Failed to toggle schedule');
    }
  };

  const getTypeIcon = (type: ScheduleType) => {
    switch (type) {
      case 'RECURRING':
        return <Clock className="h-4 w-4" />;
      case 'ONE_TIME':
        return <Calendar className="h-4 w-4" />;
      case 'EVENT_DRIVEN':
        return <AlertCircle className="h-4 w-4" />;
    }
  };

  const getTypeColor = (type: ScheduleType) => {
    switch (type) {
      case 'RECURRING':
        return 'bg-blue-500/10 text-blue-500 border-blue-500/20';
      case 'ONE_TIME':
        return 'bg-purple-500/10 text-purple-500 border-purple-500/20';
      case 'EVENT_DRIVEN':
        return 'bg-orange-500/10 text-orange-500 border-orange-500/20';
    }
  };

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleString();
  };

  const stats = {
    totalSchedules: schedules.length,
    activeSchedules: schedules.filter(s => s.active).length,
    recurringSchedules: schedules.filter(s => s.type === 'RECURRING').length,
    oneTimeSchedules: schedules.filter(s => s.type === 'ONE_TIME').length,
    eventDrivenSchedules: schedules.filter(s => s.type === 'EVENT_DRIVEN').length,
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">Loading schedules...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Schedule Management</h1>
          <p className="text-muted-foreground mt-1">
            Time-based dynamic rate limiting with cron scheduling and timezone support
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setShowEmergencyDialog(true)} variant="destructive">
            <AlertCircle className="mr-2 h-4 w-4" />
            Emergency Limit
          </Button>
          <Button onClick={handleCreateSchedule}>
            <Plus className="mr-2 h-4 w-4" />
            Create Schedule
          </Button>
        </div>
      </div>

      {/* Stats */}
      <ScheduleStats stats={stats} />

      {/* Schedule Cards */}
      <div className="grid gap-4">
        {schedules.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Calendar className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-medium">No schedules configured</p>
              <p className="text-sm text-muted-foreground mb-4">
                Create your first schedule to start managing time-based rate limits
              </p>
              <Button onClick={handleCreateSchedule}>
                <Plus className="mr-2 h-4 w-4" />
                Create Schedule
              </Button>
            </CardContent>
          </Card>
        ) : (
          schedules.map((schedule) => (
            <Card key={schedule.name} className={schedule.active ? 'border-primary' : ''}>
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="space-y-1 flex-1">
                    <div className="flex items-center gap-2">
                      <CardTitle className="text-xl">{schedule.name}</CardTitle>
                      {schedule.active && (
                        <Badge variant="default" className="bg-green-500">
                          Active
                        </Badge>
                      )}
                      {!schedule.enabled && (
                        <Badge variant="secondary">Disabled</Badge>
                      )}
                    </div>
                    <CardDescription>Pattern: {schedule.keyPattern}</CardDescription>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleToggleSchedule(schedule)}
                    >
                      {schedule.enabled ? (
                        <PowerOff className="h-4 w-4" />
                      ) : (
                        <Power className="h-4 w-4" />
                      )}
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleEditSchedule(schedule)}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleDeleteSchedule(schedule.name)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                  <div>
                    <div className="text-sm font-medium mb-1">Type</div>
                    <Badge variant="outline" className={getTypeColor(schedule.type)}>
                      <span className="mr-1">{getTypeIcon(schedule.type)}</span>
                      {schedule.type.replace('_', ' ')}
                    </Badge>
                  </div>
                  <div>
                    <div className="text-sm font-medium mb-1">Priority</div>
                    <div className="text-sm text-muted-foreground">{schedule.priority}</div>
                  </div>
                  <div>
                    <div className="text-sm font-medium mb-1">Timezone</div>
                    <div className="text-sm text-muted-foreground">{schedule.timezone}</div>
                  </div>
                  {schedule.cronExpression && (
                    <div>
                      <div className="text-sm font-medium mb-1">Cron Expression</div>
                      <div className="text-sm text-muted-foreground font-mono">
                        {schedule.cronExpression}
                      </div>
                    </div>
                  )}
                  {schedule.startTime && (
                    <div>
                      <div className="text-sm font-medium mb-1">Start Time</div>
                      <div className="text-sm text-muted-foreground">
                        {formatDateTime(schedule.startTime)}
                      </div>
                    </div>
                  )}
                  {schedule.endTime && (
                    <div>
                      <div className="text-sm font-medium mb-1">End Time</div>
                      <div className="text-sm text-muted-foreground">
                        {formatDateTime(schedule.endTime)}
                      </div>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Dialogs */}
      {showCreateDialog && (
        <ScheduleForm
          schedule={editingSchedule}
          onClose={() => {
            setShowCreateDialog(false);
            setEditingSchedule(null);
          }}
          onSuccess={() => {
            setShowCreateDialog(false);
            setEditingSchedule(null);
            loadSchedules();
          }}
        />
      )}

      {showEmergencyDialog && (
        <EmergencyScheduleForm
          onClose={() => setShowEmergencyDialog(false)}
          onSuccess={() => {
            setShowEmergencyDialog(false);
            loadSchedules();
          }}
        />
      )}
    </div>
  );
};

export default Scheduling;
