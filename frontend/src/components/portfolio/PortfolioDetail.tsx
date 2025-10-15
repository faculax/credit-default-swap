import React, { useState, useEffect } from 'react';
import { portfolioService, CdsPortfolio, CdsPortfolioConstituent, BondPortfolioConstituent, BasketPortfolioConstituent, PortfolioPricingResponse } from '../../services/portfolioService';
import { cdsTradeService, CDSTradeResponse } from '../../services/cdsTradeService';
import AttachInstrumentsModal from './AttachInstrumentsModal';
import SimulationPanel from './simulation/SimulationPanel';
import EnhancedOverview from './EnhancedOverview';

interface PortfolioDetailProps {
  portfolioId: number;
  onBack?: () => void;
}

const PortfolioDetail: React.FC<PortfolioDetailProps> = ({ portfolioId, onBack }) => {
  const [portfolio, setPortfolio] = useState<CdsPortfolio | null>(null);
  const [constituents, setConstituents] = useState<CdsPortfolioConstituent[]>([]);
  const [bondConstituents, setBondConstituents] = useState<BondPortfolioConstituent[]>([]);
  const [basketConstituents, setBasketConstituents] = useState<BasketPortfolioConstituent[]>([]);
  const [riskSummary, setRiskSummary] = useState<PortfolioPricingResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'constituents' | 'concentration' | 'simulation'>('overview');
  const [showAttachModal, setShowAttachModal] = useState(false);
  const [valuationDate, setValuationDate] = useState<string>('');

  useEffect(() => {
    loadPortfolioData();
  }, [portfolioId]);

  const loadPortfolioData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [portfolioData, constituentsData, bondsData, basketsData] = await Promise.all([
        portfolioService.getPortfolioById(portfolioId),
        portfolioService.getConstituents(portfolioId),
        portfolioService.getPortfolioBonds(portfolioId),
        portfolioService.getPortfolioBaskets(portfolioId)
      ]);
      
      setPortfolio(portfolioData);
      setConstituents(constituentsData);
      setBondConstituents(bondsData);
      setBasketConstituents(basketsData);
      
      // Try to load cached risk summary
      try {
        const cachedRisk = await portfolioService.getRiskSummary(portfolioId);
        setRiskSummary(cachedRisk);
        setValuationDate(cachedRisk.valuationDate);
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

  const getBusinessDaysFromToday = (days: number): string => {
    const today = new Date();
    const target = new Date(today);
    let addedDays = 0;
    
    while (addedDays < days) {
      target.setDate(target.getDate() + 1);
      const dayOfWeek = target.getDay();
      if (dayOfWeek !== 0 && dayOfWeek !== 6) {
        addedDays++;
      }
    }
    
    return target.toISOString().split('T')[0];
  };

  const handleQuickValuationDate = async (option: 'today' | 't+1' | 't+7' | 't+45') => {
    let newDate: string;
    
    switch(option) {
      case 'today':
        newDate = new Date().toISOString().split('T')[0];
        break;
      case 't+1':
        newDate = getBusinessDaysFromToday(1);
        break;
      case 't+7':
        newDate = getBusinessDaysFromToday(7);
        break;
      case 't+45':
        newDate = getBusinessDaysFromToday(45);
        break;
    }
    
    setValuationDate(newDate);
    await handlePriceWithDate(newDate);
  };

  const handlePriceWithDate = async (date: string) => {
    try {
      setPricingLoading(true);
      setError(null);
      const result = await portfolioService.pricePortfolio(portfolioId, date);
      setRiskSummary(result);
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Failed to price portfolio';
      setError(errorMessage);
      console.error('Error pricing portfolio:', err);
    } finally {
      setPricingLoading(false);
    }
  };

  const handlePriceNow = async () => {
    const today = new Date().toISOString().split('T')[0];
    setValuationDate(today);
    await handlePriceWithDate(today);
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

  const handleDetachBond = async (bondId: number) => {
    if (!window.confirm('Are you sure you want to remove this bond from the portfolio?')) {
      return;
    }

    try {
      await portfolioService.removeBond(portfolioId, bondId);
      loadPortfolioData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to remove bond';
      alert(errorMessage);
    }
  };

  const handleDetachBasket = async (basketId: number) => {
    if (!window.confirm('Are you sure you want to remove this basket from the portfolio?')) {
      return;
    }

    try {
      await portfolioService.removeBasket(portfolioId, basketId);
      loadPortfolioData();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to remove basket';
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
          <div className="flex items-center space-x-3">
            {/* Valuation Date Quick Selector */}
            <div className="flex items-center space-x-2">
              <span className="text-xs text-fd-text-muted">Valuation:</span>
              <div className="flex space-x-1">
                <button
                  onClick={() => handleQuickValuationDate('today')}
                  disabled={pricingLoading}
                  className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                    valuationDate === new Date().toISOString().split('T')[0]
                      ? 'bg-fd-green text-fd-dark' 
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  Today
                </button>
                <button
                  onClick={() => handleQuickValuationDate('t+1')}
                  disabled={pricingLoading}
                  className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                    valuationDate === getBusinessDaysFromToday(1) 
                      ? 'bg-fd-green text-fd-dark' 
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  T+1
                </button>
                <button
                  onClick={() => handleQuickValuationDate('t+7')}
                  disabled={pricingLoading}
                  className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                    valuationDate === getBusinessDaysFromToday(7) 
                      ? 'bg-fd-green text-fd-dark' 
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  T+7
                </button>
                <button
                  onClick={() => handleQuickValuationDate('t+45')}
                  disabled={pricingLoading}
                  className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                    valuationDate === getBusinessDaysFromToday(45) 
                      ? 'bg-fd-green text-fd-dark' 
                      : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                  } disabled:opacity-50 disabled:cursor-not-allowed`}
                >
                  T+45
                </button>
              </div>
            </div>

            <button
              onClick={() => setShowAttachModal(true)}
              className="bg-fd-green hover:bg-fd-green-hover text-fd-dark font-medium py-2 px-4 rounded transition-colors"
            >
              + Add Instruments
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
              Constituents ({constituents.length + bondConstituents.length + basketConstituents.length})
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
            <button
              onClick={() => setActiveTab('simulation')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'simulation'
                  ? 'border-fd-green text-fd-green'
                  : 'border-transparent text-fd-text-muted hover:text-fd-text hover:border-fd-border'
              }`}
            >
              Monte Carlo Simulation
            </button>
          </nav>
        </div>

      {/* Tab Content */}
      <div className="p-6">
        {activeTab === 'overview' && (
          <EnhancedOverview 
            portfolioId={portfolioId}
            cdsConstituents={constituents}
            bondConstituents={bondConstituents}
            basketConstituents={basketConstituents}
            pricingData={riskSummary}
          />
        )}

        {activeTab === 'constituents' && (
          <>
            {constituents.length === 0 && bondConstituents.length === 0 && basketConstituents.length === 0 ? (
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
                <h3 className="mt-2 text-sm font-medium text-fd-text">No instruments in portfolio</h3>
                <p className="mt-1 text-sm text-fd-text-muted">Add CDS trades or bonds to get started.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-fd-border">
                  <thead className="bg-fd-dark">
                    <tr>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Type
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        ID
                      </th>
                      <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                        Reference / Issuer
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
                      <tr key={`cds-${constituent.id}`} className="hover:bg-fd-dark transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-900 text-blue-200">
                            🛡️ CDS
                          </span>
                        </td>
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
                    {bondConstituents.map((bondConstituent) => (
                      <tr key={`bond-${bondConstituent.id}`} className="hover:bg-fd-dark transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-900 text-green-200">
                            📜 Bond
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                          {bondConstituent.bond.isin}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {bondConstituent.bond.issuer}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {formatCurrency(bondConstituent.bond.notional)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                          {bondConstituent.weightType}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {formatCurrency(bondConstituent.weightValue)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <button
                            onClick={() => handleDetachBond(bondConstituent.bond.id)}
                            className="text-red-400 hover:text-red-300 font-medium"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                    {basketConstituents.map((basketConstituent) => (
                      <tr key={`basket-${basketConstituent.id}`} className="hover:bg-fd-dark transition-colors">
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-purple-900 text-purple-200">
                            🗂️ Basket
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-fd-text">
                          {basketConstituent.basket.name}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {basketConstituent.basket.basketType === 'FIRST_TO_DEFAULT' 
                            ? 'FTD' 
                            : basketConstituent.basket.basketType === 'NTH_TO_DEFAULT'
                            ? `${basketConstituent.basket.kthToDefault}th-to-Default`
                            : 'Tranchette'
                          } ({basketConstituent.basket.numberOfConstituents} names)
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {formatCurrency(basketConstituent.basket.notional)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text-muted">
                          {basketConstituent.weightType}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-fd-text">
                          {formatCurrency(basketConstituent.weightValue)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                          <button
                            onClick={() => handleDetachBasket(basketConstituent.basket.id)}
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

        {activeTab === 'simulation' && (
          <SimulationPanel portfolioId={portfolioId} />
        )}
      </div>
      </div>

      {showAttachModal && (
        <AttachInstrumentsModal
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
