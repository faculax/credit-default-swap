import React, { useState } from 'react';
import './App.css';
import TopBar from './components/top-bar/TopBar';
import CDSTradeForm from './components/cds-trade-form/CDSTradeForm';
import ConfirmationModal from './components/confirmation-modal/ConfirmationModal';
import { CDSTrade } from './data/referenceData';
import { cdsTradeService, CDSTradeRequest, CDSTradeResponse } from './services/cdsTradeService';

function App() {
  const [isConfirmationOpen, setIsConfirmationOpen] = useState(false);
  const [bookedTrade, setBookedTrade] = useState<CDSTradeResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleTradeSubmit = async (trade: CDSTrade) => {
    setIsSubmitting(true);
    setSubmitError(null);
    
    try {
      // Convert frontend trade to backend request format
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

      console.log('Submitting CDS Trade to backend:', tradeRequest);
      
      // Call the backend API through the gateway
      const savedTrade = await cdsTradeService.createTrade(tradeRequest);
      
      console.log('Trade saved successfully:', savedTrade);
      
      setBookedTrade(savedTrade);
      setIsConfirmationOpen(true);
      
    } catch (error) {
      console.error('Failed to save trade:', error);
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setSubmitError(`Failed to book trade: ${errorMessage}`);
      
      // Show error alert for now
      alert(`Error booking trade: ${errorMessage}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCloseConfirmation = () => {
    setIsConfirmationOpen(false);
    setBookedTrade(null);
  };

  return (
    <div className="min-h-screen bg-fd-dark">
      <TopBar />
      
      <div className="p-8">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-fd-text mb-4">
            <span className="text-fd-text">CREDIT DEFAULT </span>
            <span className="text-fd-green">SWAP</span>
          </h1>
          <p className="text-fd-text-muted text-lg">
            Single-Name CDS Trade Entry Platform
          </p>
        </div>

        <CDSTradeForm onSubmit={handleTradeSubmit} />
        
        <ConfirmationModal
          isOpen={isConfirmationOpen}
          trade={bookedTrade}
          onClose={handleCloseConfirmation}
        />
      </div>
    </div>
  );
}

export default App;
