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
    <div className="bg-white rounded-lg shadow-lg p-6">
      <h2 className="text-xl font-bold text-gray-900 mb-4">Interactive Testing Panel</h2>
      
      {/* Error Display */}
      {error && (
        <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
          {error}
        </div>
      )}

      {/* Single Request Testing */}
      <div className="mb-6 p-4 border border-gray-200 rounded-lg">
        <h3 className="text-lg font-medium text-gray-900 mb-3">Single Request Test</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Key
            </label>
            <input
              type="text"
              value={customKey}
              onChange={(e) => setCustomKey(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="user:test"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tokens
            </label>
            <input
              type="number"
              value={customTokens}
              onChange={(e) => setCustomTokens(Number(e.target.value))}
              min="1"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={runSingleRequest}
              disabled={loading}
              className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Checking...' : 'Test Request'}
            </button>
          </div>
        </div>

        {/* Last Response Display */}
        {lastResponse && (
          <div className={`mt-3 p-3 rounded-md ${
            lastResponse.allowed ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
          }`}>
            <strong>{lastResponse.allowed ? 'Request Allowed' : 'Request Denied'}</strong>
            <br />
            Key: {lastResponse.key}, Tokens: {lastResponse.tokensRequested}
            {lastResponse.retryAfter && (
              <span>, Retry After: {lastResponse.retryAfter}s</span>
            )}
          </div>
        )}
      </div>

      {/* Scenario Testing */}
      <div className="mb-6 p-4 border border-gray-200 rounded-lg">
        <h3 className="text-lg font-medium text-gray-900 mb-3">Scenario Testing</h3>
        
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Test Scenario
          </label>
          <select
            value={selectedScenario}
            onChange={(e) => setSelectedScenario(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {testScenarios.map((scenario) => (
              <option key={scenario.id} value={scenario.id}>
                {scenario.name}
              </option>
            ))}
          </select>
        </div>

        {scenario && (
          <div className="mb-4 p-3 bg-gray-50 rounded-md">
            <p className="text-sm text-gray-700 mb-2">{scenario.description}</p>
            <div className="text-xs text-gray-600">
              <span className="mr-4">Requests: {scenario.config.requestCount}</span>
              <span className="mr-4">Tokens: {scenario.config.tokens}</span>
              <span className="mr-4">Interval: {scenario.config.intervalMs}ms</span>
              <span>Mode: {scenario.config.concurrent ? 'Concurrent' : 'Sequential'}</span>
            </div>
          </div>
        )}

        <button
          onClick={runScenarioTest}
          disabled={isRunningTest || loading}
          className="w-full px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isRunningTest ? 'Running Test...' : 'Run Scenario Test'}
        </button>
      </div>

      {/* Test Results */}
      {testResults.length > 0 && (
        <div className="border border-gray-200 rounded-lg">
          <h3 className="text-lg font-medium text-gray-900 p-4 border-b border-gray-200">
            Test Results
          </h3>
          <div className="p-4 max-h-60 overflow-y-auto">
            {testResults.map((result, index) => (
              <div key={index} className="text-sm font-mono text-gray-700 mb-1">
                {result}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default TestingPanel;