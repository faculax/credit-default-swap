import React from 'react';
import { render, screen } from '@testing-library/react';
import SimulationResults from '../SimulationResults';
import { SimulationResponse } from '../../../../services/simulationService';

const baseSimulation: SimulationResponse = {
  runId: 'r1',
  status: 'COMPLETE',
  valuationDate: '2025-01-05',
  paths: 20000,
  portfolioId: 99,
  horizons: [
    {
      tenor: '5Y',
      loss: { mean: 100000, var95: 120000, var99: 150000, es97_5: 180000 },
      diversification: { benefitPct: 12.34 },
      panyDefault: 0.42,
      pAnyDefault: 0.42
    }
  ] as any,
  seedUsed: 111
};

describe('SimulationResults', () => {
  it('renders metrics and horizon cards when COMPLETE', () => {
    render(<SimulationResults simulation={baseSimulation} onCancel={jest.fn()} onDownload={jest.fn()} onReset={jest.fn()} />);
    expect(screen.getByText(/Simulation Status/i)).toBeInTheDocument();
    expect(screen.getByText(/Run ID: r1/)).toBeInTheDocument();
    expect(screen.getByText(/Probability of Any Default/)).toBeInTheDocument();
    expect(screen.getByText(/Expected Loss/)).toBeInTheDocument();
    expect(screen.getByText(/VaR 95%/)).toBeInTheDocument();
    expect(screen.getByText(/VaR 99%/)).toBeInTheDocument();
    expect(screen.getByText(/Expected Shortfall 97.5%/)).toBeInTheDocument();
    expect(screen.getByText(/Diversification Benefit/)).toBeInTheDocument();
  });

  it('shows loading state when RUNNING', () => {
  render(<SimulationResults simulation={{ ...baseSimulation, status: 'RUNNING', horizons: undefined } as any} onCancel={jest.fn()} onDownload={jest.fn()} onReset={jest.fn()} />);
    expect(screen.getByText(/Running Monte Carlo simulation/i)).toBeInTheDocument();
  });

  it('shows error state', () => {
  render(<SimulationResults simulation={{ ...baseSimulation, status: 'FAILED', errorMessage: 'Boom' }} onCancel={jest.fn()} onDownload={jest.fn()} onReset={jest.fn()} />);
    expect(screen.getByText(/Error:/)).toBeInTheDocument();
    expect(screen.getByText(/Boom/)).toBeInTheDocument();
  });
});
