import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import PortfolioList from '../PortfolioList';

jest.mock('../../services/portfolioService', () => {
  return {
    portfolioService: {
      getAllPortfolios: jest.fn().mockResolvedValue([
        { id: 1, name: 'Test Portfolio', description: 'Desc', constituents: [], createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
      ])
    }
  };
});

describe('PortfolioList', () => {
  it('loads and displays portfolios', async () => {
    render(<PortfolioList />);
    await waitFor(() => expect(screen.getByText(/CDS Portfolios/i)).toBeInTheDocument());
    expect(screen.getByText(/Test Portfolio/i)).toBeInTheDocument();
  });

  it('supports creating portfolio modal toggle', async () => {
    render(<PortfolioList />);
    await screen.findByText(/CDS Portfolios/i);
    fireEvent.click(screen.getByRole('button', { name: /Create Portfolio/i }));
    // Modal renders a form title maybe
    expect(screen.getByText(/Create Portfolio/i)).toBeInTheDocument();
  });
});
