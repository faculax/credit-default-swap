import React, { useState, useRef } from 'react';
import './App.css';
import TopBar from './components/top-bar/TopBar';
import CDSTradeForm from './components/cds-trade-form/CDSTradeForm';
import ConfirmationModal from './components/confirmation-modal/ConfirmationModal';
import CDSBlotter, { CDSBlotterRef } from './components/cds-blotter/CDSBlotter';
import TradeDetailModal from './components/trade-detail-modal/TradeDetailModal';
import PortfolioPage from './components/portfolio/PortfolioPage';
import MarginStatementsPage from './components/margin/MarginStatementsPage';
import SimmDashboard from './components/SimmDashboard';
import SaCcrDashboard from './components/SaCcrDashboard';
import ReconciliationDashboard from './components/dashboard/ReconciliationDashboard';
import BondPage from './components/bond/BondPage';
import BasketPage from './components/basket/BasketPage';
import DailyPnlDashboard from './components/DailyPnlDashboard';
import AccountingEventsDashboard from './components/AccountingEventsDashboard';
import EodJobMonitor from './components/EodJobMonitor';
import RiskReportingDashboard from './components/RiskReportingDashboard';
import { CDSTrade } from './data/referenceData';
import { cdsTradeService, CDSTradeRequest, CDSTradeResponse } from './services/cdsTradeService';

type ViewMode = 'form' | 'blotter' | 'portfolios' | 'margin-statements' | 'simm' | 'sa-ccr' | 'reconciliation' | 'bonds' | 'baskets' | 'daily-pnl' | 'accounting-events' | 'eod-jobs' | 'risk-reporting';

function App() {
  const [currentView, setCurrentView] = useState<ViewMode>('form');
  const [isConfirmationOpen, setIsConfirmationOpen] = useState(false);
  const [bookedTrade, setBookedTrade] = useState<CDSTradeResponse | null>(null);

  const [selectedTrade, setSelectedTrade] = useState<CDSTradeResponse | null>(null);
  const [isTradeDetailOpen, setIsTradeDetailOpen] = useState(false);
  const blotterRef = useRef<CDSBlotterRef>(null);

  const handleTradeSubmit = async (trade: CDSTrade) => {
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
      alert(`Error booking trade: ${errorMessage}`);
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
              onClick={() => setCurrentView('reconciliation')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'reconciliation'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Reconciliation Dashboard
            </button>
            <button
              onClick={() => setCurrentView('daily-pnl')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'daily-pnl'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Daily P&L
            </button>
            <button
              onClick={() => setCurrentView('eod-jobs')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'eod-jobs'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              EOD Jobs
            </button>
            <button
              onClick={() => setCurrentView('risk-reporting')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'risk-reporting'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Risk Reporting
            </button>
            <button
              onClick={() => setCurrentView('accounting-events')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                currentView === 'accounting-events'
                  ? 'bg-fd-green text-fd-dark'
                  : 'text-fd-text hover:text-fd-green hover:bg-fd-dark'
              }`}
            >
              Accounting Events
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
        ) : currentView === 'margin-statements' ? (
          <MarginStatementsPage />
        ) : currentView === 'simm' ? (
          <SimmDashboard />
        ) : currentView === 'sa-ccr' ? (
          <SaCcrDashboard />
        ) : currentView === 'reconciliation' ? (
          <ReconciliationDashboard />
        ) : currentView === 'bonds' ? (
          <BondPage />
        ) : currentView === 'baskets' ? (
          <BasketPage />
        ) : currentView === 'daily-pnl' ? (
          <DailyPnlDashboard />
        ) : currentView === 'eod-jobs' ? (
          <EodJobMonitor />
        ) : currentView === 'risk-reporting' ? (
          <RiskReportingDashboard />
        ) : currentView === 'accounting-events' ? (
          <AccountingEventsDashboard />
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
          onTradesUpdated={handleTradesUpdated}
        />
      </div>
    </div>
  );
}

export default App;
