/**
 * Flow Test Model
 * 
 * Defines TypeScript interfaces for E2E flow test generation.
 * Supports Playwright and Cypress frameworks for multi-service user journeys.
 * 
 * @module models/flow-test-model
 */

import type { AllureSeverity } from './frontend-test-model.js';

/**
 * Flow test types for different E2E scenarios
 */
export type FlowTestType = 
  | 'user-journey'      // Complete user workflow across services
  | 'api-orchestration' // Multi-service API coordination
  | 'cross-service'     // Service-to-service interaction
  | 'e2e-integration'   // Full stack integration test
  | 'performance-flow'; // Performance testing of flows

/**
 * E2E testing framework support
 */
export type E2EFramework = 'playwright' | 'cypress';

/**
 * Service interaction type
 */
export type InteractionType = 
  | 'http-call'     // REST API call
  | 'ui-action'     // User interface interaction
  | 'navigation'    // Page navigation
  | 'form-submit'   // Form submission
  | 'validation'    // Data validation
  | 'wait-for'      // Wait for condition
  | 'assertion';    // Test assertion

/**
 * HTTP methods for API calls
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * Service names in the CDS platform
 */
export type ServiceName = 'backend' | 'frontend' | 'gateway' | 'risk-engine';

/**
 * Single step in a flow test
 */
export interface FlowStep {
  /** Step number in sequence */
  stepNumber: number;
  
  /** Human-readable step description */
  description: string;
  
  /** Type of interaction */
  type: InteractionType;
  
  /** Target service for this step */
  service: ServiceName;
  
  /** API endpoint (for http-call type) */
  endpoint?: string;
  
  /** HTTP method (for http-call type) */
  method?: HttpMethod;
  
  /** UI selector (for ui-action, navigation types) */
  selector?: string;
  
  /** Action to perform (for ui-action type) */
  action?: 'click' | 'type' | 'select' | 'hover' | 'scroll';
  
  /** Input data for the step */
  input?: Record<string, unknown>;
  
  /** Expected output/result */
  expectedResult?: string;
  
  /** Wait condition before next step */
  waitFor?: {
    /** What to wait for */
    condition: 'element' | 'response' | 'timeout' | 'state-change';
    /** Selector or value to wait for */
    value: string;
    /** Timeout in milliseconds */
    timeout?: number;
  };
  
  /** Assertions to verify after this step */
  assertions?: FlowAssertion[];
}

/**
 * Assertion to verify during flow execution
 */
export interface FlowAssertion {
  /** What to assert */
  type: 'status-code' | 'response-body' | 'element-visible' | 'element-text' | 'state-value';
  
  /** Assertion description */
  description: string;
  
  /** Expected value */
  expected: unknown;
  
  /** Actual value selector/path */
  actual: string;
  
  /** Optional custom matcher */
  matcher?: 'equals' | 'contains' | 'matches' | 'greater-than' | 'less-than';
}

/**
 * Complete user journey spanning multiple services
 */
export interface UserJourney {
  /** Unique journey identifier */
  journeyId: string;
  
  /** Journey title */
  title: string;
  
  /** Journey description */
  description: string;
  
  /** Services involved in this journey */
  services: ServiceName[];
  
  /** Sequential steps in the journey */
  steps: FlowStep[];
  
  /** Setup required before journey */
  setup?: {
    /** Test data to create */
    testData?: Record<string, unknown>;
    /** Services to mock */
    mocks?: ServiceMock[];
    /** Authentication setup */
    authentication?: {
      type: 'basic' | 'bearer' | 'session';
      credentials?: Record<string, string>;
    };
  };
  
  /** Cleanup after journey */
  teardown?: {
    /** Data to clean up */
    cleanup?: string[];
    /** Services to reset */
    resetServices?: ServiceName[];
  };
  
  /** Expected journey duration (for performance testing) */
  expectedDuration?: number;
}

/**
 * Service mock configuration
 */
export interface ServiceMock {
  /** Service to mock */
  service: ServiceName;
  
  /** Endpoint to mock */
  endpoint: string;
  
  /** HTTP method */
  method: HttpMethod;
  
  /** Mock response */
  response: {
    status: number;
    body: unknown;
    headers?: Record<string, string>;
  };
  
  /** Optional delay for response */
  delay?: number;
}

/**
 * API orchestration test (multiple service coordination)
 */
export interface ApiOrchestrationTest {
  /** Test identifier */
  testId: string;
  
  /** Test title */
  title: string;
  
  /** Description of the orchestration */
  description: string;
  
  /** Services involved */
  services: ServiceName[];
  
  /** API call sequence */
  apiCalls: ApiCall[];
  
  /** Data flow between services */
  dataFlow?: DataFlowMapping[];
}

/**
 * Single API call in orchestration
 */
export interface ApiCall {
  /** Call sequence number */
  sequence: number;
  
  /** Target service */
  service: ServiceName;
  
  /** API endpoint */
  endpoint: string;
  
  /** HTTP method */
  method: HttpMethod;
  
  /** Request payload */
  payload?: Record<string, unknown>;
  
  /** Expected status code */
  expectedStatus: number;
  
  /** Extract data from response */
  extractData?: {
    /** Variable name to store */
    as: string;
    /** Path to extract (JSONPath or XPath) */
    path: string;
  };
  
  /** Use data from previous call */
  useData?: {
    /** Variable to use */
    from: string;
    /** Where to inject (JSONPath) */
    into: string;
  };
}

/**
 * Data flow mapping between services
 */
export interface DataFlowMapping {
  /** Source service */
  from: ServiceName;
  
  /** Target service */
  to: ServiceName;
  
  /** Data mapping rules */
  mapping: {
    /** Source field path */
    source: string;
    /** Target field path */
    target: string;
    /** Optional transformation */
    transform?: 'uppercase' | 'lowercase' | 'format-date' | 'parse-number';
  }[];
}

/**
 * Playwright test configuration
 */
export interface PlaywrightTestConfig {
  /** Browser type */
  browser: 'chromium' | 'firefox' | 'webkit';
  
  /** Viewport size */
  viewport?: {
    width: number;
    height: number;
  };
  
  /** Base URL */
  baseURL?: string;
  
  /** Screenshot on failure */
  screenshot?: boolean;
  
  /** Video recording */
  video?: boolean;
  
  /** Trace recording */
  trace?: boolean;
  
  /** Timeout in milliseconds */
  timeout?: number;
}

/**
 * Cypress test configuration
 */
export interface CypressTestConfig {
  /** Base URL */
  baseUrl?: string;
  
  /** Viewport size */
  viewportWidth?: number;
  viewportHeight?: number;
  
  /** Video recording */
  video?: boolean;
  
  /** Screenshots */
  screenshotOnRunFailure?: boolean;
  
  /** Command timeout */
  defaultCommandTimeout?: number;
  
  /** Request timeout */
  requestTimeout?: number;
}

/**
 * Flow test template
 */
export interface FlowTestTemplate {
  /** Template identifier */
  templateId: string;
  
  /** Flow test type */
  type: FlowTestType;
  
  /** E2E framework */
  framework: E2EFramework;
  
  /** Required imports */
  imports: string[];
  
  /** Setup code */
  setupCode?: string;
  
  /** Teardown code */
  teardownCode?: string;
  
  /** Framework-specific configuration */
  config?: PlaywrightTestConfig | CypressTestConfig;
  
  /** Test timeout in milliseconds */
  timeout?: number;
  
  /** Retry configuration */
  retry?: {
    attempts: number;
    delay?: number;
  };
}

/**
 * Generated flow test metadata
 */
export interface GeneratedFlowTest {
  /** Test file path */
  filePath: string;
  
  /** Test type */
  type: FlowTestType;
  
  /** Framework used */
  framework: E2EFramework;
  
  /** Story ID this test was generated from */
  storyId: string;
  
  /** Epic path */
  epicPath: string;
  
  /** Services covered */
  services: ServiceName[];
  
  /** Number of steps */
  stepCount: number;
  
  /** Test code */
  code: string;
  
  /** Allure metadata */
  allureMetadata: {
    epic: string;
    feature: string;
    story: string;
    severity: AllureSeverity;
    description: string;
  };
  
  /** Generation timestamp */
  generatedAt: Date;
}

/**
 * Cross-service integration scenario
 */
export interface CrossServiceScenario {
  /** Scenario identifier */
  scenarioId: string;
  
  /** Scenario title */
  title: string;
  
  /** Description */
  description: string;
  
  /** Source service */
  sourceService: ServiceName;
  
  /** Target service */
  targetService: ServiceName;
  
  /** Integration flow */
  flow: FlowStep[];
  
  /** Expected result */
  expectedResult: string;
  
  /** Error handling */
  errorHandling?: {
    /** Expected error scenarios */
    scenarios: {
      description: string;
      triggerCondition: string;
      expectedErrorCode: string;
      expectedErrorMessage: string;
    }[];
  };
}

/**
 * Performance flow test configuration
 */
export interface PerformanceFlowConfig {
  /** Flow identifier */
  flowId: string;
  
  /** Performance thresholds */
  thresholds: {
    /** Max response time (ms) */
    maxResponseTime: number;
    /** Max total duration (ms) */
    maxTotalDuration: number;
    /** Min throughput (requests/sec) */
    minThroughput?: number;
  };
  
  /** Load configuration */
  load?: {
    /** Number of concurrent users */
    concurrentUsers: number;
    /** Ramp-up time (seconds) */
    rampUp: number;
    /** Test duration (seconds) */
    duration: number;
  };
  
  /** Metrics to collect */
  metrics: ('response-time' | 'throughput' | 'error-rate' | 'latency')[];
}

/**
 * Flow test suite (collection of related flows)
 */
export interface FlowTestSuite {
  /** Suite identifier */
  suiteId: string;
  
  /** Suite title */
  title: string;
  
  /** Suite description */
  description: string;
  
  /** Epic this suite belongs to */
  epic: string;
  
  /** User journeys in this suite */
  journeys: UserJourney[];
  
  /** API orchestration tests */
  orchestrationTests: ApiOrchestrationTest[];
  
  /** Cross-service scenarios */
  crossServiceScenarios: CrossServiceScenario[];
  
  /** Performance flows */
  performanceFlows?: PerformanceFlowConfig[];
  
  /** Suite-level setup */
  setup?: {
    /** Services to start */
    services: ServiceName[];
    /** Initial test data */
    testData?: Record<string, unknown>;
  };
  
  /** Suite-level teardown */
  teardown?: {
    /** Services to stop */
    services: ServiceName[];
    /** Data cleanup */
    cleanup?: string[];
  };
}

/**
 * Flow test generation options
 */
export interface FlowTestGenerationOptions {
  /** Preferred E2E framework */
  framework: E2EFramework;
  
  /** Output directory for generated tests */
  outputDir: string;
  
  /** Include performance tests */
  includePerformance?: boolean;
  
  /** Include error scenarios */
  includeErrorHandling?: boolean;
  
  /** Generate Docker Compose for services */
  generateDockerCompose?: boolean;
  
  /** Test data generation strategy */
  testDataStrategy?: 'inline' | 'fixtures' | 'api-generated';
  
  /** Parallelization support */
  parallel?: boolean;
  
  /** Maximum test duration (ms) */
  maxTestDuration?: number;
}
