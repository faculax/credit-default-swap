import { apiUrl } from '../config/api';
import {
  AccountingEvent,
  AccountingSummary,
  GenerateEventsResponse,
  PostingConfirmation,
  MarkPostedResponse,
  BulkPostingConfirmation,
  BulkMarkPostedResponse,
  HealthCheckResponse,
  EventStatus,
} from '../types/accounting';

class AccountingService {
  /**
   * Generate accounting events from daily P&L for a specific date
   */
  async generateEvents(date: string, jobId?: string): Promise<GenerateEventsResponse> {
    const url = jobId 
      ? apiUrl(`/accounting/events/generate?date=${date}&jobId=${jobId}`)
      : apiUrl(`/accounting/events/generate?date=${date}`);
    
    const response = await fetch(url, { method: 'POST' });
    
    if (!response.ok) {
      throw new Error(`Failed to generate accounting events: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Get all accounting events for a specific date
   */
  async getEventsByDate(date: string): Promise<AccountingEvent[]> {
    const response = await fetch(apiUrl(`/accounting/events/${date}`));
    
    if (!response.ok) {
      throw new Error(`Failed to fetch accounting events: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Get pending (unposted) accounting events for a specific date
   */
  async getPendingEvents(date: string): Promise<AccountingEvent[]> {
    const response = await fetch(apiUrl(`/accounting/events/${date}/pending`));
    
    if (!response.ok) {
      throw new Error(`Failed to fetch pending events: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Get accounting events within a date range with optional status filter
   */
  async getEventsByDateRange(
    startDate: string,
    endDate: string,
    status?: EventStatus
  ): Promise<AccountingEvent[]> {
    let url = apiUrl(`/accounting/events?startDate=${startDate}&endDate=${endDate}`);
    
    if (status) {
      url += `&status=${status}`;
    }
    
    const response = await fetch(url);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch accounting events: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Get accounting summary for a specific date
   */
  async getSummary(date: string): Promise<AccountingSummary> {
    const response = await fetch(apiUrl(`/accounting/summary/${date}`));
    
    if (!response.ok) {
      throw new Error(`Failed to fetch accounting summary: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Mark a single accounting event as posted to GL
   */
  async markEventAsPosted(
    eventId: number,
    confirmation: PostingConfirmation
  ): Promise<MarkPostedResponse> {
    const response = await fetch(
      apiUrl(`/accounting/events/${eventId}/mark-posted`),
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(confirmation),
      }
    );
    
    if (!response.ok) {
      throw new Error(`Failed to mark event as posted: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Mark multiple accounting events as posted to GL in a batch
   */
  async markEventsAsPostedBatch(
    confirmation: BulkPostingConfirmation
  ): Promise<BulkMarkPostedResponse> {
    const response = await fetch(
      apiUrl('/accounting/events/mark-posted-batch'),
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(confirmation),
      }
    );
    
    if (!response.ok) {
      throw new Error(`Failed to mark events as posted: ${response.statusText}`);
    }
    
    return response.json();
  }

  /**
   * Health check for accounting service
   */
  async healthCheck(): Promise<HealthCheckResponse> {
    const response = await fetch(apiUrl('/accounting/health'));
    
    if (!response.ok) {
      throw new Error(`Accounting service health check failed: ${response.statusText}`);
    }
    
    return response.json();
  }
}

const accountingService = new AccountingService();
export default accountingService;
