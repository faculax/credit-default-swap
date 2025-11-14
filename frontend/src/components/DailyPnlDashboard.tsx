import React, { useState, useMemo } from 'react';
import { apiUrl } from '../config/api';

interface DailyPnlResult {
  id: number;
  tradeId: number;
  pnlDate: string;
  totalPnl: number;
  pnlPercentage: number;
  marketPnl: number;
  thetaPnl: number;
  accruedPnl: number;
  creditEventPnl: number;
  tradePnl: number;
  unexplainedPnl: number;
  cs01Pnl: number;
  ir01Pnl: number;
  largePnlFlag: boolean;
  unexplainedPnlFlag: boolean;
  creditEventFlag: boolean;
  newTradeFlag: boolean;
  referenceEntity: string;
  buySellProtection: string;
  portfolioName?: string;
  traderName?: string;
}

interface PnlSummary {
  date: string;
  totalTrades: number;
  totalPnl: number;
  totalMarketPnl: number;
  totalThetaPnl: number;
  totalAccruedPnl: number;
  totalCreditEventPnl?: number;
  totalTradePnl?: number;
  totalUnexplainedPnl?: number;
  largeMoversCount: number;
  unexplainedCount: number;
  creditEventsCount: number;
  newTradesCount: number;
}

interface AttributionData {
  marketPnl: number;
  thetaPnl: number;
  accruedPnl: number;
  creditEventPnl: number;
  tradePnl: number;
  unexplainedPnl: number;
}

const DailyPnlDashboard: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [summary, setSummary] = useState<PnlSummary | null>(null);
  const [pnlResults, setPnlResults] = useState<DailyPnlResult[]>([]);
  const [activeTab, setActiveTab] = useState<'all' | 'winners' | 'losers' | 'large' | 'unexplained'>('all');
  const [viewMode, setViewMode] = useState<'table' | 'entity'>('table');
  const [selectedEntity, setSelectedEntity] = useState<string | null>(null);
  const [showAttribution, setShowAttribution] = useState(true);
  const [entitySearchQuery, setEntitySearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = React.useCallback(async () => {
    if (!selectedDate) return;

    setLoading(true);
    setError(null);

    try {
      // Fetch summary
      const summaryResponse = await fetch(
        apiUrl(`/eod/daily-pnl/date/${selectedDate}/summary`)
      );
      
      if (summaryResponse.ok) {
        const summaryData = await summaryResponse.json();
        setSummary(summaryData);
      } else if (summaryResponse.status === 404) {
        setSummary(null);
        setPnlResults([]);
        setError(`No P&L data found for ${selectedDate}`);
        setLoading(false);
        return;
      }

      // Fetch P&L results based on active tab
      let endpoint = '';
      switch (activeTab) {
        case 'winners':
          endpoint = `/eod/daily-pnl/date/${selectedDate}/winners?limit=20`;
          break;
        case 'losers':
          endpoint = `/eod/daily-pnl/date/${selectedDate}/losers?limit=20`;
          break;
        case 'large':
          endpoint = `/eod/daily-pnl/date/${selectedDate}/large-movers`;
          break;
        case 'unexplained':
          endpoint = `/eod/daily-pnl/date/${selectedDate}/unexplained`;
          break;
        default:
          endpoint = `/eod/daily-pnl/date/${selectedDate}`;
      }

      const pnlResponse = await fetch(apiUrl(endpoint));
      if (pnlResponse.ok) {
        const pnlData = await pnlResponse.json();
        setPnlResults(pnlData);
      } else if (pnlResponse.status === 404) {
        // No results for this filter - that's okay, just show empty
        setPnlResults([]);
      } else {
        throw new Error(`Failed to fetch P&L data: ${pnlResponse.statusText}`);
      }

    } catch (err) {
      console.error('Error fetching P&L data:', err);
      setError('Failed to fetch P&L data. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [selectedDate, activeTab]);

  React.useEffect(() => {
    fetchData();
  }, [fetchData]);

  const formatCurrency = (value: number | undefined | null) => {
    if (value === undefined || value === null) return '$0';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatPercent = (value: number | undefined | null) => {
    if (value === undefined || value === null) return '0%';
    return `${value.toFixed(2)}%`;
  };

  const getPnlColor = (value: number | undefined | null) => {
    if (!value) return 'text-fd-text';
    return value >= 0 ? 'text-green-400' : 'text-red-400';
  };

  // Calculate attribution breakdown from P&L results
  const attributionData = useMemo((): AttributionData => {
    if (!summary) {
      return {
        marketPnl: 0,
        thetaPnl: 0,
        accruedPnl: 0,
        creditEventPnl: 0,
        tradePnl: 0,
        unexplainedPnl: 0
      };
    }

    return {
      marketPnl: summary.totalMarketPnl || 0,
      thetaPnl: summary.totalThetaPnl || 0,
      accruedPnl: summary.totalAccruedPnl || 0,
      creditEventPnl: summary.totalCreditEventPnl || 0,
      tradePnl: summary.totalTradePnl || 0,
      unexplainedPnl: summary.totalUnexplainedPnl || 0
    };
  }, [summary]);

  // Export to CSV
  const exportToCSV = () => {
    if (!pnlResults || pnlResults.length === 0) {
      alert('No data to export');
      return;
    }

    const headers = [
      'Trade ID',
      'Entity',
      'Total P&L',
      'Market Movement',
      'Time Decay',
      'Accrued Interest',
      'Credit Events',
      'New Trades',
      'Unexplained'
    ];

    const rows = pnlResults.map(row => [
      row.tradeId,
      row.referenceEntity || '',
      row.totalPnl?.toFixed(2) || '0',
      row.marketPnl?.toFixed(2) || '0',
      row.thetaPnl?.toFixed(2) || '0',
      row.accruedPnl?.toFixed(2) || '0',
      row.creditEventPnl?.toFixed(2) || '0',
      row.tradePnl?.toFixed(2) || '0',
      row.unexplainedPnl?.toFixed(2) || '0'
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    // Include active tab in filename for clarity
    const tabSuffix = activeTab !== 'all' ? `-${activeTab}` : '';
    link.download = `daily-pnl-${selectedDate}${tabSuffix}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="p-6 max-w-screen-2xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-fd-text mb-2">
          Daily P&L Dashboard
        </h1>
        <p className="text-fd-text-muted">
          View daily profit & loss with full attribution breakdown
        </p>
      </div>

      {/* Date Selector */}
      <div className="bg-fd-darker border border-fd-border rounded-lg p-6 mb-6">
        <label htmlFor="pnl-date" className="block text-sm font-medium text-fd-text mb-2">
          Valuation Date
        </label>
        <input
          type="date"
          id="pnl-date"
          value={selectedDate}
          onChange={(e) => setSelectedDate(e.target.value)}
          className="px-4 py-2 border border-fd-border rounded-md bg-fd-input text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green"
        />
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-text-muted mb-1">Total P&L</div>
            <div className={`text-2xl font-bold ${getPnlColor(summary.totalPnl)}`}>
              {formatCurrency(summary.totalPnl)}
            </div>
            <div className="text-xs text-fd-text-muted mt-1">
              {summary.totalTrades} trades
            </div>
          </div>

          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-text-muted mb-1">Market P&L</div>
            <div className={`text-2xl font-bold ${getPnlColor(summary.totalMarketPnl)}`}>
              {formatCurrency(summary.totalMarketPnl)}
            </div>
            <div className="text-xs text-fd-text-muted mt-1">
              Spread & rate moves
            </div>
          </div>

          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-text-muted mb-1">Theta P&L</div>
            <div className={`text-2xl font-bold ${getPnlColor(summary.totalThetaPnl)}`}>
              {formatCurrency(summary.totalThetaPnl)}
            </div>
            <div className="text-xs text-fd-text-muted mt-1">
              Time decay / carry
            </div>
          </div>

          <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
            <div className="text-sm text-fd-text-muted mb-1">Accrued P&L</div>
            <div className={`text-2xl font-bold ${getPnlColor(summary.totalAccruedPnl)}`}>
              {formatCurrency(summary.totalAccruedPnl)}
            </div>
            <div className="text-xs text-fd-text-muted mt-1">
              Accrued interest change
            </div>
          </div>

          <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
            <div className="text-sm text-fd-text-muted mb-1">Large Movers</div>
            <div className="text-xl font-bold text-fd-accent">{summary.largeMoversCount}</div>
          </div>

          <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
            <div className="text-sm text-fd-text-muted mb-1">Unexplained</div>
            <div className="text-xl font-bold text-yellow-400">{summary.unexplainedCount}</div>
          </div>

          <div className="bg-fd-card border border-fd-border rounded-lg p-4">
            <div className="text-sm text-fd-text-muted mb-1">Credit Events</div>
            <div className="text-xl font-bold text-red-400">{summary.creditEventsCount}</div>
          </div>

          <div className="bg-fd-card border border-fd-border rounded-lg p-4">
            <div className="text-sm text-fd-text-muted mb-1">New Trades</div>
            <div className="text-xl font-bold text-blue-400">{summary.newTradesCount}</div>
          </div>
        </div>
      )}

      {/* Attribution Breakdown Chart */}
      {showAttribution && summary && (
        <div className="bg-fd-card border border-fd-border rounded-lg p-6 mb-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-semibold text-fd-text">P&L Attribution</h2>
            <button
              onClick={() => setShowAttribution(false)}
              className="text-fd-text-muted hover:text-fd-text text-sm"
            >
              Hide
            </button>
          </div>
          
          <div className="space-y-3">
            {[
              { label: 'Market P&L', value: attributionData.marketPnl, color: 'bg-blue-500' },
              { label: 'Theta P&L', value: attributionData.thetaPnl, color: 'bg-green-500' },
              { label: 'Accrued P&L', value: attributionData.accruedPnl, color: 'bg-cyan-500' },
              { label: 'Credit Event P&L', value: attributionData.creditEventPnl, color: 'bg-red-500' },
              { label: 'Trade P&L', value: attributionData.tradePnl, color: 'bg-purple-500' },
              { label: 'Unexplained P&L', value: attributionData.unexplainedPnl, color: 'bg-yellow-500' }
            ].map(item => {
              const maxAbsValue = Math.max(
                Math.abs(attributionData.marketPnl),
                Math.abs(attributionData.thetaPnl),
                Math.abs(attributionData.accruedPnl),
                Math.abs(attributionData.creditEventPnl),
                Math.abs(attributionData.tradePnl),
                Math.abs(attributionData.unexplainedPnl)
              );
              const widthPercent = maxAbsValue > 0 ? (Math.abs(item.value) / maxAbsValue) * 100 : 0;
              
              return (
                <div key={item.label} className="flex items-center">
                  <div className="w-40 text-sm text-fd-text-muted">{item.label}</div>
                  <div className="flex-1 flex items-center">
                    <div className="w-full bg-fd-background rounded-full h-6 relative">
                      <div
                        className={`${item.color} h-6 rounded-full transition-all`}
                        style={{ width: `${widthPercent}%` }}
                      />
                      <div className="absolute inset-0 flex items-center justify-end px-2">
                        <span className={`text-xs font-medium ${getPnlColor(item.value)}`}>
                          {formatCurrency(item.value)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {!showAttribution && summary && (
        <div className="bg-fd-card border border-fd-border rounded-lg p-4 mb-6">
          <button
            onClick={() => setShowAttribution(true)}
            className="text-fd-primary hover:text-fd-accent text-sm"
          >
            Show P&L Attribution Chart
          </button>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-red-900/20 border border-red-500 text-red-400 rounded-lg p-4 mb-6">
          {error}
        </div>
      )}

      {/* Controls */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex gap-2">
          <button
            onClick={() => setViewMode('table')}
            className={`px-4 py-2 text-sm rounded-md transition-colors ${
              viewMode === 'table'
                ? 'bg-fd-primary text-white'
                : 'bg-fd-card border border-fd-border text-fd-text-muted hover:text-fd-text'
            }`}
          >
            📊 Table View
          </button>
          <button
            onClick={() => setViewMode('entity')}
            className={`px-4 py-2 text-sm rounded-md transition-colors ${
              viewMode === 'entity'
                ? 'bg-fd-primary text-white'
                : 'bg-fd-card border border-fd-border text-fd-text-muted hover:text-fd-text'
            }`}
          >
            🏢 Entity View
          </button>
        </div>
        
        <button
          onClick={exportToCSV}
          disabled={!pnlResults || pnlResults.length === 0}
          className="px-4 py-2 text-sm bg-fd-accent text-white rounded-md hover:bg-fd-accent/80 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          📥 Export CSV
        </button>
      </div>

      {/* Tabs */}
      <div className="bg-fd-card border border-fd-border rounded-lg mb-6">
        <div className="flex border-b border-fd-border">
          {[
            { key: 'all', label: 'All Trades' },
            { key: 'winners', label: 'Top Winners' },
            { key: 'losers', label: 'Top Losers' },
            { key: 'large', label: 'Large Movers' },
            { key: 'unexplained', label: 'Unexplained' },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as any)}
              className={`px-6 py-3 text-sm font-medium transition-colors ${
                activeTab === tab.key
                  ? 'border-b-2 border-fd-primary text-fd-primary'
                  : 'text-fd-text-muted hover:text-fd-text'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Results Content */}
        <div className="p-6">
          {loading ? (
            <div className="text-center py-12 text-fd-text-muted">Loading P&L data...</div>
          ) : pnlResults.length === 0 ? (
            <div className="text-center py-12">
              <div className="text-fd-text-muted text-lg mb-2">
                {activeTab === 'all' && 'No P&L results available for this date'}
                {activeTab === 'winners' && 'No winning trades found'}
                {activeTab === 'losers' && 'No losing trades found'}
                {activeTab === 'large' && 'No large P&L movers found'}
                {activeTab === 'unexplained' && 'No unexplained P&L found'}
              </div>
              {activeTab !== 'all' && (
                <button
                  onClick={() => setActiveTab('all')}
                  className="text-fd-primary hover:text-fd-accent text-sm"
                >
                  View all trades
                </button>
              )}
            </div>
          ) : viewMode === 'entity' ? (
            /* Entity Drill-Down View */
            <div className="space-y-4">
              {/* Entity Search */}
              <div className="mb-4">
                <input
                  type="text"
                  placeholder="🔍 Search entities..."
                  value={entitySearchQuery}
                  onChange={(e) => setEntitySearchQuery(e.target.value)}
                  className="w-full md:w-96 px-4 py-2 border border-fd-border rounded-md bg-fd-card text-fd-text placeholder-fd-muted focus:outline-none focus:ring-2 focus:ring-fd-primary"
                />
                {entitySearchQuery && (
                  <button
                    onClick={() => setEntitySearchQuery('')}
                    className="ml-2 text-sm text-fd-text-muted hover:text-fd-text"
                  >
                    Clear
                  </button>
                )}
              </div>
              
              {(() => {
                // Group by reference entity
                const entityGroups = pnlResults.reduce((acc, result) => {
                  const entity = result.referenceEntity || 'Unknown';
                  if (!acc[entity]) {
                    acc[entity] = {
                      trades: [],
                      totalPnl: 0,
                      totalMarketPnl: 0,
                      totalThetaPnl: 0,
                      totalAccruedPnl: 0
                    };
                  }
                  acc[entity].trades.push(result);
                  acc[entity].totalPnl += result.totalPnl || 0;
                  acc[entity].totalMarketPnl += result.marketPnl || 0;
                  acc[entity].totalThetaPnl += result.thetaPnl || 0;
                  acc[entity].totalAccruedPnl += result.accruedPnl || 0;
                  return acc;
                }, {} as Record<string, { trades: DailyPnlResult[], totalPnl: number, totalMarketPnl: number, totalThetaPnl: number, totalAccruedPnl: number }>);

                // Filter by search query
                const filteredEntities = Object.entries(entityGroups).filter(([entity]) =>
                  entity.toLowerCase().includes(entitySearchQuery.toLowerCase())
                );

                // Sort by absolute P&L
                const sortedEntities = filteredEntities.sort(
                  ([, a], [, b]) => Math.abs(b.totalPnl) - Math.abs(a.totalPnl)
                );

                // Show message if no results after filtering
                if (sortedEntities.length === 0) {
                  return (
                    <div className="text-center py-12">
                      <div className="text-fd-text-muted mb-2">
                        {entitySearchQuery 
                          ? `No entities found matching "${entitySearchQuery}"`
                          : 'No entities to display'}
                      </div>
                      {entitySearchQuery && (
                        <button
                          onClick={() => setEntitySearchQuery('')}
                          className="text-fd-primary hover:text-fd-accent text-sm"
                        >
                          Clear search
                        </button>
                      )}
                    </div>
                  );
                }

                return sortedEntities.map(([entity, group]) => (
                  <div key={entity} className="border border-fd-border rounded-lg overflow-hidden">
                    <button
                      onClick={() => setSelectedEntity(selectedEntity === entity ? null : entity)}
                      className="w-full flex items-center justify-between p-4 bg-fd-background hover:bg-fd-hover transition-colors"
                    >
                      <div className="flex items-center gap-4">
                        <span className="text-lg font-semibold text-fd-text">{entity}</span>
                        <span className="text-sm text-fd-text-muted">({group.trades.length} trades)</span>
                      </div>
                      <div className="flex items-center gap-4">
                        <div className={`text-lg font-bold ${getPnlColor(group.totalPnl)}`}>
                          {formatCurrency(group.totalPnl)}
                        </div>
                        <span className="text-fd-text-muted">{selectedEntity === entity ? '▼' : '▶'}</span>
                      </div>
                    </button>
                    
                    {selectedEntity === entity && (
                      <div className="border-t border-fd-border bg-fd-card">
                        <div className="grid grid-cols-4 gap-4 p-4 bg-fd-background/50">
                          <div>
                            <div className="text-xs text-fd-text-muted mb-1">Market P&L</div>
                            <div className={`font-semibold ${getPnlColor(group.totalMarketPnl)}`}>
                              {formatCurrency(group.totalMarketPnl)}
                            </div>
                          </div>
                          <div>
                            <div className="text-xs text-fd-text-muted mb-1">Theta P&L</div>
                            <div className={`font-semibold ${getPnlColor(group.totalThetaPnl)}`}>
                              {formatCurrency(group.totalThetaPnl)}
                            </div>
                          </div>
                          <div>
                            <div className="text-xs text-fd-text-muted mb-1">Accrued P&L</div>
                            <div className={`font-semibold ${getPnlColor(group.totalAccruedPnl)}`}>
                              {formatCurrency(group.totalAccruedPnl)}
                            </div>
                          </div>
                          <div>
                            <div className="text-xs text-fd-text-muted mb-1">Total Trades</div>
                            <div className="font-semibold text-fd-text">{group.trades.length}</div>
                          </div>
                        </div>
                        
                        <div className="p-4">
                          <table className="w-full text-sm">
                            <thead>
                              <tr className="text-left text-xs text-fd-text-muted border-b border-fd-border">
                                <th className="pb-2">Trade ID</th>
                                <th className="pb-2 text-right">Total P&L</th>
                                <th className="pb-2 text-right">Market</th>
                                <th className="pb-2 text-right">Theta</th>
                                <th className="pb-2 text-right">Accrued</th>
                                <th className="pb-2">Flags</th>
                              </tr>
                            </thead>
                            <tbody>
                              {group.trades.map(trade => (
                                <tr key={trade.id} className="border-b border-fd-border/50">
                                  <td className="py-2 font-mono">{trade.tradeId}</td>
                                  <td className={`py-2 text-right font-semibold ${getPnlColor(trade.totalPnl)}`}>
                                    {formatCurrency(trade.totalPnl)}
                                  </td>
                                  <td className={`py-2 text-right ${getPnlColor(trade.marketPnl)}`}>
                                    {formatCurrency(trade.marketPnl)}
                                  </td>
                                  <td className={`py-2 text-right ${getPnlColor(trade.thetaPnl)}`}>
                                    {formatCurrency(trade.thetaPnl)}
                                  </td>
                                  <td className={`py-2 text-right ${getPnlColor(trade.accruedPnl)}`}>
                                    {formatCurrency(trade.accruedPnl)}
                                  </td>
                                  <td className="py-2">
                                    <div className="flex gap-1">
                                      {trade.largePnlFlag && <span className="text-xs">🔥</span>}
                                      {trade.unexplainedPnlFlag && <span className="text-xs">❓</span>}
                                      {trade.creditEventFlag && <span className="text-xs">⚠️</span>}
                                      {trade.newTradeFlag && <span className="text-xs">✨</span>}
                                    </div>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </div>
                ));
              })()}
            </div>
          ) : (
            /* Table View */
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-sm text-fd-text-muted border-b border-fd-border">
                    <th className="pb-3 pr-4">Trade ID</th>
                    <th className="pb-3 pr-4">Reference Entity</th>
                    <th className="pb-3 pr-4 text-right">Total P&L</th>
                    <th className="pb-3 pr-4 text-right">Market P&L</th>
                    <th className="pb-3 pr-4 text-right">Theta P&L</th>
                    <th className="pb-3 pr-4 text-right">Accrued P&L</th>
                    <th className="pb-3 pr-4 text-right">Unexplained</th>
                    <th className="pb-3 pr-4">Flags</th>
                  </tr>
                </thead>
                <tbody>
                  {pnlResults.map((result) => (
                    <tr key={result.id} className="border-b border-fd-border hover:bg-fd-hover">
                      <td className="py-3 pr-4">
                        <span className="font-mono text-sm">{result.tradeId}</span>
                      </td>
                      <td className="py-3 pr-4">
                        <div className="text-sm">{result.referenceEntity}</div>
                        <div className="text-xs text-fd-text-muted">{result.buySellProtection}</div>
                      </td>
                      <td className={`py-3 pr-4 text-right font-semibold ${getPnlColor(result.totalPnl)}`}>
                        {formatCurrency(result.totalPnl)}
                        <div className="text-xs text-fd-text-muted">{formatPercent(result.pnlPercentage)}</div>
                      </td>
                      <td className={`py-3 pr-4 text-right ${getPnlColor(result.marketPnl)}`}>
                        {formatCurrency(result.marketPnl)}
                      </td>
                      <td className={`py-3 pr-4 text-right ${getPnlColor(result.thetaPnl)}`}>
                        {formatCurrency(result.thetaPnl)}
                      </td>
                      <td className={`py-3 pr-4 text-right ${getPnlColor(result.accruedPnl)}`}>
                        {formatCurrency(result.accruedPnl)}
                      </td>
                      <td className={`py-3 pr-4 text-right ${getPnlColor(result.unexplainedPnl)}`}>
                        {formatCurrency(result.unexplainedPnl)}
                      </td>
                      <td className="py-3 pr-4">
                        <div className="flex flex-wrap gap-1">
                          {result.largePnlFlag && (
                            <span className="px-2 py-1 text-xs rounded bg-fd-accent/20 text-fd-accent">
                              LARGE
                            </span>
                          )}
                          {result.unexplainedPnlFlag && (
                            <span className="px-2 py-1 text-xs rounded bg-yellow-400/20 text-yellow-400">
                              UNEXP
                            </span>
                          )}
                          {result.creditEventFlag && (
                            <span className="px-2 py-1 text-xs rounded bg-red-400/20 text-red-400">
                              CE
                            </span>
                          )}
                          {result.newTradeFlag && (
                            <span className="px-2 py-1 text-xs rounded bg-blue-400/20 text-blue-400">
                              NEW
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DailyPnlDashboard;

