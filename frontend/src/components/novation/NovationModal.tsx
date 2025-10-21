import React, { useState, useEffect } from 'react';
import { CDSTradeResponse } from '../../services/cdsTradeService';

interface CcpAccount {
  id: number;
  ccpName: string;
  memberFirm: string;
  memberId: string;
  accountNumber: string;
  accountName?: string;
  accountType: 'HOUSE' | 'CLIENT' | 'SEGREGATED';
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_APPROVAL';
  eligibleProductTypes: string[];
}

interface NovationModalProps {
  isOpen: boolean;
  trade: CDSTradeResponse | null;
  onClose: () => void;
  onConfirm: (tradeId: number, ccpName: string, memberFirm: string) => Promise<void>;
}

const NovationModal: React.FC<NovationModalProps> = ({ isOpen, trade, onClose, onConfirm }) => {
  const [ccpAccounts, setCcpAccounts] = useState<CcpAccount[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<CcpAccount | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load CCP accounts when modal opens
  useEffect(() => {
    if (isOpen) {
      loadCcpAccounts();
    }
  }, [isOpen]);

  const loadCcpAccounts = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/novation/ccp-accounts');
      if (!response.ok) {
        throw new Error('Failed to load CCP accounts');
      }
      const accounts = await response.json();
      // Filter for active accounts eligible for CDS
      const eligibleAccounts = accounts.filter((account: CcpAccount) => 
        account.status === 'ACTIVE' && 
        account.eligibleProductTypes.includes('CDS')
      );
      setCcpAccounts(eligibleAccounts);
    } catch (error) {
      console.error('Error loading CCP accounts:', error);
      setError('Failed to load CCP accounts');
      setCcpAccounts([]);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async () => {
    if (!trade || !selectedAccount) return;

    setSubmitting(true);
    setError(null);
    try {
      await onConfirm(trade.id, selectedAccount.ccpName, selectedAccount.memberFirm);
      onClose();
    } catch (error: any) {
      setError(error.message || 'Novation failed');
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  if (!isOpen || !trade) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-orange-500/20 rounded-full flex items-center justify-center">
              <svg className="w-5 h-5 text-orange-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"></path>
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-fd-text">Novate Trade to CCP</h2>
          </div>
          <button onClick={onClose} className="text-fd-text-muted hover:text-fd-text transition-colors">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        {/* Trade Summary */}
        <div className="bg-fd-dark rounded-lg p-4 mb-6">
          <h3 className="text-lg font-semibold text-fd-text mb-3">Trade Summary</h3>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-fd-text-muted">Trade ID:</span>
              <span className="text-fd-green font-mono ml-2">CDS-{trade.id}</span>
            </div>
            <div>
              <span className="text-fd-text-muted">Reference Entity:</span>
              <span className="text-fd-text ml-2">{trade.referenceEntity}</span>
            </div>
            <div>
              <span className="text-fd-text-muted">Current Counterparty:</span>
              <span className="text-fd-text ml-2">{trade.counterparty}</span>
            </div>
            <div>
              <span className="text-fd-text-muted">Notional:</span>
              <span className="text-fd-text ml-2">{formatCurrency(trade.notionalAmount, trade.currency)}</span>
            </div>
            <div>
              <span className="text-fd-text-muted">Spread:</span>
              <span className="text-fd-text ml-2">{trade.spread} bps</span>
            </div>
            <div>
              <span className="text-fd-text-muted">Maturity:</span>
              <span className="text-fd-text ml-2">{new Date(trade.maturityDate).toLocaleDateString()}</span>
            </div>
          </div>
        </div>

        {/* CCP Account Selection */}
        <div className="mb-6">
          <h3 className="text-lg font-semibold text-fd-text mb-3">Select CCP Account</h3>
          
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-fd-green"></div>
              <span className="ml-3 text-fd-text-muted">Loading CCP accounts...</span>
            </div>
          ) : ccpAccounts.length === 0 ? (
            <div className="bg-fd-dark rounded-lg p-4 text-center">
              <p className="text-fd-text-muted">No eligible CCP accounts found</p>
            </div>
          ) : (
            <div className="space-y-3 max-h-60 overflow-y-auto">
              {ccpAccounts.map((account) => (
                <div
                  key={account.id}
                  onClick={() => setSelectedAccount(account)}
                  className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                    selectedAccount?.id === account.id
                      ? 'border-fd-green bg-fd-green/10'
                      : 'border-fd-border bg-fd-dark hover:border-fd-green/50'
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-3">
                      <div className={`w-4 h-4 rounded-full border-2 ${
                        selectedAccount?.id === account.id
                          ? 'border-fd-green bg-fd-green'
                          : 'border-fd-border'
                      }`}>
                        {selectedAccount?.id === account.id && (
                          <div className="w-full h-full rounded-full bg-fd-green flex items-center justify-center">
                            <div className="w-2 h-2 rounded-full bg-fd-dark"></div>
                          </div>
                        )}
                      </div>
                      <span className="text-fd-text font-medium">{account.ccpName}</span>
                      <span className="text-fd-text-muted text-sm">({account.accountType})</span>
                    </div>
                  </div>
                  <div className="ml-7 text-sm text-fd-text-muted">
                    <div>Member: {account.memberFirm}</div>
                    <div>Account: {account.accountNumber}</div>
                    {account.accountName && <div>Name: {account.accountName}</div>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Impact Summary */}
        {selectedAccount && (
          <div className="bg-fd-dark rounded-lg p-4 mb-6">
            <h3 className="text-lg font-semibold text-fd-text mb-3">Novation Impact</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Original Trade:</span>
                <span className="text-fd-text">Will be terminated</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">New Counterparty:</span>
                <span className="text-fd-text font-medium">{selectedAccount.ccpName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Clearing Account:</span>
                <span className="text-fd-text">{selectedAccount.accountNumber}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Economic Terms:</span>
                <span className="text-fd-text">Preserved exactly</span>
              </div>
              <div className="flex justify-between">
                <span className="text-fd-text-muted">Risk Transfer:</span>
                <span className="text-fd-text">Seamless (no gap)</span>
              </div>
            </div>
          </div>
        )}

        {/* Error Display */}
        {error && (
          <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3 mb-4">
            <div className="flex items-center space-x-2">
              <svg className="w-5 h-5 text-red-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
              </svg>
              <span className="text-red-400 text-sm">{error}</span>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex justify-end space-x-4 pt-4 border-t border-fd-border">
          <button
            onClick={onClose}
            disabled={submitting}
            className="px-6 py-2 bg-fd-border text-fd-text rounded hover:bg-fd-border/80 transition-colors disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={!selectedAccount || submitting}
            className="px-6 py-2 bg-orange-500 text-white rounded hover:bg-orange-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            {submitting && (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            )}
            <span>
              {submitting ? 'Processing...' : 'Confirm Novation'}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default NovationModal;