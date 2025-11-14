import { simulationService } from '../../services/simulationService';

describe('Contract: simulationService', () => {
  const originalFetch = globalThis.fetch;
  afterEach(() => { globalThis.fetch = originalFetch; });

  it('POST /credit-simulation/portfolio/{id} sends request body and parses response', async () => {
    const mockResponse = { runId: 'r123', portfolioId: 77, valuationDate: '2025-01-01', paths: 1000, status: 'QUEUED' };
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockResponse) });
    const req = { valuationDate: '2025-01-01', horizons: ['1Y'], paths: 1000 };
    const result = await simulationService.runSimulation(77, req as any);
  expect(globalThis.fetch).toHaveBeenCalledWith(expect.stringMatching(/credit-simulation\/portfolio\/77/), expect.objectContaining({ method: 'POST' }));
  const fetchArgs = (globalThis.fetch as jest.Mock).mock.calls[0][1];
    expect(JSON.parse(fetchArgs.body)).toEqual(req);
    expect(result).toEqual(mockResponse);
  });

  it('GET /credit-simulation/runs/{id} handles 404', async () => {
    globalThis.fetch = jest.fn().mockResolvedValue({ ok: false, status: 404, text: () => Promise.resolve('Not found') });
    await expect(simulationService.getSimulationResults('missing')).rejects.toThrow(/Simulation not found/);
  });
});
