import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { useSimulationPolling } from '../useSimulationPolling';

// Mock simulationService
jest.mock('../../services/simulationService', () => {
  let callCount = 0;
  return {
    simulationService: {
      getSimulationResults: jest.fn(async () => {
        callCount += 1;
        if (callCount === 1) {
          return { runId: 'r1', status: 'RUNNING' };
        }
        return { runId: 'r1', status: 'COMPLETE' };
      })
    }
  };
});

function Harness({ runId }: { runId: string | null }) {
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
    render(<Harness runId={'r1'} />);
    // Allow initial fetch and state update
    await act(async () => {
      jest.advanceTimersByTime(50);
    });
    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('RUNNING');
    });
    // Advance enough time for second poll (>=2000ms)
    await act(async () => {
      jest.advanceTimersByTime(2100);
    });
    await waitFor(() => {
      expect(screen.getByTestId('status').textContent).toBe('COMPLETE');
    });
  });
});
