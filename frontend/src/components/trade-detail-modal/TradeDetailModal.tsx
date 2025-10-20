import React from "react";
import { CDSTradeResponse } from "../../services/cdsTradeService";

interface TradeDetailModalProps {
  isOpen: boolean;
  trade: CDSTradeResponse | null;
  onClose: () => void;
  onTradesUpdated?: (affectedTradeIds?: number[]) => void;
}

const TradeDetailModal: React.FC<TradeDetailModalProps> = ({ 
  isOpen, 
  trade, 
  onClose, 
  onTradesUpdated 
}) => {
  if (!isOpen || !trade) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-fd-darker rounded-lg p-6 max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-fd-text">
            Trade Details - CDS-{trade.id}
          </h2>
          <button
            onClick={onClose}
            className="text-fd-text hover:text-fd-green transition-colors"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="text-fd-text">
          <p>Reference Entity: {trade.referenceEntity}</p>
          <p>Notional: {trade.notionalAmount} {trade.currency}</p>
          <p>Spread: {(trade.spread * 100).toFixed(2)}%</p>
          <p>Status: {trade.tradeStatus}</p>
        </div>
      </div>
    </div>
  );
};

export default TradeDetailModal;
