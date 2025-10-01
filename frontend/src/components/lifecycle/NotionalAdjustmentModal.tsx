import React, { useState } from 'react';
import { lifecycleService } from '../../services/lifecycleService';
import { NotionalAdjustment, NotionalAdjustmentPayload } from '../../types/lifecycle';

interface Props {
  tradeId: number;
  isOpen: boolean;
  onClose: () => void;
  onCreated: (adj: NotionalAdjustment) => void;
}

export const NotionalAdjustmentModal: React.FC<Props> = ({ tradeId, isOpen, onClose, onCreated }) => {
  const [form, setForm] = useState<NotionalAdjustmentPayload>({
    adjustmentDate: new Date().toISOString().substring(0, 10),
    adjustmentType: 'REDUCTION',
    adjustmentAmount: 0
  });
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const updateField = (field: keyof NotionalAdjustmentPayload, value: any) => {
    setForm(f => ({ ...f, [field]: value }));
  };

  const submit = async () => {
    setLoading(true);
    setError(null);
    try {
      const payload = { ...form, adjustmentReason: reason };
      const res = await lifecycleService.adjustNotional(tradeId, payload);
      onCreated(res);
      onClose();
    } catch (e: any) {
      setError(e.message || 'Failed to adjust notional');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-fd-darker w-full max-w-md rounded-lg border border-fd-border shadow-fd p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-fd-text font-semibold text-lg">Notional Adjustment</h3>
          <button onClick={onClose} className="text-fd-text-muted hover:text-fd-text">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="block text-xs text-fd-text-muted mb-1">Adjustment Date</label>
            <input
              type="date"
              value={form.adjustmentDate}
              onChange={e => updateField('adjustmentDate', e.target.value)}
              className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm"
            />
          </div>
          <div>
            <label className="block text-xs text-fd-text-muted mb-1">Adjustment Type</label>
            <select
              value={form.adjustmentType}
              onChange={e => updateField('adjustmentType', e.target.value)}
              className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm"
            >
              <option value="REDUCTION">Reduction</option>
              <option value="PARTIAL_TERMINATION">Partial Termination</option>
              <option value="FULL_TERMINATION">Full Termination</option>
            </select>
          </div>
          {form.adjustmentType !== 'FULL_TERMINATION' && (
            <div>
              <label className="block text-xs text-fd-text-muted mb-1">Adjustment Amount</label>
              <input
                type="number"
                min={0}
                step="0.01"
                value={form.adjustmentAmount}
                onChange={e => updateField('adjustmentAmount', parseFloat(e.target.value))}
                className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm"
              />
            </div>
          )}
          <div>
            <label className="block text-xs text-fd-text-muted mb-1">Reason (optional)</label>
            <textarea
              value={reason}
              onChange={e => setReason(e.target.value)}
              rows={3}
              className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm resize-none"
            />
          </div>
          {error && <div className="text-red-400 text-xs">{error}</div>}
        </div>

        <div className="flex justify-end space-x-3 mt-6">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm bg-fd-dark border border-fd-border rounded text-fd-text-muted hover:text-fd-text"
          >Cancel</button>
          <button
            disabled={loading}
            onClick={submit}
            className="px-5 py-2 text-sm bg-fd-green text-fd-dark rounded font-medium hover:bg-fd-green-hover disabled:opacity-40"
          >{loading ? 'Saving...' : 'Submit'}</button>
        </div>
      </div>
    </div>
  );
};
