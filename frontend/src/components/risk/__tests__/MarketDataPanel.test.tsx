import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import MarketDataPanel from '../MarketDataPanel';

describe('MarketDataPanel', () => {
  it('renders structured view by default with headers', () => {
    render(<MarketDataPanel />);
    expect(screen.getByText(/Market Data Used for Calculation/i)).toBeInTheDocument();
    expect(screen.getByText(/Discount Curves/i)).toBeInTheDocument();
    expect(screen.getByText(/Credit\/Default Curves/i)).toBeInTheDocument();
  });

  it('switches to raw files view', () => {
    render(<MarketDataPanel />);
    const rawBtn = screen.getByRole('button', { name: /Raw Files/i });
    fireEvent.click(rawBtn);
    expect(screen.getByText(/market.txt/i)).toBeInTheDocument();
    expect(screen.getByText(/curveconfig.xml/i)).toBeInTheDocument();
  });
});
