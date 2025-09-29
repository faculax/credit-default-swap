// API service for CDS trades
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/api';

export interface CDSTradeRequest {
  referenceEntity: string;
  notionalAmount: number;
  spread: number;
  maturityDate: string;
  effectiveDate: string;
  counterparty: string;
  tradeDate: string;
  currency: string;
  premiumFrequency: string;
  dayCountConvention: string;
  buySellProtection: 'BUY' | 'SELL';
  restructuringClause?: string;
  paymentCalendar: string;
  accrualStartDate: string;
  tradeStatus: string;
}

export interface CDSTradeResponse extends CDSTradeRequest {
  id: number;
  createdAt: string;
  updatedAt?: string;
}

class CDSTradeService {
  
  async createTrade(trade: CDSTradeRequest): Promise<CDSTradeResponse> {
    const response = await fetch(`${API_BASE_URL}/cds-trades`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(trade)
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to create trade: ${response.status} ${errorText}`);
    }

    return response.json();
  }

  async getAllTrades(): Promise<CDSTradeResponse[]> {
    const response = await fetch(`${API_BASE_URL}/cds-trades`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trades: ${response.status}`);
    }

    return response.json();
  }

  async getTradeById(id: number): Promise<CDSTradeResponse> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/${id}`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trade: ${response.status}`);
    }

    return response.json();
  }

  async updateTrade(id: number, trade: CDSTradeRequest): Promise<CDSTradeResponse> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(trade)
    });

    if (!response.ok) {
      throw new Error(`Failed to update trade: ${response.status}`);
    }

    return response.json();
  }

  async deleteTrade(id: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/${id}`, {
      method: 'DELETE'
    });

    if (!response.ok) {
      throw new Error(`Failed to delete trade: ${response.status}`);
    }
  }

  async getTradeCount(): Promise<number> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/count`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trade count: ${response.status}`);
    }

    return response.json();
  }

  async getTradesByReferenceEntity(referenceEntity: string): Promise<CDSTradeResponse[]> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/by-reference-entity/${referenceEntity}`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trades by reference entity: ${response.status}`);
    }

    return response.json();
  }

  async getTradesByCounterparty(counterparty: string): Promise<CDSTradeResponse[]> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/by-counterparty/${counterparty}`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trades by counterparty: ${response.status}`);
    }

    return response.json();
  }

  async getTradesByStatus(status: string): Promise<CDSTradeResponse[]> {
    const response = await fetch(`${API_BASE_URL}/cds-trades/by-status/${status}`);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch trades by status: ${response.status}`);
    }

    return response.json();
  }
}

export const cdsTradeService = new CDSTradeService();