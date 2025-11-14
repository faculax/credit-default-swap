import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TradeDetailModal from '../../components/trade-detail-modal/TradeDetailModal';

const mockTrade = {
  id: 99,
  referenceEntity: 'ACME', counterparty: 'BANK', currency: 'USD', notionalAmount: 1000000, spread: 100, buySellProtection: 'BUY', settlementType: 'CASH', tradeStatus: 'ACTIVE', tradeDate: '2024-01-01', effectiveDate: '2024-01-02', maturityDate: '2029-01-01', accrualStartDate: '2024-01-02', premiumFrequency: 'QUARTERLY', dayCountConvention: 'ACT_360', paymentCalendar: 'NYC', createdAt: '2025-01-01T00:00:00Z'
};

jest.mock('../../services/risk/riskService', () => ({ fetchRiskMeasures: jest.fn().mockResolvedValue({ npv: 500, currency: 'USD', valuationTimestamp: Date.now() }) }));
jest.mock('../../services/creditEventService', () => ({ creditEventService: { getCreditEventsForTrade: jest.fn().mockResolvedValue([]) } }));
jest.mock('../../services/cdsTradeService', () => ({ cdsTradeService: { getTradeById: jest.fn().mockResolvedValue({ id: 99, referenceEntity: 'ACME', counterparty: 'BANK', currency: 'USD', notionalAmount: 1000000, spread: 100, buySellProtection: 'BUY', settlementType: 'CASH', tradeStatus: 'ACTIVE', tradeDate: '2024-01-01', effectiveDate: '2024-01-02', maturityDate: '2029-01-01', accrualStartDate: '2024-01-02', premiumFrequency: 'QUARTERLY', dayCountConvention: 'ACT_360', paymentCalendar: 'NYC', createdAt: '2025-01-01T00:00:00Z' }) } }));

describe('Integration: Trade Detail Risk Tab', () => {
  it('navigates to risk tab and loads risk metrics', async () => {
    render(<TradeDetailModal isOpen trade={mockTrade as any} onClose={jest.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /Risk/i }));
    await waitFor(() => expect(screen.getByText(/Risk Analytics/i)).toBeInTheDocument());
  });
});
