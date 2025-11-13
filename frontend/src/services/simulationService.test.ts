import { simulationService } from './simulationService';

const API_BASE_URL = 'http://localhost/api';
jest.mock('../config/api', () => ({ API_BASE_URL }));

describe('simulationService', () => {
  afterEach(() => {
    (globalThis.fetch as jest.Mock | undefined)?.mockClear?.();
  });

  it('runSimulation success', async () => {
    globalThis.fetch = jest.fn(async () => ({ ok: true, json: async () => ({ runId: 'r1', status: 'QUEUED' }) })) as any;
    const res = await simulationService.runSimulation(5, { valuationDate: '2024-01-01', horizons: [], paths: 100 });
    expect(res.runId).toBe('r1');
  });

  it('runSimulation failure', async () => {
    globalThis.fetch = jest.fn(async () => ({ ok: false, text: async () => 'boom' })) as any;
    await expect(
      simulationService.runSimulation(5, { valuationDate: '2024-01-01', horizons: [], paths: 100 })
    ).rejects.toThrow(/Failed to start simulation/);
  });

  it('getSimulationResults 404', async () => {
    globalThis.fetch = jest.fn(async () => ({ ok: false, status: 404 })) as any;
    await expect(simulationService.getSimulationResults('xyz')).rejects.toThrow(/Simulation not found/);
  });

  it('cancelSimulation ignores 404', async () => {
    globalThis.fetch = jest.fn(async () => ({ ok: false, status: 404 })) as any;
    await simulationService.cancelSimulation('abc'); // should not throw
  });
});
