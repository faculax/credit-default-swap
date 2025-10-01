import { CreateCreditEventRequest } from '../components/credit-event-modal/CreditEventModal';
import { SettlementView } from '../components/settlement-view/SettlementView';

export interface CreditEvent {
  id: string;
  tradeId: number;
  eventType: string;
  eventDate: string;
  noticeDate: string;
  settlementMethod: string;
  comments?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CashSettlement {
  id: string;
  creditEventId: string;
  tradeId: number;
  notional: number;
  recoveryRate: number;
  payoutAmount: number;
  calculatedAt: string;
}

export interface PhysicalSettlementInstruction {
  id: string;
  creditEventId: string;
  tradeId: number;
  referenceObligationIsin?: string;
  proposedDeliveryDate?: string;
  notes?: string;
  status: string;
  createdAt: string;
  updatedAt?: string;
}

class CreditEventService {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';
  }

  /**
   * Record a credit event for a trade
   */
  async recordCreditEvent(tradeId: number, request: CreateCreditEventRequest): Promise<CreditEvent> {
    const response = await fetch(`${this.baseUrl}/cds-trades/${tradeId}/credit-events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.detail || `Failed to record credit event: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Get cash settlement for a credit event
   */
  async getCashSettlement(tradeId: number, eventId: string): Promise<CashSettlement> {
    const response = await fetch(
      `${this.baseUrl}/cds-trades/${tradeId}/credit-events/${eventId}/cash-settlement`
    );

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Cash settlement not found');
      }
      throw new Error(`Failed to get cash settlement: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Get physical settlement instruction for a credit event
   */
  async getPhysicalSettlement(tradeId: number, eventId: string): Promise<PhysicalSettlementInstruction> {
    const response = await fetch(
      `${this.baseUrl}/cds-trades/${tradeId}/credit-events/${eventId}/physical-instruction`
    );

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Physical settlement instruction not found');
      }
      throw new Error(`Failed to get physical settlement: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Get all credit events for a trade
   */
  async getCreditEventsForTrade(tradeId: number): Promise<CreditEvent[]> {
    const response = await fetch(`${this.baseUrl}/cds-trades/${tradeId}/credit-events`);

    if (!response.ok) {
      if (response.status === 404) {
        return []; // No credit events found
      }
      throw new Error(`Failed to get credit events: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Generate demo credit events for a trade (for testing/demo purposes)
   */
  async generateDemoCreditEvents(tradeId: number): Promise<CreditEvent[]> {
    const response = await fetch(`${this.baseUrl}/cds-trades/${tradeId}/demo-credit-events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Trade not found');
      }
      if (response.status === 400) {
        throw new Error('Cannot generate demo events for this trade (must be ACTIVE status)');
      }
      throw new Error(`Failed to generate demo credit events: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Get unified settlement view (cash or physical)
   */
  async getSettlement(tradeId: number, eventId: string): Promise<SettlementView> {
    const response = await fetch(
      `${this.baseUrl}/cds-trades/${tradeId}/credit-events/${eventId}/settlement`
    );

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Settlement not found');
      }
      throw new Error(`Failed to get settlement: ${response.status}`);
    }

    return response.json();
  }

  /**
   * Handle API errors and extract meaningful error messages
   */
  private handleApiError(error: any): string {
    if (error.response && error.response.data) {
      return error.response.data.detail || error.response.data.message || 'An error occurred';
    }
    return error.message || 'An unexpected error occurred';
  }
}

// Export singleton instance
export const creditEventService = new CreditEventService();