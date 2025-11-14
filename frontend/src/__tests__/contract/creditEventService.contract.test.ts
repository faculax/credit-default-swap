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
