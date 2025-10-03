import React, { useEffect, useState } from 'react';
import { fetchRiskMeasures } from '../../services/risk/riskService';
import { RiskMeasures } from '../../services/risk/riskTypes';
import CashflowScheduleTable from './CashflowScheduleTable';

interface Props { tradeId: number; }

const RiskMeasuresPanel: React.FC<Props> = ({ tradeId }) => {
  const [data, setData] = useState<RiskMeasures | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if(!tradeId) return;
    setLoading(true);
    fetchRiskMeasures(tradeId)
      .then(setData)
      .catch(e => setError(e.message))
      .finally(()=> setLoading(false));
  }, [tradeId]);

  if(!tradeId) return <div className="text-fd-text">No trade selected</div>;
  if(loading) return (
    <div className="flex items-center gap-2 text-fd-text">
      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-fd-green"></div>
      <span>Calculating risk measures with ORE...</span>
    </div>
  );
  if(error) return <div className="text-red-400" role="alert">Failed: {error}</div>;
  if(!data) return null;

  const formatValue = (value: number | null | undefined, decimals: number = 2): string => {
    if (value === null || value === undefined) return '-';
    return typeof value === 'number' ? value.toLocaleString(undefined, {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }) : String(value);
  };

  const formatCurrency = (value: number | null | undefined, currency: string = 'USD'): string => {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatBasisPoints = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '-';
    return (value * 10000).toFixed(2) + ' bps';
  };

  const hasCashflows = data.cashflows && data.cashflows.length > 0;
  const hasCDSMetrics = data.fairSpreadClean !== null || data.protectionLegNPV !== null;

  return (
    <div className="space-y-6">
      {/* Core ORE Valuation */}
      <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
        <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
          </svg>
          ORE Valuation
        </h3>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Net Present Value</div>
            <div className="text-2xl font-bold text-fd-green font-mono">
              {formatCurrency(data.npv, data.currency)}
            </div>
          </div>
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Currency</div>
            <div className="text-2xl font-bold text-fd-text font-mono">{data.currency || 'USD'}</div>
          </div>
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Valuation Time</div>
            <div className="text-sm text-fd-text">
              {new Date(data.valuationTimestamp).toLocaleString()}
            </div>
          </div>
        </div>
      </div>

      {/* CDS-Specific Metrics (if available) */}
      {hasCDSMetrics && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
            </svg>
            CDS Valuation Breakdown
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 text-sm">
            <div>
              <span className="text-fd-text-muted">Fair Spread (Clean):</span>
              <div className="font-mono text-fd-text font-semibold">{formatBasisPoints(data.fairSpreadClean)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Fair Spread (Dirty):</span>
              <div className="font-mono text-fd-text font-semibold">{formatBasisPoints(data.fairSpreadDirty)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Protection Leg NPV:</span>
              <div className="font-mono text-fd-text font-semibold">{formatCurrency(data.protectionLegNPV, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Premium Leg NPV:</span>
              <div className="font-mono text-fd-text font-semibold">{formatCurrency(data.premiumLegNPVClean, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Accrued Premium:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.accruedPremium, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Upfront Premium:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.upfrontPremium, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Coupon Leg BPS:</span>
              <div className="font-mono text-fd-text">{formatValue(data.couponLegBPS, 2)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Current Notional:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.currentNotional, data.currency)}</div>
            </div>
          </div>
        </div>
      )}

      {/* Credit Risk Profile (if available) */}
      {(data.defaultProbabilities && data.defaultProbabilities.length > 0) && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-1.964-1.333-2.732 0L3.268 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>
            Credit Risk Profile
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <h4 className="text-sm font-medium text-fd-text mb-2">Default Probabilities by Period</h4>
              <div className="space-y-1 text-sm">
                {data.defaultProbabilities.slice(0, 6).map((prob, idx) => (
                  <div key={idx} className="flex justify-between">
                    <span className="text-fd-text-muted">Period {idx + 1}:</span>
                    <span className="font-mono text-fd-text">{(prob * 100).toFixed(2)}%</span>
                  </div>
                ))}
              </div>
            </div>
            
            {data.expectedLosses && data.expectedLosses.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-fd-text mb-2">Expected Losses by Period</h4>
                <div className="space-y-1 text-sm">
                  {data.expectedLosses.slice(0, 6).map((loss, idx) => (
                    <div key={idx} className="flex justify-between">
                      <span className="text-fd-text-muted">Period {idx + 1}:</span>
                      <span className="font-mono text-fd-text">{formatCurrency(loss, data.currency)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Cashflow Schedule */}
      {hasCashflows && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            Cashflow Schedule
            <span className="ml-auto text-sm font-normal text-fd-text-muted">
              {data.cashflows!.length} payment{data.cashflows!.length !== 1 ? 's' : ''}
            </span>
          </h3>
          
          <CashflowScheduleTable cashflows={data.cashflows!} />
        </div>
      )}

      {/* No data message */}
      {!hasCashflows && !hasCDSMetrics && (
        <div className="bg-fd-darker p-6 rounded-md border border-dashed border-fd-border text-center">
          <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <p className="text-fd-text-muted">
            Additional CDS metrics and cashflow schedule will appear here once available.
          </p>
          <p className="text-fd-text-muted text-sm mt-2">
            Currently showing NPV calculated by ORE.
          </p>
        </div>
      )}
    </div>
  );
};

export default RiskMeasuresPanel;
