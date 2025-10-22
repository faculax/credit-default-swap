import React, { useState, useEffect } from 'react';
import SaCcrDashboard from './SaCcrDashboard';
import MarginDashboard from './MarginDashboard';
import SimmDashboard from './SimmDashboard';
import { formatCurrency, formatDateTime } from '../../utils/formatters';

interface DashboardData {
  asOfDate: string;
  generatedAt: string;
  marginSummary: {
    totalVariationMargin: number;
    totalInitialMargin: number;
    marginByCcp: Record<string, {
      ccpName: string;
      variationMargin: number;
      initialMargin: number;
      netMargin: number;
    }>;
    statementsProcessed: number;
    statementsPending: number;
    statementsFailed: number;
  };
  saCcrSummary: {
    totalExposureAtDefault: number;
    totalReplacementCost: number;
    totalPotentialFutureExposure: number;
    nettingSetCount: number;
    calculationsCount: number;
    nettingSets: Record<string, any>;
  };
  reconciliationStatus: {
    totalExceptions: number;
    failedStatements: number;
    disputedItems: number;
    lastReconciliationTime: string;
    dataFreshness: Record<string, string>;
  };
  overallMetrics: {
    totalCollateralExposure: number;
    totalCreditExposure: number;
    systemHealthScore: number;
  };
  simmCalculations?: Array<{
    id: string;
    portfolioId: string;
    calculationDate: string;
    totalInitialMargin: number;
    currency: string;
    parametersVersion: string;
    calculationStatus: 'COMPLETED' | 'FAILED' | 'PENDING';
    buckets: Array<{
      bucketNumber: string;
      assetClass: string;
      initialMargin: number;
      sensitivities: number;
      delta: number;
      vega: number;
      curvature: number;
    }>;
  }>;
}

const ReconciliationDashboard: React.FC = () => {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'margin' | 'saccr' | 'simm'>('overview');
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const fetchDashboardData = async (asOfDate?: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const dateParam = asOfDate || selectedDate;
      const response = await fetch(
        `/api/dashboard/reconciliation?asOfDate=${dateParam}`,
        {
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.status === 'SUCCESS') {
        setDashboardData(result.data);
      } else {
        throw new Error(result.message || 'Failed to fetch dashboard data');
      }
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError(err instanceof Error ? err.message : 'Unknown error occurred');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDate]);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (autoRefresh) {
      interval = setInterval(() => {
        fetchDashboardData();
      }, 30000); // Refresh every 30 seconds
    }

    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoRefresh, selectedDate]);

  const getHealthScoreColor = (score: number) => {
    if (score >= 80) return 'text-green-400';
    if (score >= 60) return 'text-yellow-400';
    return 'text-red-400';
  };

  const getDataFreshnessStatus = (timestamp: string) => {
    const now = new Date();
    const dataTime = new Date(timestamp);
    const diffHours = (now.getTime() - dataTime.getTime()) / (1000 * 60 * 60);
    
    if (diffHours < 24) return { status: 'Fresh', color: 'text-green-400' };
    return { status: 'Stale', color: 'text-yellow-400' };
  };

  const handleTabChange = (tab: 'overview' | 'margin' | 'saccr' | 'simm') => {
    setActiveTab(tab);
  };

  const handleRefresh = () => {
    fetchDashboardData();
  };

  if (loading && !dashboardData) {
    return (
      <div className="min-h-screen bg-fd-dark flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-400 mx-auto mb-4"></div>
          <p className="text-fd-text-muted">Loading dashboard data...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-fd-dark flex items-center justify-center">
        <div className="text-center max-w-md">
          <svg className="w-16 h-16 text-red-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <h2 className="text-xl font-semibold text-fd-text mb-2">Error Loading Dashboard</h2>
          <p className="text-fd-text-muted mb-4">{error}</p>
          <button
            onClick={handleRefresh}
            className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-md transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-fd-dark">
      <div className="max-w-7xl mx-auto p-6">
        {/* Page Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center space-x-3">
              <div className="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-500 rounded-lg flex items-center justify-center">
                <svg className="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
                </svg>
              </div>
              <div>
                <h1 className="text-3xl font-bold text-fd-text">Margin & Exposure Reconciliation</h1>
                <p className="text-fd-text-muted mt-1">
                  Unified view of margin, SA-CCR, and SIMM data
                </p>
              </div>
            </div>

            <div className="flex items-center space-x-4">
              {/* Date Selector */}
              <div className="flex items-center space-x-2">
                <label className="text-sm text-fd-text-muted">As of:</label>
                <input
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="bg-fd-darker border border-fd-border rounded-md px-3 py-2 text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Auto Refresh Toggle */}
              <div className="flex items-center space-x-2">
                <label className="text-sm text-fd-text-muted">Auto-refresh:</label>
                <button
                  onClick={() => setAutoRefresh(!autoRefresh)}
                  className={`w-10 h-6 rounded-full transition-colors ${
                    autoRefresh ? 'bg-blue-500' : 'bg-fd-border'
                  }`}
                >
                  <div className={`w-4 h-4 bg-white rounded-full transition-transform ${
                    autoRefresh ? 'translate-x-5' : 'translate-x-1'
                  }`}></div>
                </button>
              </div>

              {/* Refresh Button */}
              <button
                onClick={handleRefresh}
                disabled={loading}
                className="p-2 text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50 rounded-md transition-colors disabled:opacity-50"
                title="Refresh data"
              >
                <svg className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
              </button>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex space-x-1 bg-fd-darker rounded-lg p-1">
            {[
              { id: 'overview', label: 'Overview', icon: '📊' },
              { id: 'margin', label: 'Margin', icon: '💰' },
              { id: 'saccr', label: 'SA-CCR', icon: '📈' },
              { id: 'simm', label: 'SIMM', icon: '🧮' }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => handleTabChange(tab.id as any)}
                className={`px-6 py-3 rounded-md text-sm font-medium transition-colors flex items-center space-x-2 ${
                  activeTab === tab.id
                    ? 'bg-blue-500 text-white'
                    : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
                }`}
              >
                <span>{tab.icon}</span>
                <span>{tab.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Tab Content */}
        {activeTab === 'overview' && dashboardData && (
          <div className="space-y-6">
            {/* System Health Overview */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">System Health</p>
                    <p className={`text-2xl font-bold mt-1 ${getHealthScoreColor(dashboardData.overallMetrics.systemHealthScore)}`}>
                      {dashboardData.overallMetrics.systemHealthScore}%
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                    </svg>
                  </div>
                </div>
              </div>

              <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Total Collateral</p>
                    <p className="text-2xl font-bold text-fd-text mt-1">
                      {formatCurrency(dashboardData.overallMetrics.totalCollateralExposure)}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"></path>
                    </svg>
                  </div>
                </div>
              </div>

              <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Credit Exposure</p>
                    <p className="text-2xl font-bold text-fd-text mt-1">
                      {formatCurrency(dashboardData.overallMetrics.totalCreditExposure)}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-purple-500/20 rounded-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
                    </svg>
                  </div>
                </div>
              </div>

              <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Exceptions</p>
                    <p className={`text-2xl font-bold mt-1 ${dashboardData.reconciliationStatus.totalExceptions > 0 ? 'text-red-400' : 'text-green-400'}`}>
                      {dashboardData.reconciliationStatus.totalExceptions}
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-red-500/20 rounded-full flex items-center justify-center">
                    <svg className="w-6 h-6 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                  </div>
                </div>
              </div>
            </div>

            {/* Data Freshness */}
            <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
              <h3 className="text-lg font-medium text-fd-text mb-4">Data Freshness Status</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {Object.entries(dashboardData.reconciliationStatus.dataFreshness).map(([source, timestamp]) => {
                  const freshness = getDataFreshnessStatus(timestamp);
                  return (
                    <div key={source} className="flex items-center justify-between p-3 bg-fd-dark rounded-md">
                      <div>
                        <p className="font-medium text-fd-text capitalize">{source}</p>
                        <p className="text-sm text-fd-text-muted">
                          {formatDateTime(timestamp)}
                        </p>
                      </div>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${freshness.color} bg-current bg-opacity-20`}>
                        {freshness.status}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Margin Summary */}
            <div className="bg-fd-darker rounded-lg p-6 border border-fd-border">
              <h3 className="text-lg font-medium text-fd-text mb-4">Margin Statement Summary</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h4 className="text-md font-medium text-fd-text mb-3">By CCP</h4>
                  <div className="space-y-2">
                    {Object.values(dashboardData.marginSummary.marginByCcp).map((ccp) => (
                      <div key={ccp.ccpName} className="flex items-center justify-between p-3 bg-fd-dark rounded-md">
                        <span className="font-medium text-fd-text">{ccp.ccpName}</span>
                        <span className="text-fd-text">{formatCurrency(ccp.netMargin)}</span>
                      </div>
                    ))}
                  </div>
                </div>
                <div>
                  <h4 className="text-md font-medium text-fd-text mb-3">Processing Status</h4>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between p-3 bg-fd-dark rounded-md">
                      <span className="text-fd-text">Processed</span>
                      <span className="text-green-400 font-medium">{dashboardData.marginSummary.statementsProcessed}</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-fd-dark rounded-md">
                      <span className="text-fd-text">Pending</span>
                      <span className="text-yellow-400 font-medium">{dashboardData.marginSummary.statementsPending}</span>
                    </div>
                    <div className="flex items-center justify-between p-3 bg-fd-dark rounded-md">
                      <span className="text-fd-text">Failed</span>
                      <span className="text-red-400 font-medium">{dashboardData.marginSummary.statementsFailed}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'margin' && dashboardData && (
          <MarginDashboard
            asOfDate={dashboardData.asOfDate}
            onRefresh={handleRefresh}
          />
        )}

        {activeTab === 'saccr' && dashboardData && (
          <SaCcrDashboard
            saCcrSummary={dashboardData.saCcrSummary}
            asOfDate={dashboardData.asOfDate}
            onRefresh={handleRefresh}
          />
        )}

        {activeTab === 'simm' && (
          <SimmDashboard
            simmCalculations={dashboardData?.simmCalculations || []}
            asOfDate={dashboardData?.asOfDate || selectedDate}
            onRefresh={handleRefresh}
          />
        )}

        {/* Footer */}
        <div className="mt-8 text-center text-sm text-fd-text-muted">
          Last updated: {dashboardData ? formatDateTime(dashboardData.generatedAt) : 'Never'}
          {autoRefresh && <span className="ml-2">• Auto-refreshing every 30 seconds</span>}
        </div>
      </div>
    </div>
  );
};

export default ReconciliationDashboard;