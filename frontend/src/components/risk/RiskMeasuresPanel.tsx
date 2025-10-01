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

  const rows: Array<[string, number]> = [
    ['PV Clean', data.pvClean],
    ['PV Dirty', data.pvDirty],
    ['Par Spread', data.parSpread],
    ['CS01', data.cs01],
    ['DV01', data.dv01],
    ['JTD', data.jtd],
    ['Recovery01', data.recovery01]
  ];

  return (
    <div className="bg-fd-darker p-4 rounded-md border border-fd-border" aria-labelledby="risk-measures-heading">
      <h3 id="risk-measures-heading" className="text-fd-green font-semibold mb-2">Risk Measures</h3>
      <table className="w-full text-sm text-fd-text">
        <thead>
          <tr className="text-left border-b border-fd-border">
            <th className="py-1">Measure</th>
            <th className="py-1">Value</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(([label,val]) => (
            <tr key={label} className="border-b border-fd-border last:border-none">
              <td className="py-1 pr-4">{label}</td>
              <td className="py-1 font-mono">{val}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="text-xs text-fd-muted mt-2" aria-live="polite">Valuation: {new Date(data.valuationTimestamp).toLocaleTimeString()}</div>
    </div>
  );
};

export default RiskMeasuresPanel;
