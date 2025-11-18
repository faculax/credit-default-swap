/**
 * ReportPortal Integration Models
 * 
 * Type definitions for ReportPortal API entities and operations.
 * Used for uploading test results to ReportPortal for centralized reporting.
 * 
 * Story 20.8: ReportPortal Integration
 */

/**
 * ReportPortal test item status
 */
export type ReportPortalStatus = 
  | 'passed' 
  | 'failed' 
  | 'skipped' 
  | 'interrupted' 
  | 'cancelled'
  | 'in_progress';

/**
 * ReportPortal log level
 */
export type ReportPortalLogLevel = 
  | 'trace' 
  | 'debug' 
  | 'info' 
  | 'warn' 
  | 'error' 
  | 'fatal';

/**
 * ReportPortal test item type
 */
export type ReportPortalItemType = 
  | 'suite' 
  | 'story' 
  | 'test' 
  | 'scenario' 
  | 'step' 
  | 'before_class' 
  | 'before_groups' 
  | 'before_method' 
  | 'before_suite' 
  | 'before_test' 
  | 'after_class' 
  | 'after_groups' 
  | 'after_method' 
  | 'after_suite' 
  | 'after_test';

/**
 * ReportPortal launch mode
 */
export type ReportPortalLaunchMode = 'DEFAULT' | 'DEBUG';

/**
 * ReportPortal configuration
 */
export interface ReportPortalConfig {
  /** ReportPortal server endpoint */
  endpoint: string;
  
  /** ReportPortal API token */
  token: string;
  
  /** ReportPortal project name */
  project: string;
  
  /** Launch name template */
  launchName?: string;
  
  /** Launch description */
  launchDescription?: string;
  
  /** Launch attributes/tags */
  launchAttributes?: ReportPortalAttribute[];
  
  /** Launch mode */
  mode?: ReportPortalLaunchMode;
  
  /** Enable debug mode */
  debug?: boolean;
  
  /** Request timeout in milliseconds */
  timeout?: number;
  
  /** Maximum number of retries */
  maxRetries?: number;
  
  /** Enable attachment upload */
  uploadAttachments?: boolean;
  
  /** Maximum attachment size in bytes */
  maxAttachmentSize?: number;
}

/**
 * ReportPortal attribute (tag)
 */
export interface ReportPortalAttribute {
  /** Attribute key */
  key?: string;
  
  /** Attribute value */
  value: string;
  
  /** System attribute flag */
  system?: boolean;
}

/**
 * ReportPortal launch
 */
export interface ReportPortalLaunch {
  /** Launch UUID */
  id?: string;
  
  /** Launch name */
  name: string;
  
  /** Launch description */
  description?: string;
  
  /** Launch start time (Unix timestamp in milliseconds) */
  startTime: number;
  
  /** Launch end time (Unix timestamp in milliseconds) */
  endTime?: number;
  
  /** Launch attributes */
  attributes?: ReportPortalAttribute[];
  
  /** Launch mode */
  mode?: ReportPortalLaunchMode;
  
  /** Rerun flag */
  rerun?: boolean;
  
  /** Rerun of launch UUID */
  rerunOf?: string;
}

/**
 * ReportPortal test item (suite, test, step)
 */
export interface ReportPortalTestItem {
  /** Item UUID */
  id?: string;
  
  /** Item name */
  name: string;
  
  /** Item description */
  description?: string;
  
  /** Item type */
  type: ReportPortalItemType;
  
  /** Item start time (Unix timestamp in milliseconds) */
  startTime: number;
  
  /** Item end time (Unix timestamp in milliseconds) */
  endTime?: number;
  
  /** Item status */
  status?: ReportPortalStatus;
  
  /** Parent item UUID */
  launchUuid?: string;
  
  /** Parent item UUID (for nested items) */
  parentId?: string;
  
  /** Item attributes */
  attributes?: ReportPortalAttribute[];
  
  /** Code reference (e.g., class.method) */
  codeRef?: string;
  
  /** Test case ID (for retries/reruns) */
  testCaseId?: string;
  
  /** Unique ID (for merging) */
  uniqueId?: string;
  
  /** Retry flag */
  retry?: boolean;
  
  /** Has children flag */
  hasStats?: boolean;
  
  /** Parameters */
  parameters?: ReportPortalParameter[];
}

/**
 * ReportPortal test parameter
 */
export interface ReportPortalParameter {
  /** Parameter key */
  key: string;
  
  /** Parameter value */
  value: string;
}

/**
 * ReportPortal log entry
 */
export interface ReportPortalLog {
  /** Log UUID */
  id?: string;
  
  /** Associated item UUID */
  itemUuid?: string;
  
  /** Log timestamp (Unix timestamp in milliseconds) */
  time: number;
  
  /** Log message */
  message: string;
  
  /** Log level */
  level: ReportPortalLogLevel;
  
  /** Attachment file */
  file?: ReportPortalFile;
}

/**
 * ReportPortal attachment file
 */
export interface ReportPortalFile {
  /** File name */
  name: string;
  
  /** File content (Base64 encoded) */
  content?: string;
  
  /** File content type (MIME type) */
  contentType?: string;
}

/**
 * ReportPortal API request: Start Launch
 */
export interface StartLaunchRequest {
  /** Launch name */
  name: string;
  
  /** Launch description */
  description?: string;
  
  /** Launch start time */
  startTime: number;
  
  /** Launch attributes */
  attributes?: ReportPortalAttribute[];
  
  /** Launch mode */
  mode?: ReportPortalLaunchMode;
  
  /** Rerun flag */
  rerun?: boolean;
  
  /** Rerun of launch UUID */
  rerunOf?: string;
}

/**
 * ReportPortal API response: Start Launch
 */
export interface StartLaunchResponse {
  /** Launch UUID */
  id: string;
  
  /** Launch number */
  number?: number;
}

/**
 * ReportPortal API request: Finish Launch
 */
export interface FinishLaunchRequest {
  /** Launch end time */
  endTime: number;
  
  /** Launch status */
  status?: ReportPortalStatus;
  
  /** Launch description */
  description?: string;
  
  /** Launch attributes */
  attributes?: ReportPortalAttribute[];
}

/**
 * ReportPortal API response: Finish Launch
 */
export interface FinishLaunchResponse {
  /** Launch UUID */
  id: string;
  
  /** Launch number */
  number?: number;
  
  /** Link to launch */
  link?: string;
}

/**
 * ReportPortal API request: Start Test Item
 */
export interface StartTestItemRequest {
  /** Item name */
  name: string;
  
  /** Item description */
  description?: string;
  
  /** Item type */
  type: ReportPortalItemType;
  
  /** Item start time */
  startTime: number;
  
  /** Launch UUID */
  launchUuid: string;
  
  /** Parent item UUID */
  parentUuid?: string;
  
  /** Item attributes */
  attributes?: ReportPortalAttribute[];
  
  /** Code reference */
  codeRef?: string;
  
  /** Test case ID */
  testCaseId?: string;
  
  /** Unique ID */
  uniqueId?: string;
  
  /** Retry flag */
  retry?: boolean;
  
  /** Has stats flag */
  hasStats?: boolean;
  
  /** Parameters */
  parameters?: ReportPortalParameter[];
}

/**
 * ReportPortal API response: Start Test Item
 */
export interface StartTestItemResponse {
  /** Item UUID */
  id: string;
}

/**
 * ReportPortal API request: Finish Test Item
 */
export interface FinishTestItemRequest {
  /** Item end time */
  endTime: number;
  
  /** Item status */
  status: ReportPortalStatus;
  
  /** Item description */
  description?: string;
  
  /** Item attributes */
  attributes?: ReportPortalAttribute[];
  
  /** Issue information */
  issue?: ReportPortalIssue;
}

/**
 * ReportPortal API response: Finish Test Item
 */
export interface FinishTestItemResponse {
  /** Item UUID */
  id: string;
  
  /** Link to item */
  link?: string;
}

/**
 * ReportPortal issue information
 */
export interface ReportPortalIssue {
  /** Issue type */
  issueType: string;
  
  /** Issue comment */
  comment?: string;
  
  /** Auto-analyzed flag */
  autoAnalyzed?: boolean;
  
  /** Ignore analyzer flag */
  ignoreAnalyzer?: boolean;
  
  /** External system issues */
  externalSystemIssues?: ReportPortalExternalIssue[];
}

/**
 * ReportPortal external system issue
 */
export interface ReportPortalExternalIssue {
  /** Ticket ID */
  ticketId: string;
  
  /** External system URL */
  url?: string;
  
  /** Bug tracking system ID */
  btsUrl?: string;
  
  /** Bug tracking system project */
  btsProject?: string;
}

/**
 * ReportPortal API request: Save Log
 */
export interface SaveLogRequest {
  /** Associated item UUID */
  itemUuid: string;
  
  /** Log timestamp */
  time: number;
  
  /** Log message */
  message: string;
  
  /** Log level */
  level: ReportPortalLogLevel;
  
  /** Launch UUID */
  launchUuid: string;
  
  /** Attachment file */
  file?: ReportPortalFile;
}

/**
 * ReportPortal API response: Save Log
 */
export interface SaveLogResponse {
  /** Log UUID */
  id: string;
}

/**
 * ReportPortal API error response
 */
export interface ReportPortalErrorResponse {
  /** Error code */
  errorCode?: number;
  
  /** Error message */
  message: string;
  
  /** Error details */
  details?: string;
}

/**
 * ReportPortal upload result
 */
export interface ReportPortalUploadResult {
  /** Upload success flag */
  success: boolean;
  
  /** Launch UUID */
  launchId?: string;
  
  /** Launch number */
  launchNumber?: number;
  
  /** Launch URL */
  launchUrl?: string;
  
  /** Number of test items uploaded */
  itemsUploaded: number;
  
  /** Number of logs uploaded */
  logsUploaded: number;
  
  /** Number of attachments uploaded */
  attachmentsUploaded: number;
  
  /** Upload duration in milliseconds */
  duration: number;
  
  /** Error message (if failed) */
  error?: string;
  
  /** Upload statistics */
  stats: ReportPortalUploadStats;
}

/**
 * ReportPortal upload statistics
 */
export interface ReportPortalUploadStats {
  /** Total tests */
  total: number;
  
  /** Passed tests */
  passed: number;
  
  /** Failed tests */
  failed: number;
  
  /** Skipped tests */
  skipped: number;
  
  /** Test suites */
  suites: number;
  
  /** Total steps */
  steps: number;
}

/**
 * ReportPortal batch upload options
 */
export interface ReportPortalBatchUploadOptions {
  /** Source directory containing test results */
  sourceDir: string;
  
  /** Result file patterns to include */
  patterns?: string[];
  
  /** Launch name */
  launchName: string;
  
  /** Launch description */
  launchDescription?: string;
  
  /** Launch attributes */
  launchAttributes?: ReportPortalAttribute[];
  
  /** Story ID filter */
  storyId?: string;
  
  /** Test framework filter */
  framework?: string;
  
  /** Merge launches flag */
  mergeLaunches?: boolean;
  
  /** Enable parallel upload */
  parallel?: boolean;
  
  /** Batch size for parallel upload */
  batchSize?: number;
}

/**
 * ReportPortal test result mapping
 */
export interface ReportPortalTestMapping {
  /** Original test ID */
  testId: string;
  
  /** Original test path */
  testPath: string;
  
  /** ReportPortal item UUID */
  itemUuid: string;
  
  /** ReportPortal item type */
  itemType: ReportPortalItemType;
  
  /** Mapping timestamp */
  timestamp: number;
}

/**
 * ReportPortal upload session
 */
export interface ReportPortalUploadSession {
  /** Session ID */
  sessionId: string;
  
  /** Launch UUID */
  launchId: string;
  
  /** Launch start time */
  startTime: number;
  
  /** Configuration used */
  config: ReportPortalConfig;
  
  /** Test mappings */
  mappings: Map<string, ReportPortalTestMapping>;
  
  /** Upload statistics */
  stats: ReportPortalUploadStats;
}
