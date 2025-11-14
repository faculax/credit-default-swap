import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import TradeDetailModal from '../../components/trade-detail-modal/TradeDetailModal';

// Inline literals inside mock factories to avoid referencing hoisted consts
const mockTrade = { id: 101, referenceEntity: 'ACME', counterparty: 'BANK', currency: 'USD', notionalAmount: 1000000, spread: 80, buySellProtection: 'BUY', settlementType: 'PHYSICAL', tradeStatus: 'ACTIVE', tradeDate: '2024-01-01', effectiveDate: '2024-01-02', maturityDate: '2029-01-01', accrualStartDate: '2024-01-02', premiumFrequency: 'QUARTERLY', dayCountConvention: 'ACT_360', paymentCalendar: 'NYC', createdAt: '2025-01-01T00:00:00Z' };

jest.mock('../../services/risk/riskService', () => ({
  fetchRiskMeasures: jest.fn().mockResolvedValue({ npv: 500, currency: 'USD', valuationTimestamp: Date.now() })
}));
jest.mock('../../services/creditEventService', () => ({
  creditEventService: {
    getCreditEventsForTrade: jest.fn().mockResolvedValue([]),
    recordCreditEvent: jest.fn().mockResolvedValue({
      creditEvent: {
        id: 'evt1', tradeId: 101, eventType: 'RESTRUCTURING', eventDate: '2025-01-01', noticeDate: '2025-01-01', settlementMethod: 'PHYSICAL', comments: '', createdAt: '2025-01-02'
      },
      affectedTradeIds: [101]
    })
  }
}));
jest.mock('../../services/cdsTradeService', () => ({
  cdsTradeService: { getTradeById: jest.fn().mockResolvedValue({ id: 101, referenceEntity: 'ACME', counterparty: 'BANK', currency: 'USD', notionalAmount: 1000000, spread: 80, buySellProtection: 'BUY', settlementType: 'PHYSICAL', tradeStatus: 'ACTIVE', tradeDate: '2024-01-01', effectiveDate: '2024-01-02', maturityDate: '2029-01-01', accrualStartDate: '2024-01-02', premiumFrequency: 'QUARTERLY', dayCountConvention: 'ACT_360', paymentCalendar: 'NYC', createdAt: '2025-01-01T00:00:00Z' }) }
}));

describe('Integration: Credit Event Recording', () => {
  it('records credit event and shows success notification', async () => {
    render(<TradeDetailModal isOpen trade={mockTrade as any} onClose={jest.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /Credit Events/i }));
  // First button opens form, second submits inside form
  const recordButtons = screen.getAllByRole('button', { name: /Record Credit Event/i });
  fireEvent.click(recordButtons[0]);
  const submitButtons = screen.getAllByRole('button', { name: /Record Credit Event/i });
  fireEvent.click(submitButtons.at(-1)!);
    await waitFor(() => expect(screen.getByText(/Success!/i)).toBeInTheDocument());
  });
});
