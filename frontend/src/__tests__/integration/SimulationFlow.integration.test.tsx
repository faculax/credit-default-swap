import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SimulationPanel from '../../components/portfolio/simulation/SimulationPanel';
import { simulationService } from '../../services/simulationService';

jest.mock('../../hooks/useSimulationPolling', () => ({
  useSimulationPolling: (runId: string | null) => ({ simulation: runId ? { runId, status: 'COMPLETE', portfolioId: 1, paths: 1000, valuationDate: '2025-01-01' } : null, loading: false, error: null })
}));

describe('Integration: Simulation flow', () => {
  it('submits form and displays results', async () => {
    jest.spyOn(simulationService, 'runSimulation').mockResolvedValue({ runId: 'flow1' } as any);
    render(<SimulationPanel portfolioId={1} />);
    fireEvent.click(screen.getByRole('button', { name: /Run Simulation/i }));
    await waitFor(() => expect(screen.getByText(/Simulation Status/i)).toBeInTheDocument());
  });
});
