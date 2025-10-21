import React, { useState, useEffect } from 'react';
import { apiUrl } from '../../config/api';

interface MarginPosition {
  id: number;
  positionType: string;
  amount: number;
  currency: string;
  effectiveDate: string;
  accountNumber: string;
  portfolioCode: string;
  productClass: string;
  nettingSetId: string;
  createdAt: string;
}

interface MarginPositionsModalProps {
  statementId: number;
  statementName: string;
  onClose: () => void;
}

const MarginPositionsModal: React.FC<MarginPositionsModalProps> = ({ 
  statementId, 
  statementName,
  onClose 
}) => {
  const [positions, setPositions] = useState<MarginPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadPositions = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await fetch(apiUrl(`/margin-statements/${statementId}/positions`));
      if (!response.ok) {
        throw new Error('Failed to load positions');
      }
      
      const data = await response.json();
      setPositions(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load positions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPositions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statementId]);

  const formatCurrency = (amount: number, currency: string) => {
    if (amount === undefined || amount === null) return '-';
    if (!currency) currency = 'USD';
    try {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(amount);
    } catch {
      return `${currency} ${amount.toFixed(2)}`;
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch {
      return dateString;
    }
  };

  const getPositionTypeColor = (type: string) => {
    if (!type) return 'bg-gray-500/20 text-gray-300 border-gray-500/30';
    switch (type) {
      case 'VARIATION_MARGIN':
        return 'bg-blue-500/20 text-blue-300 border-blue-500/30';
      case 'INITIAL_MARGIN':
        return 'bg-purple-500/20 text-purple-300 border-purple-500/30';
      case 'EXCESS_COLLATERAL':
        return 'bg-green-500/20 text-green-300 border-green-500/30';
      default:
        return 'bg-gray-500/20 text-gray-300 border-gray-500/30';
    }
  };

  const getPositionTypeIcon = (type: string) => {
    if (!type) return null;
    switch (type) {
      case 'VARIATION_MARGIN':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
          </svg>
        );
      case 'INITIAL_MARGIN':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
          </svg>
        );
      case 'EXCESS_COLLATERAL':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        );
      default:
        return null;
    }
  };

  const formatPositionType = (type: string) => {
    if (!type) return 'Unknown';
    return type.split('_').map(word => 
      word.charAt(0) + word.slice(1).toLowerCase()
    ).join(' ');
  };

  const getTotalByType = (type: string) => {
    return positions
      .filter(p => p.positionType === type)
      .reduce((sum, p) => sum + p.amount, 0);
  };

  const totalVariationMargin = getTotalByType('VARIATION_MARGIN');
  const totalInitialMargin = getTotalByType('INITIAL_MARGIN');
  const totalExcess = getTotalByType('EXCESS_COLLATERAL');
  const grandTotal = totalVariationMargin + totalInitialMargin + totalExcess;

  // Handle escape key to close modal
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50 p-4">
      <div className="bg-fd-darker rounded-lg shadow-2xl border border-fd-border w-full max-w-6xl max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-fd-border bg-gradient-to-r from-fd-dark to-fd-darker">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                </svg>
              </div>
              <div>
                <h2 className="text-2xl font-bold text-fd-text">Margin Positions</h2>
                <p className="text-sm text-fd-text-muted">{statementName}</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-fd-text-muted hover:text-fd-text transition-colors p-2 hover:bg-fd-border/50 rounded-lg"
              aria-label="Close modal"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-fd-green mb-4"></div>
              <p className="text-fd-text-muted">Loading positions...</p>
            </div>
          ) : error ? (
            <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-6">
              <div className="flex items-start space-x-3">
                <svg className="w-6 h-6 text-red-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <div>
                  <h3 className="text-red-300 font-semibold mb-1">Error Loading Positions</h3>
                  <p className="text-red-200 text-sm">{error}</p>
                  <button
                    onClick={loadPositions}
                    className="mt-4 px-4 py-2 bg-red-500/20 text-red-300 rounded-lg hover:bg-red-500/30 transition-colors text-sm font-medium"
                  >
                    Try Again
                  </button>
                </div>
              </div>
            </div>
          ) : positions.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20">
              <div className="w-20 h-20 bg-fd-border/50 rounded-full flex items-center justify-center mb-4">
                <svg className="w-10 h-10 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path>
                </svg>
              </div>
              <h3 className="text-fd-text text-lg font-semibold mb-2">No Positions Found</h3>
              <p className="text-fd-text-muted text-sm">This statement does not have any positions recorded.</p>
            </div>
          ) : (
            <div className="space-y-6">
              {/* Summary Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-gradient-to-br from-blue-500/10 to-blue-600/5 border border-blue-500/20 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-blue-300 text-sm font-medium">Variation Margin</span>
                    <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
                    </svg>
                  </div>
                  <div className="text-2xl font-bold text-blue-200">
                    {formatCurrency(totalVariationMargin, positions[0]?.currency || 'USD')}
                  </div>
                  <div className="text-xs text-blue-300/60 mt-1">
                    {positions.filter(p => p.positionType === 'VARIATION_MARGIN').length} position(s)
                  </div>
                </div>

                <div className="bg-gradient-to-br from-purple-500/10 to-purple-600/5 border border-purple-500/20 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-purple-300 text-sm font-medium">Initial Margin</span>
                    <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                    </svg>
                  </div>
                  <div className="text-2xl font-bold text-purple-200">
                    {formatCurrency(totalInitialMargin, positions[0]?.currency || 'USD')}
                  </div>
                  <div className="text-xs text-purple-300/60 mt-1">
                    {positions.filter(p => p.positionType === 'INITIAL_MARGIN').length} position(s)
                  </div>
                </div>

                <div className="bg-gradient-to-br from-green-500/10 to-green-600/5 border border-green-500/20 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-green-300 text-sm font-medium">Excess Collateral</span>
                    <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                    </svg>
                  </div>
                  <div className="text-2xl font-bold text-green-200">
                    {formatCurrency(totalExcess, positions[0]?.currency || 'USD')}
                  </div>
                  <div className="text-xs text-green-300/60 mt-1">
                    {positions.filter(p => p.positionType === 'EXCESS_COLLATERAL').length} position(s)
                  </div>
                </div>

                <div className="bg-gradient-to-br from-cyan-500/10 to-cyan-600/5 border border-cyan-500/20 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-cyan-300 text-sm font-medium">Total Margin</span>
                    <svg className="w-5 h-5 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                    </svg>
                  </div>
                  <div className="text-2xl font-bold text-cyan-200">
                    {formatCurrency(grandTotal, positions[0]?.currency || 'USD')}
                  </div>
                  <div className="text-xs text-cyan-300/60 mt-1">
                    {positions.length} total position(s)
                  </div>
                </div>
              </div>

              {/* Positions Table */}
              <div className="bg-fd-dark rounded-lg border border-fd-border overflow-hidden">
                <div className="px-4 py-3 bg-gradient-to-r from-fd-darker to-fd-dark border-b border-fd-border">
                  <h3 className="text-lg font-semibold text-fd-text">Position Details</h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-fd-darker">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Type
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Amount
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Netting Set
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Portfolio
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Product Class
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Effective Date
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-fd-dark divide-y divide-fd-border">
                      {positions.map((position) => (
                        <tr key={position.id} className="hover:bg-fd-darker/50 transition-colors">
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className={`inline-flex items-center space-x-2 px-3 py-1.5 rounded-lg border ${getPositionTypeColor(position.positionType)}`}>
                              {getPositionTypeIcon(position.positionType)}
                              <span className="text-sm font-medium">
                                {formatPositionType(position.positionType)}
                              </span>
                            </div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-fd-text font-semibold">
                              {formatCurrency(position.amount, position.currency)}
                            </div>
                            <div className="text-xs text-fd-text-muted">{position.currency}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm text-fd-text font-mono">{position.nettingSetId || '-'}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm text-fd-text">{position.portfolioCode || '-'}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm text-fd-text">{position.productClass || '-'}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm text-fd-text">{formatDate(position.effectiveDate)}</div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-fd-border bg-fd-darker flex justify-between items-center">
          <div className="text-sm text-fd-text-muted">
            {positions.length > 0 && (
              <span>Showing {positions.length} position{positions.length !== 1 ? 's' : ''}</span>
            )}
          </div>
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-fd-green text-fd-dark rounded-lg hover:bg-fd-green/90 transition-colors font-medium"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default MarginPositionsModal;
