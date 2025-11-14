import { basketService } from '../../services/basketService';

describe('Contract: basketService', () => {
  const originalFetch = globalThis.fetch;
  afterEach(() => { globalThis.fetch = originalFetch; });

  it('getAllBaskets performs GET and returns array', async () => {
    const mockBaskets = [{ id: 1, name: 'B1', type: 'FIRST_TO_DEFAULT', currency: 'USD', notional: 1000000, maturityDate: '2030-01-01', constituentCount: 0 }];
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockBaskets) });
    const result = await basketService.getAllBaskets();
    expect(result).toEqual(mockBaskets);
  const [url] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/baskets$/);
  });
});
