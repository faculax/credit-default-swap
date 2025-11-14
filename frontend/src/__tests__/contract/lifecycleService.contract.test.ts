import { lifecycleService } from '../../services/lifecycleService';

describe('Contract: lifecycleService', () => {
  const originalFetch = globalThis.fetch;
  afterEach(() => { globalThis.fetch = originalFetch; });

  it('payCoupon posts to pay endpoint with optional body', async () => {
    const mockPeriod = { id: 1, paymentDate: '2025-01-01' };
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockPeriod) });
    const result = await lifecycleService.payCoupon(33, 1, true);
    expect(result).toEqual(mockPeriod);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/coupon-periods\/1\/pay/);
    expect(opts.method).toBe('POST');
    expect(JSON.parse(opts.body)).toEqual({ payOnTime: true });
  });

  it('unpayCoupon posts without body', async () => {
    const mockPeriod = { id: 2, paymentDate: '2025-02-01' };
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockPeriod) });
    const result = await lifecycleService.unpayCoupon(33, 2);
    expect(result).toEqual(mockPeriod);
  const [, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(opts.body).toBeUndefined();
  });
});
