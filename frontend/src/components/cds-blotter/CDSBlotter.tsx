import React, { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { cdsTradeService, CDSTradeResponse } from '../../services/cdsTradeService';
import { creditEventService } from '../../services/creditEventService';
import { novationService } from '../../services/novationService';
import NovationModal from '../novation/NovationModal';

interface CDSBlotterProps {
  onTradeSelect?: (trade: CDSTradeResponse) => void;
}

export interface CDSBlotterRef {
  refreshTrades: () => void;
}

const CDSBlotter = forwardRef<CDSBlotterRef, CDSBlotterProps>(({ onTradeSelect }, ref) => {
  const [trades, setTrades] = useState<CDSTradeResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedTradeId, setSelectedTradeId] = useState<number | null>(null);
  const [generatingEvents, setGeneratingEvents] = useState<number | null>(null);
  const [showNovationModal, setShowNovationModal] = useState(false);
  const [selectedTradeForNovation, setSelectedTradeForNovation] = useState<CDSTradeResponse | null>(null);
  const [showSuccessNotification, setShowSuccessNotification] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string>('Novation completed successfully');

  useEffect(() => {
    loadTrades();
  }, []);

  // Expose refreshTrades method via ref
  useImperativeHandle(ref, () => ({
    refreshTrades: loadTrades
  }));

  const loadTrades = async () => {
    try {
      setLoading(true);
      setError(null);
      const allTrades = await cdsTradeService.getAllTrades();
      setTrades(allTrades);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load trades';
      setError(errorMessage);
      console.error('Error loading trades:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleTradeClick = (trade: CDSTradeResponse) => {
    setSelectedTradeId(trade.id);
    if (onTradeSelect) {
      onTradeSelect(trade);
    }
  };

  const handleGenerateDemoEvents = async (trade: CDSTradeResponse, event: React.MouseEvent) => {
    event.stopPropagation(); // Prevent triggering the row click
    
    if (generatingEvents === trade.id) return; // Already generating
    
    try {
      setGeneratingEvents(trade.id);
      const generatedEvents = await creditEventService.generateDemoCreditEvents(trade.id);
      
      if (generatedEvents.length === 0) {
        alert('No demo credit events were generated for this trade. The system determined this reference entity is unlikely to have credit events.');
      } else {
        alert(`Successfully generated ${generatedEvents.length} demo credit event(s) for trade CDS-${trade.id}. View them in the trade details.`);
      }
      
      // Optionally refresh the trades to update any status changes
      loadTrades();
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to generate demo credit events';
      alert(`Error: ${errorMessage}`);
      console.error('Error generating demo credit events:', error);
    } finally {
      setGeneratingEvents(null);
    }
  };

  const handleNovateClick = (trade: CDSTradeResponse, event: React.MouseEvent) => {
    event.stopPropagation(); // Prevent triggering the row click
    setSelectedTradeForNovation(trade);
    setShowNovationModal(true);
  };

  const handleNovationConfirm = async (tradeId: number, ccpName: string, memberFirm: string) => {
    try {
      await novationService.executeNovation({
        tradeId,
        ccpName,
        memberFirm,
        actor: 'operations_user' // TODO: Get from user context
      });
      
      // Close modal and refresh trades
      setShowNovationModal(false);
      setSelectedTradeForNovation(null);
      await loadTrades();
      
      // Show success notification
      setSuccessMessage('Novation completed successfully!');
      setShowSuccessNotification(true);
      setTimeout(() => setShowSuccessNotification(false), 5000);
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to execute novation';
      alert(`Error: ${errorMessage}`);
      console.error('Error executing novation:', error);
    }
  };

  const formatCurrency = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'ACTIVE':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      case 'CREDIT_EVENT_RECORDED':
        return 'bg-orange-100 text-orange-800';
      case 'SETTLED_CASH':
      case 'SETTLED_PHYSICAL':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
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
            <h3 className="text-sm font-medium text-red-800">Error Loading Trades</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
            <div className="mt-4">
              <button
                onClick={loadTrades}
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

  if (trades.length === 0) {
    return (
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
        <h3 className="mt-2 text-sm font-medium text-fd-text">No trades found</h3>
        <p className="mt-1 text-sm text-fd-text-muted">Get started by creating your first CDS trade.</p>
      </div>
    );
  }

  return (
    <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border">
      <div className="px-6 py-4 border-b border-fd-border">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-fd-text">CDS Trade Blotter</h2>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-fd-text-muted">
              {trades.length} trade{trades.length !== 1 ? 's' : ''}
            </span>
            <button
              onClick={loadTrades}
              className="px-3 py-1 bg-fd-green text-fd-dark rounded hover:bg-fd-green-hover text-sm font-medium"
            >
              Refresh
            </button>
          </div>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-fd-border">
          <thead className="bg-fd-dark">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Trade ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Reference Entity
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Counterparty
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Direction
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Notional
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Spread (bps)
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Trade Date
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Maturity
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-fd-darker divide-y divide-fd-border">
            {trades.map((trade) => (
              <tr
                key={trade.id}
                onClick={() => handleTradeClick(trade)}
                className={`cursor-pointer hover:bg-fd-dark transition-colors ${
                  selectedTradeId === trade.id ? 'bg-fd-dark ring-2 ring-fd-green' : ''
                }`}
              >
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                  CDS-{trade.id}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                  {trade.referenceEntity}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                  {trade.counterparty}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <span className={`${trade.buySellProtection === 'BUY' ? 'text-blue-400' : 'text-orange-400'}`}>
                    {trade.buySellProtection === 'BUY' ? 'Buy Protection' : 'Sell Protection'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text font-medium">
                  {formatCurrency(trade.notionalAmount, trade.currency)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                  {trade.spread}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                  {formatDate(trade.tradeDate)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                  {formatDate(trade.maturityDate)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(trade.tradeStatus)}`}>
                    {trade.tradeStatus.replace(/_/g, ' ')}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  <div className="flex items-center space-x-2" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={(e) => handleGenerateDemoEvents(trade, e)}
                      disabled={generatingEvents === trade.id || trade.tradeStatus !== 'ACTIVE'}
                      className={`inline-flex items-center justify-center w-8 h-8 rounded transition-colors ${
                        trade.tradeStatus === 'ACTIVE' 
                          ? 'text-fd-text-muted hover:text-fd-green hover:bg-fd-green/10'
                          : 'text-fd-text-muted/50 cursor-not-allowed'
                      }`}
                      title={
                        trade.tradeStatus === 'ACTIVE' 
                          ? 'Generate demo credit events' 
                          : 'Only available for ACTIVE trades'
                      }
                    >
                      {generatingEvents === trade.id ? (
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-fd-green"></div>
                      ) : (
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path 
                            strokeLinecap="round" 
                            strokeLinejoin="round" 
                            strokeWidth={2} 
                            d="M8 6h8V4a2 2 0 00-2-2H10a2 2 0 00-2 2v2zm8 0v2H8V6h8zm0 2v8a2 2 0 01-2 2H10a2 2 0 01-2-2V8h8z"
                          />
                          <circle cx="10" cy="10" r="1" fill="currentColor"/>
                          <circle cx="14" cy="10" r="1" fill="currentColor"/>
                          <circle cx="10" cy="14" r="1" fill="currentColor"/>
                          <circle cx="14" cy="14" r="1" fill="currentColor"/>
                          <circle cx="12" cy="12" r="1" fill="currentColor"/>
                          <circle cx="12" cy="16" r="1" fill="currentColor"/>
                        </svg>
                      )}
                    </button>

                    <button
                      onClick={(e) => handleNovateClick(trade, e)}
                      className="inline-flex items-center justify-center w-8 h-8 rounded transition-colors text-fd-text-muted hover:text-fd-cyan hover:bg-fd-cyan/10"
                      title="Novate this trade to a CCP"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <NovationModal
        isOpen={showNovationModal}
        trade={selectedTradeForNovation}
        onClose={() => {
          setShowNovationModal(false);
          setSelectedTradeForNovation(null);
        }}
        onConfirm={handleNovationConfirm}
      />

      {/* Success Notification */}
      {showSuccessNotification && (
        <div className="fixed top-4 right-4 z-[60] animate-fade-in">
          <div className="bg-fd-dark border-2 border-fd-green rounded-lg shadow-lg p-4 flex items-start gap-3 min-w-[320px] max-w-[480px]">
            <div className="flex-shrink-0 mt-0.5">
              <svg className="w-6 h-6 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </div>
            <div className="flex-1">
              <h4 className="text-fd-text font-semibold mb-1">Success!</h4>
              <p className="text-fd-text-muted text-sm leading-relaxed">{successMessage}</p>
            </div>
            <button
              onClick={() => setShowSuccessNotification(false)}
              className="flex-shrink-0 text-fd-text-muted hover:text-fd-text transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
              </svg>
            </button>
          </div>
        </div>
      )}
    </div>
  );
});

CDSBlotter.displayName = 'CDSBlotter';

export default CDSBlotter;