import { fetchRiskMeasures, runRiskScenarios } from '../../services/risk/riskService';

describe('Contract: riskService', () => {
  const originalFetch = globalThis.fetch;
  afterEach(() => { globalThis.fetch = originalFetch; });

  it('fetchRiskMeasures posts scenario calculate and returns first element', async () => {
    const mockMeasures = [{ npv: 123, currency: 'USD', valuationTimestamp: Date.now() }];
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockMeasures) });
    const result = await fetchRiskMeasures(10, '2025-01-01');
    expect(result).toEqual(mockMeasures[0]);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/risk\/scenario\/calculate/);
    expect(opts.method).toBe('POST');
  });

  it('runRiskScenarios returns aggregated scenarios', async () => {
    const base = [{ npv: 200, currency: 'USD', valuationTimestamp: Date.now() }];
    const shifted = [{ npv: 210, currency: 'USD', valuationTimestamp: Date.now() }];
  globalThis.fetch = jest.fn()
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(base) })
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(shifted) });
    const result = await runRiskScenarios(5, [25]);
    expect(result.base).toEqual(base[0]);
    expect(result.scenarios[0].measures).toEqual(shifted[0]);
  });
});
