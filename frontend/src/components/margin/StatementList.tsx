import React, { useState, useEffect } from 'react';

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
}

interface StatementListProps {
  refreshTrigger?: number;
}

const StatementList: React.FC<StatementListProps> = ({ refreshTrigger }) => {
  const [statements, setStatements] = useState<MarginStatement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<string>('ALL');
  const [selectedStatement, setSelectedStatement] = useState<MarginStatement | null>(null);

  const statusFilters = ['ALL', 'PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DISPUTED', 'RETRYING'];

  useEffect(() => {
    loadStatements();
  }, [refreshTrigger]);

  const loadStatements = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await fetch('/api/margin-statements');
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

  const handleRetryFailed = async () => {
    try {
      const response = await fetch('/api/margin-statements/retry-failed', {
        method: 'POST'
      });
      
      if (!response.ok) {
        throw new Error('Failed to retry statements');
      }
      
      // Refresh the list after retry
      setTimeout(() => {
        loadStatements();
      }, 1000);
      
    } catch (err: any) {
      setError(err.message || 'Failed to retry statements');
    }
  };

  const formatDateTime = (dateString: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
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
        return 'bg-yellow-100 text-yellow-800';
      case 'PROCESSING':
        return 'bg-blue-100 text-blue-800';
      case 'PROCESSED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'DISPUTED':
        return 'bg-orange-100 text-orange-800';
      case 'RETRYING':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
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
      case 'DISPUTED':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
          </svg>
        );
      case 'RETRYING':
        return (
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
          </svg>
        );
      default:
        return null;
    }
  };

  const filteredStatements = statements.filter(statement => 
    filter === 'ALL' || statement.status === filter
  );

  const failedCount = statements.filter(s => s.status === 'FAILED').length;

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-fd-green"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error Loading Statements</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
            <div className="mt-4">
              <button
                onClick={loadStatements}
                className="bg-red-100 px-3 py-2 rounded-md text-sm font-medium text-red-800 hover:bg-red-200"
              >
                Retry
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-fd-darker rounded-lg border border-fd-border">
      {/* Header */}
      <div className="p-6 border-b border-fd-border">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-green-500/20 rounded-full flex items-center justify-center">
              <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-fd-text">Margin Statements</h2>
          </div>
          
          <div className="flex items-center space-x-4">
            {failedCount > 0 && (
              <button
                onClick={handleRetryFailed}
                className="px-4 py-2 bg-orange-500 text-white rounded hover:bg-orange-600 transition-colors flex items-center space-x-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                </svg>
                <span>Retry Failed ({failedCount})</span>
              </button>
            )}
            
            <button
              onClick={loadStatements}
              className="px-4 py-2 bg-fd-green text-fd-dark rounded hover:bg-fd-green/80 transition-colors flex items-center space-x-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
              <span>Refresh</span>
            </button>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex space-x-1 bg-fd-dark rounded-lg p-1">
          {statusFilters.map(status => (
            <button
              key={status}
              onClick={() => setFilter(status)}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                filter === status
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text-muted hover:text-fd-text hover:bg-fd-border/50'
              }`}
            >
              {status}
              {status !== 'ALL' && (
                <span className="ml-1 text-xs">
                  ({statements.filter(s => s.status === status).length})
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Statements Table */}
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-fd-dark">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Statement
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                CCP & Account
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Date & Size
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Timing
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-fd-darker divide-y divide-fd-border">
            {filteredStatements.map((statement) => (
              <tr
                key={statement.id}
                className="hover:bg-fd-dark transition-colors cursor-pointer"
                onClick={() => setSelectedStatement(statement)}
              >
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex flex-col">
                    <div className="text-sm font-medium text-fd-text">
                      {statement.statementId}
                    </div>
                    <div className="text-sm text-fd-text-muted">
                      {statement.fileName}
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex flex-col">
                    <div className="text-sm font-medium text-fd-text">
                      {statement.ccpName}
                    </div>
                    <div className="text-sm text-fd-text-muted">
                      {statement.accountNumber}
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex flex-col">
                    <div className="text-sm text-fd-text">
                      {new Date(statement.statementDate).toLocaleDateString()}
                    </div>
                    <div className="text-sm text-fd-text-muted">
                      {formatFileSize(statement.fileSize)} {statement.format}
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(statement.status)}`}>
                    <span className="mr-1">{getStatusIcon(statement.status)}</span>
                    {statement.status}
                    {statement.retryCount > 0 && (
                      <span className="ml-1">({statement.retryCount})</span>
                    )}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                  <div className="flex flex-col">
                    <div>Created: {formatDateTime(statement.createdAt)}</div>
                    {statement.processedAt && (
                      <div>Processed: {formatDateTime(statement.processedAt)}</div>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setSelectedStatement(statement);
                    }}
                    className="text-fd-green hover:text-fd-green/80 font-medium"
                  >
                    View Details
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filteredStatements.length === 0 && (
        <div className="text-center py-12">
          <svg
            className="mx-auto h-12 w-12 text-fd-text-muted"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-fd-text">No statements found</h3>
          <p className="mt-1 text-sm text-fd-text-muted">
            {filter === 'ALL' ? 'Upload your first margin statement to get started.' : `No statements with status: ${filter}`}
          </p>
        </div>
      )}

      {/* Statement Detail Modal */}
      {selectedStatement && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-xl font-bold text-fd-text">Statement Details</h3>
              <button 
                onClick={() => setSelectedStatement(null)}
                className="text-fd-text-muted hover:text-fd-text transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-fd-text-muted">Statement ID:</span>
                  <span className="text-fd-text ml-2 font-mono">{selectedStatement.statementId}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">CCP:</span>
                  <span className="text-fd-text ml-2">{selectedStatement.ccpName}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">Member Firm:</span>
                  <span className="text-fd-text ml-2">{selectedStatement.memberFirm}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">Account:</span>
                  <span className="text-fd-text ml-2">{selectedStatement.accountNumber}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">Date:</span>
                  <span className="text-fd-text ml-2">{new Date(selectedStatement.statementDate).toLocaleDateString()}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">Currency:</span>
                  <span className="text-fd-text ml-2">{selectedStatement.currency}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">Format:</span>
                  <span className="text-fd-text ml-2">{selectedStatement.format}</span>
                </div>
                <div>
                  <span className="text-fd-text-muted">File Size:</span>
                  <span className="text-fd-text ml-2">{formatFileSize(selectedStatement.fileSize)}</span>
                </div>
              </div>

              {selectedStatement.errorMessage && (
                <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3">
                  <div className="text-red-400 text-sm font-medium mb-1">Error Message:</div>
                  <div className="text-red-300 text-sm">{selectedStatement.errorMessage}</div>
                </div>
              )}

              <div className="flex justify-end space-x-4 pt-4 border-t border-fd-border">
                <button
                  onClick={() => setSelectedStatement(null)}
                  className="px-4 py-2 bg-fd-border text-fd-text rounded hover:bg-fd-border/80 transition-colors"
                >
                  Close
                </button>
                {selectedStatement.status === 'PROCESSED' && (
                  <button
                    onClick={() => {
                      // Navigate to positions view
                      window.open(`/api/margin-statements/${selectedStatement.id}/positions`, '_blank');
                    }}
                    className="px-4 py-2 bg-fd-green text-fd-dark rounded hover:bg-fd-green/80 transition-colors"
                  >
                    View Positions
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StatementList;