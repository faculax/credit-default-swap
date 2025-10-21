import React, { useEffect, useState } from 'react';
import { lifecycleService } from '../../services/lifecycleService';
import { AccrualEvent } from '../../types/lifecycle';

interface Props {
  tradeId: number;
}

export const AccrualHistoryPanel: React.FC<Props> = ({ tradeId }) => {
  const [accruals, setAccruals] = useState<AccrualEvent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [range, setRange] = useState({ start: '', end: '' });
  const [cumulative, setCumulative] = useState<number | null>(null);

  const loadAccruals = async () => {
    if (!range.start || !range.end) return;
    setLoading(true);
    setError(null);
    try {
      const data = await lifecycleService.getAccrualEvents(tradeId, range.start, range.end);
      setAccruals(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load accrual events');
    } finally {
      setLoading(false);
    }
  };

  const loadCumulative = async () => {
    try {
      const cum = await lifecycleService.getCumulativeAccrual(tradeId);
      setCumulative(cum);
    } catch {}
  };

  useEffect(() => {
    loadCumulative();
  }, [tradeId, loadCumulative]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-fd-text font-semibold">Accrual History</h4>
        <div className="flex items-center space-x-2">
          <input
            type="date"
            value={range.start}
            onChange={(e) => setRange((r) => ({ ...r, start: e.target.value }))}
            className="bg-fd-dark border border-fd-border rounded px-2 py-1 text-xs text-fd-text"
          />
          <span className="text-fd-text-muted text-xs">to</span>
          <input
            type="date"
            value={range.end}
            onChange={(e) => setRange((r) => ({ ...r, end: e.target.value }))}
            className="bg-fd-dark border border-fd-border rounded px-2 py-1 text-xs text-fd-text"
          />
          <button
            onClick={loadAccruals}
            disabled={!range.start || !range.end}
            className="px-3 py-1 text-xs bg-fd-green text-fd-dark rounded font-medium disabled:opacity-40"
          >
            Load
          </button>
        </div>
      </div>
      {cumulative !== null && (
        <div className="bg-fd-dark border border-fd-border rounded p-3 flex items-center justify-between">
          <span className="text-fd-text-muted text-xs uppercase tracking-wide">
            Cumulative Accrual
          </span>
          <span className="text-fd-green font-semibold">
            {cumulative.toLocaleString(undefined, {
              minimumFractionDigits: 2,
              maximumFractionDigits: 2,
            })}
          </span>
        </div>
      )}
      {loading && <div className="text-fd-text-muted text-sm">Loading accruals...</div>}
      {error && <div className="text-red-400 text-sm">{error}</div>}
      {!loading && accruals.length === 0 && !error && range.start && range.end && (
        <div className="text-fd-text-muted text-sm">No accruals in range.</div>
      )}
      {accruals.length > 0 && (
        <div className="overflow-x-auto border border-fd-border rounded max-h-72 overflow-y-auto">
          <table className="min-w-full text-xs">
            <thead className="bg-fd-dark sticky top-0">
              <tr className="text-fd-text-muted">
                <th className="px-3 py-2 text-left font-medium">Date</th>
                <th className="px-3 py-2 text-left font-medium">Accrual</th>
                <th className="px-3 py-2 text-left font-medium">Cumulative</th>
                <th className="px-3 py-2 text-left font-medium">Notional</th>
                <th className="px-3 py-2 text-left font-medium">Version</th>
              </tr>
            </thead>
            <tbody>
              {accruals.map((a) => (
                <tr key={a.id} className="border-t border-fd-border hover:bg-fd-darker/40">
                  <td className="px-3 py-1 text-fd-text font-mono">{a.accrualDate}</td>
                  <td className="px-3 py-1 text-fd-green font-medium">
                    {a.accrualAmount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-3 py-1 text-fd-text-muted">
                    {a.cumulativeAccrual.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </td>
                  <td className="px-3 py-1 text-fd-text-muted">
                    {a.notionalAmount.toLocaleString()}
                  </td>
                  <td className="px-3 py-1 text-fd-text-muted">{a.tradeVersion}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
