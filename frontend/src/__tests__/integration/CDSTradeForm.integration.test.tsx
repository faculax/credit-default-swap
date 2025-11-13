import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import CDSTradeForm from '../../components/cds-trade-form/CDSTradeForm';

describe('Integration: CDSTradeForm submission', () => {
  it('fills random data and submits successfully', () => {
    jest.useFakeTimers();
    const onSubmit = jest.fn();
    render(<CDSTradeForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByRole('button', { name: /Fill Random Data/i }));
    fireEvent.click(screen.getByRole('button', { name: /Book Trade/i }));
    jest.runAllTimers();
    expect(onSubmit).toHaveBeenCalled();
    jest.useRealTimers();
  });
});
