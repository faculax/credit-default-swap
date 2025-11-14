import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CDSTradeForm from '../../components/cds-trade-form/CDSTradeForm';

jest.mock('../../services/bondService', () => ({
  bondService: {
    getBondsByIssuer: jest.fn().mockResolvedValue([
      { id: 1, issuer: 'ACME', seniority: 'SENIOR', couponRate: 5, maturityDate: '2028-12-31', isin: 'US0000000001' }
    ])
  }
}));

describe('Integration: CDSTradeForm submission', () => {
  it('fills random data and submits successfully', async () => {
    jest.useFakeTimers();
    const onSubmit = jest.fn();
    render(<CDSTradeForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByRole('button', { name: /Fill Random Data/i }));
    const obligationSelect = await screen.findByLabelText(/Obligation \(Bond\)/i);
    fireEvent.change(obligationSelect, { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: /Book Trade/i }));
    jest.runAllTimers();
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
    jest.useRealTimers();
  });
});
