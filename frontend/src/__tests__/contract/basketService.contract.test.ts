import { basketService } from '../../services/basketService';

describe('basketService contract', () => {
  beforeEach(() => { (globalThis as any).fetch = jest.fn(); });
  afterEach(() => jest.resetAllMocks());

  it('createBasket posts payload', async () => {
    const mockBasket = { id: 7, name: 'Test Basket', constituents: [] } as any;
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve(mockBasket) });
    const res = await basketService.createBasket({ name: 'Test Basket', constituents: [], description: 'x' } as any);
    expect(res.id).toBe(7);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/\/baskets$/);
    expect(opts.method).toBe('POST');
  });

  it('priceBasket builds query params', async () => {
    const mockPricing = { basketId: 3, npv: 1000 } as any;
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve(mockPricing) });
    const res = await basketService.priceBasket(3, { valuationDate: '2025-11-13', paths: 500, seed: 42, includeSensitivities: true } as any);
    expect(res.basketId).toBe(3);
  const [url] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/valuationDate=2025-11-13/);
    expect(url).toMatch(/paths=500/);
    expect(url).toMatch(/seed=42/);
    expect(url).toMatch(/includeSensitivities=true/);
  });
});
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
