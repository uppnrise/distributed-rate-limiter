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
    { name: 'Allowed', value: currentMetrics.totalAllowedRequests, color: '#22c55e' },
    { name: 'Denied', value: currentMetrics.totalDeniedRequests, color: '#ef4444' },
  ] : [];

  // Custom tooltip for charts
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-dark-800 border border-dark-600 rounded-xl p-3 shadow-2xl">
          <p className="text-dark-300 font-semibold mb-2">{label}</p>
          {payload.map((entry: any, index: number) => (
            <p key={index} className="text-sm" style={{ color: entry.color }}>
              {entry.dataKey === 'successRate' && `Success Rate: ${entry.value.toFixed(1)}%`}
              {entry.dataKey === 'errorRate' && `Error Rate: ${entry.value.toFixed(1)}%`}
              {entry.dataKey === 'throughput' && `Throughput: ${entry.value.toFixed(1)}/s`}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400 bg-clip-text text-transparent mb-2">
            Real-time Performance Monitor
          </h1>
          <p className="text-dark-400 text-lg">Live system metrics and performance analytics</p>
        </div>
        
        <button
          onClick={onTogglePolling}
          className={`px-6 py-3 rounded-xl font-semibold text-white shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105 ${
            isPolling 
              ? 'bg-gradient-to-r from-error-600 to-error-700 hover:from-error-700 hover:to-error-800' 
              : 'bg-gradient-to-r from-success-600 to-success-700 hover:from-success-700 hover:to-success-800'
          }`}
        >
          <span className="flex items-center space-x-2">
            <span className="text-lg">{isPolling ? '⏸️' : '▶️'}</span>
            <span>{isPolling ? 'Stop Monitoring' : 'Start Monitoring'}</span>
          </span>
        </button>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Success Rate Over Time */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-xl">📈</span>
              <span>Success Rate Trend</span>
            </h3>
            <p className="text-dark-400 text-sm mt-1">Percentage of allowed requests over time</p>
          </div>
          <div className="card-body">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                <XAxis 
                  dataKey="time" 
                  tick={{ fontSize: 12, fill: '#9ca3af' }}
                  interval="preserveStartEnd"
                  stroke="#6b7280"
                />
                <YAxis 
                  domain={[0, 100]}
                  tick={{ fontSize: 12, fill: '#9ca3af' }}
                  label={{ value: '%', angle: -90, position: 'insideLeft', style: { textAnchor: 'middle', fill: '#9ca3af' } }}
                  stroke="#6b7280"
                />
                <Tooltip content={<CustomTooltip />} />
                <Line 
                  type="monotone" 
                  dataKey="successRate" 
                  stroke="#22c55e" 
                  strokeWidth={3}
                  dot={{ r: 4, fill: '#22c55e' }}
                  activeDot={{ r: 6, fill: '#16a34a' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Request Distribution */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-xl">🥧</span>
              <span>Request Distribution</span>
            </h3>
            <p className="text-dark-400 text-sm mt-1">Breakdown of allowed vs denied requests</p>
          </div>
          <div className="card-body">
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }: any) => 
                    `${name} ${(percent * 100).toFixed(0)}%`
                  }
                  outerRadius={90}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {pieData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  formatter={(value: number) => [value.toLocaleString(), 'Requests']}
                  contentStyle={{
                    backgroundColor: '#1e293b',
                    border: '1px solid #475569',
                    borderRadius: '12px',
                    color: '#e2e8f0'
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Error Rate Trend */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-xl">📉</span>
              <span>Error Rate Trend</span>
            </h3>
            <p className="text-dark-400 text-sm mt-1">Rate of denied requests over time</p>
          </div>
          <div className="card-body">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                <XAxis 
                  dataKey="time" 
                  tick={{ fontSize: 12, fill: '#9ca3af' }}
                  interval="preserveStartEnd"
                  stroke="#6b7280"
                />
                <YAxis 
                  domain={[0, 100]}
                  tick={{ fontSize: 12, fill: '#9ca3af' }}
                  label={{ value: '%', angle: -90, position: 'insideLeft', style: { textAnchor: 'middle', fill: '#9ca3af' } }}
                  stroke="#6b7280"
                />
                <Tooltip content={<CustomTooltip />} />
                <Line 
                  type="monotone" 
                  dataKey="errorRate" 
                  stroke="#ef4444" 
                  strokeWidth={3}
                  dot={{ r: 4, fill: '#ef4444' }}
                  activeDot={{ r: 6, fill: '#dc2626' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Active Keys Monitor */}
        <div className="card">
          <div className="card-header">
            <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
              <span className="text-xl">🔑</span>
              <span>Active Keys Monitor</span>
            </h3>
            <p className="text-dark-400 text-sm mt-1">Currently tracked rate limit keys</p>
          </div>
          <div className="card-body">
            <div className="max-h-72 overflow-y-auto scrollbar-thin">
              {currentMetrics?.keyMetrics && Object.keys(currentMetrics.keyMetrics).length > 0 ? (
                <div className="space-y-3">
                  {Object.entries(currentMetrics.keyMetrics).map(([key, metrics]) => (
                    <div key={key} className="bg-dark-700/50 rounded-xl p-4 border border-dark-600 hover:bg-dark-700 transition-colors">
                      <div className="flex items-center justify-between mb-2">
                        <span className="font-semibold text-dark-200 font-mono text-sm">{key}</span>
                        <span className="text-xs text-dark-500">
                          {new Date(metrics.lastAccess).toLocaleTimeString()}
                        </span>
                      </div>
                      <div className="grid grid-cols-3 gap-3">
                        <div className="text-center">
                          <div className="text-lg font-bold text-primary-400">{metrics.requestCount}</div>
                          <div className="text-xs text-dark-400">Total</div>
                        </div>
                        <div className="text-center">
                          <div className="text-lg font-bold text-success-400">{metrics.allowedCount}</div>
                          <div className="text-xs text-dark-400">Allowed</div>
                        </div>
                        <div className="text-center">
                          <div className="text-lg font-bold text-error-400">{metrics.deniedCount}</div>
                          <div className="text-xs text-dark-400">Denied</div>
                        </div>
                      </div>
                      {/* Success rate bar */}
                      <div className="mt-3">
                        <div className="bg-dark-800 rounded-full h-2">
                          <div 
                            className="bg-gradient-to-r from-success-600 to-success-500 h-2 rounded-full transition-all duration-500"
                            style={{ 
                              width: `${(metrics.allowedCount / metrics.requestCount) * 100}%` 
                            }}
                          ></div>
                        </div>
                        <div className="flex justify-between text-xs text-dark-500 mt-1">
                          <span>Success Rate</span>
                          <span>{((metrics.allowedCount / metrics.requestCount) * 100).toFixed(1)}%</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12">
                  <div className="text-6xl mb-4">🕳️</div>
                  <p className="text-dark-500 text-lg">No active keys to display</p>
                  <p className="text-dark-600 text-sm mt-1">Start sending requests to see activity</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Real-time Status Footer */}
      <div className="card">
        <div className="card-body">
          <div className="flex flex-col md:flex-row items-center justify-between space-y-4 md:space-y-0">
            <div className="flex items-center space-x-4">
              <div className={`w-4 h-4 rounded-full ${isPolling ? 'bg-success-500 animate-pulse-slow' : 'bg-dark-500'}`}></div>
              <div>
                <span className="font-semibold text-dark-200">
                  {isPolling ? 'Live monitoring active' : 'Monitoring paused'}
                </span>
                <p className="text-sm text-dark-400">
                  {isPolling ? 'Automatically updating every 3 seconds' : 'Click "Start Monitoring" to resume'}
                </p>
              </div>
            </div>
            
            {currentMetrics && (
              <div className="flex items-center space-x-6 text-sm">
                <div className="text-center">
                  <div className="text-lg font-bold text-primary-400">
                    {Object.keys(currentMetrics.keyMetrics || {}).length}
                  </div>
                  <div className="text-dark-400">Active Keys</div>
                </div>
                <div className="text-center">
                  <div className="text-lg font-bold text-success-400">
                    {new Date().toLocaleTimeString()}
                  </div>
                  <div className="text-dark-400">Last Update</div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default RealTimeMonitor;