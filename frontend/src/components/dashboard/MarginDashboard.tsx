import React, { useState, useEffect } from 'react';
import { formatCurrency, formatDateTime } from '../../utils/formatters';

interface MarginStatement {
  id: number;
  statementId: string;
  ccpName: string;
  memberFirm: string;
  accountNumber: string;
  statementDate: string;
  currency: string;
  format: string;
  fileName: string;
  fileSize: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  processedAt: string | null;
  errorMessage: string;
  retryCount: number;
  variationMargin?: number;
  initialMargin?: number;
  totalPositions?: number;
}

interface MarginPosition {
  id: number;
  positionType: string;
  amount: number;
  currency: string;
  accountNumber: string;
  portfolioCode: string;
  nettingSetId: string;
  productClass: string;
  effectiveDate: string;
}

interface MarginDashboardProps {
  asOfDate: string;
  onRefresh: () => void;
}

const MarginDashboard: React.FC<MarginDashboardProps> = ({
  asOfDate,
  onRefresh
}) => {
  const [statements, setStatements] = useState<MarginStatement[]>([]);
  const [selectedStatement, setSelectedStatement] = useState<MarginStatement | null>(null);
  const [positions, setPositions] = useState<MarginPosition[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingPositions, setLoadingPositions] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<string>('ALL');

  const statusFilters = ['ALL', 'PROCESSED', 'PENDING', 'FAILED'];

  useEffect(() => {
    loadStatements();
  }, [asOfDate]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadStatements = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await fetch(`/api/margin-statements?asOfDate=${asOfDate}`);
      if (!response.ok) {
        throw new Error('Failed to load statements');
      }
      
      const data = await response.json();
      setStatements(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load statements');
    } finally {
      setLoading(false);
    }
  };

  const loadPositions = async (statementId: number) => {
    try {
      setLoadingPositions(true);
      const response = await fetch(`/api/margin-statements/${statementId}/positions`);
      if (!response.ok) {
        throw new Error('Failed to load positions');
      }
      const data = await response.json();
      setPositions(data);
    } catch (err: any) {
      console.error('Failed to load positions:', err);
      setPositions([]);
    } finally {
      setLoadingPositions(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'text-yellow-400';
      case 'PROCESSING':
        return 'text-blue-400';
      case 'PROCESSED':
        return 'text-green-400';
      case 'FAILED':
        return 'text-red-400';
      case 'DISPUTED':
        return 'text-orange-400';
      default:
        return 'text-fd-text-muted';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        );
      case 'PROCESSING':
        return (
          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-current"></div>
        );
      case 'PROCESSED':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        );
      case 'FAILED':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        );
      default:
        return null;
    }
  };

  const filteredStatements = statements.filter(statement => 
    filter === 'ALL' || statement.status === filter
  );

  const handleStatementClick = (statement: MarginStatement) => {
    setSelectedStatement(statement);
    if (statement.status === 'PROCESSED') {
      loadPositions(statement.id);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-400"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-4">
        <div className="flex items-center">
          <svg className="w-5 h-5 text-red-400 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <div>
            <h3 className="text-sm font-medium text-red-400">Error Loading Statements</h3>
            <p className="text-sm text-red-300 mt-1">{error}</p>
          </div>
        </div>
        <button
          onClick={loadStatements}
          className="mt-3 px-3 py-1 bg-red-500/30 text-red-400 rounded text-sm hover:bg-red-500/40 transition-colors"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border">
      {/* Header */}
      <div className="px-6 py-4 border-b border-fd-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <svg className="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            <div>
              <h3 className="text-xl font-semibold text-fd-text">Margin Statements</h3>
              <p className="text-sm text-fd-text-muted">Daily VM/IM positions from CCPs</p>
            </div>
          </div>
          
          <div className="flex items-center space-x-3">
            <span className="text-sm text-fd-text-muted">As of {asOfDate}</span>
            <button
              onClick={() => { loadStatements(); onRefresh(); }}
              className="p-2 text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50 rounded-md transition-colors"
              title="Refresh statements"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
            </button>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex space-x-1 bg-fd-dark rounded-lg p-1 mt-4">
          {statusFilters.map(status => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                filter === status
                  ? 'bg-blue-500 text-white'
                  : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
              }`}
            >
              {status}
              <span className="ml-1 text-xs">
                ({status === 'ALL' ? statements.length : statements.filter(s => s.status === status).length})
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Statements Grid */}
      <div className="p-6">
        {filteredStatements.length === 0 ? (
          <div className="text-center py-12">
            <svg className="mx-auto h-12 w-12 text-fd-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
            </svg>
            <h3 className="mt-2 text-sm font-medium text-fd-text">No margin statements found</h3>
            <p className="mt-1 text-sm text-fd-text-muted">
              {filter === 'ALL' ? `No statements available for ${asOfDate}` : `No statements with status: ${filter}`}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredStatements.map((statement) => (
              <div
                key={statement.id}
                className="bg-fd-dark rounded-lg p-4 border border-fd-border hover:border-blue-500/50 cursor-pointer transition-all duration-200 hover:shadow-lg"
                onClick={() => handleStatementClick(statement)}
              >
                {/* Statement Header */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center space-x-2">
                    <div className={`flex items-center space-x-1 ${getStatusColor(statement.status)}`}>
                      {getStatusIcon(statement.status)}
                      <span className="text-xs font-medium uppercase">{statement.status}</span>
                    </div>
                  </div>
                  <span className="text-xs text-fd-text-muted">
                    {new Date(statement.statementDate).toLocaleDateString()}
                  </span>
                </div>

                {/* Statement Details */}
                <div className="space-y-2">
                  <div>
                    <h4 className="font-medium text-fd-text truncate">{statement.statementId}</h4>
                    <p className="text-sm text-fd-text-muted">{statement.ccpName}</p>
                  </div>

                  <div className="flex justify-between text-sm">
                    <span className="text-fd-text-muted">Account:</span>
                    <span className="text-fd-text font-mono">{statement.accountNumber}</span>
                  </div>

                  {statement.status === 'PROCESSED' && (
                    <>
                      {statement.variationMargin !== undefined && (
                        <div className="flex justify-between text-sm">
                          <span className="text-fd-text-muted">VM:</span>
                          <span className={`font-medium ${statement.variationMargin >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                            {formatCurrency(statement.variationMargin)}
                          </span>
                        </div>
                      )}
                      {statement.initialMargin !== undefined && (
                        <div className="flex justify-between text-sm">
                          <span className="text-fd-text-muted">IM:</span>
                          <span className="text-fd-text font-medium">
                            {formatCurrency(statement.initialMargin)}
                          </span>
                        </div>
                      )}
                      {statement.totalPositions !== undefined && (
                        <div className="flex justify-between text-sm">
                          <span className="text-fd-text-muted">Positions:</span>
                          <span className="text-fd-text">{statement.totalPositions}</span>
                        </div>
                      )}
                    </>
                  )}

                  <div className="flex justify-between text-xs text-fd-text-muted">
                    <span>{formatFileSize(statement.fileSize)} {statement.format}</span>
                    <span>{formatDateTime(statement.createdAt)}</span>
                  </div>
                </div>

                {statement.errorMessage && (
                  <div className="mt-2 p-2 bg-red-500/20 border border-red-500/50 rounded text-xs text-red-400">
                    {statement.errorMessage.substring(0, 100)}...
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Statement Detail Modal */}
      {selectedStatement && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-fd-darker rounded-lg shadow-lg border border-fd-border p-6 max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-fd-text">{selectedStatement.statementId}</h3>
              <button 
                onClick={() => setSelectedStatement(null)}
                className="text-fd-text-muted hover:text-fd-text transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            {/* Statement Info */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6 text-sm">
              <div>
                <span className="text-fd-text-muted">CCP:</span>
                <div className="text-fd-text font-medium">{selectedStatement.ccpName}</div>
              </div>
              <div>
                <span className="text-fd-text-muted">Account:</span>
                <div className="text-fd-text font-mono">{selectedStatement.accountNumber}</div>
              </div>
              <div>
                <span className="text-fd-text-muted">Date:</span>
                <div className="text-fd-text">{new Date(selectedStatement.statementDate).toLocaleDateString()}</div>
              </div>
              <div>
                <span className="text-fd-text-muted">Status:</span>
                <div className={`flex items-center space-x-1 ${getStatusColor(selectedStatement.status)}`}>
                  {getStatusIcon(selectedStatement.status)}
                  <span className="font-medium">{selectedStatement.status}</span>
                </div>
              </div>
            </div>

            {/* Positions Table */}
            {selectedStatement.status === 'PROCESSED' && (
              <div className="border border-fd-border rounded-lg">
                <div className="px-4 py-3 border-b border-fd-border bg-fd-dark">
                  <h4 className="font-medium text-fd-text">Margin Positions</h4>
                </div>
                
                {loadingPositions ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-400"></div>
                  </div>
                ) : positions.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead className="bg-fd-dark text-xs">
                        <tr>
                          <th className="px-4 py-2 text-left text-fd-text-muted">Netting Set</th>
                          <th className="px-4 py-2 text-left text-fd-text-muted">Account</th>
                          <th className="px-4 py-2 text-left text-fd-text-muted">Product Class</th>
                          <th className="px-4 py-2 text-left text-fd-text-muted">Type</th>
                          <th className="px-4 py-2 text-right text-fd-text-muted">Amount</th>
                          <th className="px-4 py-2 text-right text-fd-text-muted">Currency</th>
                        </tr>
                      </thead>
                      <tbody className="text-sm">
                        {positions.map((position) => (
                          <tr key={position.id} className="border-t border-fd-border hover:bg-fd-dark/50">
                            <td className="px-4 py-2 text-fd-text font-mono text-xs">{position.nettingSetId}</td>
                            <td className="px-4 py-2 text-fd-text">{position.accountNumber}</td>
                            <td className="px-4 py-2 text-fd-text">{position.productClass}</td>
                            <td className="px-4 py-2 text-fd-text">
                              <span className={`px-2 py-1 rounded text-xs ${position.positionType === 'VARIATION_MARGIN' ? 'bg-green-500/20 text-green-400' : 'bg-blue-500/20 text-blue-400'}`}>
                                {position.positionType === 'VARIATION_MARGIN' ? 'VM' : 'IM'}
                              </span>
                            </td>
                            <td className={`px-4 py-2 text-right font-medium ${position.amount >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                              {formatCurrency(position.amount)}
                            </td>
                            <td className="px-4 py-2 text-right text-fd-text-muted">{position.currency}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="text-center py-8 text-fd-text-muted">
                    No positions found
                  </div>
                )}
              </div>
            )}

            {selectedStatement.errorMessage && (
              <div className="mt-4 bg-red-500/20 border border-red-500/50 rounded-lg p-3">
                <div className="text-red-400 text-sm font-medium mb-1">Error Message:</div>
                <div className="text-red-300 text-sm">{selectedStatement.errorMessage}</div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default MarginDashboard;