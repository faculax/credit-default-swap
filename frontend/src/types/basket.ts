// Basket types for Epic 15
// Basket & Multi-Name Credit Derivatives

export type BasketType = 'FIRST_TO_DEFAULT' | 'NTH_TO_DEFAULT' | 'TRANCHETTE';

export interface BasketConstituent {
  id?: number;
  issuer: string;
  weight?: number;
  normalizedWeight?: number;
  recoveryOverride?: number;
  effectiveRecovery?: number;
  seniority?: string;
  sector?: string;
  hazardCurveId?: string;
}

export interface Basket {
  id?: number;
  name: string;
  type: BasketType;
  nth?: number;
  attachmentPoint?: number;
  detachmentPoint?: number;
  premiumFrequency: string;
  dayCount: string;
  currency: string;
  notional: number;
  maturityDate: string;
  createdAt?: string;
  updatedAt?: string;
  constituents: BasketConstituent[];
  constituentCount?: number;
}

export interface ConvergenceDiagnostics {
  pathsUsed: number;
  standardErrorFairSpreadBps?: number;
  iterations: number;
  converged: boolean;
  convergenceMessage?: string;
}

export interface Sensitivities {
  spreadDv01: number;
  correlationBeta: number;
  recovery01: number;
  bumpSizes?: { [key: string]: number };
}

export interface TrancheLossPoint {
  tenor: string;
  etl: number;
}

export interface BasketPricingResult {
  basketId: number;
  valuationDate: string;
  type: BasketType;
  notional: number;
  fairSpreadBps: number;
  premiumLegPv: number;
  protectionLegPv: number;
  pv: number;
  expectedTrancheLossPct?: number;
  etlTimeline?: TrancheLossPoint[];
  convergence: ConvergenceDiagnostics;
  sensitivities?: Sensitivities;
  constituents: BasketConstituent[];
  seedUsed: number;
}

export interface BasketPricingRequest {
  valuationDate?: string;
  paths?: number;
  seed?: number;
  includeSensitivities?: boolean;
  includeEtlTimeline?: boolean;
}
