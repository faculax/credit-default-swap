import React, { useState, useEffect } from 'react';
import { Bond, bondService } from '../../services/bondService';
import BondCreationModal from './BondCreationModal';
import BondDetailView from './BondDetailView';

const BondList: React.FC = () => {
  const [bonds, setBonds] = useState<Bond[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedBondId, setSelectedBondId] = useState<number | null>(null);

  useEffect(() => {
    loadBonds();
  }, []);

  const loadBonds = async () => {
    try {
      setIsLoading(true);
      const bondsData = await bondService.getAllBonds();
      setBonds(bondsData);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load bonds');
    } finally {
      setIsLoading(false);
    }
  };

  const handleBondCreated = (newBond: Bond) => {
    setBonds((prev) => [...prev, newBond]);
  };

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatPercent = (value: number) => {
    return `${(value * 100).toFixed(2)}%`;
  };

  if (isLoading) {
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
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error Loading Bonds</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
            <div className="mt-4">
              <button
                onClick={loadBonds}
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
      {/* Header */}
      <div className="px-6 py-4 border-b border-fd-border">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-fd-text">Bonds</h2>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-fd-text-muted">
              {bonds.length} bond{bonds.length !== 1 ? 's' : ''}
            </span>
            <button
              onClick={() => setIsCreateModalOpen(true)}
              className="px-3 py-1 bg-fd-green text-fd-dark rounded hover:bg-fd-green-hover text-sm font-medium"
            >
              + New Bond
            </button>
          </div>
        </div>
      </div>

      {/* Bonds Table */}
      {bonds.length === 0 ? (
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
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-fd-text">No bonds found</h3>
          <p className="mt-1 text-sm text-fd-text-muted">Create your first bond to get started.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-fd-border">
            <thead className="bg-fd-dark">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  ISIN
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Issuer
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Seniority
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Notional
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Coupon
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Maturity
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-fd-darker divide-y divide-fd-border">
              {bonds.map((bond) => (
                <tr key={bond.id} className="cursor-pointer hover:bg-fd-dark transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm font-medium text-fd-text">{bond.isin || 'N/A'}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">{bond.issuer}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">
                      {bond.seniority === 'SR_UNSEC'
                        ? 'Sr. Unsec.'
                        : bond.seniority === 'SR_SEC'
                          ? 'Sr. Sec.'
                          : 'Subord.'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">
                      {formatCurrency(bond.notional, bond.currency)}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">{formatPercent(bond.couponRate)}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-fd-text">{bond.maturityDate}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <button
                      onClick={() => setSelectedBondId(bond.id!)}
                      className="text-fd-green hover:text-fd-green-hover transition-colors"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Modals */}
      <BondCreationModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleBondCreated}
      />

      {selectedBondId && (
        <BondDetailView bondId={selectedBondId} onClose={() => setSelectedBondId(null)} />
      )}
    </div>
  );
};

export default BondList;
