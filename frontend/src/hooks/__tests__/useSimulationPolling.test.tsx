import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { useSimulationPolling } from '../useSimulationPolling';
import { simulationService } from '../../services/simulationService';

// Stub module and control implementations per-test to avoid hoisting issues
jest.mock('../../services/simulationService', () => ({
  simulationService: {
    getSimulationResults: jest.fn()
  }
}));

function Harness({ runId }: Readonly<{ runId: string | null }>) {
  const { simulation, loading, error } = useSimulationPolling(runId, true);
  return (
    <div>
      <div data-testid="loading">{loading ? 'loading' : 'idle'}</div>
      <div data-testid="status">{simulation?.status || 'none'}</div>
      <div data-testid="error">{error || 'none'}</div>
    </div>
  );
}

describe('useSimulationPolling', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });
  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('returns initial state when runId is null', () => {
    render(<Harness runId={null} />);
    expect(screen.getByTestId('status').textContent).toBe('none');
  });

  it('polls until COMPLETE status', async () => {
    let callCount = 0;
    (simulationService.getSimulationResults as jest.Mock).mockImplementation(() => {
      callCount += 1;
      if (callCount === 1) return Promise.resolve({ runId: 'r1', status: 'RUNNING' });
      return Promise.resolve({ runId: 'r1', status: 'COMPLETE' });
    });
    render(<Harness runId={'r1'} />);
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('RUNNING'));
    await act(async () => { jest.advanceTimersByTime(2100); });
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('COMPLETE'));
  });

  it('handles fetch error and stops polling', async () => {
    (simulationService.getSimulationResults as jest.Mock).mockRejectedValue(new Error('network fail'));
    render(<Harness runId={'err1'} />);
    await waitFor(() => expect(screen.getByTestId('error').textContent).toMatch(/network fail/));
  });
});
