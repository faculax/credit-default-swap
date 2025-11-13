import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { useSimulationPolling } from '../useSimulationPolling';

// Mock simulationService with deterministic sequence via setter
jest.mock('../../services/simulationService', () => {
  let mockSequence: any[] = [];
  return {
    simulationService: {
      getSimulationResults: jest.fn(() => {
        const next = mockSequence.shift();
        if (next instanceof Error) return Promise.reject(next);
        return Promise.resolve(next);
      })
    },
    __setMockSequence: (seq: any[]) => { mockSequence = seq; }
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
    // Prepare sequence: RUNNING then COMPLETE
  const { __setMockSequence } = require('../../services/simulationService');
  __setMockSequence([ { runId: 'r1', status: 'RUNNING' }, { runId: 'r1', status: 'COMPLETE' } ]);
    render(<Harness runId={'r1'} />);
    // Initial async fetch microtask flush
    await Promise.resolve();
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('RUNNING'));
    // Advance timers to trigger interval callback
    jest.advanceTimersByTime(2000);
    await Promise.resolve();
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('COMPLETE'));
  });

  it('handles fetch error and stops polling', async () => {
  const { __setMockSequence } = require('../../services/simulationService');
  __setMockSequence([ new Error('network fail') ]);
    render(<Harness runId={'err1'} />);
    await Promise.resolve();
    await waitFor(() => expect(screen.getByTestId('error').textContent).toMatch(/network fail/));
  });
});
