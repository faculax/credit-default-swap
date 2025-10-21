import React, { useState } from 'react';
import { formatCurrency, formatNumber } from '../../utils/formatters';

interface SaCcrNettingSetData {
  nettingSetId: string;
  exposureAtDefault: number;
  replacementCost: number;
  potentialFutureExposure: number;
  alphaFactor: number;
  effectiveNotional: number;
}

interface SaCcrSummary {
  totalExposureAtDefault: number;
  totalReplacementCost: number;
  totalPotentialFutureExposure: number;
  nettingSetCount: number;
  calculationsCount: number;
  nettingSets: Record<string, SaCcrNettingSetData>;
}

interface SaCcrDashboardProps {
  saCcrSummary: SaCcrSummary;
  asOfDate: string;
  onRefresh: () => void;
}

const SaCcrDashboard: React.FC<SaCcrDashboardProps> = ({
  saCcrSummary,
  asOfDate,
  onRefresh
}) => {
  const [expandedNettingSet, setExpandedNettingSet] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'ead' | 'rc' | 'pfe'>('ead');

  const getSortedNettingSets = () => {
    const nettingSets = Object.values(saCcrSummary.nettingSets || {});
    return nettingSets.sort((a, b) => {
      switch (sortBy) {
        case 'ead':
          return b.exposureAtDefault - a.exposureAtDefault;
        case 'rc':
          return b.replacementCost - a.replacementCost;
        case 'pfe':
          return b.potentialFutureExposure - a.potentialFutureExposure;
        default:
          return 0;
      }
    });
  };

  const getExposureColor = (exposure: number, total: number) => {
    const percentage = (exposure / total) * 100;
    if (percentage > 40) return 'text-red-400';
    if (percentage > 20) return 'text-yellow-400';
    return 'text-green-400';
  };

  const toggleNettingSetDetails = (nettingSetId: string) => {
    setExpandedNettingSet(expandedNettingSet === nettingSetId ? null : nettingSetId);
  };

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          <svg className="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
          </svg>
          <div>
            <h3 className="text-xl font-semibold text-fd-text">SA-CCR Exposures</h3>
            <p className="text-sm text-fd-text-muted">Basel III Regulatory Capital</p>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          <span className="text-sm text-fd-text-muted">As of {asOfDate}</span>
          <button
            onClick={onRefresh}
            className="p-2 text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50 rounded-md transition-colors"
            title="Refresh SA-CCR data"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {/* Total EAD */}
        <div className="bg-fd-dark rounded-lg p-6 border border-fd-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-fd-text-muted">Total EAD</p>
              <p className="text-2xl font-bold text-fd-text mt-1">
                {formatCurrency(saCcrSummary.totalExposureAtDefault)}
              </p>
              <p className="text-xs text-fd-text-muted mt-1">
                α × (RC + PFE)
              </p>
            </div>
            <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
              </svg>
            </div>
          </div>
        </div>

        {/* Replacement Cost */}
        <div className="bg-fd-dark rounded-lg p-6 border border-fd-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-fd-text-muted">Total RC</p>
              <p className="text-2xl font-bold text-fd-text mt-1">
                {formatCurrency(saCcrSummary.totalReplacementCost)}
              </p>
              <p className="text-xs text-fd-text-muted mt-1">
                Replacement Cost
              </p>
            </div>
            <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1"></path>
              </svg>
            </div>
          </div>
        </div>

        {/* Potential Future Exposure */}
        <div className="bg-fd-dark rounded-lg p-6 border border-fd-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-fd-text-muted">Total PFE</p>
              <p className="text-2xl font-bold text-fd-text mt-1">
                {formatCurrency(saCcrSummary.totalPotentialFutureExposure)}
              </p>
              <p className="text-xs text-fd-text-muted mt-1">
                Potential Future Exposure
              </p>
            </div>
            <div className="w-12 h-12 bg-yellow-500/20 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z"></path>
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* Netting Sets Table */}
      <div className="bg-fd-dark rounded-lg border border-fd-border">
        <div className="px-6 py-4 border-b border-fd-border">
          <div className="flex items-center justify-between">
            <h4 className="text-lg font-medium text-fd-text">
              Netting Sets ({saCcrSummary.nettingSetCount})
            </h4>
            <div className="flex items-center space-x-2">
              <label className="text-sm text-fd-text-muted">Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'ead' | 'rc' | 'pfe')}
                className="bg-fd-darker border border-fd-border rounded-md px-3 py-1 text-sm text-fd-text focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="ead">EAD</option>
                <option value="rc">RC</option>
                <option value="pfe">PFE</option>
              </select>
            </div>
          </div>
        </div>

        <div className="overflow-hidden">
          {getSortedNettingSets().map((nettingSet) => (
            <div key={nettingSet.nettingSetId} className="border-b border-fd-border last:border-b-0">
              {/* Netting Set Row */}
              <div
                className="px-6 py-4 hover:bg-fd-darker/50 cursor-pointer transition-colors"
                onClick={() => toggleNettingSetDetails(nettingSet.nettingSetId)}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <svg 
                      className={`w-4 h-4 text-fd-text-muted transition-transform ${
                        expandedNettingSet === nettingSet.nettingSetId ? 'rotate-90' : ''
                      }`} 
                      fill="none" 
                      stroke="currentColor" 
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7"></path>
                    </svg>
                    <div>
                      <p className="font-medium text-fd-text">{nettingSet.nettingSetId}</p>
                      <p className="text-sm text-fd-text-muted">
                        Notional: {formatCurrency(nettingSet.effectiveNotional)}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-8 text-right">
                    <div>
                      <p className={`font-medium ${getExposureColor(nettingSet.exposureAtDefault, saCcrSummary.totalExposureAtDefault)}`}>
                        {formatCurrency(nettingSet.exposureAtDefault)}
                      </p>
                      <p className="text-xs text-fd-text-muted">EAD</p>
                    </div>
                    <div>
                      <p className="font-medium text-fd-text">
                        {formatCurrency(nettingSet.replacementCost)}
                      </p>
                      <p className="text-xs text-fd-text-muted">RC</p>
                    </div>
                    <div>
                      <p className="font-medium text-fd-text">
                        {formatCurrency(nettingSet.potentialFutureExposure)}
                      </p>
                      <p className="text-xs text-fd-text-muted">PFE</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Expanded Details */}
              {expandedNettingSet === nettingSet.nettingSetId && (
                <div className="px-6 pb-4 bg-fd-darker/30">
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4">
                    <div className="bg-fd-dark rounded-md p-3">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">Alpha Factor</p>
                      <p className="text-lg font-semibold text-fd-text mt-1">
                        {formatNumber(nettingSet.alphaFactor, 2)}
                      </p>
                    </div>
                    <div className="bg-fd-dark rounded-md p-3">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">Effective Notional</p>
                      <p className="text-lg font-semibold text-fd-text mt-1">
                        {formatCurrency(nettingSet.effectiveNotional)}
                      </p>
                    </div>
                    <div className="bg-fd-dark rounded-md p-3">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">RC Component</p>
                      <p className="text-lg font-semibold text-fd-text mt-1">
                        {formatNumber((nettingSet.replacementCost / nettingSet.exposureAtDefault) * 100, 1)}%
                      </p>
                    </div>
                    <div className="bg-fd-dark rounded-md p-3">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">PFE Component</p>
                      <p className="text-lg font-semibold text-fd-text mt-1">
                        {formatNumber((nettingSet.potentialFutureExposure / nettingSet.exposureAtDefault) * 100, 1)}%
                      </p>
                    </div>
                  </div>
                  
                  {/* Formula Display */}
                  <div className="mt-4 p-3 bg-fd-dark rounded-md border border-fd-border">
                    <p className="text-xs text-fd-text-muted uppercase tracking-wide mb-2">SA-CCR Formula</p>
                    <p className="font-mono text-sm text-fd-text">
                      EAD = α × (RC + PFE) = {formatNumber(nettingSet.alphaFactor, 2)} × ({formatCurrency(nettingSet.replacementCost)} + {formatCurrency(nettingSet.potentialFutureExposure)}) = {formatCurrency(nettingSet.exposureAtDefault)}
                    </p>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {getSortedNettingSets().length === 0 && (
          <div className="px-6 py-8 text-center">
            <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            <p className="text-fd-text-muted">No SA-CCR calculations available</p>
            <p className="text-sm text-fd-text-muted mt-1">Run calculations to view exposure data</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default SaCcrDashboard;