import React, { useState } from 'react';
import { Basket, BasketPricingResult } from '../../types/basket';
import { basketService } from '../../services/basketService';

interface BasketDetailViewProps {
  basket: Basket;
  onClose: () => void;
}

const BasketDetailView: React.FC<BasketDetailViewProps> = ({ basket, onClose }) => {
  const [pricingResult, setPricingResult] = useState<BasketPricingResult | null>(null);
  const [isPricing, setIsPricing] = useState(false);
  const [pricingError, setPricingError] = useState<string | null>(null);
  const [showSensitivities, setShowSensitivities] = useState(false);

  const handlePrice = async () => {
    setIsPricing(true);
    setPricingError(null);

    try {
      const result = await basketService.priceBasket(basket.id!, {
        paths: 50000,
        includeSensitivities: true,
      });
      setPricingResult(result);
      setShowSensitivities(true);
    } catch (error: any) {
      setPricingError(error.message || 'Failed to price basket');
    } finally {
      setIsPricing(false);
    }
  };

  const formatCurrency = (value: number | undefined, currency: string) => {
    if (value === undefined) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatBps = (value: number | undefined) => {
    if (value === undefined) return 'N/A';
    return `${value.toFixed(2)} bps`;
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'FIRST_TO_DEFAULT':
        return 'First-to-Default';
      case 'NTH_TO_DEFAULT':
        return `${basket.nth}-th-to-Default`;
      case 'TRANCHETTE':
        return `Tranchette [${(basket.attachmentPoint! * 100).toFixed(1)}% - ${(basket.detachmentPoint! * 100).toFixed(1)}%]`;
      default:
        return type;
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-fd-darker rounded-lg shadow-xl border border-fd-border max-w-6xl w-full max-h-[90vh] overflow-y-auto m-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="sticky top-0 bg-fd-darker border-b border-fd-border px-6 py-4 flex justify-between items-center z-10">
          <div>
            <h2 className="text-xl font-semibold text-fd-text">{basket.name}</h2>
            <p className="text-sm text-fd-text-muted mt-1">{getTypeLabel(basket.type)}</p>
          </div>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
            aria-label="Close modal"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <div className="p-6">
          {/* Basic Information */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div>
              <p className="text-xs text-fd-text-muted uppercase tracking-wide">Currency</p>
              <p className="text-sm font-medium text-fd-text mt-1">{basket.currency}</p>
            </div>
            <div>
              <p className="text-xs text-fd-text-muted uppercase tracking-wide">Notional</p>
              <p className="text-sm font-medium text-fd-text mt-1">
                {formatCurrency(basket.notional, basket.currency)}
              </p>
            </div>
            <div>
              <p className="text-xs text-fd-text-muted uppercase tracking-wide">Maturity</p>
              <p className="text-sm font-medium text-fd-text mt-1">{basket.maturityDate}</p>
            </div>
            <div>
              <p className="text-xs text-fd-text-muted uppercase tracking-wide">Frequency</p>
              <p className="text-sm font-medium text-fd-text mt-1">{basket.premiumFrequency}</p>
            </div>
          </div>

          {/* Pricing Action */}
          <div className="mb-6">
            <button
              onClick={handlePrice}
              disabled={isPricing}
              className="px-4 py-2 text-sm font-medium text-fd-dark bg-fd-green border border-transparent rounded-md hover:bg-fd-green-hover focus:outline-none focus:ring-2 focus:ring-fd-green disabled:bg-fd-green/50 disabled:cursor-not-allowed"
            >
              {isPricing ? 'Pricing...' : pricingResult ? 'Reprice' : 'Price Basket'}
            </button>
            {pricingError && <p className="mt-2 text-sm text-red-400">{pricingError}</p>}
          </div>

          {/* Pricing Results */}
          {pricingResult && (
            <>
              <div className="mb-6">
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                  Pricing Metrics
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                    <p className="text-xs text-fd-text-muted uppercase tracking-wide">
                      Fair Spread
                    </p>
                    <p className="text-2xl font-bold text-fd-green mt-2">
                      {formatBps(pricingResult.fairSpreadBps)}
                    </p>
                  </div>
                  <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                    <p className="text-xs text-fd-text-muted uppercase tracking-wide">Premium PV</p>
                    <p className="text-2xl font-bold text-fd-text mt-2">
                      {formatCurrency(pricingResult.premiumLegPv, basket.currency)}
                    </p>
                  </div>
                  <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                    <p className="text-xs text-fd-text-muted uppercase tracking-wide">
                      Protection PV
                    </p>
                    <p className="text-2xl font-bold text-fd-text mt-2">
                      {formatCurrency(pricingResult.protectionLegPv, basket.currency)}
                    </p>
                  </div>
                </div>
              </div>

              {/* Convergence Diagnostics */}
              {pricingResult.convergence && (
                <div className="mb-6">
                  <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                    Convergence Diagnostics
                  </h3>
                  <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <p className="text-xs text-fd-text-muted">Paths Used</p>
                        <p className="text-lg font-semibold text-fd-text mt-1">
                          {pricingResult.convergence.pathsUsed.toLocaleString()}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Iterations</p>
                        <p className="text-lg font-semibold text-fd-text mt-1">
                          {pricingResult.convergence.iterations}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Standard Error</p>
                        <p className="text-lg font-semibold text-fd-text mt-1">
                          {formatBps(pricingResult.convergence.standardErrorFairSpreadBps)}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Sensitivities */}
              {showSensitivities && pricingResult.sensitivities && (
                <div className="mb-6">
                  <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                    Risk Sensitivities
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">
                        Spread DV01
                      </p>
                      <p className="text-lg font-semibold text-fd-text mt-2">
                        {formatCurrency(pricingResult.sensitivities.spreadDv01, basket.currency)}
                      </p>
                      <p className="text-xs text-fd-text-muted mt-1">Per 1bp parallel shift</p>
                    </div>
                    <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">
                        Correlation Beta
                      </p>
                      <p className="text-lg font-semibold text-fd-text mt-2">
                        {pricingResult.sensitivities.correlationBeta?.toFixed(4) || 'N/A'}
                      </p>
                      <p className="text-xs text-fd-text-muted mt-1">Per 1% correlation shift</p>
                    </div>
                    <div className="bg-fd-dark border border-fd-border rounded-lg p-4">
                      <p className="text-xs text-fd-text-muted uppercase tracking-wide">
                        Recovery01
                      </p>
                      <p className="text-lg font-semibold text-fd-text mt-2">
                        {formatCurrency(pricingResult.sensitivities.recovery01, basket.currency)}
                      </p>
                      <p className="text-xs text-fd-text-muted mt-1">Per 1% recovery shift</p>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}

          {/* Constituents Table */}
          <div>
            <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
              Basket Constituents ({basket.constituents.length})
            </h3>
            <div className="bg-fd-dark border border-fd-border rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-fd-border">
                <thead className="bg-fd-darker">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                      Issuer
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                      Weight
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                      Seniority
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                      Recovery
                    </th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                      Sector
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-fd-border">
                  {basket.constituents.map((constituent, idx) => (
                    <tr key={idx} className="hover:bg-fd-darker transition-colors">
                      <td className="px-4 py-3 text-sm text-fd-text">{constituent.issuer}</td>
                      <td className="px-4 py-3 text-sm text-fd-text">
                        {((constituent.weight || 0) * 100).toFixed(2)}%
                      </td>
                      <td className="px-4 py-3 text-sm text-fd-text">{constituent.seniority}</td>
                      <td className="px-4 py-3 text-sm text-fd-text">
                        {constituent.recoveryOverride
                          ? `${(constituent.recoveryOverride * 100).toFixed(1)}%`
                          : 'Default'}
                      </td>
                      <td className="px-4 py-3 text-sm text-fd-text-muted">
                        {constituent.sector || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BasketDetailView;
