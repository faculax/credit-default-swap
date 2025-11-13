import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import RiskMeasuresPanel from '../../components/risk/RiskMeasuresPanel';

jest.mock('../../services/risk/riskService', () => ({ fetchRiskMeasures: jest.fn().mockResolvedValue({ npv: 1000, currency: 'USD', valuationTimestamp: Date.now(), fairSpreadClean: 0.01 }) }));
jest.mock('../../services/lifecycleService', () => ({
  lifecycleService: {
    getCouponSchedule: jest.fn().mockResolvedValue([
      { id: 1, paymentDate: '2025-01-10', periodStartDate: '2024-12-10', periodEndDate: '2025-01-10', accrualDays: 31, notionalAmount: 1000000, couponAmount: 10000, paid: false },
      { id: 2, paymentDate: '2025-02-10', periodStartDate: '2025-01-10', periodEndDate: '2025-02-10', accrualDays: 31, notionalAmount: 1000000, couponAmount: 10000, paid: false }
    ]),
    payCoupon: jest.fn().mockResolvedValue({ id: 1, paid: true }),
    unpayCoupon: jest.fn(),
    generateCouponSchedule: jest.fn(),
  }
}));
jest.mock('../../services/creditEventService', () => ({ creditEventService: { getCreditEventsForTrade: jest.fn().mockResolvedValue([]) } }));

describe('Integration: RiskMeasuresPanel coupon flow', () => {
  it('renders coupon schedule and pays first coupon', async () => {
    render(<RiskMeasuresPanel tradeId={555} trade={{ id: 555, tradeStatus: 'ACTIVE' } as any} />);
    await waitFor(() => expect(screen.getByText(/Coupon Payment Schedule/i)).toBeInTheDocument());
    const payOnTimeBtn = screen.getByRole('button', { name: /Pay On Time/i });
    fireEvent.click(payOnTimeBtn);
    expect(payOnTimeBtn).toBeDisabled();
  });
});
