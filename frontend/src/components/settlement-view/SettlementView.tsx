import React from 'react';

export interface SettlementView {
  type: 'cash' | 'physical';
  tradeId: string;
  creditEventId: string;
  createdAt: string;

  // Cash settlement fields
  notional?: number;
  recoveryRate?: number;
  payoutAmount?: number;
  calculatedAt?: string;

  // Physical settlement fields
  referenceObligationIsin?: string;
  proposedDeliveryDate?: string;
  notes?: string;
  status?: string;
}

interface SettlementViewProps {
  settlement: SettlementView | null;
  isLoading?: boolean;
  error?: string;
}

const SettlementViewComponent: React.FC<SettlementViewProps> = ({
  settlement,
  isLoading = false,
  error,
}) => {
  if (isLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
        <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
        <div className="h-4 bg-gray-200 rounded w-2/3 mb-2"></div>
        <div className="h-4 bg-gray-200 rounded w-1/4"></div>
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
            <h3 className="text-sm font-medium text-red-800">Settlement Error</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!settlement) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-md p-4">
        <p className="text-sm text-gray-600">No settlement information available.</p>
      </div>
    );
  }

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const formatPercentage = (rate: number): string => {
    return `${(rate * 100).toFixed(2)}%`;
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-6">
      <div className="flex items-center mb-4">
        <div
          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
            settlement.type === 'cash' ? 'bg-green-100 text-green-800' : 'bg-blue-100 text-blue-800'
          }`}
        >
          {settlement.type === 'cash' ? 'Cash Settlement' : 'Physical Settlement'}
        </div>
      </div>

      {settlement.type === 'cash' && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700">Notional Amount</label>
              <p className="mt-1 text-lg font-semibold text-gray-900">
                {settlement.notional ? formatCurrency(settlement.notional) : 'N/A'}
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700">Recovery Rate</label>
              <p className="mt-1 text-lg font-semibold text-gray-900">
                {settlement.recoveryRate !== undefined
                  ? formatPercentage(settlement.recoveryRate)
                  : 'N/A'}
              </p>
            </div>
          </div>

          <div className="border-t border-gray-200 pt-4">
            <label className="block text-sm font-medium text-gray-700">Payout Amount</label>
            <p className="mt-1 text-2xl font-bold text-green-600">
              {settlement.payoutAmount ? formatCurrency(settlement.payoutAmount) : 'N/A'}
            </p>
          </div>

          {settlement.calculatedAt && (
            <div className="text-sm text-gray-500">
              Calculated on {formatDate(settlement.calculatedAt)}
            </div>
          )}
        </div>
      )}

      {settlement.type === 'physical' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-gray-700">Status</span>
            <span
              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                settlement.status === 'DRAFT'
                  ? 'bg-yellow-100 text-yellow-800'
                  : settlement.status === 'PENDING'
                    ? 'bg-blue-100 text-blue-800'
                    : settlement.status === 'CONFIRMED'
                      ? 'bg-green-100 text-green-800'
                      : 'bg-gray-100 text-gray-800'
              }`}
            >
              {settlement.status || 'Unknown'}
            </span>
          </div>

          {settlement.referenceObligationIsin && (
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Reference Obligation ISIN
              </label>
              <p className="mt-1 text-sm text-gray-900 font-mono">
                {settlement.referenceObligationIsin}
              </p>
            </div>
          )}

          {settlement.proposedDeliveryDate && (
            <div>
              <label className="block text-sm font-medium text-gray-700">
                Proposed Delivery Date
              </label>
              <p className="mt-1 text-sm text-gray-900">
                {formatDate(settlement.proposedDeliveryDate)}
              </p>
            </div>
          )}

          {settlement.notes && (
            <div>
              <label className="block text-sm font-medium text-gray-700">Notes</label>
              <p className="mt-1 text-sm text-gray-900 whitespace-pre-wrap">{settlement.notes}</p>
            </div>
          )}

          {settlement.createdAt && (
            <div className="text-sm text-gray-500">
              Created on {formatDate(settlement.createdAt)}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default SettlementViewComponent;
