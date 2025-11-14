import React from 'react';
import { render, screen } from '@testing-library/react';
import SettlementViewComponent, { SettlementView } from '../SettlementView';

describe('SettlementViewComponent', () => {
  it('renders loading state without empty message yet', () => {
    render(<SettlementViewComponent settlement={null} isLoading />);
    // Should not render the "No settlement information" placeholder while loading
    expect(screen.queryByText(/No settlement information/i)).toBeNull();
  });

  it('renders error state', () => {
    render(<SettlementViewComponent settlement={null} error="Boom" />);
    expect(screen.getByText(/Settlement Error/i)).toBeInTheDocument();
  });

  it('renders cash settlement details', () => {
    const settlement: SettlementView = {
      type: 'cash',
      tradeId: '1',
      creditEventId: 'ev1',
      createdAt: new Date().toISOString(),
      notional: 1000000,
      recoveryRate: 0.4,
      payoutAmount: 600000
    };
    render(<SettlementViewComponent settlement={settlement} />);
    expect(screen.getByText(/Cash Settlement/i)).toBeInTheDocument();
    expect(screen.getByText(/Recovery Rate/i)).toBeInTheDocument();
  });

  it('renders physical settlement fields', () => {
    const settlement: SettlementView = {
      type: 'physical',
      tradeId: '2',
      creditEventId: 'ev2',
      createdAt: new Date().toISOString(),
      referenceObligationIsin: 'US1234567890',
      proposedDeliveryDate: new Date().toISOString(),
      status: 'PENDING'
    };
    render(<SettlementViewComponent settlement={settlement} />);
    expect(screen.getByText(/Physical Settlement/i)).toBeInTheDocument();
    expect(screen.getByText(/Reference Obligation ISIN/i)).toBeInTheDocument();
  });
});
