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
    { id: 'dashboard' as const, name: 'Dashboard', icon: '📊' },
    { id: 'testing' as const, name: 'Testing', icon: '🧪' },
    { id: 'monitoring' as const, name: 'Real-time Monitor', icon: '📈' },
  ];

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-xl font-bold text-gray-900">
                🚦 Rate Limiter Testing Dashboard
              </h1>
            </div>
            <div className="flex items-center space-x-4">
              {/* Connection Status */}
              <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
                health?.status === 'UP' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              }`}>
                <div className={`w-2 h-2 rounded-full ${
                  health?.status === 'UP' ? 'bg-green-500' : 'bg-red-500'
                }`}></div>
                <span>{health?.status || 'Unknown'}</span>
              </div>
              
              {/* Refresh Button */}
              <button
                onClick={refreshMetrics}
                disabled={loading}
                className="px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? '🔄' : '↻'} Refresh
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Error Banner */}
      {error && (
        <div className="bg-red-50 border-l-4 border-red-400 p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <p className="text-sm text-red-700">{error}</p>
            </div>
            <div className="ml-auto pl-3">
              <button
                onClick={clearError}
                className="text-red-400 hover:text-red-600"
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
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <nav className="flex space-x-8">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <span className="mr-2">{tab.icon}</span>
                {tab.name}
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'dashboard' && (
          <div className="space-y-6">
            <DashboardOverview 
              metrics={metrics} 
              health={health} 
              loading={loading} 
            />
            
            {/* Recent Test Results */}
            {testResults.length > 0 && (
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-4">Recent Test Results</h2>
                <div className="space-y-3">
                  {testResults.slice(0, 5).map((result, index) => (
                    <div key={index} className="border border-gray-200 rounded-lg p-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <h3 className="font-medium text-gray-900">{result.scenario}</h3>
                          <p className="text-sm text-gray-600 mt-1">
                            {result.totalRequests} requests • {result.successRate.toFixed(1)}% success rate
                          </p>
                        </div>
                        <div className="text-right text-sm text-gray-500">
                          {new Date(result.endTime).toLocaleString()}
                        </div>
                      </div>
                    </div>
                  ))}
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
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="text-center text-sm text-gray-500">
            Distributed Rate Limiter Testing Dashboard • Built with React + TypeScript
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
