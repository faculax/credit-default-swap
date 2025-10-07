import React, { useState, useEffect } from 'react';
import { portfolioService, CdsPortfolio, CdsPortfolioConstituent, PortfolioPricingResponse } from '../../services/portfolioService';
import { cdsTradeService, CDSTradeResponse } from '../../services/cdsTradeService';
import AttachTradesModal from './AttachTradesModal';

interface PortfolioDetailProps {
  portfolioId: number;
  onBack?: () => void;
}

const PortfolioDetail: React.FC<PortfolioDetailProps> = ({ portfolioId, onBack }) => {
  const [portfolio, setPortfolio] = useState<CdsPortfolio | null>(null);
  const [constituents, setConstituents] = useState<CdsPortfolioConstituent[]>([]);
  const [riskSummary, setRiskSummary] = useState<PortfolioPricingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'constituents' | 'concentration'>('overview');
  const [showAttachModal, setShowAttachModal] = useState(false);

  useEffect(() => {
    loadPortfolioData();
  }, [portfolioId]);

  const loadPortfolioData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [portfolioData, constituentsData] = await Promise.all([
        portfolioService.getPortfolioById(portfolioId),
        portfolioService.getConstituents(portfolioId)
      ]);
      
      setPortfolio(portfolioData);
      setConstituents(constituentsData);
      
      // Try to load cached risk summary
      try {
        const cachedRisk = await portfolioService.getRiskSummary(portfolioId);
        setRiskSummary(cachedRisk);
      } catch (err) {
        // No cached risk available, that's okay
        console.log('No cached risk summary available');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load portfolio';
      setError(errorMessage);
      console.error('Error loading portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  const handlePriceNow = async () => {
    try {
      setPricingLoading(true);
      setError(null);
      const today = new Date().toISOString().split('T')[0];
      const result = await portfolioService.pricePortfolio(portfolioId, today);
      setRiskSummary(result);
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Failed to price portfolio';
      setError(errorMessage);
      console.error('Error pricing portfolio:', err);
    } finally {
      setPricingLoading(false);
    }
  };

  const handleDetachConstituent = async (constituentId: number) => {
    if (!window.confirm('Are you sure you want to remove this trade from the portfolio?')) {
      return;
    }

    try {
      await portfolioService.detachConstituent(portfolioId, constituentId);
      loadPortfolioData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to detach constituent';
      alert(errorMessage);
    }
  };

  const handleAttachSuccess = () => {
    setShowAttachModal(false);
    loadPortfolioData();
  };

  const formatCurrency = (amount: number | undefined) => {
    if (amount === undefined || amount === null) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  };

  const formatNumber = (num: number | undefined, decimals: number = 2) => {
    if (num === undefined || num === null) return '-';
    return num.toFixed(decimals);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-fd-green"></div>
      </div>
    );
  }

  if (error || !portfolio) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error || 'Portfolio not found'}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border p-6">
        <div className="flex justify-between items-center">
          <div className="flex items-center space-x-4">
            {onBack && (
              <button
                onClick={onBack}
                className="text-fd-green hover:text-fd-green-hover font-medium"
              >
                ← Back
              </button>
            )}
            <div>
              <h2 className="text-2xl font-semibold text-fd-text">{portfolio.name}</h2>
              {portfolio.description && (
                <p className="text-sm text-fd-text-muted mt-1">{portfolio.description}</p>
              )}
            </div>
          </div>
          <div className="flex space-x-3">
            <button
              onClick={() => setShowAttachModal(true)}
              className="bg-fd-green hover:bg-fd-green-hover text-fd-dark font-medium py-2 px-4 rounded transition-colors"
            >
              + Add Trades
            </button>
            <button
              onClick={handlePriceNow}
              disabled={pricingLoading || constituents.length === 0}
              className="bg-fd-green hover:bg-fd-green-hover text-fd-dark font-medium py-2 px-4 rounded transition-colors disabled:bg-fd-text-muted/50 disabled:cursor-not-allowed"
            >
              {pricingLoading ? 'Pricing...' : 'Reprice Now'}
            </button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-fd-darker rounded-lg shadow-fd border border-fd-border">
        <div className="border-b border-fd-border px-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('overview')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'overview'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab('constituents')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'constituents'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Constituents ({constituents.length})
            </button>
            <button
              onClick={() => setActiveTab('concentration')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'concentration'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Concentration
            </button>
          </nav>
        </div>

      {/* Tab Content */}
      <div className="p-6">
        {activeTab === 'overview' && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {riskSummary ? (
              <>
                <MetricCard label="Portfolio PV" value={formatCurrency(riskSummary.aggregate.pv)} />
                <MetricCard label="Accrued" value={formatCurrency(riskSummary.aggregate.accrued)} />
                <MetricCard label="CS01" value={formatCurrency(riskSummary.aggregate.cs01)} />
                <MetricCard label="REC01" value={formatCurrency(riskSummary.aggregate.rec01)} />
                <MetricCard label="JTD" value={formatCurrency(riskSummary.aggregate.jtd)} />
                <MetricCard label="Fair Spread (Weighted)" value={`${formatNumber(riskSummary.aggregate.fairSpreadBpsWeighted, 2)} bps`} />
                <div className="col-span-full mt-4">
                  <p className="text-xs text-fd-text-muted">
                    Last calculated: {new Date(riskSummary.valuationDate).toLocaleString()}
                    {' • '}
                    {riskSummary.completeness.priced} of {riskSummary.completeness.constituents} trades priced
                  </p>
                </div>
              </>
            ) : (
              <div className="col-span-full bg-fd-dark p-6 rounded-lg text-center">
                <p className="text-fd-text-muted">No risk metrics available. Click "Reprice Now" to calculate.</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'constituents' && (
          <>
            {constituents.length === 0 ? (
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
                <h3 className="mt-2 text-sm font-medium text-fd-text">No trades in portfolio</h3>
                <p className="mt-1 text-sm text-fd-text-muted">Add trades to get started.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-fd-border">
                  <thead className="bg-fd-dark">
                    <tr>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Trade ID
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Reference Entity
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Notional
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Weight Type
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Weight Value
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="bg-fd-darker divide-y divide-fd-border">
                    {constituents.map((constituent) => (
                      <tr key={constituent.id} className="hover:bg-fd-dark transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                          CDS-{constituent.trade.id}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {constituent.trade.referenceEntity}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {formatCurrency(constituent.trade.notionalAmount)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                          {constituent.weightType}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {constituent.weightType === 'PERCENT'
                            ? `${(constituent.weightValue * 100).toFixed(2)}%`
                            : formatCurrency(constituent.weightValue)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <button
                            onClick={() => handleDetachConstituent(constituent.id)}
                            className="text-red-400 hover:text-red-300 font-medium"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}

        {activeTab === 'concentration' && (
          <div className="space-y-4">
            {riskSummary?.concentration ? (
              <>
                <div className="bg-fd-dark p-6 rounded-lg border border-fd-border">
                  <h3 className="text-lg font-semibold text-fd-text mb-4">Top 5 CS01 Concentration</h3>
                  <div className="text-3xl font-bold text-fd-green">
                    {formatNumber(riskSummary.concentration.top5PctCs01, 1)}%
                  </div>
                  <p className="text-sm text-fd-text-muted mt-2">
                    of total CS01 is concentrated in the top 5 contributors
                  </p>
                </div>

                <div className="bg-fd-dark p-6 rounded-lg border border-fd-border">
                  <h3 className="text-lg font-semibold text-fd-text mb-4">Sector Breakdown</h3>
                  <div className="space-y-3">
                    {riskSummary.concentration.sectorBreakdown.map((sector) => (
                      <div key={sector.sector} className="flex items-center justify-between">
                        <div className="flex items-center space-x-3 flex-1">
                          <span className="text-sm font-medium text-fd-text w-32">{sector.sector}</span>
                          <div className="flex-1 bg-fd-darker rounded-full h-4">
                            <div
                              className={`h-4 rounded-full ${
                                sector.cs01Pct > 25 ? 'bg-red-500' : 'bg-fd-green'
                              }`}
                              style={{ width: `${Math.min(sector.cs01Pct, 100)}%` }}
                            ></div>
                          </div>
                        </div>
                        <span className="text-sm font-semibold text-fd-text ml-3">
                          {formatNumber(sector.cs01Pct, 1)}%
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <div className="bg-fd-dark p-6 rounded-lg border border-fd-border text-center">
                <p className="text-fd-text-muted">No concentration metrics available. Price the portfolio first.</p>
              </div>
            )}
          </div>
        )}
      </div>
      </div>

      {showAttachModal && (
        <AttachTradesModal
          portfolioId={portfolioId}
          onClose={() => setShowAttachModal(false)}
          onSuccess={handleAttachSuccess}
        />
      )}
    </div>
  );
};

const MetricCard: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div className="bg-fd-dark p-4 rounded-lg border border-fd-border">
    <p className="text-sm text-fd-text-muted mb-1">{label}</p>
    <p className="text-xl font-semibold text-fd-text">{value}</p>
  </div>
);

export default PortfolioDetail;
