import React from 'react';
import type { SystemMetrics } from '../../types/metrics.types';

interface DashboardOverviewProps {
  metrics: SystemMetrics | null;
  health: { status: string } | null;
  loading: boolean;
}

const DashboardOverview: React.FC<DashboardOverviewProps> = ({ metrics, health, loading }) => {
  if (loading && !metrics) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="bg-white rounded-lg shadow p-6 animate-pulse">
            <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
            <div className="h-8 bg-gray-200 rounded w-1/2"></div>
          </div>
        ))}
      </div>
    );
  }

  const totalRequests = metrics ? metrics.totalAllowedRequests + metrics.totalDeniedRequests : 0;
  const successRate = totalRequests > 0 ? (metrics!.totalAllowedRequests / totalRequests * 100) : 0;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      {/* Total Requests */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center">
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-600">Total Requests</p>
            <p className="text-2xl font-bold text-gray-900">
              {totalRequests.toLocaleString()}
            </p>
          </div>
          <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
      </div>

      {/* Success Rate */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center">
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-600">Success Rate</p>
            <p className="text-2xl font-bold text-green-600">
              {successRate.toFixed(1)}%
            </p>
          </div>
          <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-green-600" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
      </div>

      {/* Denied Requests */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center">
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-600">Denied Requests</p>
            <p className="text-2xl font-bold text-red-600">
              {metrics?.totalDeniedRequests.toLocaleString() || '0'}
            </p>
          </div>
          <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
            <svg className="w-4 h-4 text-red-600" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
      </div>

      {/* System Health */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center">
          <div className="flex-1">
            <p className="text-sm font-medium text-gray-600">System Health</p>
            <p className={`text-2xl font-bold ${
              health?.status === 'UP' ? 'text-green-600' : 'text-red-600'
            }`}>
              {health?.status || 'Unknown'}
            </p>
            {metrics && (
              <p className="text-xs text-gray-500 mt-1">
                Redis: {metrics.redisConnected ? 'Connected' : 'Disconnected'}
              </p>
            )}
          </div>
          <div className={`w-8 h-8 rounded-full flex items-center justify-center ${
            health?.status === 'UP' ? 'bg-green-100' : 'bg-red-100'
          }`}>
            <div className={`w-3 h-3 rounded-full ${
              health?.status === 'UP' ? 'bg-green-600' : 'bg-red-600'
            }`}></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardOverview;