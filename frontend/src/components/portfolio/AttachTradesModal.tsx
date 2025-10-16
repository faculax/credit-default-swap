import React, { useState, useEffect } from 'react';
import { portfolioService, ConstituentRequest } from '../../services/portfolioService';
import { cdsTradeService, CDSTradeResponse } from '../../services/cdsTradeService';

interface AttachTradesModalProps {
  portfolioId: number;
  onClose: () => void;
  onSuccess: () => void;
}

const AttachTradesModal: React.FC<AttachTradesModalProps> = ({
  portfolioId,
  onClose,
  onSuccess,
}) => {
  const [availableTrades, setAvailableTrades] = useState<CDSTradeResponse[]>([]);
  const [selectedTrades, setSelectedTrades] = useState<Map<number, ConstituentRequest>>(new Map());
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [weightType, setWeightType] = useState<'NOTIONAL' | 'PERCENT'>('NOTIONAL');

  useEffect(() => {
    loadAvailableTrades();
  }, []);

  const loadAvailableTrades = async () => {
    try {
      setLoading(true);
      const trades = await cdsTradeService.getAllTrades();
      setAvailableTrades(trades);
    } catch (err) {
      console.error('Error loading trades:', err);
      setError('Failed to load available trades');
    } finally {
      setLoading(false);
    }
  };

  const handleTradeToggle = (trade: CDSTradeResponse) => {
    const newSelected = new Map(selectedTrades);

    if (newSelected.has(trade.id)) {
      newSelected.delete(trade.id);
    } else {
      const weightValue = weightType === 'NOTIONAL' ? trade.notionalAmount : 0.0;

      newSelected.set(trade.id, {
        tradeId: trade.id,
        weightType: weightType,
        weightValue: weightValue,
      });
    }

    setSelectedTrades(newSelected);
  };

  const handleWeightChange = (tradeId: number, value: number) => {
    const newSelected = new Map(selectedTrades);
    const existing = newSelected.get(tradeId);

    if (existing) {
      newSelected.set(tradeId, {
        ...existing,
        weightValue: value,
      });
      setSelectedTrades(newSelected);
    }
  };

  const handleWeightTypeChange = (newType: 'NOTIONAL' | 'PERCENT') => {
    setWeightType(newType);

    // Update all selected trades with new weight type
    const newSelected = new Map(selectedTrades);
    newSelected.forEach((req, tradeId) => {
      const trade = availableTrades.find((t) => t.id === tradeId);
      if (trade) {
        newSelected.set(tradeId, {
          ...req,
          weightType: newType,
          weightValue: newType === 'NOTIONAL' ? trade.notionalAmount : 0.0,
        });
      }
    });
    setSelectedTrades(newSelected);
  };

  const handleSubmit = async () => {
    if (selectedTrades.size === 0) {
      setError('Please select at least one trade');
      return;
    }

    // Validate percent weights
    if (weightType === 'PERCENT') {
      const total = Array.from(selectedTrades.values()).reduce(
        (sum, req) => sum + req.weightValue,
        0
      );

      if (Math.abs(total - 1.0) > 0.05) {
        setError(`Percent weights must sum to 1.0 (±0.05), current sum: ${total.toFixed(4)}`);
        return;
      }
    }

    try {
      setSubmitting(true);
      setError(null);

      await portfolioService.attachTrades(portfolioId, {
        trades: Array.from(selectedTrades.values()),
      });

      onSuccess();
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Failed to attach trades';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-fd-darker rounded-lg shadow-xl border border-fd-border max-w-4xl w-full mx-4 max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 py-4 border-b border-fd-border">
          <h3 className="text-lg font-semibold text-fd-text">Attach Trades to Portfolio</h3>
        </div>

        <div className="px-6 py-4 border-b border-fd-border">
          <div className="flex items-center space-x-4">
            <label className="text-sm font-medium text-fd-text">Weight Type:</label>
            <div className="flex space-x-3">
              <label className="flex items-center cursor-pointer">
                <input
                  type="radio"
                  value="NOTIONAL"
                  checked={weightType === 'NOTIONAL'}
                  onChange={() => handleWeightTypeChange('NOTIONAL')}
                  className="mr-2 accent-fd-green"
                />
                <span className="text-sm text-fd-text">Notional</span>
              </label>
              <label className="flex items-center cursor-pointer">
                <input
                  type="radio"
                  value="PERCENT"
                  checked={weightType === 'PERCENT'}
                  onChange={() => handleWeightTypeChange('PERCENT')}
                  className="mr-2 accent-fd-green"
                />
                <span className="text-sm text-fd-text">Percent</span>
              </label>
            </div>
            <div className="ml-auto text-sm text-fd-text-muted">
              {selectedTrades.size} trade(s) selected
            </div>
          </div>
        </div>

        {error && (
          <div className="mx-6 mt-4 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded text-sm">
            {error}
          </div>
        )}

        <div className="flex-1 overflow-y-auto px-6 py-4">
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-fd-green"></div>
            </div>
          ) : (
            <table className="min-w-full divide-y divide-fd-border">
              <thead className="bg-fd-dark">
                <tr>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider"
                  >
                    Select
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider"
                  >
                    Trade ID
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider"
                  >
                    Reference Entity
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider"
                  >
                    Notional
                  </th>
                  <th
                    scope="col"
                    className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider"
                  >
                    Weight Value
                  </th>
                </tr>
              </thead>
              <tbody className="bg-fd-darker divide-y divide-fd-border">
                {availableTrades.map((trade) => {
                  const isSelected = selectedTrades.has(trade.id);
                  const weight = selectedTrades.get(trade.id);

                  return (
                    <tr
                      key={trade.id}
                      className={`transition-colors ${isSelected ? 'bg-fd-dark ring-2 ring-fd-green' : 'hover:bg-fd-dark'}`}
                    >
                      <td className="px-6 py-4 whitespace-nowrap">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => handleTradeToggle(trade)}
                          className="h-4 w-4 accent-fd-green border-fd-border rounded cursor-pointer"
                        />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                        CDS-{trade.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                        {trade.referenceEntity}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                        {formatCurrency(trade.notionalAmount)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {isSelected && weight ? (
                          <input
                            type="number"
                            value={weight.weightValue}
                            onChange={(e) =>
                              handleWeightChange(trade.id, parseFloat(e.target.value) || 0)
                            }
                            step={weightType === 'PERCENT' ? '0.01' : '1000'}
                            min="0"
                            max={weightType === 'PERCENT' ? '1' : undefined}
                            className="w-32 px-2 py-1 text-sm bg-fd-dark border border-fd-border text-fd-text rounded focus:outline-none focus:ring-2 focus:ring-fd-green focus:border-transparent"
                          />
                        ) : (
                          <span className="text-sm text-fd-text-muted">-</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div className="px-6 py-4 border-t border-fd-border flex justify-end space-x-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-darker focus:outline-none focus:ring-2 focus:ring-fd-green"
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className="px-4 py-2 text-sm font-medium text-fd-dark bg-fd-green border border-transparent rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green disabled:bg-fd-green/50 disabled:cursor-not-allowed"
            disabled={submitting || selectedTrades.size === 0}
          >
            {submitting ? 'Attaching...' : `Attach ${selectedTrades.size} Trade(s)`}
          </button>
        </div>
      </div>
    </div>
  );
};

export default AttachTradesModal;
