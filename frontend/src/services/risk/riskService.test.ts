import { fetchRiskMeasures, runRiskScenarios } from './riskService';

const API_BASE = 'http://localhost/api';
jest.mock('../../config/api', () => ({ API_BASE_URL: API_BASE }));

describe('riskService', () => {
  afterEach(() => {
    (globalThis.fetch as jest.Mock | undefined)?.mockClear?.();
  });

  it('fetchRiskMeasures returns first element of array', async () => {
  globalThis.fetch = jest.fn(async () => ({
      ok: true,
      json: async () => ([{ dv01: 1 }])
    })) as any;
    const res = await fetchRiskMeasures(123);
    expect(res).toEqual({ dv01: 1 });
  });

  it('fetchRiskMeasures throws when empty array', async () => {
  globalThis.fetch = jest.fn(async () => ({
      ok: true,
      json: async () => ([])
    })) as any;
    await expect(fetchRiskMeasures(123)).rejects.toThrow(/No risk measures/);
  });

  it('runRiskScenarios builds scenarios skipping failed fetches', async () => {
    let call = 0;
  globalThis.fetch = jest.fn(async () => {
      call += 1;
      if (call === 1) {
        return { ok: true, json: async () => ([{ base: true }]) } as any;
      }
      if (call === 2) {
        return { ok: false } as any; // simulate failed scenario
      }
      return { ok: true, json: async () => ([{ scenario: call }]) } as any;
    }) as any;
    const result = await runRiskScenarios(55, [10, 20, 30]);
    expect(result.base).toEqual({ base: true });
    expect(result.scenarios.length).toBeGreaterThan(0);
  });
});
