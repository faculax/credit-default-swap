import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SimulationPanel from '../../components/portfolio/simulation/SimulationPanel';

jest.mock('../../services/simulationService', () => ({
  simulationService: {
    runSimulation: jest.fn().mockResolvedValue({ runId: 'flow1' }),
    cancelSimulation: jest.fn(),
    downloadResults: jest.fn(),
    getSimulationResults: jest.fn().mockResolvedValue({ runId: 'flow1', status: 'COMPLETE', portfolioId: 1, paths: 1000, valuationDate: '2025-01-01' })
  }
}));
jest.mock('../../hooks/useSimulationPolling', () => ({
  useSimulationPolling: (runId: string | null) => ({ simulation: runId ? { runId, status: 'COMPLETE', portfolioId: 1, paths: 1000, valuationDate: '2025-01-01' } : null, loading: false, error: null })
}));

describe('Integration: Simulation flow', () => {
  it('submits form and displays results', async () => {
    render(<SimulationPanel portfolioId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /Run Simulation/i }));
    await waitFor(() => expect(screen.getByText(/Simulation Status/i)).toBeInTheDocument());
  });
});
