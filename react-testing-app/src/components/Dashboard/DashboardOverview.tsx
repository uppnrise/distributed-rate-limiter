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
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="card animate-pulse">
            <div className="card-body">
              <div className="h-4 bg-dark-700 rounded w-3/4 mb-3"></div>
              <div className="h-8 bg-dark-700 rounded w-1/2 mb-2"></div>
              <div className="h-3 bg-dark-700 rounded w-2/3"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  const totalRequests = metrics ? metrics.totalAllowedRequests + metrics.totalDeniedRequests : 0;
  const successRate = totalRequests > 0 ? (metrics!.totalAllowedRequests / totalRequests * 100) : 0;

  const MetricCard = ({ 
    title, 
    value, 
    subtitle, 
    icon, 
    gradient, 
    textColor = 'text-dark-100',
    trend 
  }: {
    title: string;
    value: string;
    subtitle?: string;
    icon: string;
    gradient: string;
    textColor?: string;
    trend?: { value: number; positive: boolean };
  }) => (
    <div className="card group hover:scale-105 transition-all duration-300 overflow-hidden">
      <div className="card-body relative">
        {/* Background gradient overlay */}
        <div className={`absolute inset-0 opacity-5 ${gradient}`}></div>
        
        <div className="relative flex items-start justify-between">
          <div className="flex-1">
            <p className="text-sm font-semibold text-dark-400 uppercase tracking-wider mb-2">{title}</p>
            <p className={`text-3xl font-bold ${textColor} mb-1`}>
              {value}
            </p>
            {subtitle && (
              <p className="text-sm text-dark-400">{subtitle}</p>
            )}
            {trend && (
              <div className={`flex items-center space-x-1 mt-2 text-xs font-semibold ${
                trend.positive ? 'text-success-400' : 'text-error-400'
              }`}>
                <span>{trend.positive ? '↗' : '↘'}</span>
                <span>{Math.abs(trend.value)}%</span>
              </div>
            )}
          </div>
          
          <div className={`flex items-center justify-center w-14 h-14 rounded-2xl ${gradient} shadow-lg group-hover:scale-110 transition-transform duration-300`}>
            <span className="text-2xl">{icon}</span>
          </div>
        </div>
        
        {/* Animated border effect */}
        <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-transparent via-primary-500/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
      </div>
    </div>
  );

  return (
    <div className="space-y-8">
      {/* Header section */}
      <div className="text-center">
        <h1 className="text-4xl font-bold bg-gradient-to-r from-primary-400 via-accent-400 to-primary-400 bg-clip-text text-transparent mb-2">
          System Overview
        </h1>
        <p className="text-dark-400 text-lg">Real-time performance metrics and system health status</p>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          title="Total Requests"
          value={totalRequests.toLocaleString()}
          subtitle="All-time requests processed"
          icon="📈"
          gradient="bg-gradient-to-br from-primary-600 to-primary-700"
          trend={{ value: 12.5, positive: true }}
        />

        <MetricCard
          title="Success Rate"
          value={`${successRate.toFixed(1)}%`}
          subtitle="Requests allowed through"
          icon="✅"
          gradient="bg-gradient-to-br from-success-600 to-success-700"
          textColor={successRate >= 80 ? 'text-success-400' : successRate >= 50 ? 'text-warning-400' : 'text-error-400'}
          trend={{ value: 3.2, positive: successRate >= 80 }}
        />

        <MetricCard
          title="Denied Requests"
          value={metrics?.totalDeniedRequests.toLocaleString() || '0'}
          subtitle="Rate limit violations"
          icon="🚫"
          gradient="bg-gradient-to-br from-error-600 to-error-700"
          textColor="text-error-400"
          trend={{ value: 8.1, positive: false }}
        />

        <MetricCard
          title="System Health"
          value={health?.status || 'Unknown'}
          subtitle={metrics ? `Redis: ${metrics.redisConnected ? 'Connected' : 'Disconnected'}` : 'Checking status...'}
          icon={health?.status === 'UP' ? '💚' : '❤️‍🩹'}
          gradient={`bg-gradient-to-br ${health?.status === 'UP' ? 'from-success-600 to-success-700' : 'from-error-600 to-error-700'}`}
          textColor={health?.status === 'UP' ? 'text-success-400' : 'text-error-400'}
        />
      </div>

      {/* Additional insights section */}
      {metrics && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Performance insight */}
          <div className="card col-span-1 lg:col-span-2">
            <div className="card-header">
              <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
                <span>⚡</span>
                <span>Performance Insights</span>
              </h3>
            </div>
            <div className="card-body">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="text-center p-4 bg-dark-700/50 rounded-xl">
                  <div className="text-2xl font-bold text-primary-400">
                    {((metrics.totalAllowedRequests / totalRequests) * 100).toFixed(0)}%
                  </div>
                  <div className="text-sm text-dark-400 mt-1">Efficiency Rate</div>
                </div>
                <div className="text-center p-4 bg-dark-700/50 rounded-xl">
                  <div className="text-2xl font-bold text-accent-400">
                    {Object.keys(metrics.keyMetrics || {}).length}
                  </div>
                  <div className="text-sm text-dark-400 mt-1">Active Keys</div>
                </div>
                <div className="text-center p-4 bg-dark-700/50 rounded-xl">
                  <div className="text-2xl font-bold text-success-400">
                    {metrics.redisConnected ? 'Online' : 'Offline'}
                  </div>
                  <div className="text-sm text-dark-400 mt-1">Redis Status</div>
                </div>
              </div>
            </div>
          </div>

          {/* Quick actions */}
          <div className="card">
            <div className="card-header">
              <h3 className="text-xl font-bold text-dark-100 flex items-center space-x-3">
                <span>🎯</span>
                <span>Quick Actions</span>
              </h3>
            </div>
            <div className="card-body space-y-3">
              <button className="w-full btn-primary text-sm py-2">
                Run Load Test
              </button>
              <button className="w-full btn-secondary text-sm py-2">
                Export Metrics
              </button>
              <button className="w-full btn-secondary text-sm py-2">
                Reset Counters
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardOverview;