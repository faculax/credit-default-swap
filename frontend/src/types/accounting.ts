// Accounting Event Types

export enum EventType {
  MTM_VALUATION = 'MTM_VALUATION',
  MTM_PNL_UNREALIZED = 'MTM_PNL_UNREALIZED',
  ACCRUED_INTEREST = 'ACCRUED_INTEREST',
  REALIZED_PNL = 'REALIZED_PNL',
  CASH_SETTLEMENT = 'CASH_SETTLEMENT',
  TERMINATION = 'TERMINATION',
  NOVATION = 'NOVATION',
  CREDIT_EVENT = 'CREDIT_EVENT',
}

export enum EventStatus {
  PENDING = 'PENDING',
  POSTED = 'POSTED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export interface AccountingEvent {
  id: number;
  eventDate: string;
  eventType: EventType;
  tradeId: number;
  referenceEntityName: string;
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
  currency: string;
  currentNpv?: number;
  previousNpv?: number;
  npvChange?: number;
  accruedChange?: number;
  status: EventStatus;
  postedToGl: boolean;
  postedAt?: string;
  glBatchId?: string;
  valuationJobId?: string;
  description: string;
  errorMessage?: string;
  createdAt: string;
  updatedAt?: string;
  postedBy?: string;
}

export interface AccountingSummary {
  date: string;
  totalDebits: number;
  totalCredits: number;
  totalEvents: number;
  pendingEvents: number;
  postedEvents: number;
  balanced: boolean;
}

export interface GenerateEventsResponse {
  status: string;
  message: string;
  date: string;
  eventCount: number;
  events: AccountingEvent[];
}

export interface PostingConfirmation {
  glBatchId: string;
  postedBy: string;
}

export interface MarkPostedResponse {
  status: string;
  message: string;
  eventId: number;
  glBatchId: string;
}

export interface BulkPostingConfirmation {
  glBatchId: string;
  postedBy: string;
  eventIds: number[];
}

export interface BulkMarkPostedResponse {
  status: string;
  message: string;
  successCount: number;
  failureCount: number;
  glBatchId: string;
}

export interface HealthCheckResponse {
  status: string;
  timestamp: string;
  message: string;
}

// EOD Valuation Job Types

export enum JobStatus {
  CREATED = 'CREATED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export enum StepStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  SKIPPED = 'SKIPPED',
}

export interface EodValuationJobStep {
  id: number;
  stepNumber: number;
  stepName: string;
  status: StepStatus;
  startTime?: string;
  endTime?: string;
  durationSeconds?: number;
  recordsProcessed: number;
  recordsSuccessful: number;
  recordsFailed: number;
  errorMessage?: string;
}

export interface EodValuationJob {
  id: number;
  jobId: string;
  valuationDate: string;
  status: JobStatus;
  startTime: string;
  endTime?: string;
  durationSeconds?: number;
  totalTrades: number;
  successfulTrades: number;
  failedTrades: number;
  dryRun: boolean;
  triggeredBy: string;
  errorMessage?: string;
  steps: EodValuationJobStep[];
}

export interface EodJobSummary {
  jobId: string;
  valuationDate: string;
  status: JobStatus;
  startTime: string;
  endTime?: string;
  durationSeconds?: number;
  totalTrades: number;
  completedSteps: number;
  totalSteps: number;
}

export interface TriggerJobRequest {
  valuationDate: string;
  dryRun?: boolean;
}

export interface TriggerJobResponse {
  jobId: string;
  message: string;
  status: string;
}
