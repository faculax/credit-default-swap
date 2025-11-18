/**
 * Code Validator
 * 
 * Validates generated test code for syntax, compilation, quality, and best practices.
 * Provides comprehensive validation reports and auto-fix capabilities.
 * 
 * @module code-validator
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import * as crypto from 'node:crypto';
import type {
  ValidationRule,
  ValidationIssue,
  ValidationResult,
  ValidationReport,
  ValidationConfig,
  ValidationRuleType,
  CodeQualityMetrics,
  QualityAssessment,
  ValidationSummary,
  TestFramework,
  AssertionValidation,
  ValidationProgressCallback,
} from '../models/validation-model.js';
import type { TestType } from '../models/story-model.js';

/**
 * Code Validator Class
 * 
 * Validates test code against quality rules and best practices
 */
export class CodeValidator {
  private readonly config: ValidationConfig;
  private readonly rules: Map<string, ValidationRule>;
  private progressCallback?: ValidationProgressCallback;

  // Performance optimizations - pre-compiled patterns
  private readonly assertionPatterns: Record<string, RegExp> = {
    junit: /\b(assert(?:True|False|Equals|NotNull|Null|Throws)|verify|expect)\b/g,
    jest: /\b(expect|assert|toBe|toEqual|toMatch|toContain|toThrow)\b/g,
    playwright: /\b(expect|toBeVisible|toHaveText|toContainText|toBeEnabled)\b/g,
    cypress: /\b(expect|should|cy\.(?:get|contains).*should)\b/g,
    vitest: /\b(expect|assert|toBe|toEqual|toMatch|toContain|toThrow)\b/g,
    mocha: /\b(expect|assert|should)\b/g,
  };

  private readonly importPatterns: Record<string, RegExp> = {
    junit: /import\s+(?:static\s+)?org\.junit\.jupiter\.api\./,
    jest: /import\s+.*from\s+['"](?:@jest\/globals|jest)['"]/,
    playwright: /import\s+.*from\s+['"]@playwright\/test['"]/,
    cypress: /\/\/\/\s*<reference\s+types=["']cypress["']\s*\/>/,
    vitest: /import\s+.*from\s+['"]vitest['"]/,
    mocha: /import\s+.*from\s+['"]mocha['"]/,
  };

  private readonly testStructurePatterns: Record<string, Record<string, RegExp>> = {
    junit: {
      testClass: /@Test|@ParameterizedTest|@RepeatedTest/,
      setup: /@BeforeEach|@BeforeAll/,
      teardown: /@AfterEach|@AfterAll/,
    },
    jest: {
      describe: /\bdescribe\s*\(/,
      test: /\b(?:test|it)\s*\(/,
      beforeEach: /\bbeforeEach\s*\(/,
      afterEach: /\bafterEach\s*\(/,
    },
    playwright: {
      testDescribe: /test\.describe\s*\(/,
      test: /test\s*\(/,
      beforeEach: /test\.beforeEach\s*\(/,
      afterEach: /test\.afterEach\s*\(/,
    },
    cypress: {
      describe: /\bdescribe\s*\(/,
      it: /\bit\s*\(/,
      beforeEach: /\bbeforeEach\s*\(/,
      afterEach: /\bafterEach\s*\(/,
    },
    vitest: {
      describe: /\bdescribe\s*\(/,
      test: /\b(?:test|it)\s*\(/,
      beforeEach: /\bbeforeEach\s*\(/,
      afterEach: /\bafterEach\s*\(/,
    },
    mocha: {
      describe: /\bdescribe\s*\(/,
      it: /\bit\s*\(/,
      beforeEach: /\bbeforeEach\s*\(/,
      afterEach: /\bafterEach\s*\(/,
    },
  };

  // Memoization caches
  private readonly metricsCache = new Map<string, CodeQualityMetrics>();
  private readonly frameworkCache = new Map<string, TestFramework>();

  constructor(config?: Partial<ValidationConfig>) {
    this.config = {
      rules: this.getDefaultRules(),
      checkSyntax: true,
      checkCompilation: true,
      checkQuality: true,
      failOnWarnings: false,
      failOnInfo: false,
      autoFix: false,
      qualityThreshold: 70,
      ...config,
    };
    
    this.rules = new Map(this.config.rules.map(rule => [rule.id, rule]));
  }

  /**
   * Validate a single test file
   */
  public async validate(filePath: string): Promise<ValidationResult> {
    const startTime = Date.now();
    const content = await fs.readFile(filePath, 'utf-8');
    const framework = this.detectFramework(content, filePath);
    const testType = this.detectTestType(content, filePath);
    const issues: ValidationIssue[] = [];

    // Syntax validation
    if (this.config.checkSyntax) {
      const syntaxIssues = await this.validateSyntax(filePath, content, framework);
      issues.push(...syntaxIssues);
    }

    // Compilation validation
    if (this.config.checkCompilation && issues.length === 0) {
      const compilationIssues = await this.validateCompilation(filePath, content, framework);
      issues.push(...compilationIssues);
    }

    // Quality checks
    if (this.config.checkQuality) {
      const qualityIssues = await this.validateQuality(filePath, content, framework, testType);
      issues.push(...qualityIssues);
    }

    // Calculate metrics
    const metrics = await this.calculateMetrics(content, framework, testType);

    const duration = Date.now() - startTime;
    const valid = !issues.some(i => i.severity === 'error');

    return {
      filePath,
      framework,
      testType,
      valid,
      issues,
      metrics,
      timestamp: new Date(),
      duration,
    };
  }

  /**
   * Validate multiple test files
   */
  public async validateAll(filePaths: string[]): Promise<ValidationReport> {
    const results: ValidationResult[] = [];
    const totalFiles = filePaths.length;
    
    for (let i = 0; i < filePaths.length; i++) {
      if (this.progressCallback) {
        this.progressCallback({
          currentFile: filePaths[i],
          filesProcessed: i,
          totalFiles,
          percentComplete: Math.round((i / totalFiles) * 100),
          issuesFound: results.reduce((sum, r) => sum + r.issues.length, 0),
        });
      }

      try {
        const result = await this.validate(filePaths[i]);
        results.push(result);
      } catch (error) {
        console.error(`Failed to validate ${filePaths[i]}:`, error);
      }
    }

    return this.generateReport(results);
  }

  /**
   * Set progress callback
   */
  public onProgress(callback: ValidationProgressCallback): void {
    this.progressCallback = callback;
  }

  /**
   * Validate syntax
   */
  private async validateSyntax(
    filePath: string,
    content: string,
    framework: TestFramework
  ): Promise<ValidationIssue[]> {
    const issues: ValidationIssue[] = [];
    const rule = this.rules.get('syntax-error');
    if (!rule || !rule.enabled) return issues;

    // Check for common syntax issues
    const lines = content.split('\n');
    
    // Unclosed brackets/braces
    const openBraces = (content.match(/{/g) || []).length;
    const closeBraces = (content.match(/}/g) || []).length;
    if (openBraces !== closeBraces) {
      issues.push({
        ruleId: 'syntax-error',
        severity: 'error',
        message: `Mismatched braces: ${openBraces} opening, ${closeBraces} closing`,
        filePath,
        autoFixable: false,
      });
    }

    // Unclosed parentheses
    const openParens = (content.match(/\(/g) || []).length;
    const closeParens = (content.match(/\)/g) || []).length;
    if (openParens !== closeParens) {
      issues.push({
        ruleId: 'syntax-error',
        severity: 'error',
        message: `Mismatched parentheses: ${openParens} opening, ${closeParens} closing`,
        filePath,
        autoFixable: false,
      });
    }

    // Missing semicolons (for Java/TypeScript)
    if (framework === 'junit' || framework === 'jest' || framework === 'playwright') {
      // eslint-disable-next-line unicorn/no-array-for-each
      lines.forEach((line, idx) => {
        const trimmed = line.trim();
        if (
          trimmed.length > 0 &&
          !trimmed.endsWith(';') &&
          !trimmed.endsWith('{') &&
          !trimmed.endsWith('}') &&
          !trimmed.startsWith('//') &&
          !trimmed.startsWith('*') &&
          !trimmed.startsWith('import') &&
          !trimmed.startsWith('export') &&
          /^(const|let|var|return|throw)\s+/.test(trimmed)
        ) {
          issues.push({
            ruleId: 'syntax-error',
            severity: 'warning',
            message: 'Statement may be missing semicolon',
            filePath,
            line: idx + 1,
            snippet: trimmed,
            autoFixable: true,
            suggestion: `${trimmed};`,
          });
        }
      });
    }

    return issues;
  }

  /**
   * Validate compilation (stub - would integrate with tsc/javac in production)
   */
  private async validateCompilation(
    filePath: string,
    content: string,
    framework: TestFramework
  ): Promise<ValidationIssue[]> {
    const issues: ValidationIssue[] = [];
    
    // Check for required imports
    const requiredImports = this.getRequiredImports(framework);
    const hasRequiredImports = requiredImports.every(imp => 
      content.includes(imp) || this.importPatterns[framework]?.test(content)
    );

    if (!hasRequiredImports) {
      issues.push({
        ruleId: 'missing-imports',
        severity: 'error',
        message: `Missing required imports for ${framework}`,
        filePath,
        autoFixable: true,
        suggestion: `Add required imports: ${requiredImports.join(', ')}`,
      });
    }

    return issues;
  }

  /**
   * Validate code quality
   */
  private async validateQuality(
    filePath: string,
    content: string,
    framework: TestFramework,
    testType: TestType
  ): Promise<ValidationIssue[]> {
    const issues: ValidationIssue[] = [];

    // Check assertions
    const assertionValidation = this.validateAssertions(content, framework);
    if (!assertionValidation.hasAssertions) {
      issues.push({
        ruleId: 'missing-assertions',
        severity: 'error',
        message: 'Test has no assertions',
        filePath,
        autoFixable: false,
      });
    } else if (assertionValidation.perTestCase < 1) {
      issues.push({
        ruleId: 'insufficient-assertions',
        severity: 'warning',
        message: `Low assertion count: ${assertionValidation.count} assertions`,
        filePath,
        autoFixable: false,
      });
    }

    // Check test structure
    const hasTestStructure = this.validateTestStructure(content, framework);
    if (!hasTestStructure) {
      issues.push({
        ruleId: 'invalid-structure',
        severity: 'error',
        message: `Missing required test structure for ${framework}`,
        filePath,
        autoFixable: false,
      });
    }

    // Check naming conventions
    const namingIssues = this.validateNaming(filePath, content, framework);
    issues.push(...namingIssues);

    // Check best practices
    const bestPracticeIssues = this.validateBestPractices(content, framework);
    issues.push(...bestPracticeIssues);

    return issues;
  }

  /**
   * Validate assertions
   */
  private validateAssertions(content: string, framework: TestFramework): AssertionValidation {
    const pattern = this.assertionPatterns[framework] || this.assertionPatterns.jest;
    // eslint-disable-next-line unicorn/prefer-regexp-test
    const matches = content.match(pattern) || [];
    const count = matches.length;
    
    // Count test cases
    let testCaseCount = 0;
    if (framework === 'junit') {
      testCaseCount = (content.match(/@Test/g) || []).length;
    } else {
      testCaseCount = (content.match(/\b(?:test|it)\s*\(/g) || []).length;
    }

    return {
      hasAssertions: count > 0,
      count,
      types: [...new Set(matches)],
      missing: count === 0 ? ['Add assertions to verify expected behavior'] : [],
      perTestCase: testCaseCount > 0 ? count / testCaseCount : 0,
    };
  }

  /**
   * Validate test structure
   */
  private validateTestStructure(content: string, framework: TestFramework): boolean {
    const patterns = this.testStructurePatterns[framework];
    if (!patterns) return true;

    // Check for test methods/functions
    if (framework === 'junit') {
      return patterns.testClass.test(content);
    } else {
      return patterns.test?.test(content) || patterns.it?.test(content) || false;
    }
  }

  /**
   * Validate naming conventions
   */
  private validateNaming(
    filePath: string,
    content: string,
    framework: TestFramework
  ): ValidationIssue[] {
    const issues: ValidationIssue[] = [];
    const fileName = path.basename(filePath);

    // Check file naming
    if (framework === 'junit') {
      if (!fileName.endsWith('Test.java') && !fileName.endsWith('IT.java')) {
        issues.push({
          ruleId: 'invalid-test-filename',
          severity: 'warning',
          message: 'JUnit test files should end with Test.java or IT.java',
          filePath,
          autoFixable: false,
        });
      }
    } else if (framework === 'jest' || framework === 'playwright') {
      if (!fileName.includes('.test.') && !fileName.includes('.spec.')) {
        issues.push({
          ruleId: 'invalid-test-filename',
          severity: 'warning',
          message: 'Test files should include .test. or .spec. in the name',
          filePath,
          autoFixable: false,
        });
      }
    } else if (framework === 'cypress') {
      if (!fileName.endsWith('.cy.ts') && !fileName.endsWith('.cy.js')) {
        issues.push({
          ruleId: 'invalid-test-filename',
          severity: 'warning',
          message: 'Cypress test files should end with .cy.ts or .cy.js',
          filePath,
          autoFixable: false,
        });
      }
    }

    return issues;
  }

  /**
   * Validate best practices
   */
  private validateBestPractices(content: string, framework: TestFramework): ValidationIssue[] {
    const issues: ValidationIssue[] = [];

    // Check for hardcoded values
    if (/(?:localhost|127\.0\.0\.1|192\.168\.)/.test(content)) {
      issues.push({
        ruleId: 'hardcoded-urls',
        severity: 'warning',
        message: 'Avoid hardcoded URLs/IPs; use configuration',
        filePath: '',
        autoFixable: false,
      });
    }

    // Check for console.log/System.out (should use proper logging)
    if (/console\.log|System\.out\.println/.test(content)) {
      issues.push({
        ruleId: 'debug-statements',
        severity: 'info',
        message: 'Remove debug statements (console.log/System.out)',
        filePath: '',
        autoFixable: true,
      });
    }

    // Check for incomplete task comments
    // eslint-disable-next-line sonarjs/todo-tag, unicorn/prefer-regexp-test
    const incompleteMatches = content.match(/\/\/\s*TODO|\/\*\s*TODO/g);
    if (incompleteMatches && incompleteMatches.length > 0) {
      issues.push({
        ruleId: 'incomplete-implementation',
        severity: 'warning',
        message: `Test has ${incompleteMatches.length} incomplete task comments`,
        filePath: '',
        autoFixable: false,
      });
    }

    return issues;
  }

  /**
   * Calculate code quality metrics
   */
  private async calculateMetrics(
    content: string,
    framework: TestFramework,
    testType: TestType
  ): Promise<CodeQualityMetrics> {
    const hash = this.hashContent(content);
    const cached = this.metricsCache.get(hash);
    if (cached) return cached;

    const lines = content.split('\n');
    const assertionValidation = this.validateAssertions(content, framework);
    
    // Count test cases
    let testCaseCount = 0;
    if (framework === 'junit') {
      testCaseCount = (content.match(/@Test/g) || []).length;
    } else {
      testCaseCount = (content.match(/\b(?:test|it)\s*\(/g) || []).length;
    }

    // Check setup/teardown
    const patterns = this.testStructurePatterns[framework];
    const hasSetup = patterns?.setup?.test(content) || patterns?.beforeEach?.test(content) || false;
    const hasTeardown = patterns?.teardown?.test(content) || patterns?.afterEach?.test(content) || false;

    // Count mocks
    const mockCount = (content.match(/\b(?:mock|stub|spy|jest\.fn|sinon\.|cy\.stub)\b/g) || []).length;

    // Count incomplete comments
    const incompleteCommentCount = (content.match(/\/\/\s*(?:TODO|FIXME|XXX)|\/\*\s*(?:TODO|FIXME|XXX)/g) || []).length;

    const metrics: CodeQualityMetrics = {
      linesOfCode: lines.filter(l => l.trim().length > 0).length,
      testCaseCount,
      assertionCount: assertionValidation.count,
      assertionsPerTest: assertionValidation.perTestCase,
      hasSetup,
      hasTeardown,
      incompleteCommentCount,
      mockCount,
    };

    this.metricsCache.set(hash, metrics);
    return metrics;
  }

  /**
   * Detect test framework from content
   */
  private detectFramework(content: string, filePath: string): TestFramework {
    const hash = this.hashContent(content + filePath);
    const cached = this.frameworkCache.get(hash);
    if (cached) return cached;

    let framework: TestFramework;

    if (filePath.endsWith('.java') || content.includes('@Test')) {
      framework = 'junit';
    } else if (filePath.endsWith('.cy.ts') || filePath.endsWith('.cy.js')) {
      framework = 'cypress';
    } else if (content.includes('@playwright/test')) {
      framework = 'playwright';
    } else if (content.includes('vitest')) {
      framework = 'vitest';
    } else if (content.includes('mocha')) {
      framework = 'mocha';
    } else {
      framework = 'jest';
    }

    this.frameworkCache.set(hash, framework);
    return framework;
  }

  /**
   * Detect test type
   */
  private detectTestType(content: string, filePath: string): TestType {
    if (filePath.includes('integration') || content.includes('IntegrationTest')) {
      return 'integration';
    } else if (filePath.includes('e2e') || filePath.includes('.cy.')) {
      return 'flow';
    } else if (content.includes('RestController') || content.includes('api/')) {
      return 'api';
    } else if (filePath.includes('component') || content.includes('Component')) {
      return 'component';
    } else {
      return 'unit';
    }
  }

  /**
   * Get required imports for framework
   */
  private getRequiredImports(framework: TestFramework): string[] {
    const imports: Record<TestFramework, string[]> = {
      junit: ['org.junit.jupiter.api.Test'],
      jest: ['jest', '@jest/globals'],
      playwright: ['@playwright/test'],
      cypress: ['cypress'],
      vitest: ['vitest'],
      mocha: ['mocha'],
    };
    return imports[framework] || [];
  }

  /**
   * Generate validation report
   */
  private generateReport(results: ValidationResult[]): ValidationReport {
    const summary = this.calculateSummary(results);
    const quality = this.assessQuality(results);

    return {
      id: crypto.randomUUID(),
      title: 'Test Validation Report',
      timestamp: new Date(),
      results,
      summary,
      quality,
      metadata: {
        totalFiles: results.length,
        validFiles: results.filter(r => r.valid).length,
        invalidFiles: results.filter(r => !r.valid).length,
        totalIssues: results.reduce((sum, r) => sum + r.issues.length, 0),
        errorCount: summary.issuesBySeverity.error,
        warningCount: summary.issuesBySeverity.warning,
        infoCount: summary.issuesBySeverity.info,
        suggestionCount: summary.issuesBySeverity.suggestion,
      },
    };
  }

  /**
   * Calculate validation summary
   */
  private calculateSummary(results: ValidationResult[]): ValidationSummary {
    const totalTests = results.length;
    const validTests = results.filter(r => r.valid).length;
    const invalidTests = totalTests - validTests;

    const allIssues = results.flatMap(r => r.issues);
    const issuesBySeverity = {
      error: allIssues.filter(i => i.severity === 'error').length,
      warning: allIssues.filter(i => i.severity === 'warning').length,
      info: allIssues.filter(i => i.severity === 'info').length,
      suggestion: allIssues.filter(i => i.severity === 'suggestion').length,
    };

    const issuesByType = allIssues.reduce((acc, issue) => {
      const rule = this.rules.get(issue.ruleId);
      if (rule) {
        acc[rule.type] = (acc[rule.type] || 0) + 1;
      }
      return acc;
    }, {} as Record<ValidationRuleType, number>);

    const qualityScores = results.map(r => this.calculateQualityScore(r.metrics));
    const averageQualityScore = qualityScores.reduce((a, b) => a + b, 0) / qualityScores.length || 0;

    const testsByFramework = results.reduce((acc, r) => {
      acc[r.framework] = (acc[r.framework] || 0) + 1;
      return acc;
    }, {} as Record<TestFramework, number>);

    const testsByType = results.reduce((acc, r) => {
      acc[r.testType] = (acc[r.testType] || 0) + 1;
      return acc;
    }, {} as Record<TestType, number>);

    return {
      totalTests,
      validTests,
      invalidTests,
      totalIssues: allIssues.length,
      issuesBySeverity,
      issuesByType,
      averageQualityScore,
      testsByFramework,
      testsByType,
    };
  }

  /**
   * Assess overall quality
   */
  private assessQuality(results: ValidationResult[]): QualityAssessment {
    const scores = {
      syntax: 0,
      structure: 0,
      assertions: 0,
      coverage: 0,
      bestPractices: 0,
    };

    // eslint-disable-next-line unicorn/no-array-for-each
    for (const result of results) {
      const syntaxIssues = result.issues.filter(i => i.ruleId.includes('syntax')).length;
      const structureIssues = result.issues.filter(i => i.ruleId.includes('structure')).length;
      const assertionIssues = result.issues.filter(i => i.ruleId.includes('assertion')).length;
      const bestPracticeIssues = result.issues.filter(i => i.ruleId.includes('practice') || i.ruleId.includes('naming')).length;

      scores.syntax += syntaxIssues === 0 ? 100 : Math.max(0, 100 - syntaxIssues * 10);
      scores.structure += structureIssues === 0 ? 100 : Math.max(0, 100 - structureIssues * 10);
      scores.assertions += assertionIssues === 0 ? 100 : Math.max(0, 100 - assertionIssues * 10);
      scores.coverage += 75; // Placeholder
      scores.bestPractices += bestPracticeIssues === 0 ? 100 : Math.max(0, 100 - bestPracticeIssues * 5);
    }

    const count = results.length || 1;
    const breakdown = {
      syntax: Math.round(scores.syntax / count),
      structure: Math.round(scores.structure / count),
      assertions: Math.round(scores.assertions / count),
      coverage: Math.round(scores.coverage / count),
      bestPractices: Math.round(scores.bestPractices / count),
    };

    const score = Math.round(
      (breakdown.syntax + breakdown.structure + breakdown.assertions + 
       breakdown.coverage + breakdown.bestPractices) / 5
    );

    const grade = this.calculateGrade(score);
    const recommendations = this.generateRecommendations(breakdown);

    return { score, grade, breakdown, recommendations };
  }

  /**
   * Calculate quality score
   */
  private calculateQualityScore(metrics: CodeQualityMetrics): number {
    let score = 100;

    // Deduct for missing assertions
    if (metrics.assertionCount === 0) score -= 30;
    else if (metrics.assertionsPerTest < 1) score -= 15;

    // Deduct for missing setup/teardown
    if (!metrics.hasSetup) score -= 5;
    if (!metrics.hasTeardown) score -= 5;

    // Deduct for incomplete comments
    score -= Math.min(metrics.incompleteCommentCount * 5, 20);

    // Deduct for no test cases
    if (metrics.testCaseCount === 0) score -= 40;

    return Math.max(0, score);
  }

  /**
   * Calculate grade from score
   */
  private calculateGrade(score: number): 'A' | 'B' | 'C' | 'D' | 'F' {
    if (score >= 90) return 'A';
    if (score >= 80) return 'B';
    if (score >= 70) return 'C';
    if (score >= 60) return 'D';
    return 'F';
  }

  /**
   * Generate recommendations
   */
  private generateRecommendations(breakdown: QualityAssessment['breakdown']): string[] {
    const recommendations: string[] = [];

    if (breakdown.syntax < 80) {
      recommendations.push('Fix syntax errors to improve code quality');
    }
    if (breakdown.structure < 80) {
      recommendations.push('Improve test structure and organization');
    }
    if (breakdown.assertions < 80) {
      recommendations.push('Add more assertions to verify behavior');
    }
    if (breakdown.coverage < 80) {
      recommendations.push('Increase test coverage of requirements');
    }
    if (breakdown.bestPractices < 80) {
      recommendations.push('Follow framework best practices and naming conventions');
    }

    return recommendations;
  }

  /**
   * Hash content for caching
   */
  private hashContent(content: string): string {
    return crypto.createHash('md5').update(content).digest('hex');
  }

  /**
   * Get default validation rules
   */
  private getDefaultRules(): ValidationRule[] {
    return [
      {
        id: 'syntax-error',
        type: 'syntax',
        severity: 'error',
        name: 'Syntax Error',
        description: 'Code has syntax errors',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'missing-imports',
        type: 'compilation',
        severity: 'error',
        name: 'Missing Imports',
        description: 'Required imports are missing',
        enabled: true,
        autoFixable: true,
      },
      {
        id: 'missing-assertions',
        type: 'assertion',
        severity: 'error',
        name: 'Missing Assertions',
        description: 'Test has no assertions',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'insufficient-assertions',
        type: 'assertion',
        severity: 'warning',
        name: 'Insufficient Assertions',
        description: 'Test has too few assertions',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'invalid-structure',
        type: 'structure',
        severity: 'error',
        name: 'Invalid Structure',
        description: 'Test structure is invalid',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'invalid-test-filename',
        type: 'naming',
        severity: 'warning',
        name: 'Invalid Test Filename',
        description: 'Test file naming does not follow conventions',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'hardcoded-urls',
        type: 'best-practice',
        severity: 'warning',
        name: 'Hardcoded URLs',
        description: 'Avoid hardcoded URLs in tests',
        enabled: true,
        autoFixable: false,
      },
      {
        id: 'debug-statements',
        type: 'best-practice',
        severity: 'info',
        name: 'Debug Statements',
        description: 'Remove debug statements from tests',
        enabled: true,
        autoFixable: true,
      },
      {
        id: 'incomplete-implementation',
        type: 'best-practice',
        severity: 'warning',
        name: 'Incomplete Implementation',
        description: 'Test has incomplete implementation (TODO comments)',
        enabled: true,
        autoFixable: false,
      },
    ];
  }
}
