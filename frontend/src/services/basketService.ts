// Basket service for Epic 15
// Basket & Multi-Name Credit Derivatives

import { API_BASE_URL } from '../config/api';
import { Basket, BasketPricingRequest, BasketPricingResult } from '../types/basket';

class BasketService {
  private baseUrl = `${API_BASE_URL}/baskets`;

  /**
   * Create a new basket
   */
  async createBasket(basket: Omit<Basket, 'id'>): Promise<Basket> {
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(basket),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to create basket');
    }

    return response.json();
  }

  /**
   * Get all baskets
   */
  async getAllBaskets(): Promise<Basket[]> {
    const response = await fetch(this.baseUrl);

    if (!response.ok) {
      throw new Error('Failed to fetch baskets');
    }

    return response.json();
  }

  /**
   * Get basket by ID
   */
  async getBasketById(id: number): Promise<Basket> {
    const response = await fetch(`${this.baseUrl}/${id}`);

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error(`Basket not found: ${id}`);
      }
      throw new Error('Failed to fetch basket');
    }

    return response.json();
  }

  /**
   * Update basket (limited fields)
   */
  async updateBasket(id: number, basket: Partial<Basket>): Promise<Basket> {
    const response = await fetch(`${this.baseUrl}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(basket),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to update basket');
    }

    return response.json();
  }

  /**
   * Price a basket
   */
  async priceBasket(id: number, request?: BasketPricingRequest): Promise<BasketPricingResult> {
    const params = new URLSearchParams();

    if (request?.valuationDate) {
      params.append('valuationDate', request.valuationDate);
    }
    if (request?.paths !== undefined) {
      params.append('paths', request.paths.toString());
    }
    if (request?.seed !== undefined) {
      params.append('seed', request.seed.toString());
    }
    if (request?.includeSensitivities !== undefined) {
      params.append('includeSensitivities', request.includeSensitivities.toString());
    }
    if (request?.includeEtlTimeline !== undefined) {
      params.append('includeEtlTimeline', request.includeEtlTimeline.toString());
    }

    const url = `${this.baseUrl}/${id}/price${params.toString() ? '?' + params.toString() : ''}`;
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to price basket');
    }

    return response.json();
  }

  /**
   * Delete basket (if needed)
   */
  async deleteBasket(id: number): Promise<void> {
    const response = await fetch(`${this.baseUrl}/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete basket');
    }
  }
}

export const basketService = new BasketService();
