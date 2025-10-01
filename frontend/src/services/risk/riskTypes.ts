export interface RiskMeasures {
  tradeId: number;
  pvClean: number;
  pvDirty: number;
  parSpread: number;
  cs01: number;
  dv01: number;
  jtd: number;
  recovery01: number;
  valuationTimestamp: string;
}

export interface ScenarioResult {
  scenario: string;
  measures: RiskMeasures;
}

export interface ScenarioResponse {
  base: RiskMeasures;
  scenarios: ScenarioResult[];
}
