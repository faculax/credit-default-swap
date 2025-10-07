const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/api';
const PORTFOLIO_BASE_URL = `${API_BASE_URL}/cds-portfolios`;

export interface CdsPortfolio {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt?: string;
  constituents?: CdsPortfolioConstituent[];
}

export interface CdsPortfolioConstituent {
  id: number;
  portfolio?: CdsPortfolio;
  trade: {
    id: number;
    referenceEntity: string;
    notionalAmount: number;
    spread: number;
    maturityDate: string;
    currency: string;
  };
  weightType: 'NOTIONAL' | 'PERCENT';
  weightValue: number;
  active: boolean;
  addedAt: string;
}

export interface ConstituentRequest {
  tradeId: number;
  weightType: 'NOTIONAL' | 'PERCENT';
  weightValue: number;
}

export interface AttachTradesRequest {
  trades: ConstituentRequest[];
}

export interface PortfolioPricingResponse {
  portfolioId: number;
  valuationDate: string;
  aggregate: {
    // Core PV metrics
    pv: number;
    accrued: number;
    premiumLegPv: number;
    protectionLegPv: number;
    
    // Spread and sensitivity metrics
    fairSpreadBpsWeighted: number;
    cs01: number;
    rec01: number;
    jtd: number;
    
    // Notional and premium metrics
    totalNotional?: number;
    upfrontPremium?: number;
    totalPaidCoupons?: number;
    
    // Position metrics
    tradeCount?: number;
    netProtectionBought?: number;
    averageMaturityYears?: string;
  };
  byTrade: {
    tradeId: number;
    referenceEntity: string;
    notional: number;
    pv: number;
    cs01: number;
    rec01: number;
    weight: number;
    sector: string;
  }[];
  concentration: {
    top5PctCs01: number;
    sectorBreakdown: {
      sector: string;
      cs01Pct: number;
    }[];
  };
  completeness: {
    constituents: number;
    priced: number;
  };
}

export const portfolioService = {
  // CRUD operations
  async createPortfolio(name: string, description?: string): Promise<CdsPortfolio> {
    const response = await fetch(PORTFOLIO_BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, description })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create portfolio');
    }
    
    return response.json();
  },

  async getAllPortfolios(): Promise<CdsPortfolio[]> {
    const response = await fetch(PORTFOLIO_BASE_URL);
    
    if (!response.ok) {
      throw new Error('Failed to fetch portfolios');
    }
    
    const text = await response.text();
    
    // Check if response is empty
    if (!text || text.trim() === '') {
      return [];
    }
    
    try {
      return JSON.parse(text);
    } catch (error) {
      console.error('Failed to parse portfolio response:', text.substring(0, 500));
      throw new Error('Invalid response format from server');
    }
  },

  async getPortfolioById(id: number): Promise<CdsPortfolio> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${id}`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch portfolio');
    }
    
    const text = await response.text();
    
    try {
      return JSON.parse(text);
    } catch (error) {
      console.error('Failed to parse portfolio response:', text.substring(0, 500));
      throw new Error('Invalid response format from server');
    }
  },

  async updatePortfolio(id: number, name: string, description?: string): Promise<CdsPortfolio> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, description })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to update portfolio');
    }
    
    return response.json();
  },

  async deletePortfolio(id: number): Promise<void> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${id}`, {
      method: 'DELETE'
    });
    
    if (!response.ok) {
      throw new Error('Failed to delete portfolio');
    }
  },

  // Constituent management
  async attachTrades(portfolioId: number, request: AttachTradesRequest): Promise<CdsPortfolioConstituent[]> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${portfolioId}/constituents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to attach trades');
    }
    
    return response.json();
  },

  async detachConstituent(portfolioId: number, constituentId: number): Promise<void> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${portfolioId}/constituents/${constituentId}`, {
      method: 'DELETE'
    });
    
    if (!response.ok) {
      throw new Error('Failed to detach constituent');
    }
  },

  async getConstituents(portfolioId: number): Promise<CdsPortfolioConstituent[]> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${portfolioId}/constituents`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch constituents');
    }
    
    const text = await response.text();
    
    // Check if response is empty
    if (!text || text.trim() === '') {
      return [];
    }
    
    try {
      return JSON.parse(text);
    } catch (error) {
      console.error('Failed to parse constituents response:', text.substring(0, 500));
      throw new Error('Invalid response format from server');
    }
  },

  // Pricing
  async pricePortfolio(portfolioId: number, valuationDate: string): Promise<PortfolioPricingResponse> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${portfolioId}/price?valuationDate=${valuationDate}`, {
      method: 'POST'
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to price portfolio');
    }
    
    return response.json();
  },

  async getRiskSummary(portfolioId: number): Promise<PortfolioPricingResponse> {
    const response = await fetch(`${PORTFOLIO_BASE_URL}/${portfolioId}/risk-summary`);
    
    if (!response.ok) {
      throw new Error('Failed to fetch risk summary');
    }
    
    return response.json();
  }
};
