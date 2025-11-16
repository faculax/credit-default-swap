/**
 * Test Type Label Schema Integration
 * 
 * This module integrates with the shared test-type-schema.json to ensure
 * consistent test labeling across frontend and backend services.
 * 
 * Schema location: schema/test-type-schema.json
 */

/**
 * Valid test types as defined in the shared schema.
 * @see schema/test-type-schema.json - testTypes[].id
 */
type TestType = 'unit' | 'integration' | 'contract' | 'e2e' | 'performance' | 'security';

/**
 * Valid service identifiers as defined in the shared schema.
 * @see schema/test-type-schema.json - microservices[].id
 */
type Service = 'frontend' | 'backend' | 'gateway' | 'risk-engine';

/**
 * Valid severity levels for test prioritization.
 * @see schema/test-type-schema.json - validationRules.severityEnum
 */
type Severity = 'trivial' | 'minor' | 'normal' | 'critical' | 'blocker';

/**
 * Story ID pattern validation per schema.
 * Format: PREFIX-NUMBER or PREFIX-NUMBER.NUMBER
 * Examples: UTS-2.3, PROJ-123, EPIC-5
 * @see schema/test-type-schema.json - validationRules.storyIdPattern
 */
interface StoryIdOptions {
  /** Story ID following pattern: ^(UTS|PROJ|EPIC)-[0-9]+(\.[0-9]+)?$ */
  storyId: string;
  
  /** Test type classification - must match schema testTypes[].id */
  testType?: TestType;
  
  /** Service where test runs - must match schema microservices[].id */
  service?: Service;
  
  /** Optional microservice sub-component identifier */
  microservice?: string;
  
  /** Optional severity level for test prioritization */
  severity?: Severity;
  
  /** Optional epic label for grouping in Allure reports */
  epic?: string;
  
  /** Optional feature label for categorization */
  feature?: string;
}

type TestFn = jest.ProvidesCallback | jest.DoneCallback | (() => void | Promise<void>);

/**
 * Validates story ID format against schema pattern.
 * @param storyId - Story ID to validate
 * @returns true if valid, false otherwise
 */
function isValidStoryId(storyId: string): boolean {
  const pattern = /^(UTS|PROJ|EPIC)-\d+(\.\d+)?$/;
  return pattern.test(storyId);
}

/**
 * Wraps a Jest test with Allure story traceability labels.
 * 
 * This function enriches test names with metadata tags that are extracted
 * by Allure reporters to generate comprehensive test reports with traceability.
 *
 * @param options - Story metadata options (storyId required, others optional)
 * @returns A function that wraps the test with enriched metadata
 * 
 * @example
 * ```typescript
 * withStoryId({ 
 *   storyId: 'UTS-2.3', 
 *   testType: 'unit', 
 *   service: 'frontend',
 *   severity: 'normal'
 * })('renders component correctly', () => {
 *   // test implementation
 * });
 * ```
 * 
 * @see schema/test-type-schema.json for valid values
 */
export function withStoryId(options: StoryIdOptions) {
  const { 
    storyId, 
    testType = 'unit', 
    service = 'frontend', 
    microservice = '',
    severity,
    epic,
    feature
  } = options;

  // Validate story ID format (development-time check)
  if (process.env.NODE_ENV !== 'production' && !isValidStoryId(storyId)) {
    console.warn(
      `[testHelpers] Invalid story ID format: "${storyId}". ` +
      `Expected pattern: ^(UTS|PROJ|EPIC)-[0-9]+(\\.[0-9]+)?$`
    );
  }

  return (testName: string, testFn: TestFn, timeout?: number) => {
    // Auto-generate feature label based on service for Allure Behaviors grouping
    const defaultFeature = `Frontend Service`;
    
    // Determine Epic based on test type for 3-level hierarchy in Allure Behaviors
    let defaultEpic: string;
    if (testType === 'unit') {
      defaultEpic = 'Unit Tests';
    } else if (testType === 'integration') {
      defaultEpic = 'Integration Tests';
    } else if (testType === 'e2e') {
      defaultEpic = 'E2E Tests';
    } else {
      defaultEpic = `${testType} Tests`;
    }
    
    // Build metadata tags for Allure extraction
    const tags: string[] = [
      `[story:${storyId}]`,
      `[testType:${testType}]`,
      `[service:${service}]`,
      `[feature:${feature || defaultFeature}]`,
      `[epic:${epic || defaultEpic}]`
    ];
    
    if (microservice) tags.push(`[microservice:${microservice}]`);
    if (severity) tags.push(`[severity:${severity}]`);
    
    const enrichedTestName = `${testName} ${tags.join(' ')}`;
    
    // eslint-disable-next-line no-undef
    return test(enrichedTestName, testFn as jest.ProvidesCallback, timeout);
  };
}

/**
 * Applies story metadata to a Jest describe block.
 * Useful for grouping multiple tests under the same story context.
 * 
 * @param options - Story metadata options (storyId required, others optional)
 * @param suiteName - Name of the test suite
 * @param suiteFn - Suite function containing tests
 * 
 * @example
 * ```typescript
 * describeStory(
 *   { storyId: 'UTS-2.3', testType: 'unit', service: 'frontend' },
 *   'Component Rendering',
 *   () => {
 *     withStoryId({ storyId: 'UTS-2.3', testType: 'unit', service: 'frontend' })(
 *       'should render with default props',
 *       () => {
 *         // test implementation
 *       }
 *     );
 *   }
 * );
 * ```
 * 
 * @see schema/test-type-schema.json for valid values
 */
export function describeStory(options: StoryIdOptions, suiteName: string, suiteFn: () => void) {
  const { 
    storyId, 
    testType = 'unit', 
    service = 'frontend', 
    microservice = '',
    severity,
    epic,
    feature
  } = options;
  
  // Validate story ID format (development-time check)
  if (process.env.NODE_ENV !== 'production' && !isValidStoryId(storyId)) {
    console.warn(
      `[testHelpers] Invalid story ID format: "${storyId}". ` +
      `Expected pattern: ^(UTS|PROJ|EPIC)-[0-9]+(\\.[0-9]+)?$`
    );
  }
  
  // Auto-generate feature label based on service for Allure Behaviors grouping
  const defaultFeature = `Frontend Service`;
  
  // Determine Epic based on test type for 3-level hierarchy in Allure Behaviors
  let defaultEpic: string;
  if (testType === 'unit') {
    defaultEpic = 'Unit Tests';
  } else if (testType === 'integration') {
    defaultEpic = 'Integration Tests';
  } else if (testType === 'e2e') {
    defaultEpic = 'E2E Tests';
  } else {
    defaultEpic = `${testType} Tests`;
  }
  
  // Build metadata tags for Allure extraction
  const tags: string[] = [
    `[story:${storyId}]`,
    `[testType:${testType}]`,
    `[service:${service}]`,
    `[feature:${feature || defaultFeature}]`,
    `[epic:${epic || defaultEpic}]`
  ];
  
  if (microservice) tags.push(`[microservice:${microservice}]`);
  if (severity) tags.push(`[severity:${severity}]`);
  
  const enrichedSuiteName = `${suiteName} ${tags.join(' ')}`;
  
  // eslint-disable-next-line no-undef
  describe(enrichedSuiteName, suiteFn);
}

/**
 * Type guard to check if a test type is valid per schema.
 * @param type - Test type string to validate
 * @returns true if valid test type
 */
export function isValidTestType(type: string): type is TestType {
  return ['unit', 'integration', 'contract', 'e2e', 'performance', 'security'].includes(type);
}

/**
 * Type guard to check if a service is valid per schema.
 * @param service - Service string to validate
 * @returns true if valid service
 */
export function isValidService(service: string): service is Service {
  return ['frontend', 'backend', 'gateway', 'risk-engine'].includes(service);
}

/**
 * Type guard to check if a severity is valid per schema.
 * @param severity - Severity string to validate
 * @returns true if valid severity
 */
export function isValidSeverity(severity: string): severity is Severity {
  return ['trivial', 'minor', 'normal', 'critical', 'blocker'].includes(severity);
}

// Export types for use in test files
export type { TestType, Service, Severity, StoryIdOptions };

