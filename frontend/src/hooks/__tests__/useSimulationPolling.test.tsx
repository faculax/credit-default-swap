import React from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { useSimulationPolling } from '../useSimulationPolling';

// Deterministic mock: first call RUNNING, subsequent calls COMPLETE unless mockThrowError set
let mockThrowError = false; // prefixed with 'mock' to allow safe jest.mock factory reference
let mockCallCount = 0; // renamed for jest.mock factory safety
jest.mock('../../services/simulationService', () => ({
  simulationService: {
    getSimulationResults: jest.fn(() => {
      mockCallCount += 1;
      if (mockThrowError) return Promise.reject(new Error('network fail'));
      if (mockCallCount === 1) return Promise.resolve({ runId: 'r1', status: 'RUNNING' });
      return Promise.resolve({ runId: 'r1', status: 'COMPLETE' });
    })
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
    mockThrowError = false;
  mockCallCount = 0;
    render(<Harness runId={'r1'} />);
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('RUNNING'));
    await act(async () => { jest.advanceTimersByTime(2100); });
    await waitFor(() => expect(screen.getByTestId('status').textContent).toBe('COMPLETE'));
  });

  it('handles fetch error and stops polling', async () => {
    mockThrowError = true;
  mockCallCount = 0;
    render(<Harness runId={'err1'} />);
    await waitFor(() => expect(screen.getByTestId('error').textContent).toMatch(/network fail/));
  });
});
