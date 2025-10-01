import React, { useState } from 'react';
import { lifecycleService } from '../../services/lifecycleService';
import { TradeAmendment, AmendTradePayload } from '../../types/lifecycle';

interface Props {
  tradeId: number;
  isOpen: boolean;
  onClose: () => void;
  onCreated: (amendments: TradeAmendment[]) => void;
}

interface AmendmentRow {
  fieldName: string;
  newValue: string;
}

export const AmendTradeModal: React.FC<Props> = ({ tradeId, isOpen, onClose, onCreated }) => {
  const [rows, setRows] = useState<AmendmentRow[]>([{ fieldName: '', newValue: '' }]);
  const [amendedBy, setAmendedBy] = useState('system');
  const [reason, setReason] = useState('');
  const [date, setDate] = useState(new Date().toISOString().substring(0, 10));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const addRow = () => setRows(r => [...r, { fieldName: '', newValue: '' }]);
  const updateRow = (idx: number, field: keyof AmendmentRow, value: string) => {
    setRows(r => r.map((row, i) => (i === idx ? { ...row, [field]: value } : row)));
  };
  const removeRow = (idx: number) => setRows(r => r.filter((_, i) => i !== idx));

  const submit = async () => {
    setLoading(true);
    setError(null);
    try {
      const amendments: Record<string, string> = {};
      rows.filter(r => r.fieldName && r.newValue).forEach(r => {
        amendments[r.fieldName] = r.newValue;
      });

      const payload: AmendTradePayload = {
        amendments,
        amendmentDate: date,
        amendedBy,
        amendmentReason: reason || undefined
      };

      const res = await lifecycleService.amendTrade(tradeId, payload);
      onCreated(res);
      onClose();
    } catch (e: any) {
      setError(e.message || 'Failed to amend trade');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-fd-darker w-full max-w-lg rounded-lg border border-fd-border shadow-fd p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-fd-text font-semibold text-lg">Amend Trade</h3>
          <button onClick={onClose} className="text-fd-text-muted hover:text-fd-text">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>

        <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
          <div className="grid grid-cols-3 gap-3 items-end">
            <div>
              <label className="block text-xs text-fd-text-muted mb-1">Amendment Date</label>
              <input
                type="date"
                value={date}
                onChange={e => setDate(e.target.value)}
                className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm"
              />
            </div>
            <div>
              <label className="block text-xs text-fd-text-muted mb-1">Amended By</label>
              <input
                type="text"
                value={amendedBy}
                onChange={e => setAmendedBy(e.target.value)}
                className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm"
                placeholder="user"
              />
            </div>
            <div className="flex items-end justify-end">
              <button
                onClick={addRow}
                className="px-3 py-1 text-xs bg-fd-dark border border-fd-border rounded text-fd-text-muted hover:text-fd-text"
              >Add Field</button>
            </div>
          </div>

            <div>
              <label className="block text-xs text-fd-text-muted mb-1">Reason (optional)</label>
              <textarea
                value={reason}
                onChange={e => setReason(e.target.value)}
                rows={2}
                className="w-full bg-fd-dark border border-fd-border rounded px-2 py-1 text-fd-text text-sm resize-none"
              />
            </div>

            <div className="space-y-3">
              {rows.map((row, idx) => (
                <div key={idx} className="grid grid-cols-5 gap-2 items-center bg-fd-dark rounded p-2 border border-fd-border">
                  <input
                    type="text"
                    value={row.fieldName}
                    onChange={e => updateRow(idx, 'fieldName', e.target.value)}
                    placeholder="fieldName"
                    className="col-span-2 bg-fd-darker border border-fd-border rounded px-2 py-1 text-fd-text text-xs"
                  />
                  <input
                    type="text"
                    value={row.newValue}
                    onChange={e => updateRow(idx, 'newValue', e.target.value)}
                    placeholder="new value"
                    className="col-span-2 bg-fd-darker border border-fd-border rounded px-2 py-1 text-fd-text text-xs"
                  />
                  <button
                    onClick={() => removeRow(idx)}
                    className="text-red-400 hover:text-red-300 text-xs"
                  >✕</button>
                </div>
              ))}
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
          >{loading ? 'Submitting...' : 'Apply Amendments'}</button>
        </div>
      </div>
    </div>
  );
};
