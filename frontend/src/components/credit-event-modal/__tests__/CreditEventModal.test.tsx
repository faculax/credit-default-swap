import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import CreditEventModal from '../CreditEventModal';

describe('CreditEventModal', () => {
  const onSubmit = jest.fn().mockResolvedValue(undefined);
  const onClose = jest.fn();

  const openModal = () => {
    render(<CreditEventModal isOpen tradeId={5} onClose={onClose} onSubmit={onSubmit} referenceEntity="ACME" />);
  };

  it('renders form fields', () => {
    openModal();
    expect(screen.getByLabelText(/Event Type/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Event Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Notice Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Settlement Method/i)).toBeInTheDocument();
  });

  it('validates required fields and prevents future event date', async () => {
    openModal();
    // Set invalid future date for eventDate
    const eventDate = screen.getByLabelText(/Event Date/i);
    fireEvent.change(eventDate, { target: { value: '2999-01-01' } });
    fireEvent.click(screen.getByRole('button', { name: /Record Credit Event/i }));
    expect(await screen.findByText(/cannot be in the future/i)).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits valid form', async () => {
    openModal();
    fireEvent.click(screen.getByRole('button', { name: /Record Credit Event/i }));
    // Should submit with defaults (all valid)
    expect(onSubmit).toHaveBeenCalled();
  });
});
