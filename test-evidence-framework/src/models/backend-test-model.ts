/**
 * Type definitions for Backend Test Generation (Story 20.3)
 */

import { StoryModel, ServiceName, TestType } from './story-model';

/**
 * Backend test template types
 */
export type BackendTestTemplate = 
  | 'service'         // Service layer tests (business logic)
  | 'repository'      // Repository/DAO tests (database)
  | 'controller'      // Controller/REST API tests
  | 'integration'     // Integration tests (multi-layer)
  | 'unit';           // Pure unit tests

/**
 * Test framework annotations
 */
export interface AllureAnnotations {
  epic?: string;        // @Epic
  feature?: string;     // @Feature
  story?: string;       // @Story
  severity?: 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial';
  tmsLink?: string;     // Link to test management system
  issue?: string;       // Link to issue tracker
}

/**
 * Test method configuration
 */
export interface TestMethod {
  methodName: string;           // camelCase method name
  displayName: string;          // Human-readable test name
  description: string;          // Test description
  testType: 'unit' | 'integration' | 'api';
  acceptanceCriteria?: string;  // AC reference (e.g., "AC1")
  datasetPath?: string;         // Path to test dataset
  expectedOutcome: string;      // Expected result
  assertions: string[];         // List of assertions to generate
  setupCode?: string[];         // Additional setup code
  teardownCode?: string[];      // Additional teardown code
}

/**
 * Generated test class structure
 */
export interface GeneratedTestClass {
  className: string;            // Test class name
  packageName: string;          // Java package
  imports: string[];            // Import statements
  annotations: AllureAnnotations;
  classJavadoc: string;         // Class-level documentation
  fields: TestField[];          // Class fields
  setupMethod?: TestSetupMethod;
  teardownMethod?: TestSetupMethod;
  testMethods: GeneratedTestMethod[];
  sourceStory: StoryModel;      // Original story
}

/**
 * Test class field
 */
export interface TestField {
  name: string;
  type: string;
  annotation?: string;          // e.g., @Autowired, @Mock
  initialization?: string;      // Initial value
}

/**
 * Setup/teardown method
 */
export interface TestSetupMethod {
  methodName: string;
  annotation: '@BeforeEach' | '@AfterEach' | '@BeforeAll' | '@AfterAll';
  code: string[];
}

/**
 * Generated test method
 */
export interface GeneratedTestMethod {
  config: TestMethod;
  annotations: AllureAnnotations;
  code: string;                 // Complete method body
  javadoc: string;              // Method documentation
}

/**
 * Test generation configuration
 */
export interface BackendTestGenerationConfig {
  outputDir: string;            // Output directory for tests
  basePackage: string;          // Base Java package
  testSuffix: string;           // Suffix for test classes (default: "Test")
  useAllure: boolean;           // Include Allure annotations
  useAssertJ: boolean;          // Use AssertJ assertions
  useMockito: boolean;          // Use Mockito for mocking
  generateJavadoc: boolean;     // Generate JavaDoc comments
  datasetRegistryPath: string;  // Path to dataset registry.json
  templatesDir?: string;        // Custom templates directory
}

/**
 * Template context for code generation
 */
export interface TemplateContext {
  story: StoryModel;
  template: BackendTestTemplate;
  className: string;
  packageName: string;
  testMethods: TestMethod[];
  datasets: string[];           // Dataset paths to use
  config: BackendTestGenerationConfig;
}

/**
 * Test generation result
 */
export interface GenerationResult {
  success: boolean;
  testClass: GeneratedTestClass;
  filePath: string;
  errors: string[];
  warnings: string[];
}

/**
 * Service-specific test strategy
 */
export interface ServiceTestStrategy {
  service: ServiceName;
  templates: BackendTestTemplate[];
  testTypes: TestType[];
  mockDependencies: string[];   // Dependencies to mock
  requiredDatasets: string[];   // Dataset types needed
}

/**
 * Dataset reference in test
 */
export interface DatasetReference {
  path: string;                 // Path to dataset file
  variable: string;             // Variable name in test
  type: string;                 // Java type
  loadMethod: 'DatasetLoader.load' | 'TestDataRegistry.getDataset';
}

/**
 * Assertion template
 */
export interface AssertionTemplate {
  type: 'equality' | 'notNull' | 'contains' | 'size' | 'custom';
  actual: string;               // Actual value expression
  expected?: string;            // Expected value expression
  message?: string;             // Assertion message
  assertJMethod?: string;       // AssertJ method (e.g., "assertThat")
}
