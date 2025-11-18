/**
 * Flow Test Generator
 * 
 * Generates E2E flow tests from user stories for Playwright/Cypress.
 * Supports multi-service user journeys, API orchestration, and cross-service integration.
 * 
 * @module generators/flow-test-generator
 */

import type { StoryModel } from '../models/story-model.js';
import type { TestPlan } from '../models/test-plan-model.js';
import type { AllureSeverity } from '../models/frontend-test-model.js';
import type {
  FlowTestType,
  E2EFramework,
  FlowTestTemplate,
  GeneratedFlowTest,
  ServiceName
} from '../models/flow-test-model.js';

/**
 * Flow Test Generator
 * Generates E2E tests for multi-service user journeys
 */
export class FlowTestGenerator {
  private readonly framework: E2EFramework;
  
  // Performance: Pre-compiled regex patterns
  private readonly serviceNameRegex = /\b(backend|frontend|gateway|risk-engine)\b/gi;
  private readonly endpointRegex = /(?:endpoint|route|path|url):\s*['"](\/[^'"]+)['"]/gi;
  private readonly httpMethodRegex = /\b(GET|POST|PUT|DELETE|PATCH)\b/g;
  
  // Performance: Pre-compiled mapping for flow types
  private readonly flowTypeMapping: Record<string, FlowTestType> = {
    'user': 'user-journey',
    'journey': 'user-journey',
    'flow': 'user-journey',
    'api': 'api-orchestration',
    'orchestration': 'api-orchestration',
    'integration': 'e2e-integration',
    'e2e': 'e2e-integration',
    'cross-service': 'cross-service',
    'performance': 'performance-flow'
  };
  
  // Performance: Memoization caches
  private readonly flowTypeCache = new Map<string, FlowTestType>();
  private readonly serviceCache = new Map<string, ServiceName[]>();
  private readonly severityCache = new Map<string, AllureSeverity>();

  constructor(framework: E2EFramework = 'playwright') {
    this.framework = framework;
  }

  /**
   * Generate flow test from story and test plan
   */
  public generate(story: StoryModel, plan: TestPlan): GeneratedFlowTest {
    const flowType = this.determineFlowType(story, plan);
    const services = this.extractServices(story);
    const severity = this.determineSeverity(story);
    
    const template = this.createFlowTemplate(flowType, this.framework);
    const testCode = this.generateFlowTestCode(template, story, flowType, services, severity);
    
    const filePath = this.generateFilePath(story, flowType);
    
    return {
      filePath,
      type: flowType,
      framework: this.framework,
      storyId: story.storyId,
      epicPath: story.epicPath || 'unknown',
      services,
      stepCount: this.countSteps(story),
      code: testCode,
      allureMetadata: {
        epic: story.epicTitle || 'Unknown Epic',
        feature: this.determineFeature(flowType),
        story: story.storyId,
        severity,
        description: story.title
      },
      generatedAt: new Date()
    };
  }

  /**
   * Generate multiple flow tests from a test plan
   */
  public generateFromPlan(story: StoryModel, plan: TestPlan): GeneratedFlowTest[] {
    const flowType = this.determineFlowType(story, plan);
    
    // Generate main flow test
    const mainTest = this.generate(story, plan);
    
    const tests: GeneratedFlowTest[] = [mainTest];
    
    // Generate additional error scenario tests if applicable
    if (plan.requiresFlowTests) {
      const errorTest = this.generateErrorScenarioTest(story, plan, flowType);
      if (errorTest) {
        tests.push(errorTest);
      }
    }
    
    return tests;
  }

  /**
   * Determine flow test type from story and plan
   * Performance: Memoized with cache
   */
  private determineFlowType(story: StoryModel, plan: TestPlan): FlowTestType {
    const cached = this.flowTypeCache.get(story.storyId);
    if (cached) {
      return cached;
    }

    let flowType: FlowTestType = 'user-journey'; // Default
    
    // Check story title
    const text = story.title.toLowerCase();
    
    if (text.includes('api') && (text.includes('orchestrat') || text.includes('coordinat'))) {
      flowType = 'api-orchestration';
    } else if (text.includes('cross-service') || text.includes('service-to-service')) {
      flowType = 'cross-service';
    } else if (text.includes('performance') || text.includes('load')) {
      flowType = 'performance-flow';
    } else if (text.includes('e2e') || text.includes('end-to-end')) {
      flowType = 'e2e-integration';
    }
    
    this.flowTypeCache.set(story.storyId, flowType);
    return flowType;
  }

  /**
   * Extract services involved from story
   * Performance: Memoized with cache
   */
  private extractServices(story: StoryModel): ServiceName[] {
    const cached = this.serviceCache.get(story.storyId);
    if (cached) {
      return cached;
    }

    const services = new Set<ServiceName>();
    const text = `${story.title} ${JSON.stringify(story.acceptanceCriteria)}`;
    
    // Find all service mentions
    const matches = text.matchAll(this.serviceNameRegex);
    for (const match of matches) {
      const service = match[0].toLowerCase() as ServiceName;
      services.add(service);
    }
    
    // Default to backend if no services found
    if (services.size === 0) {
      services.add('backend');
    }
    
    const result = Array.from(services);
    this.serviceCache.set(story.storyId, result);
    return result;
  }

  /**
   * Create flow test template
   */
  private createFlowTemplate(flowType: FlowTestType, framework: E2EFramework): FlowTestTemplate {
    if (framework === 'playwright') {
      return this.createPlaywrightTemplate(flowType);
    } else {
      return this.createCypressTemplate(flowType);
    }
  }

  /**
   * Create Playwright template
   */
  private createPlaywrightTemplate(flowType: FlowTestType): FlowTestTemplate {
    const imports = [
      "import { test, expect } from '@playwright/test';",
      "import { allure } from 'allure-playwright';"
    ];
    
    if (flowType === 'api-orchestration' || flowType === 'cross-service') {
      imports.push("import { request } from '@playwright/test';");
    }
    
    return {
      templateId: `playwright-${flowType}`,
      type: flowType,
      framework: 'playwright',
      imports,
      setupCode: this.getPlaywrightSetup(flowType),
      teardownCode: this.getPlaywrightTeardown(flowType),
      config: {
        browser: 'chromium',
        viewport: { width: 1280, height: 720 },
        screenshot: true,
        video: false,
        trace: true,
        timeout: 30000
      },
      timeout: 60000,
      retry: { attempts: 2, delay: 1000 }
    };
  }

  /**
   * Create Cypress template
   */
  private createCypressTemplate(flowType: FlowTestType): FlowTestTemplate {
    const imports = [
      "/// <reference types=\"cypress\" />",
      "import '@shelex/cypress-allure-plugin';"
    ];
    
    return {
      templateId: `cypress-${flowType}`,
      type: flowType,
      framework: 'cypress',
      imports,
      setupCode: this.getCypressSetup(flowType),
      teardownCode: this.getCypressTeardown(flowType),
      config: {
        baseUrl: 'http://localhost:3000',
        viewportWidth: 1280,
        viewportHeight: 720,
        video: false,
        screenshotOnRunFailure: true,
        defaultCommandTimeout: 10000,
        requestTimeout: 10000
      },
      timeout: 60000,
      retry: { attempts: 2, delay: 1000 }
    };
  }

  /**
   * Generate flow test code
   */
  private generateFlowTestCode(
    template: FlowTestTemplate,
    story: StoryModel,
    flowType: FlowTestType,
    services: ServiceName[],
    severity: AllureSeverity
  ): string {
    if (template.framework === 'playwright') {
      return this.generatePlaywrightTest(template, story, flowType, services, severity);
    } else {
      return this.generateCypressTest(template, story, flowType, services, severity);
    }
  }

  /**
   * Generate Playwright test
   * Performance: Optimized string building with array join
   */
  private generatePlaywrightTest(
    template: FlowTestTemplate,
    story: StoryModel,
    flowType: FlowTestType,
    services: ServiceName[],
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      '',
      '/**',
      ` * ${story.title}`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ` * @feature ${this.determineFeature(flowType)}`,
      ` * @story ${story.storyId}`,
      ` * @services ${services.join(', ')}`,
      ' */',
      ''
    ];
    
    // Test describe block
    parts.push(
      `test.describe('${story.title}', () => {`,
      `  test.beforeEach(async ({ page }) => {`,
      `    await allure.epic('${story.epicTitle}');`,
      `    await allure.feature('${this.determineFeature(flowType)}');`,
      `    await allure.story('${story.storyId}: ${story.title}');`,
      `    await allure.severity('${severity}');`,
      `    await allure.parameter('services', '${services.join(', ')}');`,
      '  });',
      ''
    );
    
    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }
    
    // Generate test cases based on flow type
    switch (flowType) {
      case 'user-journey':
        parts.push(...this.generateUserJourneyTest(story, 'playwright'));
        break;
      case 'api-orchestration':
        parts.push(...this.generateApiOrchestrationTest(story, 'playwright'));
        break;
      case 'cross-service':
        parts.push(...this.generateCrossServiceTest(story, 'playwright'));
        break;
      case 'e2e-integration':
        parts.push(...this.generateE2EIntegrationTest(story, 'playwright'));
        break;
      case 'performance-flow':
        parts.push(...this.generatePerformanceFlowTest(story, 'playwright'));
        break;
    }
    
    if (template.teardownCode) {
      parts.push('', template.teardownCode);
    }
    
    parts.push('});', '');
    
    return parts.join('\n');
  }

  /**
   * Generate Cypress test
   * Performance: Optimized string building with array join
   */
  private generateCypressTest(
    template: FlowTestTemplate,
    story: StoryModel,
    flowType: FlowTestType,
    services: ServiceName[],
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      '',
      '/**',
      ` * ${story.title}`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ` * @feature ${this.determineFeature(flowType)}`,
      ` * @story ${story.storyId}`,
      ` * @services ${services.join(', ')}`,
      ' */',
      '',
      `describe('${story.title}', () => {`,
      '  beforeEach(() => {',
      `    cy.allure().epic('${story.epicTitle}');`,
      `    cy.allure().feature('${this.determineFeature(flowType)}');`,
      `    cy.allure().story('${story.storyId}: ${story.title}');`,
      `    cy.allure().severity('${severity}');`,
      `    cy.allure().parameter('services', '${services.join(', ')}');`,
      '  });',
      ''
    ];
    
    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }
    
    // Generate test cases based on flow type
    switch (flowType) {
      case 'user-journey':
        parts.push(...this.generateUserJourneyTest(story, 'cypress'));
        break;
      case 'api-orchestration':
        parts.push(...this.generateApiOrchestrationTest(story, 'cypress'));
        break;
      case 'cross-service':
        parts.push(...this.generateCrossServiceTest(story, 'cypress'));
        break;
      case 'e2e-integration':
        parts.push(...this.generateE2EIntegrationTest(story, 'cypress'));
        break;
      case 'performance-flow':
        parts.push(...this.generatePerformanceFlowTest(story, 'cypress'));
        break;
    }
    
    if (template.teardownCode) {
      parts.push('', template.teardownCode);
    }
    
    parts.push('});', '');
    
    return parts.join('\n');
  }

  /**
   * Generate user journey test steps
   */
  private generateUserJourneyTest(story: StoryModel, framework: E2EFramework): string[] {
    const parts: string[] = [];
    const testFunc = framework === 'playwright' ? 'test' : 'it';
    const pageParam = framework === 'playwright' ? '{ page }' : '';
    
    parts.push(
      `  ${testFunc}('should complete user journey successfully', async (${pageParam}) => {`,
      `    // Step 1: Navigate to application`,
      framework === 'playwright' 
        ? `    await page.goto('/');`
        : `    cy.visit('/');`,
      '',
      `    // Step 2: Perform user actions`,
      `    // TODO: Add specific user interactions based on acceptance criteria`,
      framework === 'playwright'
        ? `    await page.waitForSelector('[data-testid="main-content"]');`
        : `    cy.get('[data-testid="main-content"]').should('be.visible');`,
      '',
      `    // Step 3: Verify expected results`,
      `    // TODO: Add assertions for expected outcomes`,
      framework === 'playwright'
        ? `    await expect(page.locator('[data-testid="main-content"]')).toBeVisible();`
        : `    cy.get('[data-testid="main-content"]').should('contain', 'Expected Text');`,
      '  });'
    );
    
    return parts;
  }

  /**
   * Generate API orchestration test steps
   */
  private generateApiOrchestrationTest(story: StoryModel, framework: E2EFramework): string[] {
    const parts: string[] = [];
    const testFunc = framework === 'playwright' ? 'test' : 'it';
    const requestParam = framework === 'playwright' ? '{ request }' : '';
    
    parts.push(
      `  ${testFunc}('should orchestrate API calls across services', async (${requestParam}) => {`,
      `    // Step 1: Call first service`,
      framework === 'playwright'
        ? `    const response1 = await request.get('/api/v1/resource');`
        : `    cy.request('GET', '/api/v1/resource').as('response1');`,
      framework === 'playwright'
        ? `    expect(response1.status()).toBe(200);`
        : `    cy.get('@response1').its('status').should('eq', 200);`,
      '',
      `    // Step 2: Extract data and call second service`,
      framework === 'playwright'
        ? `    const data1 = await response1.json();`
        : `    cy.get('@response1').then((response) => {`,
      framework === 'playwright'
        ? `    const response2 = await request.post('/api/v1/process', { data: { id: data1.id } });`
        : `      const data1 = response.body;`,
      framework === 'playwright'
        ? `    expect(response2.status()).toBe(201);`
        : `      cy.request('POST', '/api/v1/process', { id: data1.id }).its('status').should('eq', 201);`,
      framework === 'cypress' ? '    });' : '',
      '',
      `    // Step 3: Verify final state`,
      `    // TODO: Add verification of orchestrated result`,
      '  });'
    );
    
    return parts;
  }

  /**
   * Generate cross-service test steps
   */
  private generateCrossServiceTest(story: StoryModel, framework: E2EFramework): string[] {
    const parts: string[] = [];
    const testFunc = framework === 'playwright' ? 'test' : 'it';
    const requestParam = framework === 'playwright' ? '{ request }' : '';
    
    parts.push(
      `  ${testFunc}('should integrate across multiple services', async (${requestParam}) => {`,
      `    // Service 1: Create resource`,
      framework === 'playwright'
        ? `    const createResponse = await request.post('/api/v1/create', { data: { name: 'Test' } });`
        : `    cy.request('POST', '/api/v1/create', { name: 'Test' }).as('createResponse');`,
      framework === 'playwright'
        ? `    const resourceId = (await createResponse.json()).id;`
        : `    cy.get('@createResponse').its('body.id').as('resourceId');`,
      '',
      `    // Service 2: Process resource`,
      framework === 'playwright'
        ? `    const processResponse = await request.post(\`/api/v1/process/\${resourceId}\`);`
        : `    cy.get('@resourceId').then((id) => {`,
      framework === 'playwright'
        ? `    expect(processResponse.status()).toBe(200);`
        : `      cy.request('POST', \`/api/v1/process/\${id}\`).its('status').should('eq', 200);`,
      framework === 'cypress' ? '    });' : '',
      '',
      `    // Service 3: Verify result`,
      `    // TODO: Add cross-service verification`,
      '  });'
    );
    
    return parts;
  }

  /**
   * Generate E2E integration test steps
   */
  private generateE2EIntegrationTest(story: StoryModel, framework: E2EFramework): string[] {
    const parts: string[] = [];
    const testFunc = framework === 'playwright' ? 'test' : 'it';
    const pageParam = framework === 'playwright' ? '{ page, request }' : '';
    
    parts.push(
      `  ${testFunc}('should complete end-to-end integration flow', async (${pageParam}) => {`,
      `    // UI Step: Navigate and interact`,
      framework === 'playwright'
        ? `    await page.goto('/');`
        : `    cy.visit('/');`,
      framework === 'playwright'
        ? `    await page.click('[data-testid="action-button"]');`
        : `    cy.get('[data-testid="action-button"]').click();`,
      '',
      `    // API Step: Verify backend state`,
      framework === 'playwright'
        ? `    const apiResponse = await request.get('/api/v1/state');`
        : `    cy.request('GET', '/api/v1/state').as('stateResponse');`,
      framework === 'playwright'
        ? `    expect(apiResponse.status()).toBe(200);`
        : `    cy.get('@stateResponse').its('status').should('eq', 200);`,
      '',
      `    // UI Step: Verify visual update`,
      framework === 'playwright'
        ? `    await expect(page.locator('[data-testid="result"]')).toBeVisible();`
        : `    cy.get('[data-testid="result"]').should('be.visible');`,
      '  });'
    );
    
    return parts;
  }

  /**
   * Generate performance flow test steps
   */
  private generatePerformanceFlowTest(story: StoryModel, framework: E2EFramework): string[] {
    const parts: string[] = [];
    const testFunc = framework === 'playwright' ? 'test' : 'it';
    const pageParam = framework === 'playwright' ? '{ page }' : '';
    
    parts.push(
      `  ${testFunc}('should complete flow within performance thresholds', async (${pageParam}) => {`,
      `    const startTime = Date.now();`,
      '',
      `    // Execute flow`,
      framework === 'playwright'
        ? `    await page.goto('/');`
        : `    cy.visit('/');`,
      framework === 'playwright'
        ? `    await page.waitForLoadState('networkidle');`
        : `    cy.wait(1000); // Wait for network to settle`,
      '',
      `    const endTime = Date.now();`,
      `    const duration = endTime - startTime;`,
      '',
      `    // Performance assertion`,
      `    console.log(\`Flow completed in \${duration}ms\`);`,
      framework === 'playwright'
        ? `    expect(duration).toBeLessThan(5000); // 5 second threshold`
        : `    expect(duration).to.be.lessThan(5000); // 5 second threshold`,
      '  });'
    );
    
    return parts;
  }

  /**
   * Generate error scenario test
   */
  private generateErrorScenarioTest(
    story: StoryModel,
    plan: TestPlan,
    flowType: FlowTestType
  ): GeneratedFlowTest | null {
    // Only generate error scenarios for certain flow types
    if (!['api-orchestration', 'cross-service', 'e2e-integration'].includes(flowType)) {
      return null;
    }
    
    const services = this.extractServices(story);
    const severity: AllureSeverity = 'normal'; // Error scenarios typically normal priority
    const template = this.createFlowTemplate(flowType, this.framework);
    
    const errorTestCode = this.generateErrorScenarioCode(template, story, flowType, services, severity);
    const filePath = this.generateFilePath(story, flowType).replace('.spec.', '.error.spec.');
    
    return {
      filePath,
      type: flowType,
      framework: this.framework,
      storyId: `${story.storyId}-error`,
      epicPath: story.epicPath || 'unknown',
      services,
      stepCount: 3, // Typical error scenario steps
      code: errorTestCode,
      allureMetadata: {
        epic: story.epicTitle || 'Unknown Epic',
        feature: `${this.determineFeature(flowType)} - Error Handling`,
        story: `${story.storyId}-error`,
        severity,
        description: `Error scenarios for: ${story.title}`
      },
      generatedAt: new Date()
    };
  }

  /**
   * Generate error scenario test code
   */
  private generateErrorScenarioCode(
    template: FlowTestTemplate,
    story: StoryModel,
    flowType: FlowTestType,
    services: ServiceName[],
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      '',
      '/**',
      ` * ${story.title} - Error Scenarios`,
      ` * Story ID: ${story.storyId}-error`,
      ' */',
      ''
    ];
    
    const testFunc = this.framework === 'playwright' ? 'test' : 'it';
    const describeBlock = this.framework === 'playwright' ? 'test.describe' : 'describe';
    const requestParam = this.framework === 'playwright' ? '{ request }' : '';
    
    parts.push(
      `${describeBlock}('${story.title} - Error Handling', () => {`,
      `  ${testFunc}('should handle service unavailable gracefully', async (${requestParam}) => {`,
      `    // Simulate service unavailable`,
      this.framework === 'playwright'
        ? `    const response = await request.get('/api/v1/unavailable').catch(() => null);`
        : `    cy.request({ url: '/api/v1/unavailable', failOnStatusCode: false }).as('errorResponse');`,
      '',
      `    // Verify error handling`,
      this.framework === 'playwright'
        ? `    expect(response).toBeNull();`
        : `    cy.get('@errorResponse').its('status').should('be.gte', 500);`,
      '  });',
      '',
      `  ${testFunc}('should handle invalid data gracefully', async (${requestParam}) => {`,
      `    // Send invalid data`,
      this.framework === 'playwright'
        ? `    const response = await request.post('/api/v1/resource', { data: { invalid: true } });`
        : `    cy.request({ method: 'POST', url: '/api/v1/resource', body: { invalid: true }, failOnStatusCode: false }).as('validationResponse');`,
      '',
      `    // Verify validation error`,
      this.framework === 'playwright'
        ? `    expect(response.status()).toBe(400);`
        : `    cy.get('@validationResponse').its('status').should('eq', 400);`,
      '  });',
      '});',
      ''
    );
    
    return parts.join('\n');
  }

  /**
   * Determine test feature from flow type
   */
  private determineFeature(flowType: FlowTestType): string {
    const featureMap: Record<FlowTestType, string> = {
      'user-journey': 'User Journeys',
      'api-orchestration': 'API Orchestration',
      'cross-service': 'Cross-Service Integration',
      'e2e-integration': 'End-to-End Integration',
      'performance-flow': 'Performance Testing'
    };
    
    return featureMap[flowType] || 'E2E Tests';
  }

  /**
   * Determine severity from story
   * Performance: Memoized with cached regex
   */
  private determineSeverity(story: StoryModel): AllureSeverity {
    const cached = this.severityCache.get(story.storyId);
    if (cached) {
      return cached;
    }

    let severity: AllureSeverity = 'normal'; // Default for E2E tests
    
    const title = story.title.toLowerCase();
    
    if (title.includes('critical')) {
      severity = 'critical';
    } else if (title.includes('blocker')) {
      severity = 'blocker';
    } else if (title.includes('minor')) {
      severity = 'minor';
    } else if (title.includes('trivial')) {
      severity = 'trivial';
    }
    
    this.severityCache.set(story.storyId, severity);
    return severity;
  }

  /**
   * Generate file path for flow test
   */
  private generateFilePath(story: StoryModel, flowType: FlowTestType): string {
    const epicName = story.epicPath?.split('/').pop() || 'unknown-epic';
    // eslint-disable-next-line unicorn/prefer-string-replace-all
    const storyIdClean = story.storyId.toLowerCase().replace(/[^a-z0-9-]/g, '-');
    // eslint-disable-next-line unicorn/prefer-string-replace-all
    const flowTypeClean = flowType.replace(/-/g, '_');
    const extension = this.framework === 'playwright' ? 'spec.ts' : 'cy.ts';
    
    return `e2e/${epicName}/${flowTypeClean}/${storyIdClean}.${extension}`;
  }

  /**
   * Count steps in story
   */
  private countSteps(story: StoryModel): number {
    const criteria = Array.isArray(story.acceptanceCriteria)
      ? story.acceptanceCriteria
      : [story.acceptanceCriteria];
    return Math.max(criteria.length, 3); // Minimum 3 steps
  }

  /**
   * Get Playwright setup code
   */
  private getPlaywrightSetup(flowType: FlowTestType): string {
    if (flowType === 'api-orchestration' || flowType === 'cross-service') {
      return `  test.beforeAll(async ({ request }) => {
    // Setup: Ensure services are ready
    await request.get('/api/health').catch(() => null);
  });`;
    }
    return '';
  }

  /**
   * Get Playwright teardown code
   */
  private getPlaywrightTeardown(flowType: FlowTestType): string {
    return `  test.afterAll(async () => {
    // Cleanup: Reset test data
    // TODO: Add cleanup logic
  });`;
  }

  /**
   * Get Cypress setup code
   */
  private getCypressSetup(flowType: FlowTestType): string {
    if (flowType === 'api-orchestration' || flowType === 'cross-service') {
      return `  before(() => {
    // Setup: Ensure services are ready
    cy.request({ url: '/api/health', failOnStatusCode: false });
  });`;
    }
    return '';
  }

  /**
   * Get Cypress teardown code
   */
  private getCypressTeardown(flowType: FlowTestType): string {
    return `  after(() => {
    // Cleanup: Reset test data
    // TODO: Add cleanup logic
  });`;
  }
}
