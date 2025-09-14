import React, { useState } from 'react';
import { useRateLimitApi } from '../../hooks/useRateLimitApi';
import { testScenarios } from '../../utils/testScenarios';
import type { TestResult } from '../../types/config.types';

interface TestingPanelProps {
  onTestResult?: (result: TestResult) => void;
}

const TestingPanel: React.FC<TestingPanelProps> = ({ onTestResult }) => {
  const { checkRateLimit, loading, error, lastResponse } = useRateLimitApi();
  const [selectedScenario, setSelectedScenario] = useState(testScenarios[0].id);
  const [customKey, setCustomKey] = useState('user:test');
  const [customTokens, setCustomTokens] = useState(1);
  const [isRunningTest, setIsRunningTest] = useState(false);
  const [testResults, setTestResults] = useState<string[]>([]);

  const runSingleRequest = async () => {
    const result = await checkRateLimit({
      key: customKey,
      tokens: customTokens,
    });

    if (result) {
      const message = `${new Date().toLocaleTimeString()}: ${result.allowed ? '✅ ALLOWED' : '❌ DENIED'} - Key: ${result.key}, Tokens: ${result.tokensRequested}`;
      setTestResults(prev => [message, ...prev.slice(0, 9)]); // Keep last 10 results
    }
  };

  const runScenarioTest = async () => {
    const scenario = testScenarios.find(s => s.id === selectedScenario);
    if (!scenario) return;

    setIsRunningTest(true);
    setTestResults([]);

    const startTime = new Date().toISOString();
    let allowedCount = 0;
    let deniedCount = 0;
    const responseTimes: number[] = [];

    try {
      for (let i = 0; i < scenario.config.requestCount; i++) {
        const requestStart = performance.now();
        
        const result = await checkRateLimit({
          key: `${scenario.config.key}_${i % 5}`, // Distribute across 5 keys
          tokens: scenario.config.tokens,
        });

        const responseTime = performance.now() - requestStart;
        responseTimes.push(responseTime);

        if (result) {
          if (result.allowed) {
            allowedCount++;
          } else {
            deniedCount++;
          }

          const message = `Request ${i + 1}: ${result.allowed ? '✅' : '❌'} - ${responseTime.toFixed(1)}ms`;
          setTestResults(prev => [message, ...prev.slice(0, 19)]); // Show last 20
        }

        // Wait between requests if not concurrent
        if (!scenario.config.concurrent && i < scenario.config.requestCount - 1) {
          await new Promise(resolve => setTimeout(resolve, scenario.config.intervalMs));
        }
      }

      const endTime = new Date().toISOString();
      const averageResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length;
      const successRate = (allowedCount / (allowedCount + deniedCount)) * 100;

      const testResult: TestResult = {
        scenario: scenario.name,
        startTime,
        endTime,
        totalRequests: scenario.config.requestCount,
        allowedRequests: allowedCount,
        deniedRequests: deniedCount,
        averageResponseTime,
        successRate,
      };

      onTestResult?.(testResult);

    } catch (err) {
      console.error('Test scenario failed:', err);
    } finally {
      setIsRunningTest(false);
    }
  };

  const scenario = testScenarios.find(s => s.id === selectedScenario);

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="text-center">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400 bg-clip-text text-transparent mb-2">
          Interactive Testing Lab
        </h1>
        <p className="text-dark-400 text-lg">Test rate limiting behavior with single requests and comprehensive scenarios</p>
      </div>

      {/* Error Display */}
      {error && (
        <div className="card border-error-500/50 bg-error-500/10">
          <div className="card-body">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-error-500 rounded-full flex items-center justify-center">
                <span className="text-white text-sm">⚠</span>
              </div>
              <div>
                <h4 className="font-semibold text-error-300">Test Error</h4>
                <p className="text-error-400 text-sm">{error}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-8">
        {/* Single Request Testing */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-2xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-2xl">🎯</span>
              <span>Single Request Test</span>
            </h3>
            <p className="text-dark-400 mt-1">Test individual rate limit checks with custom parameters</p>
          </div>
          <div className="card-body space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-dark-300 mb-2">
                  Rate Limit Key
                </label>
                <input
                  type="text"
                  value={customKey}
                  onChange={(e) => setCustomKey(e.target.value)}
                  className="input"
                  placeholder="e.g., user:test, api:endpoint"
                />
                <p className="text-xs text-dark-500 mt-1">Unique identifier for rate limiting</p>
              </div>
              <div>
                <label className="block text-sm font-semibold text-dark-300 mb-2">
                  Token Count
                </label>
                <input
                  type="number"
                  value={customTokens}
                  onChange={(e) => setCustomTokens(Number(e.target.value))}
                  min="1"
                  className="input"
                  placeholder="1"
                />
                <p className="text-xs text-dark-500 mt-1">Number of tokens to consume</p>
              </div>
            </div>

            <button
              onClick={runSingleRequest}
              disabled={loading}
              className="w-full btn-primary group"
            >
              <span className="flex items-center justify-center space-x-2">
                {loading ? (
                  <>
                    <div className="spinner"></div>
                    <span>Testing Request...</span>
                  </>
                ) : (
                  <>
                    <span>🚀</span>
                    <span>Test Rate Limit</span>
                  </>
                )}
              </span>
            </button>

            {/* Last Response Display */}
            {lastResponse && (
              <div className={`p-4 rounded-xl border transition-all duration-300 ${
                lastResponse.allowed 
                  ? 'bg-success-500/10 border-success-500/30 text-success-300' 
                  : 'bg-error-500/10 border-error-500/30 text-error-300'
              }`}>
                <div className="flex items-center space-x-3 mb-2">
                  <span className="text-xl">{lastResponse.allowed ? '✅' : '❌'}</span>
                  <span className="font-bold text-lg">
                    {lastResponse.allowed ? 'Request Allowed' : 'Request Denied'}
                  </span>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-dark-400">Key:</span>
                    <span className="ml-2 font-mono">{lastResponse.key}</span>
                  </div>
                  <div>
                    <span className="text-dark-400">Tokens:</span>
                    <span className="ml-2 font-mono">{lastResponse.tokensRequested}</span>
                  </div>
                  {lastResponse.retryAfter && (
                    <div className="col-span-2">
                      <span className="text-dark-400">Retry After:</span>
                      <span className="ml-2 font-mono">{lastResponse.retryAfter}s</span>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Scenario Testing */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-2xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-2xl">🧪</span>
              <span>Scenario Testing</span>
            </h3>
            <p className="text-dark-400 mt-1">Run comprehensive test scenarios to evaluate system behavior</p>
          </div>
          <div className="card-body space-y-6">
            <div>
              <label className="block text-sm font-semibold text-dark-300 mb-2">
                Test Scenario
              </label>
              <select
                value={selectedScenario}
                onChange={(e) => setSelectedScenario(e.target.value)}
                className="select"
              >
                {testScenarios.map((scenario) => (
                  <option key={scenario.id} value={scenario.id}>
                    {scenario.name}
                  </option>
                ))}
              </select>
            </div>

            {scenario && (
              <div className="bg-dark-700/50 rounded-xl p-4 border border-dark-600">
                <h4 className="font-semibold text-dark-200 mb-2">{scenario.name}</h4>
                <p className="text-sm text-dark-400 mb-3">{scenario.description}</p>
                <div className="grid grid-cols-2 gap-3 text-xs">
                  <div className="bg-dark-800/50 rounded-lg p-2">
                    <span className="text-dark-500">Requests:</span>
                    <span className="ml-2 font-semibold text-primary-400">{scenario.config.requestCount}</span>
                  </div>
                  <div className="bg-dark-800/50 rounded-lg p-2">
                    <span className="text-dark-500">Tokens:</span>
                    <span className="ml-2 font-semibold text-accent-400">{scenario.config.tokens}</span>
                  </div>
                  <div className="bg-dark-800/50 rounded-lg p-2">
                    <span className="text-dark-500">Interval:</span>
                    <span className="ml-2 font-semibold text-warning-400">{scenario.config.intervalMs}ms</span>
                  </div>
                  <div className="bg-dark-800/50 rounded-lg p-2">
                    <span className="text-dark-500">Mode:</span>
                    <span className="ml-2 font-semibold text-success-400">
                      {scenario.config.concurrent ? 'Concurrent' : 'Sequential'}
                    </span>
                  </div>
                </div>
              </div>
            )}

            <button
              onClick={runScenarioTest}
              disabled={isRunningTest || loading}
              className="w-full btn-success group"
            >
              <span className="flex items-center justify-center space-x-2">
                {isRunningTest ? (
                  <>
                    <div className="spinner"></div>
                    <span>Running Scenario...</span>
                  </>
                ) : (
                  <>
                    <span>⚡</span>
                    <span>Execute Scenario</span>
                  </>
                )}
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Test Results */}
      {testResults.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h3 className="text-2xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-2xl">📊</span>
              <span>Live Test Results</span>
            </h3>
            <p className="text-dark-400 mt-1">Real-time output from your test execution</p>
          </div>
          <div className="card-body">
            <div className="bg-dark-900/50 rounded-xl p-4 max-h-80 overflow-y-auto scrollbar-thin">
              <div className="space-y-2 font-mono text-sm">
                {testResults.map((result, index) => (
                  <div 
                    key={index} 
                    className={`p-2 rounded-lg transition-all duration-300 ${
                      result.includes('✅') 
                        ? 'bg-success-500/10 text-success-300 border-l-2 border-success-500' 
                        : result.includes('❌')
                        ? 'bg-error-500/10 text-error-300 border-l-2 border-error-500'
                        : 'bg-dark-700/50 text-dark-300'
                    }`}
                  >
                    {result}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TestingPanel;