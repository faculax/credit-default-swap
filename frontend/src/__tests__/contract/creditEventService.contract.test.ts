import { creditEventService } from '../../services/creditEventService';

describe('creditEventService contract', () => {
  beforeEach(() => { (globalThis as any).fetch = jest.fn(); });
  afterEach(() => jest.resetAllMocks());

  it('recordCreditEvent posts payload and returns response', async () => {
    const mockResp = { creditEvent: { id: 'e1', tradeId: 1, eventType: 'BANKRUPTCY' }, affectedTradeIds: [1] } as any;
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: true, json: () => Promise.resolve(mockResp) });
    const req: any = { eventType: 'BANKRUPTCY', eventDate: '2025-11-12', noticeDate: '2025-11-13', settlementMethod: 'CASH' };
    const res = await creditEventService.recordCreditEvent(1, req);
    expect(res.creditEvent.id).toBe('e1');
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/cds-trades\/1\/credit-events$/);
    expect(opts.method).toBe('POST');
  });

  it('getCreditEventsForTrade returns empty array on 404', async () => {
  (globalThis.fetch as jest.Mock).mockResolvedValue({ ok: false, status: 404 });
    const events = await creditEventService.getCreditEventsForTrade(99);
    expect(events).toEqual([]);
  });
});
import { creditEventService } from '../../services/creditEventService';

describe('Contract: creditEventService', () => {
  const originalFetch = globalThis.fetch;
  afterEach(() => { globalThis.fetch = originalFetch; });

  it('recordCreditEvent posts and returns response', async () => {
    const mockResponse = { creditEvent: { id: 'evt1', tradeId: 55, eventType: 'RESTRUCTURING', eventDate: '2025-01-01', noticeDate: '2025-01-01', settlementMethod: 'PHYSICAL', comments: '', createdAt: '2025-01-02' }, affectedTradeIds: [55] };
  globalThis.fetch = jest.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve(mockResponse) });
    const req = { eventType: 'RESTRUCTURING', eventDate: '2025-01-01', noticeDate: '2025-01-01', settlementMethod: 'PHYSICAL', comments: '' };
    const result = await creditEventService.recordCreditEvent(55, req as any);
    expect(result).toEqual(mockResponse);
  const [url, opts] = (globalThis.fetch as jest.Mock).mock.calls[0];
    expect(url).toMatch(/credit-events$/);
    expect(opts.method).toBe('POST');
  });
});
