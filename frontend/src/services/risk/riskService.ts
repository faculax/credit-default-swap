import { RiskMeasures, ScenarioResponse } from './riskTypes';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/api';

export async function fetchRiskMeasures(tradeId: number): Promise<RiskMeasures> {
  const res = await fetch(`${API_BASE}/risk/cds/${tradeId}`);
  if(!res.ok) throw new Error('Failed to fetch risk measures');
  return res.json();
}

export async function runRiskScenarios(tradeId: number, parallelBpsShifts: number[]): Promise<ScenarioResponse> {
  const res = await fetch(`${API_BASE}/risk/cds/${tradeId}/scenarios`, {
    method: 'POST',
    headers: {'Content-Type':'application/json'},
    body: JSON.stringify({ parallelBpsShifts })
  });
  if(!res.ok) throw new Error('Failed to run scenarios');
  return res.json();
}
