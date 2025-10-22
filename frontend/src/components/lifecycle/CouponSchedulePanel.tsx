import React, { useEffect, useState, useCallback } from 'react';
import { lifecycleService } from '../../services/lifecycleService';
import { CouponPeriod } from '../../types/lifecycle';

interface Props {
  tradeId: number;
  autoGenerate?: boolean;
}

export const CouponSchedulePanel: React.FC<Props> = ({ tradeId, autoGenerate = false }) => {
  const [periods, setPeriods] = useState<CouponPeriod[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadSchedule = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await lifecycleService.getCouponSchedule(tradeId);
      setPeriods(data);
    } catch (e: any) {
      setError(e.message || 'Failed to load coupon schedule');
    } finally {
      setLoading(false);
    }
  }, [tradeId]);

  const generateSchedule = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await lifecycleService.generateCouponSchedule(tradeId);
      setPeriods(data);
    } catch (e: any) {
      setError(e.message || 'Failed to generate coupon schedule');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSchedule().then(() => {
      if (autoGenerate && periods.length === 0) {
        generateSchedule();
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tradeId]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-fd-text font-semibold">Coupon Schedule</h4>
        <div className="space-x-2">
          <button
            onClick={loadSchedule}
            className="px-3 py-1 text-xs bg-fd-dark border border-fd-border rounded hover:bg-fd-darker text-fd-text-muted"
          >
            Refresh
          </button>
          <button
            onClick={generateSchedule}
            className="px-3 py-1 text-xs bg-fd-green text-fd-dark rounded font-medium hover:bg-fd-green-hover"
          >
            Generate
          </button>
        </div>
      </div>
      {loading && <div className="text-fd-text-muted text-sm">Loading schedule...</div>}
      {error && <div className="text-red-400 text-sm">{error}</div>}
      {!loading && periods.length === 0 && !error && (
        <div className="text-fd-text-muted text-sm">No coupon periods yet. Generate schedule.</div>
      )}
      {periods.length > 0 && (
        <div className="overflow-x-auto border border-fd-border rounded">
          <table className="min-w-full text-sm">
            <thead className="bg-fd-dark">
              <tr className="text-fd-text-muted">
                <th className="px-3 py-2 text-left font-medium">Start</th>
                <th className="px-3 py-2 text-left font-medium">End</th>
                <th className="px-3 py-2 text-left font-medium">Payment</th>
                <th className="px-3 py-2 text-left font-medium">Days</th>
                <th className="px-3 py-2 text-left font-medium">Notional</th>
              </tr>
            </thead>
            <tbody>
              {periods.map((p) => (
                <tr key={p.id} className="border-t border-fd-border hover:bg-fd-darker/50">
                  <td className="px-3 py-2 text-fd-text font-mono">{p.periodStartDate}</td>
                  <td className="px-3 py-2 text-fd-text font-mono">{p.periodEndDate}</td>
                  <td className="px-3 py-2 text-fd-text font-mono">{p.paymentDate}</td>
                  <td className="px-3 py-2 text-fd-text-muted">{p.accrualDays}</td>
                  <td className="px-3 py-2 text-fd-green font-medium">
                    {p.notionalAmount.toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
