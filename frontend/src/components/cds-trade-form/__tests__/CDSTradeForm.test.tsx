import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import CDSTradeForm from '../CDSTradeForm';

describe('CDSTradeForm', () => {
  it('validates required fields and prevents submit with missing data', () => {
    const onSubmit = jest.fn();
    render(<CDSTradeForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByRole('button', { name: /Book Trade/i }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText(/Reference Entity is required/i)).toBeInTheDocument();
  });

  it('fills random data and submits', () => {
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
