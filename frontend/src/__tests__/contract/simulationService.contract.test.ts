import { simulationService } from '../../services/simulationService';

describe('simulationService contract', () => {
  beforeEach(() => {
    (globalThis as any).fetch = jest.fn();
  });
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('POSTs runSimulation with expected body shape', async () => {
    const mockResponse = {
      runId: 'r1', portfolioId: 7, valuationDate: '2025-11-13', paths: 1000, status: 'QUEUED'
    };
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve(mockResponse) });
    const req = { valuationDate: '2025-11-13', horizons: ['1M'], paths: 1000, includePerPath: false };
    const result = await simulationService.runSimulation(7, req);
    expect(result.runId).toBe('r1');
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/credit-simulation\/portfolio\/7$/);
    expect(opts.method).toBe('POST');
    const body = JSON.parse(opts.body);
    expect(body.paths).toBe(1000);
    expect(body.horizons).toEqual(['1M']);
  });

  it('throws descriptive error on non-ok response', async () => {
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: false, text: () => Promise.resolve('Bad') });
    await expect(simulationService.runSimulation(1, { valuationDate: 'x', horizons: [], paths: 1 })).rejects.toThrow(/Failed to start simulation/);
  });
});
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
