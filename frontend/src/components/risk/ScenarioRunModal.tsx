import React, { useState } from 'react';
import { runRiskScenarios } from '../../services/risk/riskService';
import { ScenarioResponse } from '../../services/risk/riskTypes';

interface Props { tradeId: number; isOpen: boolean; onClose: ()=>void; }

const ScenarioRunModal: React.FC<Props> = ({ tradeId, isOpen, onClose }) => {
  const [shifts, setShifts] = useState<string>('10,-10');
  const [result, setResult] = useState<ScenarioResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if(!isOpen) return null;

  const submit = async () => {
    setLoading(true); setError(null);
    try {
      const parsed = shifts.split(',').map(s=> parseInt(s.trim(),10)).filter(n=> !isNaN(n));
      const res = await runRiskScenarios(tradeId, parsed);
      setResult(res);
    } catch(e:any){ setError(e.message);} finally { setLoading(false);} }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center" role="dialog" aria-modal="true" aria-labelledby="scenario-modal-title">
      <div className="bg-fd-dark border border-fd-border rounded-md p-4 w-full max-w-xl">
        <h3 id="scenario-modal-title" className="text-fd-green font-semibold mb-2">Scenario Run</h3>
        <label className="block text-sm text-fd-text mb-2">Parallel bp shifts (comma separated)
          <input value={shifts} onChange={e=>setShifts(e.target.value)} className="mt-1 w-full bg-fd-darker border border-fd-border rounded px-2 py-1 text-fd-text" />
        </label>
        <div className="flex gap-2 mb-4">
          <button onClick={submit} className="bg-fd-green text-fd-dark px-3 py-1 rounded disabled:opacity-50" disabled={loading}>Run</button>
          <button onClick={onClose} className="bg-fd-darker border border-fd-border text-fd-text px-3 py-1 rounded">Close</button>
        </div>
        {loading && <div className="text-fd-text animate-pulse">Running scenarios...</div>}
        {error && <div className="text-red-400" role="alert">{error}</div>}
        {result && (
          <table className="w-full text-sm text-fd-text border-t border-fd-border mt-2">
            <thead>
              <tr>
                <th className="text-left py-1">Scenario</th>
                <th className="text-left py-1">PV Clean</th>
                <th className="text-left py-1">Par Spread</th>
              </tr>
            </thead>
            <tbody>
              {result.scenarios.map(s => (
                <tr key={s.scenario} className="border-t border-fd-border">
                  <td className="py-1">{s.scenario}</td>
                  <td className="py-1 font-mono">{s.measures.pvClean}</td>
                  <td className="py-1 font-mono">{s.measures.parSpread}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default ScenarioRunModal;
