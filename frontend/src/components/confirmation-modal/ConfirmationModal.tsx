import React from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';

interface ConfirmationModalProps {
  isOpen: boolean;
  trade: CDSTradeResponse | null;
  onClose: () => void;
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({ isOpen, trade, onClose }) => {
  if (!isOpen || !trade) return null;

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-fd-green rounded-full flex items-center justify-center">
              <svg className="w-5 h-5 text-fd-dark" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-fd-text">Trade Booked Successfully</h2>
          </div>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <div className="bg-fd-dark rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between mb-2">
            <span className="text-fd-text-muted">Trade ID:</span>
            <span className="text-fd-green font-mono">CDS-{trade.id}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-fd-text-muted">Booking Time:</span>
            <span className="text-fd-text">{new Date(trade.createdAt).toLocaleString()}</span>
          </div>
        </div>

        <div className="space-y-4">
          <h3 className="text-lg font-semibold text-fd-text border-b border-fd-border pb-2">Trade Details</h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Reference Entity:</span>
                <span className="text-fd-text">{trade.referenceEntity}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Counterparty:</span>
                <span className="text-fd-text">{trade.counterparty}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Direction:</span>
                <span className={`${trade.buySellProtection === 'BUY' ? 'text-blue-400' : 'text-orange-400'}`}>
                  {trade.buySellProtection === 'BUY' ? 'Buy Protection' : 'Sell Protection'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Settlement Type:</span>
                <span className={`${trade.settlementType === 'CASH' ? 'text-fd-cyan' : 'text-fd-teal'}`}>
                  {trade.settlementType === 'CASH' ? 'Cash Settlement' : 'Physical Settlement'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Notional:</span>
                <span className="text-fd-text font-medium">
                  {formatCurrency(trade.notionalAmount, trade.currency)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Spread:</span>
                <span className="text-fd-text">{trade.spread} bps</span>
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Trade Date:</span>
                <span className="text-fd-text">{formatDate(trade.tradeDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Effective Date:</span>
                <span className="text-fd-text">{formatDate(trade.effectiveDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Maturity Date:</span>
                <span className="text-fd-text">{formatDate(trade.maturityDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Premium Frequency:</span>
                <span className="text-fd-text">{trade.premiumFrequency.replace('_', ' ')}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Status:</span>
                <span className="text-fd-green">{trade.tradeStatus}</span>
              </div>
            </div>
          </div>

          {trade.restructuringClause && (
            <div className="pt-2 border-t border-fd-border">
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Restructuring Clause:</span>
                <span className="text-fd-text">{trade.restructuringClause.replace(/_/g, ' ')}</span>
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end space-x-4 mt-8 pt-4 border-t border-fd-border">
          <button
            onClick={onClose}
            className="px-6 py-2 bg-fd-green text-fd-dark font-medium rounded hover:bg-fd-green-hover transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;