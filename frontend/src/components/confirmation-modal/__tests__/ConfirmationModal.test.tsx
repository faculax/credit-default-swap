import React from 'react';
import { render, screen } from '@testing-library/react';
import ConfirmationModal from '../ConfirmationModal';

const baseTrade: any = {
  id: 42,
  referenceEntity: 'AAPL',
  counterparty: 'BARC',
  buySellProtection: 'BUY',
  settlementType: 'CASH',
  notionalAmount: 10000000,
  currency: 'USD',
  spread: 125,
  tradeDate: new Date().toISOString(),
  effectiveDate: new Date().toISOString(),
  maturityDate: new Date(Date.now() + 86400000).toISOString(),
  premiumFrequency: 'QUARTERLY',
  tradeStatus: 'ACTIVE',
  createdAt: new Date().toISOString(),
};

describe('ConfirmationModal', () => {
  it('does not render when closed or missing trade', () => {
    const { rerender } = render(<ConfirmationModal isOpen={false} trade={null} onClose={() => {}} />);
    expect(screen.queryByText(/Trade Booked Successfully/i)).toBeNull();
    rerender(<ConfirmationModal isOpen={true} trade={null} onClose={() => {}} />);
    expect(screen.queryByText(/Trade Booked Successfully/i)).toBeNull();
  });

  it('renders trade details', () => {
    render(<ConfirmationModal isOpen={true} trade={baseTrade} onClose={() => {}} />);
    expect(screen.getByText(/Trade Booked Successfully/i)).toBeInTheDocument();
    expect(screen.getByText(/AAPL/i)).toBeInTheDocument();
    expect(screen.getByText(/Buy Protection/i)).toBeInTheDocument();
  });
});
