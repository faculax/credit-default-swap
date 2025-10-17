import React, { useState, useRef } from 'react';
import './App.css';
import TopBar from './components/top-bar/TopBar';
import CDSTradeForm from './components/cds-trade-form/CDSTradeForm';
import ConfirmationModal from './components/confirmation-modal/ConfirmationModal';
import CDSBlotter, { CDSBlotterRef } from './components/cds-blotter/CDSBlotter';
import TradeDetailModal from './components/trade-detail-modal/TradeDetailModal';
import PortfolioPage from './components/portfolio/PortfolioPage';
import BondPage from './components/bond/BondPage';
import BasketPage from './components/basket/BasketPage';
import { CDSTrade } from './data/referenceData';
import { cdsTradeService, CDSTradeRequest, CDSTradeResponse } from './services/cdsTradeService';

type ViewMode = 'form' | 'blotter' | 'portfolios' | 'bonds' | 'baskets';

function App() {
  const [currentView, setCurrentView] = useState<ViewMode>('form');
  const [isConfirmationOpen, setIsConfirmationOpen] = useState(false);
  const [bookedTrade, setBookedTrade] = useState<CDSTradeResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [selectedTrade, setSelectedTrade] = useState<CDSTradeResponse | null>(null);
  const [isTradeDetailOpen, setIsTradeDetailOpen] = useState(false);
  const blotterRef = useRef<CDSBlotterRef>(null);

  const handleTradeSubmit = async (trade: CDSTrade) => {
    setIsSubmitting(true);
    setSubmitError(null);
    
    try {
      const tradeRequest: CDSTradeRequest = {
        referenceEntity: trade.referenceEntity,
        notionalAmount: trade.notionalAmount,
        spread: trade.spread,
        maturityDate: trade.maturityDate,
        effectiveDate: trade.effectiveDate,
        counterparty: trade.counterparty,
        tradeDate: trade.tradeDate,
        currency: trade.currency,
        premiumFrequency: trade.premiumFrequency,
        dayCountConvention: trade.dayCountConvention,
        buySellProtection: trade.buySellProtection,
        restructuringClause: trade.restructuringClause,
        paymentCalendar: trade.paymentCalendar,
        accrualStartDate: trade.accrualStartDate,
        tradeStatus: trade.tradeStatus,
        recoveryRate: trade.recoveryRate,
        settlementType: trade.settlementType,
        obligation: trade.obligation
      };

      const savedTrade = await cdsTradeService.createTrade(tradeRequest);
      
      setBookedTrade(savedTrade);
      setIsConfirmationOpen(true);
      
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setSubmitError(`Failed to book trade: ${errorMessage}`);
      alert(`Error booking trade: ${errorMessage}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCloseConfirmation = () => {
    setIsConfirmationOpen(false);
    setBookedTrade(null);
  };

  const handleTradeSelect = (trade: CDSTradeResponse) => {
    setSelectedTrade(trade);
    setIsTradeDetailOpen(true);
  };

  const handleCloseTradeDetail = () => {
    setIsTradeDetailOpen(false);
    setSelectedTrade(null);
  };

  const handleTradesUpdated = (affectedTradeIds?: number[]) => {
    // When trades are updated (e.g., credit event propagated), refresh the blotter
    if (blotterRef.current) {
      blotterRef.current.refreshTrades();
    }
  };

  return (
    <div className="min-h-screen bg-fd-dark">
      <TopBar />
      
      {/* Sub-navigation bar */}
      <div className="bg-fd-darker border-b border-fd-border">
        <div className="px-8 py-4">
          <div className="flex items-center space-x-6">
            <button
              onClick={() => setCurrentView('form')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'form'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              New Single-Name CDS
            </button>
            <button
              onClick={() => setCurrentView('blotter')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'blotter'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Single-Name CDS Blotter
            </button>
            <button
              onClick={() => setCurrentView('bonds')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'bonds'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Corporate Bonds
            </button>
            <button
              onClick={() => setCurrentView('baskets')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'baskets'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Basket Derivatives
            </button>
            <button
              onClick={() => setCurrentView('portfolios')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'portfolios'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              CDS Portfolios
            </button>
          </div>
        </div>
      </div>
      
      <div className="p-8">

        {/* Content based on current view */}
        {currentView === 'form' ? (
          <CDSTradeForm onSubmit={handleTradeSubmit} />
        ) : currentView === 'blotter' ? (
          <CDSBlotter ref={blotterRef} onTradeSelect={handleTradeSelect} />
        ) : currentView === 'portfolios' ? (
          <PortfolioPage />
        ) : currentView === 'baskets' ? (
          <BasketPage />
        ) : (
          <BondPage />
        )}
        
        <ConfirmationModal
          isOpen={isConfirmationOpen}
          trade={bookedTrade}
          onClose={handleCloseConfirmation}
        />

        <TradeDetailModal
          isOpen={isTradeDetailOpen}
          trade={selectedTrade}
          onClose={handleCloseTradeDetail}
          onTradesUpdated={handleTradesUpdated}
        />
      </div>
    </div>
  );
}

export default App;
