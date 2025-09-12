import React from 'react';
import { 
  LineChart, 
  Line, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import type { MetricsHistory, SystemMetrics } from '../../types/metrics.types';

interface RealTimeMonitorProps {
  metricsHistory: MetricsHistory[];
  currentMetrics: SystemMetrics | null;
  isPolling: boolean;
  onTogglePolling: () => void;
}

const RealTimeMonitor: React.FC<RealTimeMonitorProps> = ({ 
  metricsHistory, 
  currentMetrics, 
  isPolling, 
  onTogglePolling 
}) => {
  // Prepare data for charts
  const chartData = metricsHistory.map((entry, index) => ({
    time: new Date(entry.timestamp).toLocaleTimeString(),
    successRate: entry.metrics.successRate,
    errorRate: entry.metrics.errorRate,
    throughput: entry.metrics.throughputPerSecond,
    index,
  }));

  // Prepare pie chart data
  const pieData = currentMetrics ? [
    { name: 'Allowed', value: currentMetrics.totalAllowedRequests, color: '#10B981' },
    { name: 'Denied', value: currentMetrics.totalDeniedRequests, color: '#EF4444' },
  ] : [];

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-gray-900">Real-time Monitoring</h2>
        <button
          onClick={onTogglePolling}
          className={`px-4 py-2 rounded-md text-white font-medium ${
            isPolling 
              ? 'bg-red-600 hover:bg-red-700' 
              : 'bg-green-600 hover:bg-green-700'
          }`}
        >
          {isPolling ? '⏸️ Stop Monitoring' : '▶️ Start Monitoring'}
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Success Rate Over Time */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-lg font-medium text-gray-900 mb-3">Success Rate Over Time</h3>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="time" 
                tick={{ fontSize: 12 }}
                interval="preserveStartEnd"
              />
              <YAxis 
                domain={[0, 100]}
                tick={{ fontSize: 12 }}
                label={{ value: '%', angle: -90, position: 'insideLeft' }}
              />
              <Tooltip 
                formatter={(value: number) => [`${value.toFixed(1)}%`, 'Success Rate']}
              />
              <Line 
                type="monotone" 
                dataKey="successRate" 
                stroke="#10B981" 
                strokeWidth={2}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Request Distribution */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-lg font-medium text-gray-900 mb-3">Request Distribution</h3>
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, value }: any) => `${name} ${((value / pieData.reduce((a, b) => a + b.value, 0)) * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip 
                formatter={(value: number) => [value.toLocaleString(), 'Requests']}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Error Rate Trend */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-lg font-medium text-gray-900 mb-3">Error Rate Trend</h3>
          <ResponsiveContainer width="100%" height={250}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="time" 
                tick={{ fontSize: 12 }}
                interval="preserveStartEnd"
              />
              <YAxis 
                domain={[0, 100]}
                tick={{ fontSize: 12 }}
                label={{ value: '%', angle: -90, position: 'insideLeft' }}
              />
              <Tooltip 
                formatter={(value: number) => [`${value.toFixed(1)}%`, 'Error Rate']}
              />
              <Line 
                type="monotone" 
                dataKey="errorRate" 
                stroke="#EF4444" 
                strokeWidth={2}
                dot={{ r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Key Metrics Table */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h3 className="text-lg font-medium text-gray-900 mb-3">Active Keys</h3>
          <div className="max-h-60 overflow-y-auto">
            {currentMetrics?.keyMetrics && Object.keys(currentMetrics.keyMetrics).length > 0 ? (
              <div className="space-y-2">
                {Object.entries(currentMetrics.keyMetrics).map(([key, metrics]) => (
                  <div key={key} className="bg-white rounded p-3 text-sm">
                    <div className="font-medium text-gray-900 mb-1">{key}</div>
                    <div className="grid grid-cols-3 gap-2 text-gray-600">
                      <div>Total: {metrics.requestCount}</div>
                      <div className="text-green-600">Allowed: {metrics.allowedCount}</div>
                      <div className="text-red-600">Denied: {metrics.deniedCount}</div>
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      Last: {new Date(metrics.lastAccess).toLocaleTimeString()}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-gray-500 text-center py-8">
                No active keys to display
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Real-time Status Indicator */}
      <div className="mt-6 flex items-center justify-between bg-gray-50 rounded-lg p-3">
        <div className="flex items-center space-x-3">
          <div className={`w-3 h-3 rounded-full ${isPolling ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`}></div>
          <span className="text-sm text-gray-700">
            {isPolling ? 'Live monitoring active' : 'Monitoring paused'}
          </span>
        </div>
        {currentMetrics && (
          <div className="text-sm text-gray-600">
            Last updated: {new Date().toLocaleTimeString()}
          </div>
        )}
      </div>
    </div>
  );
};

export default RealTimeMonitor;