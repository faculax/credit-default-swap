import React, { useState, useEffect } from 'react';
import { API_BASE_URL } from '../config/api';

interface FirmRiskSummary {
  id: number;
  calculationDate: string;
  totalGrossNotional: number;
  totalNetNotional: number;
  totalLongNotional: number;
  totalShortNotional: number;
  totalCs01: number;
  totalCs01Long: number;
  totalCs01Short: number;
  totalIr01: number;
  totalIr01Usd: number;
  totalIr01Eur: number;
  totalIr01Gbp: number;
  totalJtd: number;
  totalJtdLong: number;
  totalJtdShort: number;
  totalRec01: number;
  var95: number;
  var99: number;
  currency: string;
}

interface PortfolioRiskMetrics {
  id: number;
  calculationDate: string;
  portfolioId: number;
  grossNotional: number;
  netNotional: number;
  longNotional: number;
  shortNotional: number;
  cs01: number;
  cs01Long: number;
  cs01Short: number;
  ir01: number;
  ir01Usd: number;
  jtd: number;
  jtdLong: number;
  jtdShort: number;
  rec01: number;
  currency: string;
}

interface RiskConcentration {
  id: number;
  calculationDate: string;
  concentrationType: string;
  referenceEntityName: string;
  grossNotional: number;
  cs01: number;
  jtd: number;
  percentageOfTotal: number;
}

const RiskReportingDashboard: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [firmRisk, setFirmRisk] = useState<FirmRiskSummary | null>(null);
  const [portfolios, setPortfolios] = useState<PortfolioRiskMetrics[]>([]);
  const [concentrations, setConcentrations] = useState<RiskConcentration[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const formatCurrency = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '$0';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatNumber = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '0';
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  const formatPercentage = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '0.00%';
    return `${value.toFixed(2)}%`;
  };

  const fetchRiskData = async () => {
    if (!selectedDate) return;

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/eod/risk/date/${selectedDate}/summary?topConcentrations=10`
      );

      if (!response.ok) {
        if (response.status === 404) {
          setError(`No risk data found for ${selectedDate}`);
          setFirmRisk(null);
          setPortfolios([]);
          setConcentrations([]);
          return;
        }
        throw new Error(`Failed to fetch risk data: ${response.statusText}`);
      }

      const data = await response.json();
      setFirmRisk(data.firmRisk);
      setPortfolios(data.portfolios || []);
      setConcentrations(data.topConcentrations || []);
    } catch (err) {
      console.error('Error fetching risk data:', err);
      setError(err instanceof Error ? err.message : 'Failed to fetch risk data');
      setFirmRisk(null);
      setPortfolios([]);
      setConcentrations([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRiskData();
  }, [selectedDate]);

  return (
    <div className="min-h-screen bg-fd-dark">
      <div className="p-6 max-w-screen-2xl mx-auto">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-fd-text mb-2">📊 Risk Reporting</h1>
          <p className="text-fd-text-muted">
            Firm-wide and portfolio risk metrics with concentration analysis
          </p>
        </div>

        {/* Controls */}
        <div className="bg-fd-darker border border-fd-border rounded-lg p-4 mb-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium text-fd-text">
              As of:
            </label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="px-3 py-2 border border-fd-border rounded-md bg-fd-input text-fd-text focus:outline-none focus:ring-2 focus:ring-fd-green text-sm"
            />
          </div>
          <button
            onClick={fetchRiskData}
            className="px-4 py-2 bg-fd-green text-fd-dark rounded-md hover:bg-fd-green-hover transition-colors font-medium text-sm"
          >
            🔄 Refresh
          </button>
        </div>

      {/* Loading State */}
      {loading && (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-fd-green"></div>
          <p className="mt-4 text-fd-text-muted">Loading risk data...</p>
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div className="bg-red-900 bg-opacity-20 border border-red-500 rounded-lg p-4 mb-6">
          <p className="text-red-400">{error}</p>
        </div>
      )}

      {/* Firm-Wide Risk Summary */}
      {!loading && !error && firmRisk && (
        <>
          <div className="bg-fd-darker border border-fd-border rounded-lg p-6 mb-6">
            <h2 className="text-xl font-bold text-fd-text mb-6">
              Firm-Wide Risk Summary
            </h2>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                {/* Notional Exposures */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">Gross Notional</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalGrossNotional)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">Net Notional</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalNetNotional)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">Long Notional</p>
                  <p className="text-2xl font-bold text-fd-green">
                    {formatCurrency(firmRisk.totalLongNotional)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">Short Notional</p>
                  <p className="text-2xl font-bold text-red-400">
                    {formatCurrency(firmRisk.totalShortNotional)}
                  </p>
                </div>

                {/* Credit Spread Risk */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">CS01 (Total)</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalCs01)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">Credit Spread Sensitivity</p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">CS01 (Long)</p>
                  <p className="text-2xl font-bold text-fd-green">
                    {formatCurrency(firmRisk.totalCs01Long)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">CS01 (Short)</p>
                  <p className="text-2xl font-bold text-red-400">
                    {formatCurrency(firmRisk.totalCs01Short)}
                  </p>
                </div>

                {/* Interest Rate Risk */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">IR01 (Total)</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalIr01)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">Interest Rate Sensitivity</p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">IR01 (USD)</p>
                  <p className="text-xl font-semibold text-fd-text">
                    {formatCurrency(firmRisk.totalIr01Usd)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">IR01 (EUR)</p>
                  <p className="text-xl font-semibold text-fd-text">
                    {formatCurrency(firmRisk.totalIr01Eur)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">IR01 (GBP)</p>
                  <p className="text-xl font-semibold text-fd-text">
                    {formatCurrency(firmRisk.totalIr01Gbp)}
                  </p>
                </div>

                {/* Jump-to-Default Risk */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">JTD (Total)</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalJtd)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">Jump-to-Default</p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">JTD (Long)</p>
                  <p className="text-2xl font-bold text-fd-green">
                    {formatCurrency(firmRisk.totalJtdLong)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">JTD (Short)</p>
                  <p className="text-2xl font-bold text-red-400">
                    {formatCurrency(firmRisk.totalJtdShort)}
                  </p>
                </div>

                {/* Other Sensitivities */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">REC01</p>
                  <p className="text-2xl font-bold text-fd-text">
                    {formatCurrency(firmRisk.totalRec01)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">Recovery Rate Sensitivity</p>
                </div>

                {/* VaR */}
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">VaR (95%)</p>
                  <p className="text-2xl font-bold text-orange-400">
                    {formatCurrency(firmRisk.var95)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">1-Day Value at Risk</p>
                </div>
                <div>
                  <p className="text-sm text-fd-text-muted mb-1">VaR (99%)</p>
                  <p className="text-2xl font-bold text-red-400">
                    {formatCurrency(firmRisk.var99)}
                  </p>
                  <p className="text-xs text-fd-text-muted mt-1">1-Day Value at Risk</p>
                </div>
              </div>
            </div>

            {/* Portfolio Risk Metrics */}
            {portfolios.length > 0 && (
              <div className="bg-fd-darker border border-fd-border rounded-lg p-6 mb-6">
                <h2 className="text-xl font-bold text-fd-text mb-6">
                  Portfolio Risk Metrics
                </h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-fd-border">
                    <thead className="bg-fd-dark">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Portfolio
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Gross Notional
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Net Notional
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          CS01
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          IR01
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          JTD
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          REC01
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-fd-darker divide-y divide-fd-border">
                      {portfolios.map((portfolio) => (
                        <tr key={portfolio.id} className="hover:bg-fd-dark">
                          <td className="px-4 py-3 text-sm font-medium text-fd-text">
                            Portfolio #{portfolio.portfolioId}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.grossNotional)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.netNotional)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.cs01)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.ir01)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.jtd)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(portfolio.rec01)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Risk Concentrations */}
            {concentrations.length > 0 && (
              <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
                <h2 className="text-xl font-bold text-fd-text mb-6">
                  Top Risk Concentrations
                </h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-fd-border">
                    <thead className="bg-fd-dark">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Rank
                        </th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Reference Entity
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Gross Notional
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          % of Total
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          CS01
                        </th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          JTD
                        </th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                          Concentration
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-fd-darker divide-y divide-fd-border">
                      {concentrations.map((concentration, index) => (
                        <tr key={concentration.id} className="hover:bg-fd-dark">
                          <td className="px-4 py-3 text-sm font-bold text-fd-text">
                            #{index + 1}
                          </td>
                          <td className="px-4 py-3 text-sm font-medium text-fd-text">
                            {concentration.referenceEntityName}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(concentration.grossNotional)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right font-semibold text-fd-cyan">
                            {formatPercentage(concentration.percentageOfTotal)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(concentration.cs01)}
                          </td>
                          <td className="px-4 py-3 text-sm text-right text-fd-text">
                            {formatCurrency(concentration.jtd)}
                          </td>
                          <td className="px-4 py-3 text-center">
                            <div className="w-full bg-fd-input rounded-full h-2">
                              <div
                                className={`h-2 rounded-full ${
                                  concentration.percentageOfTotal > 25
                                    ? 'bg-red-600'
                                    : concentration.percentageOfTotal > 15
                                    ? 'bg-orange-500'
                                    : 'bg-green-500'
                                }`}
                                style={{
                                  width: `${Math.min(concentration.percentageOfTotal, 100)}%`,
                                }}
                              ></div>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </>
        )}

        {/* Empty State */}
        {!loading && !error && !firmRisk && (
          <div className="text-center py-16 bg-fd-darker rounded-lg border border-fd-border">
            <svg
              className="mx-auto h-16 w-16 text-fd-text-muted mb-4"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
              />
            </svg>
            <h3 className="text-lg font-medium text-fd-text mb-2">No risk data available</h3>
            <p className="text-sm text-fd-text-muted">
              Select a different date or trigger an EOD job to calculate risk metrics.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default RiskReportingDashboard;



