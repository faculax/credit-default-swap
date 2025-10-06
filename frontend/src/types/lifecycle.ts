// Epic 5 lifecycle types

export interface CouponPeriod {
  id: number;
  tradeId: number;
  periodStartDate: string;
  periodEndDate: string;
  paymentDate: string;
  accrualDays: number;
  notionalAmount: number;
  couponAmount?: number;  // Calculated: notional × spread × (days/360)
  dayCountConvention: string;
  businessDayConvention: string;
  createdAt: string;
  paid: boolean;
  paidAt: string | null;
}

export interface AccrualEvent {
  id: number;
  tradeId: number;
  couponPeriodId: number;
  accrualDate: string;
  accrualAmount: number;
  cumulativeAccrual: number;
  dayCountFraction: number;
  notionalAmount: number;
  tradeVersion: number;
  postedAt: string;
}

export interface TradeAmendment {
  id: number;
  tradeId: number;
  amendmentDate: string;
  previousVersion: number;
  newVersion: number;
  fieldName: string;
  previousValue: string | null;
  newValue: string | null;
  amendmentReason?: string;
  pnlImpactEstimate?: number;
  amendedBy: string;
  createdAt: string;
}

export interface NotionalAdjustment {
  id: number;
  tradeId: number;
  adjustmentDate: string;
  adjustmentType: 'PARTIAL_TERMINATION' | 'FULL_TERMINATION' | 'REDUCTION';
  originalNotional: number;
  adjustmentAmount: number;
  remainingNotional: number;
  unwindCashAmount?: number;
  adjustmentReason?: string;
  createdAt: string;
}

export interface LifecycleSummary {
  couponPeriods: number;
  cumulativeAccrual: number;
  amendments: number;
  notionalAdjustments: number;
}

export interface AmendTradePayload {
  amendments: Record<string, string>;
  amendmentDate: string; // ISO date
  amendedBy: string;
  amendmentReason?: string;
}

export interface NotionalAdjustmentPayload {
  adjustmentDate: string;
  adjustmentType: 'PARTIAL_TERMINATION' | 'FULL_TERMINATION' | 'REDUCTION';
  adjustmentAmount: number;
  adjustmentReason?: string;
}
