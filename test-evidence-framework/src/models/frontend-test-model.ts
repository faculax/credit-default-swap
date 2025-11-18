/**
 * Frontend Test Model
 * 
 * TypeScript interfaces for generating React Testing Library tests
 * with MSW (Mock Service Worker) integration and Allure reporting.
 */

/**
 * Allure severity levels
 */
export type AllureSeverity = 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial';

/**
 * Types of frontend tests that can be generated
 */
export type FrontendTestType = 
  | 'component'    // React component rendering and interaction tests
  | 'hook'         // Custom React hook tests
  | 'form'         // Form validation and submission tests
  | 'integration'  // Component integration with API calls
  | 'accessibility'; // A11y tests with jest-axe

/**
 * Template for generating frontend tests
 */
export interface FrontendTestTemplate {
  name: string;
  testType: FrontendTestType;
  componentPath: string;       // Path to component/hook file
  useMSW: boolean;              // Whether to include MSW handlers
  useUserEvent: boolean;        // Whether to use @testing-library/user-event
  useAxe: boolean;              // Whether to include accessibility tests
  imports: string[];            // Additional imports needed
  setupCode?: string;           // Setup code before tests
  teardownCode?: string;        // Cleanup code after tests
}

/**
 * MSW (Mock Service Worker) handler configuration
 */
export interface MSWHandler {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  path: string;                 // API endpoint path (e.g., '/api/cds-trades/:id')
  responseStatus: number;       // HTTP status code (200, 404, 500, etc.)
  responseBody: unknown;        // Mock response data
  delay?: number;               // Optional delay in milliseconds
  description: string;          // Description of what this handler mocks
}

/**
 * React component test configuration
 */
export interface ComponentTest {
  componentName: string;
  componentPath: string;
  props: Record<string, unknown>;     // Default props for rendering
  testCases: ComponentTestCase[];
  mswHandlers?: MSWHandler[];
  allureMetadata: {
    epic: string;
    feature: string;
    story: string;
    severity: AllureSeverity;
    owner?: string;
    tags?: string[];
  };
}

/**
 * Individual component test case
 */
export interface ComponentTestCase {
  description: string;
  testType: 'render' | 'interaction' | 'state-change' | 'api-call' | 'accessibility';
  interactions?: UserInteraction[];    // User events to simulate
  assertions: TestAssertion[];         // Expected outcomes
  setup?: string;                      // Setup code for this specific test
}

/**
 * User interaction to simulate in tests
 */
export interface UserInteraction {
  action: 'click' | 'type' | 'select' | 'hover' | 'focus' | 'blur';
  target: string;                      // Query selector or role
  value?: string | number;             // Value for type/select actions
  delay?: number;                      // Delay before action
}

/**
 * Test assertion configuration
 */
export interface TestAssertion {
  type: 'text-content' | 'element-exists' | 'element-not-exists' | 'attribute' | 'class' | 'api-called' | 'no-a11y-violations';
  target?: string;                     // Query selector or role
  expected: unknown;                   // Expected value
  message?: string;                    // Custom assertion message
}

/**
 * React hook test configuration
 */
export interface HookTest {
  hookName: string;
  hookPath: string;
  initialProps?: Record<string, unknown>;
  testCases: HookTestCase[];
  allureMetadata: {
    epic: string;
    feature: string;
    story: string;
    severity: AllureSeverity;
  };
}

/**
 * Individual hook test case
 */
export interface HookTestCase {
  description: string;
  actions?: HookAction[];              // Actions to perform on hook result
  assertions: HookAssertion[];         // Expected hook state/return values
}

/**
 * Action to perform on hook result
 */
export interface HookAction {
  type: 'call-function' | 'update-props' | 'wait-for-next-update';
  functionName?: string;               // Name of function to call
  args?: unknown[];                    // Arguments to pass
  newProps?: Record<string, unknown>;  // New props to pass to hook
}

/**
 * Hook assertion configuration
 */
export interface HookAssertion {
  type: 'return-value' | 'state-value' | 'error-thrown' | 'async-resolved';
  path?: string;                       // Path to value in hook result (e.g., 'current.data')
  expected: unknown;                   // Expected value
}

/**
 * Form test configuration
 */
export interface FormTest {
  formComponentName: string;
  formComponentPath: string;
  fields: FormField[];
  validationRules: ValidationRule[];
  submitHandler: {
    apiEndpoint: string;
    method: 'POST' | 'PUT' | 'PATCH';
    successResponse: unknown;
    errorResponse: unknown;
  };
  testCases: FormTestCase[];
  allureMetadata: {
    epic: string;
    feature: string;
    story: string;
    severity: AllureSeverity;
  };
}

/**
 * Form field configuration
 */
export interface FormField {
  name: string;
  type: 'text' | 'email' | 'password' | 'number' | 'select' | 'checkbox' | 'radio' | 'textarea' | 'date';
  label: string;
  required: boolean;
  defaultValue?: unknown;
  options?: Array<{ label: string; value: string | number }>; // For select/radio
}

/**
 * Form validation rule
 */
export interface ValidationRule {
  field: string;
  rule: 'required' | 'email' | 'min-length' | 'max-length' | 'pattern' | 'custom';
  value?: number | string;             // Min/max length or regex pattern
  message: string;                     // Error message to display
}

/**
 * Form test case
 */
export interface FormTestCase {
  description: string;
  testType: 'validation' | 'submission' | 'error-handling' | 'reset';
  formData?: Record<string, unknown>;  // Data to fill in form
  expectedValidation?: {
    isValid: boolean;
    errors?: Record<string, string>;   // Field name -> error message
  };
  expectedApiCall?: {
    called: boolean;
    payload?: unknown;
  };
  assertions: TestAssertion[];
}

/**
 * Generated frontend test file
 */
export interface GeneratedFrontendTest {
  filePath: string;                    // Output path for test file
  testType: FrontendTestType;
  content: string;                     // Generated test code
  imports: string[];                   // All imports used
  mswHandlers: MSWHandler[];           // MSW handlers included
  testCount: number;                   // Number of test cases
  metadata: {
    generatedAt: Date;
    storyId: string;
    componentName?: string;
    hookName?: string;
  };
}

/**
 * Frontend test generation context
 */
export interface FrontendTestContext {
  storyId: string;
  epicTitle: string;
  componentTests: ComponentTest[];
  hookTests: HookTest[];
  formTests: FormTest[];
  sharedMSWHandlers: MSWHandler[];     // Handlers shared across tests
  testDatasets: string[];              // References to test datasets from registry
}

/**
 * React Testing Library query methods
 */
export type RTLQuery = 
  | 'getByRole'
  | 'getByLabelText'
  | 'getByPlaceholderText'
  | 'getByText'
  | 'getByDisplayValue'
  | 'getByAltText'
  | 'getByTitle'
  | 'getByTestId'
  | 'queryByRole'
  | 'queryByLabelText'
  | 'queryByText'
  | 'findByRole'
  | 'findByLabelText'
  | 'findByText';

/**
 * Test utilities configuration
 */
export interface TestUtilities {
  customRenderPath?: string;           // Path to custom render function (with providers)
  setupFilesPath?: string;             // Path to Jest setup files
  mswServerPath?: string;              // Path to MSW server setup
  testUtilsPath?: string;              // Path to shared test utilities
}
