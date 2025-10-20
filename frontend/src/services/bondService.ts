import { API_BASE_URL } from '../config/api';

export interface Bond {
  id?: number;
  isin?: string;
  issuer: string;
  seniority: 'SR_UNSEC' | 'SR_SEC' | 'SUBORD';
  sector?: string;
  currency: string;
  notional: number;
  couponRate: number;
  couponFrequency: 'ANNUAL' | 'SEMI_ANNUAL' | 'QUARTERLY';
  dayCount: 'ACT_ACT' | 'THIRTY_360';
  issueDate: string;
  maturityDate: string;
  settlementDays?: number;
  faceValue?: number;
  priceConvention?: 'CLEAN' | 'DIRTY';
  createdAt?: string;
  updatedAt?: string;
}

export interface BondPricingResponse {
  bondId: number;
  valuationDate: string;
  cleanPrice?: number;
  dirtyPrice?: number;
  accruedInterest?: number;
  yieldToMaturity?: number;
  zSpread?: number;
  pv?: number;
  pvRisky?: number;
  sensitivities?: {
    irDv01?: number;
    spreadDv01?: number;
    jtd?: number;
    modifiedDuration?: number;
  };
  inputs?: {
    couponRate: number;
    couponFrequency: string;
    dayCount: string;
  };
}

class BondService {
  private baseUrl = `${API_BASE_URL}/bonds`;

  async createBond(bond: Bond): Promise<Bond> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(bond),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Failed to create bond');
    }

    return response.json();
  }

  async getAllBonds(): Promise<Bond[]> {
    const response = await fetch(this.baseUrl);

    if (!response.ok) {
      throw new Error('Failed to fetch bonds');
    }

    return response.json();
  }

  async getBondById(id: number): Promise<Bond> {
    const response = await fetch(`${this.baseUrl}/${id}`);

    if (!response.ok) {
      throw new Error('Bond not found');
    }

    return response.json();
  }

  async updateBond(id: number, bond: Partial<Bond>): Promise<Bond> {
    const response = await fetch(`${this.baseUrl}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(bond),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Failed to update bond');
    }

    return response.json();
  }

  async priceBond(
    id: number,
    valuationDate?: string,
    discountRate?: number,
    hazardRate?: number
  ): Promise<BondPricingResponse> {
    const params = new URLSearchParams();
    if (valuationDate) params.append('valuationDate', valuationDate);
    if (discountRate !== undefined) params.append('discountRate', discountRate.toString());
    if (hazardRate !== undefined) params.append('hazardRate', hazardRate.toString());

    const response = await fetch(`${this.baseUrl}/${id}/price?${params.toString()}`, {
      method: 'POST',
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || 'Failed to price bond');
    }

    return response.json();
  }

  async getBondsByIssuer(issuer: string): Promise<Bond[]> {
    const response = await fetch(`${this.baseUrl}/issuer/${issuer}`);

    if (!response.ok) {
      throw new Error('Failed to fetch bonds by issuer');
    }

    return response.json();
  }
}

export const bondService = new BondService();
