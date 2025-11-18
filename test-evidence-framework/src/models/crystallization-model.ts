/**
 * Crystallization Model
 * 
 * Type definitions for test crystallization workflow - the process of
 * reviewing, approving, and "locking" generated tests as production-ready.
 * 
 * @module crystallization-model
 */

import type { TestType } from './story-model.js';
import type { TestFramework, ValidationResult } from './validation-model.js';

/**
 * Crystallization status lifecycle
 */
export type CrystallizationStatus =
  | 'generated'      // Freshly generated, not reviewed
  | 'under-review'   // Being reviewed by developer
  | 'needs-changes'  // Review completed, changes required
  | 'approved'       // Review completed, approved for crystallization
  | 'crystallized'   // Locked and ready for production
  | 'deprecated';    // No longer valid, marked for removal

/**
 * Review decision
 */
export type ReviewDecision =
  | 'approve'        // Approve for crystallization
  | 'request-changes' // Request changes before approval
  | 'reject';        // Reject test

/**
 * Reviewer information
 */
export interface Reviewer {
  /** Reviewer name */
  name: string;
  
  /** Reviewer email */
  email?: string;
  
  /** Review timestamp */
  timestamp: Date;
  
  /** Review decision */
  decision: ReviewDecision;
  
  /** Review comments */
  comments?: string;
  
  /** Changes requested */
  changesRequested?: string[];
}

/**
 * Review metadata
 */
export interface ReviewMetadata {
  /** Review ID */
  id: string;
  
  /** Test file being reviewed */
  filePath: string;
  
  /** Current status */
  status: CrystallizationStatus;
  
  /** Review history */
  reviewers: Reviewer[];
  
  /** Creation timestamp */
  createdAt: Date;
  
  /** Last modified timestamp */
  lastModifiedAt: Date;
  
  /** Validation result */
  validationResult?: ValidationResult;
  
  /** Quality score */
  qualityScore: number;
  
  /** Review notes */
  notes?: string;
  
  /** Tags for organization */
  tags?: string[];
}

/**
 * Crystallized test metadata
 */
export interface CrystallizedTest {
  /** Unique test ID */
  id: string;
  
  /** Test file path */
  filePath: string;
  
  /** Test framework */
  framework: TestFramework;
  
  /** Test type */
  testType: TestType;
  
  /** Story ID this test covers */
  storyId: string;
  
  /** Crystallization status */
  status: CrystallizationStatus;
  
  /** Review metadata */
  review: ReviewMetadata;
  
  /** Test content hash (for change detection) */
  contentHash: string;
  
  /** Crystallization timestamp */
  crystallizedAt?: Date;
  
  /** Last validation timestamp */
  lastValidatedAt?: Date;
  
  /** Is locked (immutable) */
  locked: boolean;
  
  /** Lock reason */
  lockReason?: string;
  
  /** Version number */
  version: number;
  
  /** Previous versions */
  history?: CrystallizedTestVersion[];
}

/**
 * Historical version of a crystallized test
 */
export interface CrystallizedTestVersion {
  /** Version number */
  version: number;
  
  /** Content hash */
  contentHash: string;
  
  /** Timestamp */
  timestamp: Date;
  
  /** Author */
  author: string;
  
  /** Change description */
  changeDescription?: string;
  
  /** Quality score at this version */
  qualityScore: number;
}

/**
 * Crystallization workflow configuration
 */
export interface CrystallizationConfig {
  /** Require validation before crystallization */
  requireValidation: boolean;
  
  /** Minimum quality score to crystallize */
  minQualityScore: number;
  
  /** Require manual review */
  requireManualReview: boolean;
  
  /** Number of reviewers required */
  minReviewers: number;
  
  /** Auto-crystallize if quality score above threshold */
  autoCrystallizeThreshold?: number;
  
  /** Lock tests after crystallization */
  lockAfterCrystallization: boolean;
  
  /** Enable version history */
  enableVersioning: boolean;
  
  /** Max versions to keep */
  maxVersions: number;
}

/**
 * Crystallization registry
 */
export interface CrystallizationRegistry {
  /** Registry ID */
  id: string;
  
  /** Registry version */
  version: string;
  
  /** Last updated */
  lastUpdated: Date;
  
  /** All crystallized tests */
  tests: Map<string, CrystallizedTest>;
  
  /** Tests by status */
  byStatus: Map<CrystallizationStatus, string[]>;
  
  /** Tests by story */
  byStory: Map<string, string[]>;
  
  /** Tests by framework */
  byFramework: Map<TestFramework, string[]>;
  
  /** Tests by type */
  byType: Map<TestType, string[]>;
  
  /** Statistics */
  stats: CrystallizationStats;
}

/**
 * Crystallization statistics
 */
export interface CrystallizationStats {
  /** Total tests */
  total: number;
  
  /** Tests by status */
  byStatus: Record<CrystallizationStatus, number>;
  
  /** Average quality score */
  averageQualityScore: number;
  
  /** Tests crystallized today */
  crystallizedToday: number;
  
  /** Tests under review */
  underReview: number;
  
  /** Tests needing changes */
  needingChanges: number;
  
  /** Locked tests */
  locked: number;
  
  /** Crystallization rate (tests/day) */
  crystallizationRate: number;
}

/**
 * Crystallization operation
 */
export interface CrystallizationOperation {
  /** Operation ID */
  id: string;
  
  /** Operation type */
  type: 'review' | 'approve' | 'crystallize' | 'lock' | 'unlock' | 'deprecate';
  
  /** Test file path */
  filePath: string;
  
  /** Operator (who performed the operation) */
  operator: string;
  
  /** Timestamp */
  timestamp: Date;
  
  /** Operation result */
  success: boolean;
  
  /** Result message */
  message: string;
  
  /** Previous status */
  previousStatus: CrystallizationStatus;
  
  /** New status */
  newStatus: CrystallizationStatus;
  
  /** Operation metadata */
  metadata?: Record<string, unknown>;
}

/**
 * Crystallization workflow step
 */
export interface CrystallizationStep {
  /** Step name */
  name: string;
  
  /** Step description */
  description: string;
  
  /** Required status to execute */
  requiredStatus: CrystallizationStatus;
  
  /** Target status after execution */
  targetStatus: CrystallizationStatus;
  
  /** Step validations */
  validations: StepValidation[];
  
  /** Auto-execute if conditions met */
  autoExecute: boolean;
}

/**
 * Step validation
 */
export interface StepValidation {
  /** Validation name */
  name: string;
  
  /** Validation function identifier */
  validator: string;
  
  /** Required to pass */
  required: boolean;
  
  /** Error message if validation fails */
  errorMessage: string;
}

/**
 * Crystallization change request
 */
export interface CrystallizationChangeRequest {
  /** Request ID */
  id: string;
  
  /** Test file path */
  filePath: string;
  
  /** Requested by */
  requestedBy: string;
  
  /** Request timestamp */
  timestamp: Date;
  
  /** Change description */
  description: string;
  
  /** Specific changes requested */
  changes: RequestedChange[];
  
  /** Priority */
  priority: 'low' | 'medium' | 'high' | 'critical';
  
  /** Status */
  status: 'open' | 'in-progress' | 'completed' | 'cancelled';
  
  /** Resolution */
  resolution?: string;
  
  /** Resolved by */
  resolvedBy?: string;
  
  /** Resolution timestamp */
  resolvedAt?: Date;
}

/**
 * Requested change detail
 */
export interface RequestedChange {
  /** Change type */
  type: 'syntax' | 'logic' | 'assertion' | 'structure' | 'documentation' | 'other';
  
  /** Change description */
  description: string;
  
  /** Location in file */
  location?: {
    line: number;
    column: number;
  };
  
  /** Current code */
  current?: string;
  
  /** Suggested code */
  suggested?: string;
  
  /** Completed */
  completed: boolean;
}

/**
 * Crystallization report
 */
export interface CrystallizationReport {
  /** Report ID */
  id: string;
  
  /** Report title */
  title: string;
  
  /** Generation timestamp */
  timestamp: Date;
  
  /** Report period */
  period: {
    start: Date;
    end: Date;
  };
  
  /** Statistics */
  stats: CrystallizationStats;
  
  /** Tests by status */
  testsByStatus: Record<CrystallizationStatus, CrystallizedTest[]>;
  
  /** Recent operations */
  recentOperations: CrystallizationOperation[];
  
  /** Change requests */
  changeRequests: CrystallizationChangeRequest[];
  
  /** Quality trends */
  qualityTrends: QualityTrend[];
  
  /** Recommendations */
  recommendations: string[];
}

/**
 * Quality trend data point
 */
export interface QualityTrend {
  /** Date */
  date: Date;
  
  /** Average quality score */
  averageScore: number;
  
  /** Tests crystallized */
  testsCrystallized: number;
  
  /** Quality grade distribution */
  gradeDistribution: Record<'A' | 'B' | 'C' | 'D' | 'F', number>;
}

/**
 * Bulk crystallization options
 */
export interface BulkCrystallizationOptions {
  /** Files to crystallize */
  files: string[];
  
  /** Reviewer name */
  reviewer: string;
  
  /** Review decision */
  decision: ReviewDecision;
  
  /** Comments */
  comments?: string;
  
  /** Force crystallization (skip validations) */
  force: boolean;
  
  /** Dry run (don't actually crystallize) */
  dryRun: boolean;
}

/**
 * Bulk crystallization result
 */
export interface BulkCrystallizationResult {
  /** Total files processed */
  total: number;
  
  /** Successfully crystallized */
  succeeded: number;
  
  /** Failed to crystallize */
  failed: number;
  
  /** Skipped */
  skipped: number;
  
  /** Results per file */
  results: Map<string, CrystallizationOperation>;
  
  /** Errors encountered */
  errors: Map<string, string>;
}

/**
 * Crystallization event
 */
export interface CrystallizationEvent {
  /** Event ID */
  id: string;
  
  /** Event type */
  type: 'status-change' | 'review-added' | 'validation-completed' | 'crystallized' | 'locked' | 'deprecated';
  
  /** Test file path */
  filePath: string;
  
  /** Event timestamp */
  timestamp: Date;
  
  /** Event data */
  data: Record<string, unknown>;
  
  /** Actor (who triggered the event) */
  actor: string;
}

/**
 * Crystallization event listener
 */
export type CrystallizationEventListener = (event: CrystallizationEvent) => void | Promise<void>;
