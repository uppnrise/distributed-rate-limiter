import { useState, useEffect, useRef } from 'react';
import { rateLimiterApi, SystemMetrics } from '@/services/rateLimiterApi';

const POLL_INTERVAL = 5000; // 5 seconds

interface UseRealTimeMetricsOptions {
  enabled?: boolean;
  onError?: (error: Error) => void;
}

export function useRealTimeMetrics(options: UseRealTimeMetricsOptions = {}) {
  const { enabled = true, onError } = options;
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const intervalRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    if (!enabled) {
      setIsLoading(false);
      return;
    }

    const fetchMetrics = async () => {
      try {
        const data = await rateLimiterApi.getMetrics();
        setMetrics(data);
        setError(null);
        setIsLoading(false);
      } catch (err) {
        const error = err instanceof Error ? err : new Error('Failed to fetch metrics');
        setError(error);
        setIsLoading(false);
        onError?.(error);
      }
    };

    // Initial fetch
    fetchMetrics();

    // Set up polling
    intervalRef.current = setInterval(fetchMetrics, POLL_INTERVAL);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [enabled, onError]);

  return { metrics, isLoading, error };
}
