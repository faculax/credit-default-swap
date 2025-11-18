/**
 * Validation Model
 * 
 * Comprehensive type definitions for test code validation and quality assessment.
 * Supports syntax validation, compilation checks, quality rules, and best practices.
 * 
 * @module validation-model
 */

import type { TestType } from './story-model.js';

/**
 * Test framework types
 */
export type TestFramework = 
  | 'junit'          // Java JUnit
  | 'jest'           // JavaScript/TypeScript Jest
  | 'playwright'     // E2E Playwright
  | 'cypress'        // E2E Cypress
  | 'vitest'         // Vitest
  | 'mocha';         // Mocha

/**
 * Validation rule types
 */
export type ValidationRuleType =
  | 'syntax'           // Syntax correctness
  | 'compilation'      // TypeScript/Java compilation
  | 'assertion'        // Test has assertions
  | 'coverage'         // Test covers requirements
  | 'naming'           // Test naming conventions
  | 'structure'        // Test structure/organization
  | 'best-practice'    // Framework best practices
  | 'security'         // Security concerns
  | 'performance';     // Performance issues

/**
 * Validation severity levels
 */
export type ValidationSeverity =
  | 'error'    // Must fix - test is invalid
  | 'warning'  // Should fix - test has issues
  | 'info'     // Optional - suggestions
  | 'suggestion'; // Nice to have

/**
 * Validation rule definition
 */
export interface ValidationRule {
  /** Unique rule identifier */
  id: string;
  
  /** Rule type category */
  type: ValidationRuleType;
  
  /** Severity level */
  severity: ValidationSeverity;
  
  /** Human-readable rule name */
  name: string;
  
  /** Detailed description */
  description: string;
  
  /** Test frameworks this rule applies to */
  frameworks?: TestFramework[];
  
  /** Test types this rule applies to */
  testTypes?: TestType[];
  
  /** Whether this rule is enabled */
  enabled: boolean;
  
  /** Auto-fix available */
  autoFixable: boolean;
}

/**
 * Single validation issue found
 */
export interface ValidationIssue {
  /** Rule that triggered this issue */
  ruleId: string;
  
  /** Issue severity */
  severity: ValidationSeverity;
  
  /** Issue message */
  message: string;
  
  /** File path where issue was found */
  filePath: string;
  
  /** Line number (1-based) */
  line?: number;
  
  /** Column number (1-based) */
  column?: number;
  
  /** Code snippet showing the issue */
  snippet?: string;
  
  /** Suggested fix */
  suggestion?: string;
  
  /** Auto-fix available */
  autoFixable: boolean;
}

/**
 * Validation result for a single test file
 */
export interface ValidationResult {
  /** Test file path */
  filePath: string;
  
  /** Test framework */
  framework: TestFramework;
  
  /** Test type */
  testType: TestType;
  
  /** Overall validation status */
  valid: boolean;
  
  /** Issues found */
  issues: ValidationIssue[];
  
  /** Code quality metrics */
  metrics: CodeQualityMetrics;
  
  /** Timestamp of validation */
  timestamp: Date;
  
  /** Validation duration (ms) */
  duration: number;
}

/**
 * Code quality metrics
 */
export interface CodeQualityMetrics {
  /** Total lines of code */
  linesOfCode: number;
  
  /** Number of test cases */
  testCaseCount: number;
  
  /** Number of assertions */
  assertionCount: number;
  
  /** Assertions per test case */
  assertionsPerTest: number;
  
  /** Has setup/teardown */
  hasSetup: boolean;
  hasTeardown: boolean;
  
  /** Code complexity (cyclomatic) */
  complexity?: number;
  
  /** Test coverage percentage */
  coverage?: number;
  
  /** Number of incomplete comments (task markers in code) */
  // eslint-disable-next-line sonarjs/todo-tag
  incompleteCommentCount: number;
  
  /** Number of mock/stub usages */
  mockCount: number;
  
  /** Estimated test execution time (ms) */
  estimatedDuration?: number;
}

/**
 * Syntax validation result
 */
export interface SyntaxValidation {
  /** Syntax is valid */
  valid: boolean;
  
  /** Syntax errors found */
  errors: SyntaxError[];
  
  /** Parser used */
  parser: 'typescript' | 'java' | 'javascript';
}

/**
 * Syntax error detail
 */
export interface SyntaxError {
  /** Error message */
  message: string;
  
  /** Line number */
  line: number;
  
  /** Column number */
  column: number;
  
  /** Error code */
  code?: string;
}

/**
 * Compilation validation result
 */
export interface CompilationValidation {
  /** Compilation succeeded */
  compiled: boolean;
  
  /** Compilation errors */
  errors: CompilationError[];
  
  /** Compilation warnings */
  warnings: CompilationError[];
  
  /** Compiler used */
  compiler: 'tsc' | 'javac' | 'babel';
  
  /** Compilation output */
  output?: string;
}

/**
 * Compilation error detail
 */
export interface CompilationError {
  /** Error message */
  message: string;
  
  /** File path */
  file: string;
  
  /** Line number */
  line: number;
  
  /** Column number */
  column: number;
  
  /** Error code */
  code: string;
  
  /** Severity */
  severity: 'error' | 'warning';
}

/**
 * Test quality assessment
 */
export interface QualityAssessment {
  /** Overall quality score (0-100) */
  score: number;
  
  /** Quality grade */
  grade: 'A' | 'B' | 'C' | 'D' | 'F';
  
  /** Assessment breakdown */
  breakdown: {
    syntax: number;
    structure: number;
    assertions: number;
    coverage: number;
    bestPractices: number;
  };
  
  /** Recommendations */
  recommendations: string[];
}

/**
 * Validation report for multiple tests
 */
export interface ValidationReport {
  /** Report ID */
  id: string;
  
  /** Report title */
  title: string;
  
  /** Generation timestamp */
  timestamp: Date;
  
  /** Validation results */
  results: ValidationResult[];
  
  /** Summary statistics */
  summary: ValidationSummary;
  
  /** Overall quality assessment */
  quality: QualityAssessment;
  
  /** Report metadata */
  metadata: {
    totalFiles: number;
    validFiles: number;
    invalidFiles: number;
    totalIssues: number;
    errorCount: number;
    warningCount: number;
    infoCount: number;
    suggestionCount: number;
  };
}

/**
 * Validation summary statistics
 */
export interface ValidationSummary {
  /** Total tests validated */
  totalTests: number;
  
  /** Valid tests */
  validTests: number;
  
  /** Invalid tests */
  invalidTests: number;
  
  /** Total issues found */
  totalIssues: number;
  
  /** Issues by severity */
  issuesBySeverity: {
    error: number;
    warning: number;
    info: number;
    suggestion: number;
  };
  
  /** Issues by type */
  issuesByType: Record<ValidationRuleType, number>;
  
  /** Average quality score */
  averageQualityScore: number;
  
  /** Tests by framework */
  testsByFramework: Record<TestFramework, number>;
  
  /** Tests by type */
  testsByType: Record<TestType, number>;
}

/**
 * Validation configuration
 */
export interface ValidationConfig {
  /** Enabled validation rules */
  rules: ValidationRule[];
  
  /** Run syntax validation */
  checkSyntax: boolean;
  
  /** Run compilation validation */
  checkCompilation: boolean;
  
  /** Run quality checks */
  checkQuality: boolean;
  
  /** Fail on warnings */
  failOnWarnings: boolean;
  
  /** Fail on info issues */
  failOnInfo: boolean;
  
  /** Auto-fix issues */
  autoFix: boolean;
  
  /** TypeScript config path */
  tsconfigPath?: string;
  
  /** Java compiler options */
  javacOptions?: string[];
  
  /** Quality score threshold */
  qualityThreshold: number;
}

/**
 * Validator options
 */
export interface ValidatorOptions {
  /** Validation configuration */
  config: ValidationConfig;
  
  /** Enable verbose logging */
  verbose: boolean;
  
  /** Generate detailed report */
  detailed: boolean;
  
  /** Output format */
  format: 'json' | 'html' | 'markdown' | 'console';
}

/**
 * Framework-specific validation rules
 */
export interface FrameworkValidationRules {
  /** Framework name */
  framework: TestFramework;
  
  /** Required imports */
  requiredImports: string[];
  
  /** Required test structure */
  requiredStructure: {
    hasDescribe?: boolean;
    hasTest?: boolean;
    hasExpect?: boolean;
    hasAssertion?: boolean;
  };
  
  /** Naming patterns */
  namingPatterns: {
    testFile: RegExp;
    testCase: RegExp;
    testSuite: RegExp;
  };
  
  /** Best practices */
  bestPractices: string[];
  
  /** Anti-patterns to avoid */
  antiPatterns: string[];
}

/**
 * Assertion validation
 */
export interface AssertionValidation {
  /** Has assertions */
  hasAssertions: boolean;
  
  /** Number of assertions */
  count: number;
  
  /** Assertion types used */
  types: string[];
  
  /** Missing assertions */
  missing: string[];
  
  /** Assertions per test case */
  perTestCase: number;
}

/**
 * Coverage validation
 */
export interface CoverageValidation {
  /** Requirements covered */
  requirementsCovered: string[];
  
  /** Requirements not covered */
  requirementsMissing: string[];
  
  /** Coverage percentage */
  coveragePercent: number;
  
  /** User story ID */
  storyId: string;
  
  /** Acceptance criteria covered */
  acceptanceCriteriaCovered: number;
  
  /** Total acceptance criteria */
  totalAcceptanceCriteria: number;
}

/**
 * Best practice validation
 */
export interface BestPracticeValidation {
  /** Best practices followed */
  followed: string[];
  
  /** Best practices violated */
  violated: string[];
  
  /** Score (0-100) */
  score: number;
  
  /** Recommendations */
  recommendations: string[];
}

/**
 * Auto-fix result
 */
export interface AutoFixResult {
  /** Issue was fixed */
  fixed: boolean;
  
  /** Original code */
  original: string;
  
  /** Fixed code */
  fixed_code: string;
  
  /** Fix description */
  description: string;
  
  /** Rule ID that triggered fix */
  ruleId: string;
}

/**
 * Validation progress callback
 */
export type ValidationProgressCallback = (progress: ValidationProgress) => void;

/**
 * Validation progress
 */
export interface ValidationProgress {
  /** Current file being validated */
  currentFile: string;
  
  /** Files processed */
  filesProcessed: number;
  
  /** Total files */
  totalFiles: number;
  
  /** Percentage complete */
  percentComplete: number;
  
  /** Issues found so far */
  issuesFound: number;
  
  /** Estimated time remaining (ms) */
  estimatedTimeRemaining?: number;
}

/**
 * Generated validation report output
 */
export interface GeneratedValidationReport {
  /** Report file path */
  filePath: string;
  
  /** Report format */
  format: 'json' | 'html' | 'markdown';
  
  /** Report content */
  content: string;
  
  /** Report metadata */
  metadata: ValidationReport;
  
  /** Generation timestamp */
  timestamp: Date;
}
