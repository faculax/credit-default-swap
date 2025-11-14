import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import SimulationConfigForm from '../SimulationConfigForm';

describe('SimulationConfigForm', () => {
  const setup = (isSubmitting = false) => {
    const onSubmit = jest.fn();
    render(<SimulationConfigForm onSubmit={onSubmit} isSubmitting={isSubmitting} />);
    return { onSubmit };
  };

  it('renders initial form fields', () => {
    setup();
    expect(screen.getByLabelText(/Valuation Date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Monte Carlo Paths/i)).toBeInTheDocument();
    expect(screen.getByText('Horizons (select one or more)')).toBeInTheDocument();
  // Label text includes extra symbols (β) and current value; use partial regex match
  expect(screen.getByText(/Default Systemic Loading/i)).toBeInTheDocument();
  });

  it('toggles horizons selection', () => {
    setup();
    const horizonBtn = screen.getByRole('button', { name: '1Y' });
    fireEvent.click(horizonBtn); // select 1Y
    fireEvent.click(horizonBtn); // deselect 1Y
    // Just ensure button remains interactable without errors
    expect(horizonBtn).toBeEnabled();
  });

  it('disables submit if no horizons selected', () => {
    const { onSubmit } = setup();
    // Deselect default horizons (3Y, 5Y)
    fireEvent.click(screen.getByRole('button', { name: '3Y' }));
    fireEvent.click(screen.getByRole('button', { name: '5Y' }));
    const submitBtn = screen.getByRole('button', { name: /Run Simulation/i });
    expect(submitBtn).toBeDisabled();
    fireEvent.click(submitBtn);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits with sorted horizons and parsed seed', () => {
    const { onSubmit } = setup();
    // Add another horizon to test sorting
    fireEvent.click(screen.getByRole('button', { name: '1Y' }));
    const seedInput = screen.getByLabelText(/Random Seed/i);
    fireEvent.change(seedInput, { target: { value: '123' } });
    const submitBtn = screen.getByRole('button', { name: /Run Simulation/i });
    fireEvent.click(submitBtn);
    expect(onSubmit).toHaveBeenCalledTimes(1);
    const arg = onSubmit.mock.calls[0][0];
    expect(arg.horizons).toEqual(['1Y', '3Y', '5Y']);
    expect(arg.seed).toBe(123);
  });
});
