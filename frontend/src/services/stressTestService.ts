// Stress test service for CDS stress scenarios
import { API_BASE_URL } from '../config/api';

export interface StressScenarioRequest {
  tradeId: number;
  recoveryRates?: number[];
  spreadShifts?: number[];
  yieldCurveShifts?: number[];
  combined: boolean;
  valuationDate?: string;
}

export interface ScenarioResult {
  scenarioName: string;
  npv: number;
  jtd: number;
  deltaNpv: number;
  deltaJtd: number;
  severe: boolean;
}

export interface StressImpactResult {
  tradeId: number;
  baseNpv: number;
  baseJtd: number;
  currency: string;
  scenarioCount: number;
  baseYieldCurve: Record<string, number>;
  shiftedYieldCurves: Record<string, Record<string, number>>;
  scenarios: ScenarioResult[];
}

class StressTestService {
  async analyzeStress(request: StressScenarioRequest): Promise<StressImpactResult> {
    const response = await fetch(`${API_BASE_URL}/risk/stress/analyze`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`Failed to analyze stress scenarios: ${response.statusText}`);
    }

    return response.json();
  }
}

export const stressTestService = new StressTestService();
