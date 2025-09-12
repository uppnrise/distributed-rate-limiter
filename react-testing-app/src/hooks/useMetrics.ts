import { useState, useEffect, useCallback, useRef } from 'react';
import apiClient from '../services/apiClient';
import type { SystemMetrics, MetricsHistory } from '../types/metrics.types';

interface UseMetricsReturn {
  metrics: SystemMetrics | null;
  metricsHistory: MetricsHistory[];
  health: { status: string } | null;
  loading: boolean;
  error: string | null;
  isPolling: boolean;
  startPolling: (intervalMs?: number) => void;
  stopPolling: () => void;
  refreshMetrics: () => Promise<void>;
  clearError: () => void;
}

export const useMetrics = (autoStart = false, defaultInterval = 5000): UseMetricsReturn => {
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [metricsHistory, setMetricsHistory] = useState<MetricsHistory[]>([]);
  const [health, setHealth] = useState<{ status: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const isMountedRef = useRef(true);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const refreshMetrics = useCallback(async (): Promise<void> => {
    if (!isMountedRef.current) return;
    
    try {
      setLoading(true);
      setError(null);

      // Fetch both metrics and health in parallel
      const [metricsResponse, healthResponse] = await Promise.all([
        apiClient.getMetrics(),
        apiClient.getHealth().catch(() => ({ status: 'DOWN' })) // Graceful fallback
      ]);

      if (!isMountedRef.current) return;

      setMetrics(metricsResponse);
      setHealth(healthResponse);

      // Add to history (keep last 100 entries)
      setMetricsHistory(prev => {
        const newEntry: MetricsHistory = {
          timestamp: new Date().toISOString(),
          metrics: {
            averageResponseTime: 0, // Would be calculated from actual metrics
            throughputPerSecond: metricsResponse.totalAllowedRequests + metricsResponse.totalDeniedRequests,
            successRate: metricsResponse.totalAllowedRequests / 
              (metricsResponse.totalAllowedRequests + metricsResponse.totalDeniedRequests) * 100,
            errorRate: metricsResponse.totalDeniedRequests / 
              (metricsResponse.totalAllowedRequests + metricsResponse.totalDeniedRequests) * 100,
          }
        };

        const updated = [...prev, newEntry].slice(-100); // Keep last 100 entries
        return updated;
      });
    } catch (err: any) {
      if (!isMountedRef.current) return;
      
      const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch metrics';
      setError(errorMessage);
    } finally {
      if (isMountedRef.current) {
        setLoading(false);
      }
    }
  }, []);

  const startPolling = useCallback((intervalMs = defaultInterval) => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }

    setIsPolling(true);
    refreshMetrics(); // Initial fetch

    intervalRef.current = setInterval(() => {
      refreshMetrics();
    }, intervalMs);
  }, [refreshMetrics, defaultInterval]);

  const stopPolling = useCallback(() => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
    setIsPolling(false);
  }, []);

  // Auto-start polling if requested
  useEffect(() => {
    if (autoStart) {
      startPolling();
    }

    return () => {
      isMountedRef.current = false;
      stopPolling();
    };
  }, [autoStart, startPolling, stopPolling]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      isMountedRef.current = false;
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  return {
    metrics,
    metricsHistory,
    health,
    loading,
    error,
    isPolling,
    startPolling,
    stopPolling,
    refreshMetrics,
    clearError,
  };
};