import React, { useEffect, useState } from 'react';
import { fetchRiskMeasures } from '../../services/risk/riskService';
import { RiskMeasures } from '../../services/risk/riskTypes';

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
  if(loading) return <div className="animate-pulse text-fd-text">Loading risk measures...</div>;
  if(error) return <div className="text-red-400" role="alert">Failed: {error}</div>;
  if(!data) return null;

  const legacyRows: Array<[string, number | undefined]> = [
    ['PV Clean', data.pvClean],
    ['PV Dirty', data.pvDirty],
    ['Par Spread', data.parSpread],
    ['CS01', data.cs01],
    ['DV01', data.dv01],
    ['JTD', data.jtd],
    ['Recovery01', data.recovery01]
  ];

  const oreRows: Array<[string, number | string | undefined]> = [
    ['NPV', data.npv],
    ['Gamma', data.gamma],
    ['VaR 95%', data.var95],
    ['Expected Shortfall', data.expectedShortfall],
    ['Currency', data.currency]
  ];

  const greeksRows: Array<[string, number | undefined]> = data.greeks ? [
    ['Delta', data.greeks.delta],
    ['Rho', data.greeks.rho],
    ['Theta', data.greeks.theta],
    ['Gamma', data.greeks.gamma],
    ['Vega', data.greeks.vega]
  ] : [];

  const formatValue = (val: number | string | undefined): string => {
    if (val === null || val === undefined) return '—';
    if (typeof val === 'string') return val;
    if (typeof val === 'number') return val.toLocaleString(undefined, { maximumFractionDigits: 6 });
    return String(val);
  };

  return (
    <div className="bg-fd-darker p-4 rounded-md border border-fd-border space-y-4" aria-labelledby="risk-measures-heading">
      <h3 id="risk-measures-heading" className="text-fd-green font-semibold">Risk Measures</h3>
      
      {/* ORE Measures Section */}
      {(oreRows.some(([, val]) => val !== null && val !== undefined)) && (
        <div>
          <h4 className="text-fd-text font-medium mb-2">ORE Risk Metrics</h4>
          <table className="w-full text-sm text-fd-text">
            <thead>
              <tr className="text-left border-b border-fd-border">
                <th className="py-1 text-left">Measure</th>
                <th className="py-1 text-right">Value</th>
              </tr>
            </thead>
            <tbody>
              {oreRows.map(([label, val]) => (
                <tr key={label} className="border-b border-fd-border last:border-none">
                  <td className="py-1 pr-4 text-left">{label}</td>
                  <td className="py-1 font-mono text-right">{formatValue(val)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Greeks Section */}
      {greeksRows.length > 0 && (
        <div>
          <h4 className="text-fd-text font-medium mb-2">Greeks</h4>
          <table className="w-full text-sm text-fd-text">
            <thead>
              <tr className="text-left border-b border-fd-border">
                <th className="py-1 text-left">Greek</th>
                <th className="py-1 text-right">Value</th>
              </tr>
            </thead>
            <tbody>
              {greeksRows.map(([label, val]) => (
                <tr key={label} className="border-b border-fd-border last:border-none">
                  <td className="py-1 pr-4 text-left">{label}</td>
                  <td className="py-1 font-mono text-right">{formatValue(val)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Legacy CDS Measures Section */}
      {(legacyRows.some(([, val]) => val !== null && val !== undefined)) && (
        <div>
          <h4 className="text-fd-text font-medium mb-2">Legacy CDS Metrics</h4>
          <table className="w-full text-sm text-fd-text">
            <thead>
              <tr className="text-left border-b border-fd-border">
                <th className="py-1 text-left">Measure</th>
                <th className="py-1 text-right">Value</th>
              </tr>
            </thead>
            <tbody>
              {legacyRows.map(([label, val]) => (
                <tr key={label} className="border-b border-fd-border last:border-none">
                  <td className="py-1 pr-4 text-left">{label}</td>
                  <td className="py-1 font-mono text-right">{formatValue(val)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="text-xs text-fd-muted mt-2" aria-live="polite">
        Valuation: {new Date(data.valuationTimestamp).toLocaleTimeString()}
      </div>
    </div>
  );
};

export default RiskMeasuresPanel;
