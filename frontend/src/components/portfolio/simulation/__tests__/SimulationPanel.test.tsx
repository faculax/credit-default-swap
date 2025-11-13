import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SimulationPanel from '../SimulationPanel';

jest.mock('../../../services/simulationService', () => ({
  simulationService: {
    runSimulation: jest.fn().mockResolvedValue({ runId: 'rX' }),
    cancelSimulation: jest.fn(),
    downloadResults: jest.fn(),
    getSimulationResults: jest.fn().mockResolvedValue({ runId: 'rX', status: 'COMPLETE', horizons: [], paths: 100, valuationDate: '2025-01-01' })
  }
}));

jest.mock('../../../hooks/useSimulationPolling', () => ({
  useSimulationPolling: (runId: string | null) => ({
    simulation: runId ? { runId, status: 'COMPLETE', horizons: [], paths: 100, valuationDate: '2025-01-01' } : null,
    loading: false,
    error: null
  })
}));

describe('SimulationPanel', () => {
  it('shows form initially and transitions to results after submit', async () => {
    render(<SimulationPanel portfolioId={7} />);
    expect(screen.getByText(/Monte Carlo Simulation/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Run Simulation/i }));
    await waitFor(() => expect(screen.getByText(/Simulation Status/i)).toBeInTheDocument());
  });
});
