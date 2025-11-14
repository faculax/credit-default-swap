import React from 'react';
import { render, screen } from '@testing-library/react';
import CashflowScheduleTable from '../CashflowScheduleTable';

describe('CashflowScheduleTable', () => {
  it('renders empty state', () => {
    render(<CashflowScheduleTable cashflows={[]} />);
    expect(screen.getByText(/No cashflow data available/i)).toBeInTheDocument();
  });

  it('renders rows and total PV', () => {
    const sample = [
      {
        cashflowNo: 1,
        payDate: '2025-01-01',
        flowType: 'Payment',
        amount: 1000,
        coupon: 0.0125,
        accrual: 0.25,
        discountFactor: 0.99,
        presentValue: 990,
        presentValueBase: 990,
        currency: 'USD'
      },
      {
        cashflowNo: 2,
        payDate: '2025-04-01',
        flowType: 'Payment',
        amount: 1200,
  coupon: 0.013,
        accrual: 0.26,
        discountFactor: 0.98,
        presentValue: 1176,
        presentValueBase: 1176,
        currency: 'USD'
      }
    ];
    render(<CashflowScheduleTable cashflows={sample as any} />);
    expect(screen.getByText(/Total PV:/i)).toBeInTheDocument();
    expect(screen.getAllByText(/Payment/).length).toBeGreaterThanOrEqual(2);
  });
});
