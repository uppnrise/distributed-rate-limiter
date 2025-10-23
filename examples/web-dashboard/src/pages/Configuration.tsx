import { useState, useEffect } from "react";
import { GlobalConfigCard } from "@/components/configuration/GlobalConfigCard";
import { KeyConfigTable } from "@/components/configuration/KeyConfigTable";
import { PatternConfigTable } from "@/components/configuration/PatternConfigTable";
import { HierarchyVisualization } from "@/components/configuration/HierarchyVisualization";
import { BulkOperations } from "@/components/configuration/BulkOperations";
import { ConfigStats } from "@/components/configuration/ConfigStats";
import { toast } from "sonner";
import { rateLimiterApi } from "@/services/rateLimiterApi";
import {
  GlobalConfig,
  KeyConfig,
  PatternConfig,
  ConfigStats as ConfigStatsType,
  ConfigAlgorithm,
} from "@/types/configuration";

const Configuration = () => {
  const [loading, setLoading] = useState(true);
  const [globalConfig, setGlobalConfig] = useState<GlobalConfig>({
    defaultCapacity: 10,
    defaultRefillRate: 5,
    cleanupInterval: 300,
    algorithm: "token-bucket",
  });

  const [keyConfigs, setKeyConfigs] = useState<KeyConfig[]>([]);
  const [patternConfigs, setPatternConfigs] = useState<PatternConfig[]>([]);

  const [stats, setStats] = useState<ConfigStatsType>({
    totalKeyConfigs: 0,
    totalPatternConfigs: 0,
    mostUsedPattern: "user:*",
    cacheHitRate: 94,
    avgLookupTime: 3.2,
  });

  // Helper to convert API algorithm format to ConfigAlgorithm
  const normalizeAlgorithm = (algorithm: string): ConfigAlgorithm => {
    const normalized = algorithm.toLowerCase().replace('_', '-');
    const valid: ConfigAlgorithm[] = ['token-bucket', 'sliding-window', 'fixed-window', 'leaky-bucket'];
    return (valid.includes(normalized as ConfigAlgorithm) ? normalized : 'token-bucket') as ConfigAlgorithm;
  };

  // Load configuration from API
  useEffect(() => {
    const loadConfig = async () => {
      try {
        const config = await rateLimiterApi.getConfig();
        
        // Set global config (algorithm defaults to token-bucket as backend doesn't expose it)
        setGlobalConfig({
          defaultCapacity: config.capacity,
          defaultRefillRate: config.refillRate,
          cleanupInterval: config.cleanupIntervalMs / 1000,
          algorithm: 'token-bucket', // Backend doesn't expose global algorithm yet
        });

        // Convert keyConfigs to KeyConfig array
        const keys: KeyConfig[] = Object.entries(config.keyConfigs || {}).map(([keyName, keyConfig], index) => ({
          id: `key-${index}`,
          keyName,
          capacity: keyConfig.capacity,
          refillRate: keyConfig.refillRate,
          algorithm: normalizeAlgorithm(keyConfig.algorithm || 'TOKEN_BUCKET'),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }));
        setKeyConfigs(keys);

        // Convert patternConfigs to PatternConfig array
        const patterns: PatternConfig[] = Object.entries(config.patternConfigs || {}).map(([pattern, patternConfig], index) => ({
          id: `pattern-${index}`,
          pattern,
          description: `Rate limit configuration for ${pattern}`,
          capacity: patternConfig.capacity,
          refillRate: patternConfig.refillRate,
          algorithm: normalizeAlgorithm(patternConfig.algorithm || 'TOKEN_BUCKET'),
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }));
        setPatternConfigs(patterns);

        // Update stats
        setStats(prev => ({
          ...prev,
          totalKeyConfigs: keys.length,
          totalPatternConfigs: patterns.length,
        }));

        setLoading(false);
      } catch (error) {
        console.error('Failed to load configuration:', error);
        toast.error('Failed to load configuration from backend');
        setLoading(false);
      }
    };

    loadConfig();
  }, []);

  const handleUpdateGlobalConfig = (config: GlobalConfig) => {
    setGlobalConfig(config);
    toast.success("Global configuration updated successfully");
  };

  const handleAddKeyConfig = async (config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">) => {
    try {
      await rateLimiterApi.updateKeyConfig(config.keyName, {
        capacity: config.capacity,
        refillRate: config.refillRate,
        algorithm: config.algorithm.toUpperCase().replace('-', '_'),
      });
      
      const newConfig: KeyConfig = {
        ...config,
        id: Math.random().toString(36).substring(7),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setKeyConfigs([...keyConfigs, newConfig]);
      toast.success("Key configuration added successfully");
    } catch (error) {
      toast.error("Failed to add key configuration");
      console.error(error);
    }
  };

  const handleEditKeyConfig = async (
    id: string,
    config: Omit<KeyConfig, "id" | "createdAt" | "updatedAt">
  ) => {
    try {
      await rateLimiterApi.updateKeyConfig(config.keyName, {
        capacity: config.capacity,
        refillRate: config.refillRate,
        algorithm: config.algorithm.toUpperCase().replace('-', '_'),
      });
      
      setKeyConfigs(
        keyConfigs.map((c) =>
          c.id === id
            ? { ...config, id, createdAt: c.createdAt, updatedAt: new Date().toISOString() }
            : c
        )
      );
      toast.success("Key configuration updated successfully");
    } catch (error) {
      toast.error("Failed to update key configuration");
      console.error(error);
    }
  };

  const handleDeleteKeyConfig = async (id: string) => {
    const config = keyConfigs.find(c => c.id === id);
    if (!config) return;
    
    try {
      await rateLimiterApi.deleteKeyConfig(config.keyName);
      setKeyConfigs(keyConfigs.filter((c) => c.id !== id));
      toast.success("Key configuration deleted");
    } catch (error) {
      toast.error("Failed to delete key configuration");
      console.error(error);
    }
  };

  const handleAddPatternConfig = async (
    config: Omit<PatternConfig, "id" | "createdAt" | "updatedAt">
  ) => {
    try {
      await rateLimiterApi.updatePatternConfig(config.pattern, {
        capacity: config.capacity,
        refillRate: config.refillRate,
      });
      
      const newConfig: PatternConfig = {
        ...config,
        id: Math.random().toString(36).substring(7),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      setPatternConfigs([...patternConfigs, newConfig]);
      toast.success("Pattern configuration added successfully");
    } catch (error) {
      toast.error("Failed to add pattern configuration");
      console.error(error);
    }
  };

  const handleEditPatternConfig = async (
    id: string,
    config: Omit<PatternConfig, "id" | "createdAt" | "updatedAt">
  ) => {
    try {
      await rateLimiterApi.updatePatternConfig(config.pattern, {
        capacity: config.capacity,
        refillRate: config.refillRate,
      });
      
      setPatternConfigs(
        patternConfigs.map((c) =>
          c.id === id
            ? { ...config, id, createdAt: c.createdAt, updatedAt: new Date().toISOString() }
            : c
        )
      );
      toast.success("Pattern configuration updated successfully");
    } catch (error) {
      toast.error("Failed to update pattern configuration");
      console.error(error);
    }
  };

  const handleDeletePatternConfig = async (id: string) => {
    const config = patternConfigs.find(c => c.id === id);
    if (!config) return;
    
    try {
      await rateLimiterApi.deletePatternConfig(config.pattern);
      setPatternConfigs(patternConfigs.filter((c) => c.id !== id));
      toast.success("Pattern configuration deleted");
    } catch (error) {
      toast.error("Failed to delete pattern configuration");
      console.error(error);
    }
  };

  const handleExportJSON = () => {
    const data = {
      globalConfig,
      keyConfigs,
      patternConfigs,
      exportedAt: new Date().toISOString(),
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `rate-limiter-config-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success("Configuration exported successfully");
  };

  const handleImportJSON = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target?.result as string);
        if (data.globalConfig) setGlobalConfig(data.globalConfig);
        if (data.keyConfigs) setKeyConfigs(data.keyConfigs);
        if (data.patternConfigs) setPatternConfigs(data.patternConfigs);
        toast.success("Configuration imported successfully");
      } catch (error) {
        toast.error("Failed to import configuration. Invalid JSON format.");
      }
    };
    reader.readAsText(file);
  };

  const handleImportCSV = (file: File) => {
    toast.info("CSV import feature coming soon");
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div>
        <h2 className="text-3xl font-bold tracking-tight text-foreground">
          Configuration Management
        </h2>
        <p className="text-muted-foreground">
          Manage rate limiting rules with flexible hierarchical configuration
        </p>
      </div>

      <GlobalConfigCard config={globalConfig} onUpdate={handleUpdateGlobalConfig} />

      <KeyConfigTable
        configs={keyConfigs}
        onAdd={handleAddKeyConfig}
        onEdit={handleEditKeyConfig}
        onDelete={handleDeleteKeyConfig}
      />

      <PatternConfigTable
        configs={patternConfigs}
        onAdd={handleAddPatternConfig}
        onEdit={handleEditPatternConfig}
        onDelete={handleDeletePatternConfig}
      />

      <HierarchyVisualization />

      <BulkOperations
        onExportJSON={handleExportJSON}
        onImportJSON={handleImportJSON}
        onImportCSV={handleImportCSV}
      />

      <ConfigStats stats={stats} />
    </div>
  );
};

export default Configuration;
