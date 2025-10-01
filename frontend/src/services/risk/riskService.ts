import { RiskMeasures, ScenarioResponse } from './riskTypes';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/api';

export async function fetchRiskMeasures(tradeId: number): Promise<RiskMeasures> {
  // Use the new ORE-integrated endpoint instead of legacy stub
  const res = await fetch(`${API_BASE}/risk/scenario/calculate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      scenarioId: `base-calculation-${tradeId}`,
      tradeIds: [tradeId],
      valuationDate: new Date().toISOString().split('T')[0], // Today's date
      scenarios: {
        "base": 0.0
      }
    })
  });
  if(!res.ok) throw new Error('Failed to fetch risk measures');
  const results = await res.json();
  // Return the first result since we only requested one trade
  return results[0];
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
