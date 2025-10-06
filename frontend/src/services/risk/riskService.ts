import { RiskMeasures, ScenarioResponse } from './riskTypes';

const API_BASE = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/api';

export async function fetchRiskMeasures(tradeId: number, valuationDate?: string): Promise<RiskMeasures> {
  // Use the ORE calculation endpoint instead of legacy endpoint
  const scenarioRequest = {
    scenarioId: `cds-${tradeId}-base`,
    tradeIds: [tradeId],
    valuationDate: valuationDate || new Date().toISOString().split('T')[0], // Custom date or today's date in YYYY-MM-DD format
    scenarios: {} // Empty scenarios for base calculation
  };

  const res = await fetch(`${API_BASE}/risk/scenario/calculate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(scenarioRequest)
  });
  
  if(!res.ok) throw new Error('Failed to fetch risk measures from ORE');
  
  const results = await res.json();
  
  // The ORE endpoint returns a List<RiskMeasures>, so we take the first result
  // which corresponds to our single trade
  if (Array.isArray(results) && results.length > 0) {
    return results[0];
  } else {
    throw new Error('No risk measures returned from ORE calculation');
  }
}

export async function runRiskScenarios(tradeId: number, parallelBpsShifts: number[]): Promise<ScenarioResponse> {
  // Create base scenario request for ORE
  const baseScenarioRequest = {
    scenarioId: `cds-${tradeId}-base`,
    tradeIds: [tradeId],
    valuationDate: new Date().toISOString().split('T')[0],
    scenarios: {}
  };

  // Get base measures using ORE
  const baseRes = await fetch(`${API_BASE}/risk/scenario/calculate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(baseScenarioRequest)
  });
  
  if (!baseRes.ok) throw new Error('Failed to get base risk measures from ORE');
  const baseResults = await baseRes.json();
  
  if (!Array.isArray(baseResults) || baseResults.length === 0) {
    throw new Error('No base risk measures returned from ORE');
  }
  
  const baseMeasures = baseResults[0];

  // Calculate scenarios using ORE
  const scenarios = [];
  
  for (const shift of parallelBpsShifts) {
    const scenarioRequest = {
      scenarioId: `cds-${tradeId}-parallel-${shift}bp`,
      tradeIds: [tradeId],
      valuationDate: new Date().toISOString().split('T')[0],
      scenarios: {
        // Apply parallel shift to curve points (example - adjust as needed)
        "USD_1Y": shift / 10000, // Convert bps to decimal
        "USD_2Y": shift / 10000,
        "USD_3Y": shift / 10000,
        "USD_5Y": shift / 10000,
        "USD_7Y": shift / 10000,
        "USD_10Y": shift / 10000
      }
    };

    const res = await fetch(`${API_BASE}/risk/scenario/calculate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(scenarioRequest)
    });
    
    if (!res.ok) {
      console.warn(`Failed to calculate scenario for ${shift}bp shift`);
      continue;
    }
    
    const results = await res.json();
    if (Array.isArray(results) && results.length > 0) {
      scenarios.push({
        scenario: `PARALLEL_${shift}BP`,
        measures: results[0]
      });
    }
  }

  return {
    base: baseMeasures,
    scenarios
  };
}
