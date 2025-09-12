import { useState, useCallback } from 'react';
import apiClient from '../services/apiClient';
import type { 
  RateLimitRequest, 
  RateLimitResponse, 
  RateLimitConfig,
  ConfigurationResponse 
} from '../types/api.types';

interface UseRateLimitApiReturn {
  // State
  loading: boolean;
  error: string | null;
  lastResponse: RateLimitResponse | null;
  configuration: ConfigurationResponse | null;
  
  // Actions
  checkRateLimit: (request: RateLimitRequest) => Promise<RateLimitResponse | null>;
  updateDefaultConfig: (config: RateLimitConfig) => Promise<boolean>;
  updateKeyConfig: (key: string, config: RateLimitConfig) => Promise<boolean>;
  loadConfiguration: () => Promise<void>;
  reloadConfig: () => Promise<boolean>;
  clearError: () => void;
}

export const useRateLimitApi = (): UseRateLimitApiReturn => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastResponse, setLastResponse] = useState<RateLimitResponse | null>(null);
  const [configuration, setConfiguration] = useState<ConfigurationResponse | null>(null);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const checkRateLimit = useCallback(async (request: RateLimitRequest): Promise<RateLimitResponse | null> => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await apiClient.checkRateLimit(request);
      setLastResponse(response);
      return response;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to check rate limit';
      setError(errorMessage);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  const updateDefaultConfig = useCallback(async (config: RateLimitConfig): Promise<boolean> => {
    try {
      setLoading(true);
      setError(null);
      
      await apiClient.setDefaultConfig(config);
      await loadConfiguration(); // Reload to get updated config
      return true;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to update default configuration';
      setError(errorMessage);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const updateKeyConfig = useCallback(async (key: string, config: RateLimitConfig): Promise<boolean> => {
    try {
      setLoading(true);
      setError(null);
      
      await apiClient.setKeyConfig(key, config);
      await loadConfiguration(); // Reload to get updated config
      return true;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to update key configuration';
      setError(errorMessage);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const loadConfiguration = useCallback(async (): Promise<void> => {
    try {
      setLoading(true);
      setError(null);
      
      const config = await apiClient.getConfiguration();
      setConfiguration(config);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to load configuration';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const reloadConfig = useCallback(async (): Promise<boolean> => {
    try {
      setLoading(true);
      setError(null);
      
      await apiClient.reloadConfiguration();
      await loadConfiguration(); // Reload to get updated config
      return true;
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to reload configuration';
      setError(errorMessage);
      return false;
    } finally {
      setLoading(false);
    }
  }, [loadConfiguration]);

  return {
    loading,
    error,
    lastResponse,
    configuration,
    checkRateLimit,
    updateDefaultConfig,
    updateKeyConfig,
    loadConfiguration,
    reloadConfig,
    clearError,
  };
};