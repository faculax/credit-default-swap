export interface RiskMeasures {
  tradeId: number;
  // Legacy CDS fields
  pvClean?: number;
  pvDirty?: number;
  parSpread?: number;
  cs01?: number;
  dv01?: number;
  jtd?: number;
  recovery01?: number;
  valuationTimestamp: string;
  // New ORE fields
  npv?: number;
  gamma?: number;
  var95?: number;
  expectedShortfall?: number;
  currency?: string;
  greeks?: {
    delta?: number;
    rho?: number;
    theta?: number;
    gamma?: number;
    vega?: number;
  };
}

export interface ScenarioResult {
  scenario: string;
  measures: RiskMeasures;
}

export interface ScenarioResponse {
  base: RiskMeasures;
  scenarios: ScenarioResult[];
}
