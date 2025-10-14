import React, { useState } from 'react';
import { formatCurrency, formatNumber } from '../../utils/formatters';

interface SimmBucketData {
  bucketNumber: string;
  assetClass: string;
  initialMargin: number;
  sensitivities: number;
  delta: number;
  vega: number;
  curvature: number;
}

interface SimmCalculation {
  id: string;
  portfolioId: string;
  calculationDate: string;
  totalInitialMargin: number;
  currency: string;
  buckets: SimmBucketData[];
  parametersVersion: string;
  calculationStatus: 'COMPLETED' | 'FAILED' | 'PENDING';
}

interface SimmDashboardProps {
  simmCalculations: SimmCalculation[];
  asOfDate: string;
  onRefresh: () => void;
}

const SimmDashboard: React.FC<SimmDashboardProps> = ({
  simmCalculations,
  asOfDate,
  onRefresh
}) => {
  const [selectedCalculation, setSelectedCalculation] = useState<SimmCalculation | null>(
    simmCalculations.length > 0 ? simmCalculations[0] : null
  );
  const [expandedBuckets, setExpandedBuckets] = useState<Set<string>>(new Set());
  const [groupBy, setGroupBy] = useState<'assetClass' | 'bucket'>('assetClass');

  const toggleBucketExpansion = (bucketId: string) => {
    const newExpanded = new Set(expandedBuckets);
    if (newExpanded.has(bucketId)) {
      newExpanded.delete(bucketId);
    } else {
      newExpanded.add(bucketId);
    }
    setExpandedBuckets(newExpanded);
  };

  const getGroupedBuckets = () => {
    if (!selectedCalculation) return {};
    
    return selectedCalculation.buckets.reduce((groups, bucket) => {
      const key = groupBy === 'assetClass' ? bucket.assetClass : bucket.bucketNumber;
      if (!groups[key]) {
        groups[key] = [];
      }
      groups[key].push(bucket);
      return groups;
    }, {} as Record<string, SimmBucketData[]>);
  };

  const getAssetClassTotals = () => {
    if (!selectedCalculation) return {};
    
    return selectedCalculation.buckets.reduce((totals, bucket) => {
      const assetClass = bucket.assetClass;
      if (!totals[assetClass]) {
        totals[assetClass] = {
          initialMargin: 0,
          sensitivities: 0,
          bucketCount: 0
        };
      }
      totals[assetClass].initialMargin += bucket.initialMargin;
      totals[assetClass].sensitivities += bucket.sensitivities;
      totals[assetClass].bucketCount += 1;
      return totals;
    }, {} as Record<string, { initialMargin: number; sensitivities: number; bucketCount: number }>);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'text-green-400';
      case 'FAILED': return 'text-red-400';
      case 'PENDING': return 'text-yellow-400';
      default: return 'text-fd-text-muted';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <svg className="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
          </svg>
        );
      case 'FAILED':
        return (
          <svg className="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        );
      case 'PENDING':
        return (
          <svg className="w-4 h-4 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        );
      default:
        return null;
    }
  };

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <svg className="w-8 h-8 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
          </svg>
          <div>
            <h3 className="text-xl font-semibold text-fd-text">SIMM Calculations</h3>
            <p className="text-sm text-fd-text-muted">Initial Margin Model</p>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <span className="text-sm text-fd-text-muted">As of {asOfDate}</span>
          <button
            onClick={onRefresh}
            className="p-2 text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50 rounded-md transition-colors"
            title="Refresh SIMM data"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
          </button>
        </div>
      </div>

      {simmCalculations.length === 0 ? (
        <div className="text-center py-8">
          <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <p className="text-fd-text-muted">No SIMM calculations available</p>
          <p className="text-sm text-fd-text-muted mt-1">Upload CRIF files to generate calculations</p>
        </div>
      ) : (
        <>
          {/* Calculation Selector */}
          <div className="flex items-center space-x-4 mb-6">
            <label className="text-sm font-medium text-fd-text">Calculation:</label>
            <select
              value={selectedCalculation?.id || ''}
              onChange={(e) => {
                const calc = simmCalculations.find(c => c.id === e.target.value);
                setSelectedCalculation(calc || null);
              }}
              className="bg-fd-dark border border-fd-border rounded-md px-3 py-2 text-fd-text focus:outline-none focus:ring-2 focus:ring-purple-500"
            >
              {simmCalculations.map((calc) => (
                <option key={calc.id} value={calc.id}>
                  {calc.portfolioId} - {calc.calculationDate} ({calc.parametersVersion})
                </option>
              ))}
            </select>
          </div>

          {selectedCalculation && (
            <>
              {/* Summary Card */}
              <div className="bg-fd-dark rounded-lg p-6 border border-fd-border mb-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Total Initial Margin</p>
                    <p className="text-2xl font-bold text-fd-text mt-1">
                      {formatCurrency(selectedCalculation.totalInitialMargin)}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Portfolio</p>
                    <p className="text-lg font-semibold text-fd-text mt-1">
                      {selectedCalculation.portfolioId}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Parameters Version</p>
                    <p className="text-lg font-semibold text-fd-text mt-1">
                      {selectedCalculation.parametersVersion}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-fd-text-muted">Status</p>
                    <div className="flex items-center space-x-2 mt-1">
                      {getStatusIcon(selectedCalculation.calculationStatus)}
                      <span className={`text-lg font-semibold ${getStatusColor(selectedCalculation.calculationStatus)}`}>
                        {selectedCalculation.calculationStatus}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Asset Class Summary */}
              <div className="bg-fd-dark rounded-lg p-6 border border-fd-border mb-6">
                <h4 className="text-lg font-medium text-fd-text mb-4">Asset Class Breakdown</h4>
                <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
                  {Object.entries(getAssetClassTotals()).map(([assetClass, totals]) => (
                    <div key={assetClass} className="bg-fd-darker rounded-lg p-4">
                      <p className="text-sm font-medium text-fd-text-muted">{assetClass}</p>
                      <p className="text-lg font-bold text-fd-text mt-1">
                        {formatCurrency(totals.initialMargin)}
                      </p>
                      <p className="text-xs text-fd-text-muted mt-1">
                        {totals.bucketCount} bucket{totals.bucketCount !== 1 ? 's' : ''}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Bucket Details */}
              <div className="bg-fd-dark rounded-lg border border-fd-border">
                <div className="px-6 py-4 border-b border-fd-border">
                  <div className="flex items-center justify-between">
                    <h4 className="text-lg font-medium text-fd-text">
                      Bucket Details ({selectedCalculation.buckets.length})
                    </h4>
                    <div className="flex items-center space-x-2">
                      <label className="text-sm text-fd-text-muted">Group by:</label>
                      <select
                        value={groupBy}
                        onChange={(e) => setGroupBy(e.target.value as 'assetClass' | 'bucket')}
                        className="bg-fd-darker border border-fd-border rounded-md px-3 py-1 text-sm text-fd-text focus:outline-none focus:ring-2 focus:ring-purple-500"
                      >
                        <option value="assetClass">Asset Class</option>
                        <option value="bucket">Bucket</option>
                      </select>
                    </div>
                  </div>
                </div>

                <div className="overflow-hidden">
                  {Object.entries(getGroupedBuckets()).map(([groupKey, buckets]) => (
                    <div key={groupKey} className="border-b border-fd-border last:border-b-0">
                      {/* Group Header */}
                      <div className="px-6 py-3 bg-fd-darker/50">
                        <h5 className="font-medium text-fd-text">
                          {groupBy === 'assetClass' ? `${groupKey} Asset Class` : `Bucket ${groupKey}`}
                        </h5>
                      </div>

                      {/* Bucket Rows */}
                      {buckets.map((bucket) => (
                        <div key={`${bucket.assetClass}-${bucket.bucketNumber}`}>
                          <div
                            className="px-6 py-4 hover:bg-fd-darker/30 cursor-pointer transition-colors"
                            onClick={() => toggleBucketExpansion(`${bucket.assetClass}-${bucket.bucketNumber}`)}
                          >
                            <div className="flex items-center justify-between">
                              <div className="flex items-center space-x-4">
                                <svg 
                                  className={`w-4 h-4 text-fd-text-muted transition-transform ${
                                    expandedBuckets.has(`${bucket.assetClass}-${bucket.bucketNumber}`) ? 'rotate-90' : ''
                                  }`} 
                                  fill="none" 
                                  stroke="currentColor" 
                                  viewBox="0 0 24 24"
                                >
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                                <div>
                                  <p className="font-medium text-fd-text">
                                    {groupBy === 'assetClass' ? `Bucket ${bucket.bucketNumber}` : bucket.assetClass}
                                  </p>
                                  <p className="text-sm text-fd-text-muted">
                                    {formatNumber(bucket.sensitivities)} sensitivities
                                  </p>
                                </div>
                              </div>
                              <div className="text-right">
                                <p className="font-medium text-fd-text">
                                  {formatCurrency(bucket.initialMargin)}
                                </p>
                                <p className="text-sm text-fd-text-muted">
                                  {formatNumber((bucket.initialMargin / selectedCalculation.totalInitialMargin) * 100, 1)}%
                                </p>
                              </div>
                            </div>
                          </div>

                          {/* Expanded Risk Details */}
                          {expandedBuckets.has(`${bucket.assetClass}-${bucket.bucketNumber}`) && (
                            <div className="px-6 pb-4 bg-fd-darker/20">
                              <div className="grid grid-cols-3 gap-4 pt-4">
                                <div className="bg-fd-dark rounded-md p-3">
                                  <p className="text-xs text-fd-text-muted uppercase tracking-wide">Delta</p>
                                  <p className="text-lg font-semibold text-fd-text mt-1">
                                    {formatCurrency(bucket.delta)}
                                  </p>
                                </div>
                                <div className="bg-fd-dark rounded-md p-3">
                                  <p className="text-xs text-fd-text-muted uppercase tracking-wide">Vega</p>
                                  <p className="text-lg font-semibold text-fd-text mt-1">
                                    {formatCurrency(bucket.vega)}
                                  </p>
                                </div>
                                <div className="bg-fd-dark rounded-md p-3">
                                  <p className="text-xs text-fd-text-muted uppercase tracking-wide">Curvature</p>
                                  <p className="text-lg font-semibold text-fd-text mt-1">
                                    {formatCurrency(bucket.curvature)}
                                  </p>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
};

export default SimmDashboard;