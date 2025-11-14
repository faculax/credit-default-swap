import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import TradeDetailModal from '../TradeDetailModal';

const mockTrade = {
  id: 10,
  referenceEntity: 'ACME',
  counterparty: 'BANK_X',
  currency: 'USD',
  notionalAmount: 5000000,
  spread: 120,
  buySellProtection: 'BUY',
  settlementType: 'CASH',
  tradeStatus: 'ACTIVE',
  tradeDate: '2024-01-01',
  effectiveDate: '2024-01-02',
  maturityDate: '2029-01-01',
  accrualStartDate: '2024-01-02',
  premiumFrequency: 'QUARTERLY',
  dayCountConvention: 'ACT_360',
  paymentCalendar: 'NYC',
  createdAt: '2025-01-02T10:00:00Z',
  updatedAt: '2025-01-03T10:00:00Z',
  recoveryRate: 40,
  obligation: { isin: 'US1234567890', issuer: 'ACME', seniority: 'SENIOR', couponRate: 0.05, maturityDate: '2028-12-31' }
};

jest.mock('../../../services/risk/riskService', () => ({
  fetchRiskMeasures: jest.fn().mockResolvedValue({ npv: 1000, currency: 'USD', valuationTimestamp: Date.now() })
}));
jest.mock('../../../services/creditEventService', () => ({
  creditEventService: { getCreditEventsForTrade: jest.fn().mockResolvedValue([]), recordCreditEvent: jest.fn() }
}));
jest.mock('../../../services/cdsTradeService', () => ({
  cdsTradeService: { getTradeById: jest.fn().mockResolvedValue(mockTrade) }
}));

describe('TradeDetailModal', () => {
  it('renders and switches tabs', () => {
    render(<TradeDetailModal isOpen trade={mockTrade as any} onClose={jest.fn()} />);
    expect(screen.getByText(/CDS Trade Details/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Risk/i }));
    expect(screen.getByText(/Risk Analytics/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Credit Events/i }));
    expect(screen.getByText(/Credit Events/i)).toBeInTheDocument();
  });
});
