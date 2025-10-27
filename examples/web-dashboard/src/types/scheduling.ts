// Schedule-related types for time-based dynamic rate limiting

export type ScheduleType = 'RECURRING' | 'ONE_TIME' | 'EVENT_DRIVEN';

export interface TransitionConfig {
  rampUpMinutes: number;
  rampDownMinutes: number;
}

export interface LimitsConfig {
  capacity: number;
  refillRate: number;
  algorithm?: string;
}

export interface ScheduleRequest {
  name: string;
  keyPattern: string;
  type: ScheduleType;
  cronExpression?: string;
  timezone?: string;
  startTime?: string; // ISO 8601 format
  endTime?: string;   // ISO 8601 format
  limits: LimitsConfig;
  fallbackLimits?: LimitsConfig;
  transition?: TransitionConfig;
  priority?: number;
}

export interface ScheduleResponse {
  name: string;
  keyPattern: string;
  type: ScheduleType;
  cronExpression?: string;
  timezone: string;
  startTime?: string;
  endTime?: string;
  active: boolean;
  enabled: boolean;
  priority: number;
}

export interface EmergencyScheduleRequest {
  name?: string;
  keyPattern: string;
  duration: string; // ISO 8601 duration format (e.g., "PT1H")
  capacity: number;
  refillRate: number;
  reason?: string;
}

export interface Schedule {
  id: string;
  name: string;
  keyPattern: string;
  type: ScheduleType;
  cronExpression?: string;
  timezone: string;
  startTime?: Date;
  endTime?: Date;
  limits: LimitsConfig;
  fallbackLimits?: LimitsConfig;
  transition?: TransitionConfig;
  priority: number;
  active: boolean;
  enabled: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface ScheduleStats {
  totalSchedules: number;
  activeSchedules: number;
  recurringSchedules: number;
  oneTimeSchedules: number;
  eventDrivenSchedules: number;
}
