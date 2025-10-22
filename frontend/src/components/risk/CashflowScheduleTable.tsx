import React from 'react';
import { Cashflow } from '../../services/risk/riskTypes';

interface Props {
  cashflows: Cashflow[];
}

const CashflowScheduleTable: React.FC<Props> = ({ cashflows }) => {
  if (!cashflows || cashflows.length === 0) {
    return (
      <div className="text-fd-text-muted text-sm text-center py-4">No cashflow data available</div>
    );
  }

  const formatCurrency = (amount: number | null | undefined, currency: string = 'USD') => {
    if (amount === null || amount === undefined) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
  };

  const formatDate = (dateString: string | null | undefined) => {
    if (!dateString) return '-';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

  const formatPercent = (value: number | null | undefined) => {
    if (value === null || value === undefined) return '-';
    return (value * 100).toFixed(4) + '%';
  };

  const formatNumber = (value: number | null | undefined, decimals: number = 6) => {
    if (value === null || value === undefined) return '-';
    return value.toFixed(decimals);
  };

  const currency = cashflows[0]?.currency || 'USD';

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm text-fd-text">
        <thead>
          <tr className="text-left border-b-2 border-fd-border bg-fd-dark">
            <th className="py-2 px-3 font-medium">#</th>
            <th className="py-2 px-3 font-medium">Pay Date</th>
            <th className="py-2 px-3 font-medium">Type</th>
            <th className="py-2 px-3 font-medium text-right">Amount</th>
            <th className="py-2 px-3 font-medium text-right">Coupon</th>
            <th className="py-2 px-3 font-medium text-right">Accrual</th>
            <th className="py-2 px-3 font-medium text-right">DF</th>
            <th className="py-2 px-3 font-medium text-right">PV ({currency})</th>
            <th className="py-2 px-3 font-medium text-right">PV (USD)</th>
          </tr>
        </thead>
        <tbody>
          {cashflows.map((cf, index) => (
            <tr
              key={index}
              className="border-b border-fd-border hover:bg-fd-dark transition-colors"
            >
              <td className="py-2 px-3 text-fd-text-muted">{cf.cashflowNo || index + 1}</td>
              <td className="py-2 px-3">{formatDate(cf.payDate)}</td>
              <td className="py-2 px-3">
                <span className="inline-flex px-2 py-0.5 rounded text-xs font-medium bg-fd-green/20 text-fd-green">
                  {cf.flowType || 'Payment'}
                </span>
              </td>
              <td className="py-2 px-3 font-mono text-right">
                {formatCurrency(cf.amount, currency)}
              </td>
              <td className="py-2 px-3 font-mono text-right">{formatPercent(cf.coupon)}</td>
              <td className="py-2 px-3 font-mono text-right">{formatNumber(cf.accrual, 4)}</td>
              <td className="py-2 px-3 font-mono text-right">
                {formatNumber(cf.discountFactor, 6)}
              </td>
              <td className="py-2 px-3 font-mono text-right font-medium">
                {formatCurrency(cf.presentValue, currency)}
              </td>
              <td className="py-2 px-3 font-mono text-right text-fd-text-muted">
                {formatCurrency(cf.presentValueBase, 'USD')}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t-2 border-fd-border bg-fd-dark font-semibold">
            <td colSpan={7} className="py-2 px-3 text-right">
              Total PV:
            </td>
            <td className="py-2 px-3 font-mono text-right">
              {formatCurrency(
                cashflows.reduce((sum, cf) => sum + (cf.presentValue || 0), 0),
                currency
              )}
            </td>
            <td className="py-2 px-3 font-mono text-right text-fd-text-muted">
              {formatCurrency(
                cashflows.reduce((sum, cf) => sum + (cf.presentValueBase || 0), 0),
                'USD'
              )}
            </td>
          </tr>
        </tfoot>
      </table>

      {/* Accrual Period Details */}
      <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
        <div className="bg-fd-dark rounded p-3">
          <h5 className="font-semibold text-fd-text mb-2">Accrual Periods</h5>
          <div className="space-y-1">
            {cashflows.slice(0, 3).map((cf, idx) => (
              <div key={idx} className="flex justify-between text-fd-text-muted">
                <span>Period {idx + 1}:</span>
                <span>
                  {formatDate(cf.accrualStartDate)} → {formatDate(cf.accrualEndDate)}
                </span>
              </div>
            ))}
            {cashflows.length > 3 && (
              <div className="text-fd-text-muted italic">
                ... and {cashflows.length - 3} more periods
              </div>
            )}
          </div>
        </div>

        <div className="bg-fd-dark rounded p-3">
          <h5 className="font-semibold text-fd-text mb-2">Summary</h5>
          <div className="space-y-1">
            <div className="flex justify-between text-fd-text-muted">
              <span>Total Cashflows:</span>
              <span className="text-fd-text font-medium">{cashflows.length}</span>
            </div>
            <div className="flex justify-between text-fd-text-muted">
              <span>Notional:</span>
              <span className="text-fd-text font-medium">
                {formatCurrency(cashflows[0]?.notional, currency)}
              </span>
            </div>
            <div className="flex justify-between text-fd-text-muted">
              <span>Average Coupon:</span>
              <span className="text-fd-text font-medium">
                {formatPercent(
                  cashflows.reduce((sum, cf) => sum + (cf.coupon || 0), 0) / cashflows.length
                )}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CashflowScheduleTable;
