import { fetchRiskMeasures, runRiskScenarios } from '../../services/risk/riskService';

describe('riskService contract', () => {
  beforeEach(() => {
    (globalThis as any).fetch = jest.fn();
  });
  afterEach(() => jest.resetAllMocks());

  it('fetchRiskMeasures posts scenario request and returns first element', async () => {
    const rm = { tradeId: 5, currency: 'USD', npv: 123.45 };
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve([rm]) });
    const result = await fetchRiskMeasures(5, '2025-11-13');
    expect(result.tradeId).toBe(5);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/risk\/scenario\/calculate$/);
    expect(opts.method).toBe('POST');
    const body = JSON.parse(opts.body);
    expect(body.tradeIds).toEqual([5]);
  });

  it('runRiskScenarios loops shifts and aggregates scenarios', async () => {
    const rm = { tradeId: 9, currency: 'USD', npv: 100 };
    // First base result + two scenario calls
  (globalThis.fetch as jest.Mock)
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([rm]) })
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([rm]) })
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([rm]) });
    const result = await runRiskScenarios(9, [10, -5]);
    expect(result.base.tradeId).toBe(9);
    expect(result.scenarios.length).toBe(2);
    expect(result.scenarios[0].scenario).toMatch(/PARALLEL_10BP/);
  });
});
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
