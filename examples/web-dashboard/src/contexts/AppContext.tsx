import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { rateLimiterApi, SystemMetrics } from '@/services/rateLimiterApi';

interface AppState {
  realtimeMetrics: SystemMetrics | null;
  userPreferences: {
    theme: 'light' | 'dark' | 'system';
    defaultTimeRange: string;
    autoRefresh: boolean;
  };
  isConnected: boolean;
}

interface AppContextType extends AppState {
  updatePreferences: (prefs: Partial<AppState['userPreferences']>) => void;
  refreshData: () => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
  const [realtimeMetrics, setRealtimeMetrics] = useState<AppState['realtimeMetrics']>(null);
  const [userPreferences, setUserPreferences] = useState<AppState['userPreferences']>({
    theme: 'light',
    defaultTimeRange: '24h',
    autoRefresh: true,
  });
  const [isConnected, setIsConnected] = useState(false);

  // Load preferences from localStorage
  useEffect(() => {
    const savedPrefs = localStorage.getItem('appPreferences');
    if (savedPrefs) {
      try {
        setUserPreferences(JSON.parse(savedPrefs));
      } catch (e) {
        console.error('Failed to load preferences:', e);
      }
    }
  }, []);

  // Save preferences to localStorage
  useEffect(() => {
    localStorage.setItem('appPreferences', JSON.stringify(userPreferences));
  }, [userPreferences]);

  // Poll metrics for real-time updates
  useEffect(() => {
    if (!userPreferences.autoRefresh) {
      setIsConnected(false);
      return;
    }

    let intervalId: NodeJS.Timeout;

    const fetchMetrics = async () => {
      try {
        const metrics = await rateLimiterApi.getMetrics();
        setRealtimeMetrics(metrics);
        setIsConnected(true);
      } catch (error) {
        console.error('Failed to fetch metrics:', error);
        setIsConnected(false);
      }
    };

    // Initial fetch
    fetchMetrics();

    // Poll every 5 seconds
    intervalId = setInterval(fetchMetrics, 5000);

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [userPreferences.autoRefresh]);

  const updatePreferences = (prefs: Partial<AppState['userPreferences']>) => {
    setUserPreferences((prev) => ({ ...prev, ...prefs }));
  };

  const refreshData = () => {
    // Trigger a manual refresh of all data
    window.location.reload();
  };

  return (
    <AppContext.Provider
      value={{
        realtimeMetrics,
        userPreferences,
        isConnected,
        updatePreferences,
        refreshData,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within AppProvider');
  }
  return context;
}
