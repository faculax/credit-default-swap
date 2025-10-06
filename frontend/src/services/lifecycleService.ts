import { CouponPeriod, AccrualEvent, TradeAmendment, NotionalAdjustment, LifecycleSummary, AmendTradePayload, NotionalAdjustmentPayload } from '../types/lifecycle';
import { apiUrl } from '../config/api';

// Gateway-routed lifecycle base
const API_BASE = apiUrl('/lifecycle');

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text();
    
    // Try to parse JSON error response for better error messages
    try {
      const errorJson = JSON.parse(text);
      // If there's a detail field (Spring Boot error format), use that
      if (errorJson.detail) {
        throw new Error(errorJson.detail);
      }
      // Otherwise use the title or fallback to raw text
      throw new Error(errorJson.title || errorJson.message || text);
    } catch (parseError) {
      // If not JSON or parsing fails, use the raw text
      throw new Error(`Lifecycle API error ${response.status}: ${text}`);
    }
  }
  return response.json();
}

export const lifecycleService = {
  // Coupon Schedule
  async generateCouponSchedule(tradeId: number): Promise<CouponPeriod[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/coupon-schedule`, { method: 'POST' });
    return handleResponse<CouponPeriod[]>(res);
  },
  async getCouponSchedule(tradeId: number): Promise<CouponPeriod[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/coupon-schedule`);
    return handleResponse<CouponPeriod[]>(res);
  },
  async getCouponScheduleInRange(tradeId: number, startDate: string, endDate: string): Promise<CouponPeriod[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const res = await fetch(`${API_BASE}/trades/${tradeId}/coupon-schedule/range?${params.toString()}`);
    return handleResponse<CouponPeriod[]>(res);
  },
  async payCoupon(tradeId: number, periodId: number, payOnTime?: boolean): Promise<CouponPeriod> {
    const requestBody = payOnTime !== undefined ? { payOnTime } : undefined;
    const res = await fetch(`${API_BASE}/trades/${tradeId}/coupon-periods/${periodId}/pay`, { 
      method: 'POST',
      headers: requestBody ? { 'Content-Type': 'application/json' } : {},
      body: requestBody ? JSON.stringify(requestBody) : undefined
    });
    return handleResponse<CouponPeriod>(res);
  },
  async unpayCoupon(tradeId: number, periodId: number): Promise<CouponPeriod> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/coupon-periods/${periodId}/unpay`, { 
      method: 'POST'
    });
    return handleResponse<CouponPeriod>(res);
  },

  // Accruals
  async postDailyAccrual(tradeId: number, accrualDate: string): Promise<AccrualEvent> {
    const params = new URLSearchParams({ accrualDate });
    const res = await fetch(`${API_BASE}/trades/${tradeId}/accruals/daily?${params.toString()}`, { method: 'POST' });
    return handleResponse<AccrualEvent>(res);
  },
  async postAccrualsForPeriod(tradeId: number, startDate: string, endDate: string): Promise<AccrualEvent[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const res = await fetch(`${API_BASE}/trades/${tradeId}/accruals/period?${params.toString()}`, { method: 'POST' });
    return handleResponse<AccrualEvent[]>(res);
  },
  async getAccrualEvents(tradeId: number, startDate: string, endDate: string): Promise<AccrualEvent[]> {
    const params = new URLSearchParams({ startDate, endDate });
    const res = await fetch(`${API_BASE}/trades/${tradeId}/accruals?${params.toString()}`);
    return handleResponse<AccrualEvent[]>(res);
  },
  async getCumulativeAccrual(tradeId: number): Promise<number> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/accruals/cumulative`);
    const json = await handleResponse<{ cumulativeAccrual: number }>(res);
    return json.cumulativeAccrual;
  },
  async getNetCashForPayment(tradeId: number, paymentDate: string): Promise<number> {
    const params = new URLSearchParams({ paymentDate });
    const res = await fetch(`${API_BASE}/trades/${tradeId}/accruals/net-cash?${params.toString()}`);
    const json = await handleResponse<{ netCashAmount: number }>(res);
    return json.netCashAmount;
  },

  // Amendments
  async amendTrade(tradeId: number, payload: AmendTradePayload): Promise<TradeAmendment[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/amendments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return handleResponse<TradeAmendment[]>(res);
  },
  async getTradeAmendments(tradeId: number): Promise<TradeAmendment[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/amendments`);
    return handleResponse<TradeAmendment[]>(res);
  },
  async getAmendmentsForVersion(tradeId: number, version: number): Promise<TradeAmendment[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/amendments/version/${version}`);
    return handleResponse<TradeAmendment[]>(res);
  },

  // Notional Adjustments
  async adjustNotional(tradeId: number, payload: NotionalAdjustmentPayload): Promise<NotionalAdjustment> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/notional-adjustments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    return handleResponse<NotionalAdjustment>(res);
  },
  async getNotionalAdjustments(tradeId: number): Promise<NotionalAdjustment[]> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/notional-adjustments`);
    return handleResponse<NotionalAdjustment[]>(res);
  },
  async partiallyTerminate(tradeId: number, terminationDate: string, terminationAmount: number, reason?: string): Promise<NotionalAdjustment> {
    const params = new URLSearchParams({ terminationDate, terminationAmount: terminationAmount.toString() });
    if (reason) params.append('reason', reason);
    const res = await fetch(`${API_BASE}/trades/${tradeId}/partial-termination?${params.toString()}`, { method: 'POST' });
    return handleResponse<NotionalAdjustment>(res);
  },
  async fullyTerminate(tradeId: number, terminationDate: string, reason?: string): Promise<NotionalAdjustment> {
    const params = new URLSearchParams({ terminationDate });
    if (reason) params.append('reason', reason);
    const res = await fetch(`${API_BASE}/trades/${tradeId}/full-termination?${params.toString()}`, { method: 'POST' });
    return handleResponse<NotionalAdjustment>(res);
  },

  // Summary
  async getLifecycleSummary(tradeId: number): Promise<LifecycleSummary> {
    const res = await fetch(`${API_BASE}/trades/${tradeId}/summary`);
    return handleResponse<LifecycleSummary>(res);
  }
};
