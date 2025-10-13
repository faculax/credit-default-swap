import React, { useState } from 'react';
import './App.css';
import TopBar from './components/top-bar/TopBar';
import CDSTradeForm from './components/cds-trade-form/CDSTradeForm';
import ConfirmationModal from './components/confirmation-modal/ConfirmationModal';
import CDSBlotter from './components/cds-blotter/CDSBlotter';
import TradeDetailModal from './components/trade-detail-modal/TradeDetailModal';
import PortfolioPage from './components/portfolio/PortfolioPage';
import MarginStatementsPage from './components/margin/MarginStatementsPage';
import SimmDashboard from './components/SimmDashboard';
import SaCcrDashboard from './components/SaCcrDashboard';
import { CDSTrade } from './data/referenceData';
import { cdsTradeService, CDSTradeRequest, CDSTradeResponse } from './services/cdsTradeService';

type ViewMode = 'form' | 'blotter' | 'portfolios' | 'margin-statements' | 'simm' | 'sa-ccr';

function App() {
  const [currentView, setCurrentView] = useState<ViewMode>('form');
  const [isConfirmationOpen, setIsConfirmationOpen] = useState(false);
  const [bookedTrade, setBookedTrade] = useState<CDSTradeResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [selectedTrade, setSelectedTrade] = useState<CDSTradeResponse | null>(null);
  const [isTradeDetailOpen, setIsTradeDetailOpen] = useState(false);

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
        tradeStatus: trade.tradeStatus
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

  const handleTradeUpdate = () => {
    // Force a re-render of the blotter to refresh trade data
    // The CDSBlotter will handle its own refresh
    setCurrentView('blotter');
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
              New CDS Trade
            </button>
            <button
              onClick={() => setCurrentView('blotter')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'blotter'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              CDS Trade Blotter
            </button>
            <button
              onClick={() => setCurrentView('portfolios')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'portfolios'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Portfolios
            </button>
            <button
              onClick={() => setCurrentView('margin-statements')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'margin-statements'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Margin Statements
            </button>
            <button
              onClick={() => setCurrentView('simm')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'simm'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              SIMM Calculator
            </button>
            <button
              onClick={() => setCurrentView('sa-ccr')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'sa-ccr'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              SA-CCR Exposure
            </button>
          </div>
        </div>
      </div>
      
      <div className="p-8">

        {/* Content based on current view */}
        {currentView === 'form' ? (
          <CDSTradeForm onSubmit={handleTradeSubmit} />
        ) : currentView === 'blotter' ? (
          <CDSBlotter onTradeSelect={handleTradeSelect} />
        ) : currentView === 'portfolios' ? (
          <PortfolioPage />
        ) : currentView === 'margin-statements' ? (
          <MarginStatementsPage />
        ) : currentView === 'simm' ? (
          <SimmDashboard />
        ) : currentView === 'sa-ccr' ? (
          <SaCcrDashboard />
        ) : null}
        
        <ConfirmationModal
          isOpen={isConfirmationOpen}
          trade={bookedTrade}
          onClose={handleCloseConfirmation}
        />

        <TradeDetailModal
          isOpen={isTradeDetailOpen}
          trade={selectedTrade}
          onClose={handleCloseTradeDetail}
          onTradeUpdate={handleTradeUpdate}
        />
      </div>
    </div>
  );
}

export default App;
