export interface Cashflow {
  tradeId?: string | null;
  type?: string | null;
  cashflowNo?: number | null;
  legNo?: number | null;
  payDate?: string | null;
  flowType?: string | null;
  amount?: number | null;
  currency?: string | null;
  coupon?: number | null;
  accrual?: number | null;
  accrualStartDate?: string | null;
  accrualEndDate?: string | null;
  accruedAmount?: number | null;
  notional?: number | null;
  discountFactor?: number | null;
  presentValue?: number | null;
  fxRate?: number | null;
  presentValueBase?: number | null;
}

export interface QuoteData {
  tenor?: string;
  quoteName?: string;
  value?: number;
  type?: string;
}

export interface DiscountCurveData {
  currency?: string;
  curveId?: string;
  quotes?: QuoteData[];
}

export interface DefaultCurveData {
  referenceEntity?: string;
  currency?: string;
  curveId?: string;
  recoveryRate?: number;
  spreadQuotes?: QuoteData[];
}

export interface MarketDataSnapshot {
  valuationDate?: string;
  baseCurrency?: string;
  discountCurves?: DiscountCurveData[];
  defaultCurves?: DefaultCurveData[];
  fxRates?: { [key: string]: number };
  marketDataFileContent?: string;
  todaysMarketFileContent?: string;
  curveConfigFileContent?: string;
}

export interface RiskMeasures {
  tradeId: number;
  valuationTimestamp: string;
  
  // Core ORE fields
  npv?: number | null;
  currency?: string;
  
  // Real CDS-specific fields from ORE
  fairSpreadClean?: number | null;
  fairSpreadDirty?: number | null;
  protectionLegNPV?: number | null;
  premiumLegNPVClean?: number | null;
  premiumLegNPVDirty?: number | null;
  accruedPremium?: number | null;
  upfrontPremium?: number | null;
  couponLegBPS?: number | null;
  currentNotional?: number | null;
  originalNotional?: number | null;
  riskyAnnuity?: number | null;
  jtd?: number | null; // Jump-to-default exposure
  
  // Credit risk arrays
  defaultProbabilities?: number[] | null;
  expectedLosses?: number[] | null;
  accrualStartDates?: string[] | null;
  accrualEndDates?: string[] | null;
  
  // Cashflow schedule
  cashflows?: Cashflow[] | null;
  
  // Market data snapshot used for this calculation
  marketDataSnapshot?: MarketDataSnapshot | null;
  
  // DEPRECATED - Fake metrics (kept for backwards compatibility)
  dv01?: number | null;
  gamma?: number | null;
  var95?: number | null;
  expectedShortfall?: number | null;
  greeks?: {
    oreRuntime?: number;
    delta?: number;
    calculatedBy?: number;
    theta?: number;
    vega?: number;
  } | null;
}

export interface ScenarioResult {
  scenario: string;
  measures: RiskMeasures;
}

export interface ScenarioResponse {
  base: RiskMeasures;
  scenarios: ScenarioResult[];
}
