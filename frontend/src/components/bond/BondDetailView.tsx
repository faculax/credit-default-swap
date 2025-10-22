import React, { useState, useEffect } from 'react';
import { Bond, BondPricingResponse, bondService } from '../../services/bondService';

interface BondDetailViewProps {
  bondId: number;
  onClose: () => void;
}

const BondDetailView: React.FC<BondDetailViewProps> = ({ bondId, onClose }) => {
  const [bond, setBond] = useState<Bond | null>(null);
  const [pricing, setPricing] = useState<BondPricingResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'pricing'>('overview');

  // Pricing inputs
  const [valuationDate, setValuationDate] = useState(new Date().toISOString().split('T')[0]);
  const [discountRate, setDiscountRate] = useState('0.03');
  const [hazardRate, setHazardRate] = useState('0.01');

  useEffect(() => {
    loadBondDetails();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [bondId]);

  const loadBondDetails = async () => {
    try {
      setIsLoading(true);
      const bondData = await bondService.getBondById(bondId);
      setBond(bondData);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to load bond details');
    } finally {
      setIsLoading(false);
    }
  };

  const calculatePricing = async () => {
    if (!bond) return;

    try {
      const pricingData = await bondService.priceBond(
        bond.id!,
        valuationDate,
        parseFloat(discountRate),
        parseFloat(hazardRate)
      );
      setPricing(pricingData);
    } catch (err: any) {
      setError(err.message || 'Failed to price bond');
    }
  };

  const formatCurrency = (amount: number, currency: string = 'USD') => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatPercent = (value: number) => {
    return `${(value * 100).toFixed(4)}%`;
  };

  const formatBasisPoints = (value: number) => {
    return `${(value * 10000).toFixed(2)} bps`;
  };

  if (isLoading) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50">
        <div className="bg-fd-darker rounded-lg p-8 border border-fd-border">
          <div className="flex items-center space-x-3">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-fd-green"></div>
            <p className="text-fd-text">Loading bond details...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error && !bond) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center z-50">
        <div className="bg-fd-darker rounded-lg p-8 border border-fd-border">
          <p className="text-red-400 mb-4">{error}</p>
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-darker"
          >
            Close
          </button>
        </div>
      </div>
    );
  }

  if (!bond) return null;

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
            <h2 className="text-xl font-semibold text-fd-text">Bond Details</h2>
            <p className="text-sm text-fd-green">
              {bond.issuer} - {bond.isin || 'No ISIN'}
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-fd-text-muted hover:text-fd-text transition-colors"
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

        {/* Tabs */}
        <div className="border-b border-fd-border px-6">
          <nav className="flex space-x-8">
            <button
              onClick={() => setActiveTab('overview')}
              className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'overview'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('pricing')}
              className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'pricing'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Pricing & Analytics
            </button>
          </nav>
        </div>

        {/* Content */}
        <div className="p-6">
          {activeTab === 'overview' && (
            <div className="space-y-6">
              {/* Issuer Information */}
              <div>
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-3">
                  Issuer Information
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <p className="text-xs text-fd-text-muted">Issuer</p>
                    <p className="font-medium text-fd-text">{bond.issuer}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Seniority</p>
                    <p className="font-medium text-fd-text">
                      {bond.seniority === 'SR_UNSEC'
                        ? 'Senior Unsecured'
                        : bond.seniority === 'SR_SEC'
                          ? 'Senior Secured'
                          : 'Subordinated'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Sector</p>
                    <p className="font-medium text-fd-text">{bond.sector || 'N/A'}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">ISIN</p>
                    <p className="font-medium text-fd-text">{bond.isin || 'N/A'}</p>
                  </div>
                </div>
              </div>

              {/* Bond Terms */}
              <div>
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-3">
                  Bond Terms
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <p className="text-xs text-fd-text-muted">Notional</p>
                    <p className="font-medium text-fd-text">
                      {formatCurrency(bond.notional, bond.currency)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Coupon Rate</p>
                    <p className="font-medium text-fd-text">{formatPercent(bond.couponRate)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Frequency</p>
                    <p className="font-medium text-fd-text">
                      {bond.couponFrequency === 'SEMI_ANNUAL'
                        ? 'Semi-Annual'
                        : bond.couponFrequency === 'QUARTERLY'
                          ? 'Quarterly'
                          : 'Annual'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Day Count</p>
                    <p className="font-medium text-fd-text">
                      {bond.dayCount === 'ACT_ACT' ? 'ACT/ACT' : '30/360'}
                    </p>
                  </div>
                </div>
              </div>

              {/* Dates */}
              <div>
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-3">
                  Key Dates
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div>
                    <p className="text-xs text-fd-text-muted">Issue Date</p>
                    <p className="font-medium text-fd-text">{bond.issueDate}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Maturity Date</p>
                    <p className="font-medium text-fd-text">{bond.maturityDate}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Settlement Days</p>
                    <p className="font-medium text-fd-text">{bond.settlementDays || 2}</p>
                  </div>
                  <div>
                    <p className="text-xs text-fd-text-muted">Face Value</p>
                    <p className="font-medium text-fd-text">{bond.faceValue || 100}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'pricing' && (
            <div className="space-y-6">
              {/* Pricing Inputs */}
              <div className="bg-fd-dark p-4 rounded-lg border border-fd-border">
                <h3 className="text-sm font-semibold text-fd-text-muted uppercase tracking-wide mb-4">
                  Pricing Inputs
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-fd-text mb-1">
                      Valuation Date
                    </label>
                    <input
                      type="date"
                      value={valuationDate}
                      onChange={(e) => setValuationDate(e.target.value)}
                      className="w-full px-3 py-2 bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-fd-text mb-1">
                      Discount Rate (decimal)
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      value={discountRate}
                      onChange={(e) => setDiscountRate(e.target.value)}
                      className="w-full px-3 py-2 bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-fd-text mb-1">
                      Hazard Rate (decimal)
                    </label>
                    <input
                      type="number"
                      step="0.0001"
                      value={hazardRate}
                      onChange={(e) => setHazardRate(e.target.value)}
                      className="w-full px-3 py-2 bg-fd-darker border border-fd-border text-fd-text rounded-md focus:outline-none focus:ring-2 focus:ring-fd-green"
                    />
                  </div>
                </div>
                <button
                  onClick={calculatePricing}
                  className="mt-4 px-6 py-2 text-fd-dark bg-fd-green rounded-md hover:bg-fd-green-hover transition-colors"
                >
                  Calculate Pricing
                </button>
              </div>

              {/* Pricing Results */}
              {pricing && (
                <div className="space-y-4">
                  {/* Prices */}
                  <div className="bg-fd-dark/50 p-4 rounded-lg border border-fd-border">
                    <h4 className="text-sm font-semibold text-fd-text-muted mb-3">Prices</h4>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div>
                        <p className="text-xs text-fd-text-muted">Clean Price</p>
                        <p className="text-lg font-bold text-fd-green">
                          {pricing.cleanPrice !== undefined ? pricing.cleanPrice.toFixed(4) : 'N/A'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Dirty Price</p>
                        <p className="text-lg font-bold text-fd-green">
                          {pricing.dirtyPrice !== undefined ? pricing.dirtyPrice.toFixed(4) : 'N/A'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Accrued Interest</p>
                        <p className="text-lg font-bold text-fd-green">
                          {pricing.accruedInterest !== undefined
                            ? pricing.accruedInterest.toFixed(4)
                            : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Yields & Spreads */}
                  <div className="bg-fd-dark/50 p-4 rounded-lg border border-fd-border">
                    <h4 className="text-sm font-semibold text-fd-text-muted mb-3">
                      Yields & Spreads
                    </h4>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div>
                        <p className="text-xs text-fd-text-muted">Yield to Maturity</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.yieldToMaturity !== undefined
                            ? formatPercent(pricing.yieldToMaturity)
                            : 'N/A'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Z-Spread</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.zSpread !== undefined
                            ? formatBasisPoints(pricing.zSpread)
                            : 'N/A'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Modified Duration</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.sensitivities?.modifiedDuration !== undefined
                            ? pricing.sensitivities.modifiedDuration.toFixed(4)
                            : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Present Values */}
                  <div className="bg-fd-dark/50 p-4 rounded-lg border border-fd-border">
                    <h4 className="text-sm font-semibold text-fd-text-muted mb-3">
                      Present Values
                    </h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-fd-text-muted">Risk-Free PV</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.pv !== undefined
                            ? formatCurrency(pricing.pv, bond.currency)
                            : 'N/A'}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Risky PV (with defaults)</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.pvRisky !== undefined
                            ? formatCurrency(pricing.pvRisky, bond.currency)
                            : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Sensitivities */}
                  <div className="bg-fd-dark/50 p-4 rounded-lg border border-fd-border">
                    <h4 className="text-sm font-semibold text-fd-text-muted mb-3">
                      Risk Sensitivities
                    </h4>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div>
                        <p className="text-xs text-fd-text-muted">IR DV01</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.sensitivities?.irDv01 !== undefined
                            ? formatCurrency(pricing.sensitivities.irDv01, bond.currency)
                            : 'N/A'}
                        </p>
                        <p className="text-xs text-fd-text-muted mt-1">1bp rate move</p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Spread DV01</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.sensitivities?.spreadDv01 !== undefined
                            ? formatCurrency(pricing.sensitivities.spreadDv01, bond.currency)
                            : 'N/A'}
                        </p>
                        <p className="text-xs text-fd-text-muted mt-1">1bp hazard move</p>
                      </div>
                      <div>
                        <p className="text-xs text-fd-text-muted">Jump to Default (JTD)</p>
                        <p className="text-lg font-bold text-fd-text">
                          {pricing.sensitivities?.jtd !== undefined
                            ? formatCurrency(pricing.sensitivities.jtd, bond.currency)
                            : 'N/A'}
                        </p>
                        <p className="text-xs text-fd-text-muted mt-1">Immediate default loss</p>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {!pricing && (
                <div className="text-center py-8">
                  <p className="text-fd-text-muted">
                    Set pricing inputs above and click "Calculate Pricing" to view analytics
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-fd-darker px-6 py-4 flex justify-end border-t border-fd-border">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-fd-text bg-fd-dark border border-fd-border rounded-md hover:bg-fd-darker focus:outline-none focus:ring-2 focus:ring-fd-green"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default BondDetailView;
