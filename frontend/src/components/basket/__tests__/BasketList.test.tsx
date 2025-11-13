import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import BasketList from '../BasketList';

jest.mock('../../../services/basketService', () => ({
  basketService: {
    getAllBaskets: jest.fn()
  }
}));
import { basketService } from '../../../services/basketService';

describe('BasketList', () => {
  it('renders loading state', () => {
    (basketService.getAllBaskets as jest.Mock).mockResolvedValue([]);
    render(<BasketList />);
    expect(screen.getByText(/Loading baskets/i)).toBeInTheDocument();
  });

  it('renders empty state', async () => {
    (basketService.getAllBaskets as jest.Mock).mockResolvedValue([]);
    render(<BasketList />);
    await waitFor(() => expect(screen.getByText(/No baskets found/i)).toBeInTheDocument());
  });

  it('renders list and handles select', async () => {
    const baskets = [{ id: 1, name: 'Test Basket', type: 'FIRST_TO_DEFAULT', currency: 'USD', notional: 1000000, maturityDate: '2030-01-01', constituentCount: 3 }];
    const onSelectBasket = jest.fn();
    (basketService.getAllBaskets as jest.Mock).mockResolvedValue(baskets);
    render(<BasketList onSelectBasket={onSelectBasket} />);
    await waitFor(() => expect(screen.getByText('Test Basket')).toBeInTheDocument());
    fireEvent.click(screen.getByText(/View Details/i));
    expect(onSelectBasket).toHaveBeenCalledWith(baskets[0]);
  });
});
