import React, { useState, useEffect } from 'react';
import { portfolioService, CdsPortfolio } from '../../services/portfolioService';
import CreatePortfolioModal from './CreatePortfolioModal';

interface PortfolioListProps {
  onPortfolioSelect?: (portfolio: CdsPortfolio) => void;
}

const PortfolioList: React.FC<PortfolioListProps> = ({ onPortfolioSelect }) => {
  const [portfolios, setPortfolios] = useState<CdsPortfolio[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<number | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);

  useEffect(() => {
    loadPortfolios();
  }, []);

  const loadPortfolios = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await portfolioService.getAllPortfolios();
      setPortfolios(data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load portfolios';
      setError(errorMessage);
      console.error('Error loading portfolios:', err);
      
      // Log additional details for debugging
      if (err instanceof Error && err.message.includes('Invalid response format')) {
        console.error('The server returned malformed JSON. Check backend logs for details.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePortfolioClick = (portfolio: CdsPortfolio) => {
    setSelectedPortfolioId(portfolio.id);
    if (onPortfolioSelect) {
      onPortfolioSelect(portfolio);
    }
  };

  const handleCreateSuccess = () => {
    setShowCreateModal(false);
    loadPortfolios();
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

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
            <h3 className="text-sm font-medium text-red-800">Error Loading Portfolios</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
            <div className="mt-4">
              <button
                onClick={loadPortfolios}
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
    <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border">
      <div className="px-6 py-4 border-b border-fd-border">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-fd-text">CDS Portfolios</h2>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-fd-text-muted">
              {portfolios.length} portfolio{portfolios.length !== 1 ? 's' : ''}
            </span>
            <button
              onClick={() => setShowCreateModal(true)}
              className="px-3 py-1 bg-fd-green text-fd-dark rounded hover:bg-fd-green-hover text-sm font-medium"
            >
              + Create Portfolio
            </button>
          </div>
        </div>
      </div>

      {portfolios.length === 0 ? (
        <div className="text-center py-12">
          <svg
            className="mx-auto h-12 w-12 text-fd-text-muted"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
            />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-fd-text">No portfolios found</h3>
          <p className="mt-1 text-sm text-fd-text-muted">Create your first portfolio to get started.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-fd-border">
            <thead className="bg-fd-dark">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Description
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  # Trades
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Created
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Updated
                </th>
              </tr>
            </thead>
            <tbody className="bg-fd-darker divide-y divide-fd-border">
              {portfolios.map((portfolio) => (
                <tr
                  key={portfolio.id}
                  onClick={() => handlePortfolioClick(portfolio)}
                  className={`cursor-pointer hover:bg-fd-dark transition-colors ${
                    selectedPortfolioId === portfolio.id ? 'bg-fd-dark ring-2 ring-fd-green' : ''
                  }`}
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-fd-text">{portfolio.name}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-fd-text">{portfolio.description || '-'}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">
                      {portfolio.constituents?.filter((c: any) => c.active).length || 0}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">{formatDate(portfolio.createdAt)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">{formatDate(portfolio.updatedAt)}</div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <CreatePortfolioModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}
    </div>
  );
};

export default PortfolioList;
