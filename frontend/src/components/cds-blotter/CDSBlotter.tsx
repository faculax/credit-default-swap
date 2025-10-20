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
      
      alert('Novation completed successfully!');
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
      case 'MATURED':
        return 'bg-gray-100 text-gray-800';
      case 'DEFAULTED':
        return 'bg-red-100 text-red-800';
      case 'TERMINATED':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="bg-fd-darker rounded-lg p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-fd-border rounded mb-4"></div>
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-12 bg-fd-border rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-fd-darker rounded-lg p-6">
        <div className="text-red-400 text-center">
          <p className="text-lg font-medium">Error Loading Trades</p>
          <p className="text-sm mt-2">{error}</p>
          <button
            onClick={loadTrades}
            className="mt-4 px-4 py-2 bg-fd-green text-fd-dark rounded-md hover:bg-opacity-90 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-fd-darker rounded-lg p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-fd-text">CDS Trades</h2>
        <button
          onClick={loadTrades}
          className="px-4 py-2 bg-fd-green text-fd-dark rounded-md hover:bg-opacity-90 transition-colors"
        >
          Refresh
        </button>
      </div>

      {trades.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-fd-text text-lg">No trades found</p>
          <p className="text-fd-text-secondary text-sm mt-2">
            Book your first CDS trade using the form above
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-fd-border">
                <th className="text-left py-3 px-4 text-fd-text font-medium">Trade ID</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Reference Entity</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Notional</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Spread</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Maturity</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Counterparty</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Status</th>
                <th className="text-left py-3 px-4 text-fd-text font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {trades.map((trade) => (
                <tr
                  key={trade.id}
                  onClick={() => handleTradeClick(trade)}
                  className={`border-b border-fd-border hover:bg-fd-dark cursor-pointer transition-colors ${
                    selectedTradeId === trade.id ? 'bg-fd-dark' : ''
                  }`}
                >
                  <td className="py-3 px-4 text-fd-text">CDS-{trade.id}</td>
                  <td className="py-3 px-4 text-fd-text">{trade.referenceEntity}</td>
                  <td className="py-3 px-4 text-fd-text">
                    {formatCurrency(trade.notionalAmount, trade.currency)}
                  </td>
                  <td className="py-3 px-4 text-fd-text">{(trade.spread * 100).toFixed(2)}%</td>
                  <td className="py-3 px-4 text-fd-text">{formatDate(trade.maturityDate)}</td>
                  <td className="py-3 px-4 text-fd-text">{trade.counterparty}</td>
                  <td className="py-3 px-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(trade.tradeStatus)}`}>
                      {trade.tradeStatus}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="flex space-x-2" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={(e) => handleGenerateDemoEvents(trade, e)}
                        disabled={generatingEvents === trade.id}
                        className="px-3 py-1 bg-fd-cyan text-fd-dark text-xs rounded hover:bg-opacity-90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-1"
                        title="Generate demo credit events for this trade"
                      >
                        {generatingEvents === trade.id ? (
                          <svg className="animate-spin h-3 w-3" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                        ) : (
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                          </svg>
                        )}
                        <span>Events</span>
                      </button>
                      
                      <button
                        onClick={(e) => handleNovateClick(trade, e)}
                        className="px-3 py-1 bg-fd-green-secondary text-fd-dark text-xs rounded hover:bg-opacity-90 transition-colors flex items-center space-x-1"
                        title="Novate this trade to a CCP"
                      >
                        <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                        </svg>
                        <span>Novate</span>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <NovationModal
        isOpen={showNovationModal}
        trade={selectedTradeForNovation}
        onClose={() => {
          setShowNovationModal(false);
          setSelectedTradeForNovation(null);
        }}
        onConfirm={handleNovationConfirm}
      />
    </div>
  );
});

CDSBlotter.displayName = 'CDSBlotter';

export default CDSBlotter;