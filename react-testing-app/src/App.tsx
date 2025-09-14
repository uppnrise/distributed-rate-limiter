import { useState, useEffect } from 'react';
import DashboardOverview from './components/Dashboard/DashboardOverview';
import TestingPanel from './components/TestingPanel/TestingPanel';
import RealTimeMonitor from './components/RealTimeMonitor/RealTimeMonitor';
import { useMetrics } from './hooks/useMetrics';
import type { TestResult } from './types/config.types';

function App() {
  const {
    metrics,
    metricsHistory,
    health,
    loading,
    error,
    isPolling,
    startPolling,
    stopPolling,
    refreshMetrics,
    clearError
  } = useMetrics();

  const [activeTab, setActiveTab] = useState<'dashboard' | 'testing' | 'monitoring'>('dashboard');
  const [testResults, setTestResults] = useState<TestResult[]>([]);

  // Auto-start metrics polling on mount
  useEffect(() => {
    refreshMetrics();
  }, [refreshMetrics]);

  const handleTogglePolling = () => {
    if (isPolling) {
      stopPolling();
    } else {
      startPolling(3000); // Poll every 3 seconds
    }
  };

  const handleTestResult = (result: TestResult) => {
    setTestResults(prev => [result, ...prev.slice(0, 9)]); // Keep last 10 results
  };

  const tabs = [
    { id: 'dashboard' as const, name: 'Dashboard', icon: '📊', description: 'System overview and metrics' },
    { id: 'testing' as const, name: 'Testing', icon: '🧪', description: 'Interactive rate limit testing' },
    { id: 'monitoring' as const, name: 'Real-time Monitor', icon: '📈', description: 'Live performance monitoring' },
  ];

  return (
    <div className="min-h-screen bg-dark-900">
      {/* Animated background pattern */}
      <div className="fixed inset-0 bg-gradient-to-br from-dark-900 via-dark-800 to-dark-900 opacity-50"></div>
      <div className="fixed inset-0 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-primary-900/20 via-transparent to-transparent"></div>
      
      {/* Header */}
      <header className="relative bg-dark-800/80 backdrop-blur-xl border-b border-dark-700/50 shadow-2xl">
        <div className="max-w-7xl mx-auto px-6 lg:px-8">
          <div className="flex justify-between items-center h-20">
            <div className="flex items-center space-x-4">
              <div className="flex items-center justify-center w-12 h-12 bg-gradient-to-br from-primary-600 to-accent-600 rounded-xl shadow-lg">
                <span className="text-2xl">🚦</span>
              </div>
              <div>
                <h1 className="text-2xl font-bold bg-gradient-to-r from-primary-400 to-accent-400 bg-clip-text text-transparent">
                  Rate Limiter Dashboard
                </h1>
                <p className="text-sm text-dark-400 font-medium">Distributed Token Bucket Testing Platform</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              {/* Connection Status */}
              <div className={`flex items-center space-x-3 px-4 py-2 rounded-xl backdrop-blur-sm border transition-all duration-300 ${
                health?.status === 'UP' 
                  ? 'bg-success-500/10 border-success-500/30 text-success-400 shadow-glow-success' 
                  : 'bg-error-500/10 border-error-500/30 text-error-400 shadow-glow-error'
              }`}>
                <div className={`w-3 h-3 rounded-full ${
                  health?.status === 'UP' ? 'bg-success-500 animate-pulse-slow' : 'bg-error-500 animate-pulse'
                }`}></div>
                <span className="font-semibold text-sm">{health?.status || 'Unknown'}</span>
              </div>
              
              {/* Refresh Button */}
              <button
                onClick={refreshMetrics}
                disabled={loading}
                className="btn-secondary group relative overflow-hidden"
              >
                <span className="relative z-10 flex items-center space-x-2">
                  <span className={`transition-transform duration-300 ${loading ? 'animate-spin' : 'group-hover:rotate-180'}`}>
                    {loading ? '🔄' : '↻'}
                  </span>
                  <span className="hidden sm:block">Refresh</span>
                </span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Error Banner */}
      {error && (
        <div className="relative bg-error-500/10 border-l-4 border-error-500 backdrop-blur-sm">
          <div className="max-w-7xl mx-auto px-6 lg:px-8 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="flex-shrink-0">
                  <svg className="h-6 w-6 text-error-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                  </svg>
                </div>
                <div>
                  <p className="text-sm font-semibold text-error-300">System Error</p>
                  <p className="text-sm text-error-400">{error}</p>
                </div>
              </div>
              <button
                onClick={clearError}
                className="text-error-400 hover:text-error-300 transition-colors p-1"
              >
                <span className="sr-only">Dismiss</span>
                <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="relative bg-dark-800/60 backdrop-blur-xl border-b border-dark-700/50">
        <div className="max-w-7xl mx-auto px-6 lg:px-8">
          <nav className="flex space-x-1">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`group relative px-6 py-4 font-semibold text-sm transition-all duration-300 ${
                  activeTab === tab.id
                    ? 'text-primary-400'
                    : 'text-dark-400 hover:text-dark-200'
                }`}
              >
                <div className="flex items-center space-x-3">
                  <span className="text-lg transition-transform group-hover:scale-110">{tab.icon}</span>
                  <div className="text-left">
                    <div className="font-semibold">{tab.name}</div>
                    <div className="text-xs opacity-70 hidden lg:block">{tab.description}</div>
                  </div>
                </div>
                
                {/* Active tab indicator */}
                <div className={`absolute bottom-0 left-1/2 transform -translate-x-1/2 h-1 bg-gradient-to-r from-primary-500 to-accent-500 rounded-full transition-all duration-300 ${
                  activeTab === tab.id ? 'w-full' : 'w-0 group-hover:w-1/2'
                }`}></div>
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <main className="relative max-w-7xl mx-auto px-6 lg:px-8 py-8">
        <div className="animate-fade-in">
          {activeTab === 'dashboard' && (
            <div className="space-y-8">
              <DashboardOverview 
                metrics={metrics} 
                health={health} 
                loading={loading} 
              />
              
              {/* Recent Test Results */}
              {testResults.length > 0 && (
                <div className="card animate-slide-in">
                  <div className="card-header">
                    <h2 className="text-2xl font-bold text-dark-100 flex items-center space-x-3">
                      <span className="text-2xl">📋</span>
                      <span>Recent Test Results</span>
                    </h2>
                    <p className="text-dark-400 mt-1">Latest testing session results and performance metrics</p>
                  </div>
                  <div className="card-body">
                    <div className="space-y-4">
                      {testResults.slice(0, 5).map((result, index) => (
                        <div key={index} className="bg-dark-700/50 border border-dark-600 rounded-xl p-5 hover:bg-dark-700 transition-colors">
                          <div className="flex justify-between items-start">
                            <div className="flex-1">
                              <h3 className="font-semibold text-dark-100 text-lg">{result.scenario}</h3>
                              <div className="flex items-center space-x-6 mt-2">
                                <span className="text-sm text-dark-300">
                                  <span className="font-medium">{result.totalRequests}</span> requests
                                </span>
                                <span className={`text-sm font-semibold ${result.successRate >= 80 ? 'text-success-400' : result.successRate >= 50 ? 'text-warning-400' : 'text-error-400'}`}>
                                  {result.successRate.toFixed(1)}% success rate
                                </span>
                              </div>
                            </div>
                            <div className="text-right text-sm text-dark-500">
                              {new Date(result.endTime).toLocaleString()}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {activeTab === 'testing' && (
            <TestingPanel onTestResult={handleTestResult} />
          )}

          {activeTab === 'monitoring' && (
            <RealTimeMonitor
              metricsHistory={metricsHistory}
              currentMetrics={metrics}
              isPolling={isPolling}
              onTogglePolling={handleTogglePolling}
            />
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="relative bg-dark-800/60 backdrop-blur-xl border-t border-dark-700/50 mt-16">
        <div className="max-w-7xl mx-auto px-6 lg:px-8 py-8">
          <div className="flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
            <div className="flex items-center space-x-4">
              <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-br from-primary-600 to-accent-600 rounded-lg">
                <span className="text-sm">🚦</span>
              </div>
              <div>
                <p className="text-dark-300 font-semibold">Distributed Rate Limiter</p>
                <p className="text-dark-500 text-sm">Testing Dashboard • Built with React + TypeScript</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-6 text-sm text-dark-400">
              <span className="flex items-center space-x-2">
                <span>⚡</span>
                <span>Real-time Monitoring</span>
              </span>
              <span className="flex items-center space-x-2">
                <span>🔒</span>
                <span>Secure Testing</span>
              </span>
              <span className="flex items-center space-x-2">
                <span>📊</span>
                <span>Performance Analytics</span>
              </span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
