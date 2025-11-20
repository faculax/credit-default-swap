/**
 * Frontend Test Generator
 * 
 * Generates React Testing Library tests with MSW integration and Allure reporting.
 * Supports component tests, hook tests, form tests, and integration tests.
 */

import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import type { StoryModel } from '../models/story-model.js';
import type { TestPlan, PlannedTest } from '../models/test-plan-model.js';
import type {
  FrontendTestType,
  FrontendTestTemplate,
  GeneratedFrontendTest,
  AllureSeverity
} from '../models/frontend-test-model.js';
import type { WorkspaceContext, FrontendComponent } from '../models/workspace-context-model.js';

export class FrontendTestGenerator {
  private readonly outputDir: string;
  private readonly testUtilsPath: string;
  private readonly mswServerPath: string;
  private readonly workspaceContext?: WorkspaceContext;

  // Performance: Cache compiled regex patterns
  private readonly componentNameRegex = /\b([A-Z][a-zA-Z]+(?:Form|Page|Modal|Button|Input|Table|List|Card|Panel))\b/;
  private readonly epicNumberRegex = /epic_(\d+)/;
  
  // Performance: Cache mapping objects
  private readonly testTypeMapping: Record<string, FrontendTestType> = {
    'unit': 'component',
    'component': 'component',
    'integration': 'integration',
    'hook': 'hook',
    'form': 'form',
    'ui': 'component',
    'accessibility': 'accessibility'
  };

  // Performance: Cache computed values per story
  private readonly componentNameCache = new Map<string, string>();
  private readonly severityCache = new Map<string, AllureSeverity>();

  constructor(
    outputDir: string = 'frontend/src/__tests__',
    testUtilsPath: string = '../test-utils',
    mswServerPath: string = '../mocks/server',
    workspaceContext?: WorkspaceContext
  ) {
    this.outputDir = outputDir;
    this.testUtilsPath = testUtilsPath;
    this.mswServerPath = mswServerPath;
    this.workspaceContext = workspaceContext;
  }

  /**
   * Generate frontend tests for a story
   */
  public async generateTestsForStory(
    story: StoryModel,
    testPlan: TestPlan
  ): Promise<GeneratedFrontendTest[]> {
    const generatedTests: GeneratedFrontendTest[] = [];

    // Find frontend-related tests in the plan
    const frontendTests = testPlan.plannedTests.filter(
      (test) => test.service === 'frontend'
    );

    for (const test of frontendTests) {
      for (const testType of test.testTypes) {
        const frontendTestType = this.mapToFrontendTestType(testType);
        if (frontendTestType) {
          const template = this.createTemplate(story, test, frontendTestType);
          const generated = this.generateTest(template, story, testPlan);
          generatedTests.push(generated);
        }
      }
    }

    return generatedTests;
  }

  /**
   * Map generic test type to frontend-specific type
   * Performance: Uses pre-compiled mapping object
   */
  private mapToFrontendTestType(testType: string): FrontendTestType | null {
    return this.testTypeMapping[testType.toLowerCase()] || null;
  }

  /**
   * Create test template from story and test plan
   */
  private createTemplate(
    story: StoryModel,
    test: PlannedTest,
    testType: FrontendTestType
  ): FrontendTestTemplate {
    const componentName = this.extractComponentName(story);
    const componentPath = this.inferComponentPath(componentName, story);
    const requiresMockData = testType === 'integration' || test.testTypes.includes('integration');

    return {
      name: `${story.storyId} - ${testType}`,
      testType,
      componentPath,
      useMSW: requiresMockData,
      useUserEvent: testType === 'component' || testType === 'form',
      useAxe: testType === 'accessibility',
      imports: this.determineImports(testType, requiresMockData),
      setupCode: this.generateSetupCode(testType, requiresMockData),
      teardownCode: this.generateTeardownCode(testType)
    };
  }

  /**
   * Extract component name from story
   * Performance: Uses cached regex and memoizes results
   */
  private extractComponentName(story: StoryModel): string {
    // Check cache first
    const cached = this.componentNameCache.get(story.storyId);
    if (cached) {
      return cached;
    }

    // If workspace context is available, find actual component
    if (this.workspaceContext) {
      const relevantComponents = this.findRelevantComponents(story);
      if (relevantComponents.length > 0) {
        const componentName = relevantComponents[0].componentName;
        this.componentNameCache.set(story.storyId, componentName);
        return componentName;
      }
    }

    // Fallback: Look for component name in title
    const titleMatch = this.componentNameRegex.exec(story.title);
    if (titleMatch) {
      const componentName = titleMatch[1];
      this.componentNameCache.set(story.storyId, componentName);
      return componentName;
    }

    // Default based on epic
    const epicMatch = this.epicNumberRegex.exec(story.epicPath || '');
    const epicNum = epicMatch?.[1] || 'Unknown';
    const componentName = `Epic${epicNum}Component`;
    this.componentNameCache.set(story.storyId, componentName);
    return componentName;
  }

  /**
   * Infer component path from component name and story
   * Performance: Optimized string operations
   */
  private inferComponentPath(componentName: string, story: StoryModel): string {
    // If workspace context is available, use actual path
    if (this.workspaceContext) {
      const relevantComponents = this.findRelevantComponents(story);
      if (relevantComponents.length > 0) {
        // Return relative path from frontend/src
        return relevantComponents[0].relativePath.replace(/\.(tsx|jsx)$/, '');
      }
    }
    
    // Fallback: Convert PascalCase to kebab-case using split (faster than regex replace)
    const kebabName = componentName
      .split(/(?=[A-Z])/)
      .join('-')
      .toLowerCase();
    
    // Determine directory based on component type (faster than multiple includes())
    let directory: string;
    if (componentName.endsWith('Page')) {
      directory = 'pages';
    } else if (componentName.endsWith('Form')) {
      directory = 'forms';
    } else if (componentName.endsWith('Modal')) {
      directory = 'modals';
    } else {
      directory = 'components';
    }

    return `src/${directory}/${kebabName}`;
  }
  
  /**
   * Find relevant frontend components from workspace context
   */
  private findRelevantComponents(story: StoryModel): FrontendComponent[] {
    if (!this.workspaceContext) {
      return [];
    }
    
    const storyText = `${story.title} ${story.acceptanceCriteria.join(' ')}`.toLowerCase();
    const relevant: FrontendComponent[] = [];
    
    for (const component of this.workspaceContext.frontendComponents) {
      // Check if component name appears in story (case-insensitive)
      if (storyText.includes(component.componentName.toLowerCase())) {
        relevant.push(component);
        continue;
      }
      
      // Check if component name without suffix matches
      const baseName = component.componentName
        .replace(/(Form|Page|Modal|Button|Input|Table|List|Card|Panel)$/, '')
        .toLowerCase();
      if (baseName.length > 3 && storyText.includes(baseName)) {
        relevant.push(component);
      }
    }
    
    return relevant;
  }

  /**
   * Determine imports based on test type and requirements
   */
  private determineImports(testType: FrontendTestType, requiresMockData: boolean): string[] {
    const baseImports = [
      "import { render, screen, waitFor } from '@testing-library/react';",
      "import '@testing-library/jest-dom';"
    ];

    const imports: string[] = [...baseImports];

    if (testType === 'component' || testType === 'form' || testType === 'integration') {
      imports.push("import userEvent from '@testing-library/user-event';");
    }

    if (testType === 'hook') {
      imports.push("import { renderHook, waitFor } from '@testing-library/react';");
    }

    if (testType === 'accessibility') {
      imports.push(
        "import { axe, toHaveNoViolations } from 'jest-axe';",
        "expect.extend(toHaveNoViolations);"
      );
    }

    if (requiresMockData) {
      imports.push(
        `import { server } from '${this.mswServerPath}';`,
        "import { rest } from 'msw';"
      );
    }

    // Allure imports
    imports.push("import { allure } from 'allure-jest/dist/legacy';");

    return imports;
  }

  /**
   * Generate setup code for tests
   */
  private generateSetupCode(testType: FrontendTestType, requiresMockData: boolean): string {
    if (!requiresMockData) {
      return '';
    }

    return [
      '  beforeAll(() => server.listen());',
      '  afterEach(() => server.resetHandlers());',
      '  afterAll(() => server.close());'
    ].join('\n');
  }

  /**
   * Generate teardown code for tests
   */
  private generateTeardownCode(testType: FrontendTestType): string {
    return '';  // Most cleanup handled by testing-library
  }

  /**
   * Generate test code from template
   */
  private generateTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    testPlan: TestPlan
  ): GeneratedFrontendTest {
    const componentName = this.extractComponentName(story);
    const severity = this.determineSeverity(story);
    
    let testContent = '';

    // Generate based on test type
    switch (template.testType) {
      case 'component':
        testContent = this.generateComponentTest(template, story, componentName, severity);
        break;
      case 'hook':
        testContent = this.generateHookTest(template, story, componentName, severity);
        break;
      case 'form':
        testContent = this.generateFormTest(template, story, componentName, severity);
        break;
      case 'integration':
        testContent = this.generateIntegrationTest(template, story, componentName, severity);
        break;
      case 'accessibility':
        testContent = this.generateAccessibilityTest(template, story, componentName, severity);
        break;
    }

    const outputPath = this.determineOutputPath(componentName, template.testType, story);
    
    // Write file
    mkdirSync(dirname(outputPath), { recursive: true });
    writeFileSync(outputPath, testContent, 'utf-8');

    return {
      filePath: outputPath,
      testType: template.testType,
      content: testContent,
      imports: template.imports,
      mswHandlers: [],
      testCount: this.countTestCases(story),
      metadata: {
        generatedAt: new Date(),
        storyId: story.storyId,
        componentName
      }
    };
  }

  /**
   * Generate component test
   * Performance: Optimized string building with array join
   */
  private generateComponentTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    componentName: string,
    severity: AllureSeverity
  ): string {
    const acceptanceCriteria = Array.isArray(story.acceptanceCriteria)
      ? story.acceptanceCriteria
      : [story.acceptanceCriteria];

    // Use array for efficient string building
    const parts: string[] = [
      template.imports.join('\n'),
      `import ${componentName} from './${componentName}';`,
      '',
      '/**',
      ` * ${story.title}`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ' * @feature Frontend Components',
      ` * @story ${story.storyId}`,
      ' */',
      `describe('${componentName}', () => {`,
      '  beforeEach(() => {',
      `    allure.epic('${story.epicTitle}');`,
      '    allure.feature(\'Frontend Components\');',
      `    allure.story('${story.storyId}: ${story.title}');`,
      `    allure.severity('${severity}');`,
      '  });',
      ''
    ];

    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }

    // Generate test cases
    for (let i = 0; i < acceptanceCriteria.length; i++) {
      parts.push(this.generateComponentTestCase(acceptanceCriteria[i], i, componentName));
      if (i < acceptanceCriteria.length - 1) {
        parts.push('');
      }
    }

    parts.push('});', '');

    return parts.join('\n');
  }

  /**
   * Generate individual component test case
   */
  private generateComponentTestCase(criteria: string, index: number, componentName: string): string {
    return `  it('${criteria}', async () => {
    allure.description('${criteria}');
    
    const user = userEvent.setup();
    render(<${componentName} />);
    
    // Verify component renders
    expect(screen.getByRole('main')).toBeInTheDocument();
    
    // TODO: Add specific assertions based on acceptance criteria
    // Example: expect(screen.getByText('...')).toBeInTheDocument();
  });`;
  }

  /**
   * Generate hook test
   * Performance: Optimized string building with array join
   */
  private generateHookTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    hookName: string,
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      `import { ${hookName} } from './${hookName}';`,
      '',
      '/**',
      ` * ${story.title}`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ' * @feature React Hooks',
      ` * @story ${story.storyId}`,
      ' */',
      `describe('${hookName}', () => {`,
      '  beforeEach(() => {',
      `    allure.epic('${story.epicTitle}');`,
      '    allure.feature(\'React Hooks\');',
      `    allure.story('${story.storyId}: ${story.title}');`,
      `    allure.severity('${severity}');`,
      '  });',
      ''
    ];

    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }

    // Default hook test cases
    parts.push(
      `  it('should initialize with correct default values', () => {`,
      `    allure.description('Verify hook initializes with correct default state');`,
      '    ',
      `    const { result } = renderHook(() => ${hookName}());`,
      '    ',
      '    expect(result.current).toBeDefined();',
      '    // TODO: Add specific assertions for hook return value',
      '  });',
      '',
      `  it('should handle state updates correctly', async () => {`,
      `    allure.description('Verify hook handles state updates');`,
      '    ',
      `    const { result } = renderHook(() => ${hookName}());`,
      '    ',
      '    // TODO: Trigger state updates and verify behavior',
      '    ',
      '    await waitFor(() => {',
      '      // Assert expected state',
      '    });',
      '  });',
      '});',
      ''
    );

    return parts.join('\n');
  }

  /**
   * Generate form test
   * Performance: Optimized string building with array join
   */
  private generateFormTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    formName: string,
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      `import ${formName} from './${formName}';`,
      '',
      '/**',
      ` * ${story.title}`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ' * @feature Form Validation',
      ` * @story ${story.storyId}`,
      ' */',
      `describe('${formName}', () => {`,
      '  beforeEach(() => {',
      `    allure.epic('${story.epicTitle}');`,
      '    allure.feature(\'Form Validation\');',
      `    allure.story('${story.storyId}: ${story.title}');`,
      `    allure.severity('${severity}');`,
      '  });',
      ''
    ];

    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }

    parts.push(
      `  it('should render all form fields', () => {`,
      `    allure.description('Verify all form fields are rendered correctly');`,
      '    ',
      `    render(<${formName} />);`,
      '    ',
      '    // TODO: Add assertions for each form field',
      '    // Example: expect(screen.getByLabelText(\'Field Name\')).toBeInTheDocument();',
      '  });',
      '',
      `  it('should validate required fields', async () => {`,
      `    allure.description('Verify required field validation');`,
      '    ',
      '    const user = userEvent.setup();',
      `    render(<${formName} />);`,
      '    ',
      '    // Submit without filling required fields',
      '    const submitButton = screen.getByRole(\'button\', { name: /submit/i });',
      '    await user.click(submitButton);',
      '    ',
      '    // Verify validation errors appear',
      '    await waitFor(() => {',
      '      // TODO: Add assertions for validation error messages',
      '    });',
      '  });',
      '',
      `  it('should submit form with valid data', async () => {`,
      `    allure.description('Verify form submits successfully with valid data');`,
      '    ',
      '    const user = userEvent.setup();',
      '    const onSubmit = jest.fn();',
      `    render(<${formName} onSubmit={onSubmit} />);`,
      '    ',
      '    // Fill in form fields',
      '    // TODO: Add user interactions to fill form',
      '    ',
      '    // Submit form',
      '    const submitButton = screen.getByRole(\'button\', { name: /submit/i });',
      '    await user.click(submitButton);',
      '    ',
      '    await waitFor(() => {',
      '      expect(onSubmit).toHaveBeenCalled();',
      '    });',
      '  });',
      '});',
      ''
    );

    return parts.join('\n');
  }

  /**
   * Generate integration test with API calls
   * Performance: Optimized string building with array join
   */
  private generateIntegrationTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    componentName: string,
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      `import ${componentName} from './${componentName}';`,
      '',
      '/**',
      ` * ${story.title} - Integration Tests`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ' * @feature API Integration',
      ` * @story ${story.storyId}`,
      ' */',
      `describe('${componentName} - Integration', () => {`,
      '  beforeEach(() => {',
      `    allure.epic('${story.epicTitle}');`,
      '    allure.feature(\'API Integration\');',
      `    allure.story('${story.storyId}: ${story.title}');`,
      `    allure.severity('${severity}');`,
      '  });',
      ''
    ];

    if (template.setupCode) {
      parts.push(template.setupCode, '');
    }

    parts.push(
      `  it('should fetch and display data on mount', async () => {`,
      `    allure.description('Verify component fetches and displays API data');`,
      '    ',
      '    // Mock API response',
      '    server.use(',
      '      rest.get(\'/api/data\', (req, res, ctx) => {',
      '        return res(',
      '          ctx.status(200),',
      '          ctx.json({ data: \'test data\' })',
      '        );',
      '      })',
      '    );',
      '    ',
      `    render(<${componentName} />);`,
      '    ',
      '    // Verify loading state',
      '    expect(screen.getByText(/loading/i)).toBeInTheDocument();',
      '    ',
      '    // Wait for data to load',
      '    await waitFor(() => {',
      '      expect(screen.getByText(\'test data\')).toBeInTheDocument();',
      '    });',
      '  });',
      '',
      `  it('should handle API errors gracefully', async () => {`,
      `    allure.description('Verify component handles API errors');`,
      '    ',
      '    // Mock API error',
      '    server.use(',
      '      rest.get(\'/api/data\', (req, res, ctx) => {',
      '        return res(',
      '          ctx.status(500),',
      '          ctx.json({ error: \'Internal server error\' })',
      '        );',
      '      })',
      '    );',
      '    ',
      `    render(<${componentName} />);`,
      '    ',
      '    // Verify error message is displayed',
      '    await waitFor(() => {',
      '      expect(screen.getByText(/error/i)).toBeInTheDocument();',
      '    });',
      '  });',
      '});',
      ''
    );

    return parts.join('\n');
  }

  /**
   * Generate accessibility test
   * Performance: Optimized string building with array join
   */
  private generateAccessibilityTest(
    template: FrontendTestTemplate,
    story: StoryModel,
    componentName: string,
    severity: AllureSeverity
  ): string {
    const parts: string[] = [
      template.imports.join('\n'),
      `import ${componentName} from './${componentName}';`,
      '',
      '/**',
      ` * ${story.title} - Accessibility Tests`,
      ` * Story ID: ${story.storyId}`,
      ' * ',
      ` * @epic ${story.epicTitle}`,
      ' * @feature Accessibility',
      ` * @story ${story.storyId}`,
      ' */',
      `describe('${componentName} - Accessibility', () => {`,
      '  beforeEach(() => {',
      `    allure.epic('${story.epicTitle}');`,
      '    allure.feature(\'Accessibility\');',
      `    allure.story('${story.storyId}: ${story.title}');`,
      `    allure.severity('${severity}');`,
      '  });',
      '',
      `  it('should have no accessibility violations', async () => {`,
      `    allure.description('Verify component passes axe accessibility checks');`,
      '    ',
      `    const { container } = render(<${componentName} />);`,
      '    const results = await axe(container);',
      '    ',
      '    expect(results).toHaveNoViolations();',
      '  });',
      '',
      `  it('should be keyboard navigable', async () => {`,
      `    allure.description('Verify component is keyboard accessible');`,
      '    ',
      '    const user = userEvent.setup();',
      `    render(<${componentName} />);`,
      '    ',
      '    // TODO: Test keyboard navigation',
      '    // Example: await user.tab();',
      '  });',
      '',
      `  it('should have proper ARIA labels', () => {`,
      `    allure.description('Verify component has proper ARIA attributes');`,
      '    ',
      `    render(<${componentName} />);`,
      '    ',
      '    // TODO: Verify ARIA labels',
      '    // Example: expect(screen.getByRole(\'button\')).toHaveAccessibleName(\'...\');',
      '  });',
      '});',
      ''
    ];

    return parts.join('\n');
  }

  /**
   * Determine severity from story
   * Performance: Memoized with cached regex
   */
  private determineSeverity(story: StoryModel): AllureSeverity {
    // Check cache first
    const cached = this.severityCache.get(story.storyId);
    if (cached) {
      return cached;
    }

    let severity: AllureSeverity;

    // Check if story path indicates epic number
    const epicMatch = this.epicNumberRegex.exec(story.epicPath || '');
    if (epicMatch) {
      const epicNum = Number.parseInt(epicMatch[1], 10);
      if (epicNum <= 4) {
        severity = 'critical';
      } else if (epicNum <= 6) {
        severity = 'normal';
      } else {
        severity = 'minor';
      }
    } else {
      // Check title for severity keywords (single toLowerCase call)
      const title = story.title.toLowerCase();
      if (title.includes('critical') || title.includes('security')) {
        severity = 'blocker';
      } else if (title.includes('important') || title.includes('core')) {
        severity = 'critical';
      } else if (title.includes('nice to have') || title.includes('enhancement')) {
        severity = 'minor';
      } else {
        severity = 'normal';
      }
    }

    this.severityCache.set(story.storyId, severity);
    return severity;
  }

  /**
   * Determine output path for test file
   */
  private determineOutputPath(componentName: string, testType: FrontendTestType, story: StoryModel): string {
    // Convert PascalCase to kebab-case using split approach (simpler than regex with replace)
    const kebabName = componentName
      .split(/(?=[A-Z])/)
      .join('-')
      .toLowerCase();
    const testSuffix = testType === 'integration' ? '.integration.test.tsx' : '.test.tsx';
    const filename = `${kebabName}${testSuffix}`;
    
    return join(this.outputDir, story.normalizedId || 'generated', filename);
  }

  /**
   * Count test cases from acceptance criteria
   */
  private countTestCases(story: StoryModel): number {
    if (Array.isArray(story.acceptanceCriteria)) {
      return story.acceptanceCriteria.length;
    }
    return 1;
  }
}
