import { lifecycleService } from '../../services/lifecycleService';

describe('lifecycleService contract', () => {
  beforeEach(() => { (globalThis as any).fetch = jest.fn(); });
  afterEach(() => jest.resetAllMocks());

  it('generateCouponSchedule posts to correct endpoint', async () => {
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve([{ periodStartDate: '2025-01-01' }]) });
    const res = await lifecycleService.generateCouponSchedule(77);
    expect(res.length).toBe(1);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/lifecycle\/trades\/77\/coupon-schedule$/);
    expect(opts.method).toBe('POST');
  });

  it('payCoupon sends optional body correctly', async () => {
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve({ id: 1, paid: true }) });
    const res = await lifecycleService.payCoupon(10, 5, true);
    expect(res.paid).toBe(true);
  const [, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(JSON.parse(opts.body).payOnTime).toBe(true);
  });
});
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
