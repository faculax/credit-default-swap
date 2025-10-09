// Novation Service for frontend API calls

export interface NovationRequest {
  tradeId: number;
  ccpName: string;
  memberFirm: string;
  actor: string;
}

export interface NovationResult {
  success: boolean;
  message: string;
  originalTradeId?: number;
  ccpTradeId?: number;
  novationReference?: string;
  error?: string;
}

export interface NovationEligibilityResponse {
  tradeId: number;
  eligible: boolean;
  message: string;
  error?: string;
}

export interface NovationHistory {
  originalTradeId: number;
  novatedTrades: any[];
  count: number;
}

export class NovationService {
  private static instance: NovationService;
  private baseUrl = '/api/novation';

  private constructor() {}

  public static getInstance(): NovationService {
    if (!NovationService.instance) {
      NovationService.instance = new NovationService();
    }
    return NovationService.instance;
  }

  /**
   * Execute novation of a bilateral trade to CCP clearing
   */
  async executeNovation(request: NovationRequest): Promise<NovationResult> {
    try {
      const response = await fetch(`${this.baseUrl}/execute`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || data.message || 'Novation failed');
      }

      return data;
    } catch (error: any) {
      console.error('Novation execution failed:', error);
      throw new Error(error.message || 'Failed to execute novation');
    }
  }

  /**
   * Check if a trade is eligible for novation
   */
  async checkNovationEligibility(tradeId: number): Promise<NovationEligibilityResponse> {
    try {
      const response = await fetch(`${this.baseUrl}/eligible/${tradeId}`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || 'Failed to check novation eligibility');
      }

      return data;
    } catch (error: any) {
      console.error('Eligibility check failed:', error);
      throw new Error(error.message || 'Failed to check novation eligibility');
    }
  }

  /**
   * Get novation history for a trade
   */
  async getNovationHistory(tradeId: number): Promise<NovationHistory> {
    try {
      const response = await fetch(`${this.baseUrl}/history/${tradeId}`);
      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || 'Failed to get novation history');
      }

      return data;
    } catch (error: any) {
      console.error('Novation history fetch failed:', error);
      throw new Error(error.message || 'Failed to get novation history');
    }
  }

  /**
   * Get available CCP accounts
   */
  async getCcpAccounts(ccpName?: string, memberFirm?: string): Promise<any[]> {
    try {
      const params = new URLSearchParams();
      if (ccpName) params.append('ccpName', ccpName);
      if (memberFirm) params.append('memberFirm', memberFirm);
      
      const url = `${this.baseUrl}/ccp-accounts${params.toString() ? '?' + params.toString() : ''}`;
      const response = await fetch(url);
      
      if (!response.ok) {
        throw new Error('Failed to fetch CCP accounts');
      }

      return await response.json();
    } catch (error: any) {
      console.error('CCP accounts fetch failed:', error);
      throw new Error(error.message || 'Failed to fetch CCP accounts');
    }
  }

  /**
   * Create or update CCP account
   */
  async createCcpAccount(account: any): Promise<any> {
    try {
      const response = await fetch(`${this.baseUrl}/ccp-accounts`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(account),
      });

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || 'Failed to create CCP account');
      }

      return data;
    } catch (error: any) {
      console.error('CCP account creation failed:', error);
      throw new Error(error.message || 'Failed to create CCP account');
    }
  }
}

// Export singleton instance
export const novationService = NovationService.getInstance();