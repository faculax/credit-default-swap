/**
 * Backend Test Generator (Story 20.3)
 * 
 * Generates JUnit 5 tests for backend services with:
 * - Allure annotations for reporting
 * - Dataset injection from TestDataRegistry
 * - AssertJ assertions
 * - Mockito for dependencies
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import { StoryModel } from '../models/story-model';
import { TestPlan } from '../models/test-plan-model';
import {
  BackendTestTemplate,
  BackendTestGenerationConfig,
  GeneratedTestClass,
  GenerationResult,
  TestMethod,
  AllureAnnotations,
  TestField,
  GeneratedTestMethod
} from '../models/backend-test-model';

export class BackendTestGenerator {
  private config: BackendTestGenerationConfig;
  private datasetRegistry: any;

  constructor(config: BackendTestGenerationConfig) {
    this.config = config;
    this.loadDatasetRegistry();
  }

  /**
   * Generate tests for a story
   */
  generateTests(story: StoryModel, testPlan: TestPlan): GenerationResult[] {
    const results: GenerationResult[] = [];

    // Determine which templates to use based on test plan
    const templates = this.selectTemplates(testPlan);

    for (const template of templates) {
      try {
        const result = this.generateTestClass(story, template, testPlan);
        results.push(result);
      } catch (error) {
        results.push({
          success: false,
          testClass: {} as GeneratedTestClass,
          filePath: '',
          errors: [error instanceof Error ? error.message : String(error)],
          warnings: []
        });
      }
    }

    return results;
  }

  /**
   * Generate a single test class
   */
  private generateTestClass(
    story: StoryModel,
    template: BackendTestTemplate,
    testPlan: TestPlan
  ): GenerationResult {
    // Build class name
    const className = this.buildClassName(story, template);
    const packageName = this.buildPackageName(story, template);

    // Generate test methods from acceptance criteria
    const testMethods = this.generateTestMethods(story, template);

    // Build complete test class
    const testClass: GeneratedTestClass = {
      className,
      packageName,
      imports: this.generateImports(template, testMethods),
      annotations: this.buildAllureAnnotations(story),
      classJavadoc: this.buildClassJavadoc(story, template),
      fields: this.generateFields(template, testMethods),
      setupMethod: this.generateSetupMethod(template, testMethods),
      teardownMethod: this.generateTeardownMethod(template),
      testMethods: this.generateTestMethodCode(testMethods, story),
      sourceStory: story
    };

    // Generate Java source code
    const sourceCode = this.renderTestClass(testClass);

    // Write to file
    const filePath = this.writeTestFile(testClass, sourceCode);

    return {
      success: true,
      testClass,
      filePath,
      errors: [],
      warnings: []
    };
  }

  /**
   * Select appropriate templates based on test plan
   */
  private selectTemplates(testPlan: TestPlan): BackendTestTemplate[] {
    // Extract unique test types from plannedTests
    const testTypes = testPlan.plannedTests
      .flatMap(pt => pt.testTypes)
      .filter((type, index, self) => self.indexOf(type) === index);
    
    const templates: BackendTestTemplate[] = [];

    for (const testType of testTypes) {
      switch (testType) {
        case 'unit':
          templates.push('service', 'unit');
          break;
        case 'integration':
          templates.push('integration');
          break;
        case 'api':
          templates.push('controller');
          break;
      }
    }

    // Default to service test if no types specified
    if (templates.length === 0) {
      templates.push('service');
    }

    return [...new Set(templates)]; // Remove duplicates
  }

  /**
   * Build test class name
   */
  private buildClassName(story: StoryModel, template: BackendTestTemplate): string {
    // Extract main entity from story title
    const entity = this.extractEntityName(story.title);
    
    switch (template) {
      case 'service':
        return `${entity}ServiceTest`;
      case 'repository':
        return `${entity}RepositoryTest`;
      case 'controller':
        return `${entity}ControllerIntegrationTest`;
      case 'integration':
        return `${entity}IntegrationTest`;
      case 'unit':
        return `${entity}Test`;
      default:
        return `${entity}Test`;
    }
  }

  /**
   * Build package name
   */
  private buildPackageName(story: StoryModel, template: BackendTestTemplate): string {
    const basePackage = this.config.basePackage;
    
    // Map template to package structure
    const subPackage = template === 'integration' 
      ? 'integration' 
      : template === 'controller' 
      ? 'controller' 
      : 'service';
    
    return `${basePackage}.${subPackage}`;
  }

  /**
   * Extract entity name from story title
   */
  private extractEntityName(title: string): string {
    // Remove common prefixes/suffixes
    const cleaned = title
      .replace(/^(Create|Update|Delete|Manage|Process|Handle)\s+/i, '')
      .replace(/\s+(Entry|Processing|Management|Handler)$/i, '');
    
    // Convert to PascalCase
    return cleaned
      .split(/[\s-_]+/)
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join('');
  }

  /**
   * Generate test methods from acceptance criteria
   */
  private generateTestMethods(story: StoryModel, template: BackendTestTemplate): TestMethod[] {
    const methods: TestMethod[] = [];

    // Parse acceptance criteria (join array into string)
    const criteriaContent = Array.isArray(story.acceptanceCriteria) 
      ? story.acceptanceCriteria.join('\n') 
      : story.acceptanceCriteria || '';
    const criteria = this.parseAcceptanceCriteria(criteriaContent);

    for (let index = 0; index < criteria.length; index++) {
      const ac = criteria[index];
      const methodName = this.buildMethodName(ac.description, index);
      
      // Determine test type
      let testType: 'api' | 'integration' | 'unit';
      if (template === 'controller') {
        testType = 'api';
      } else if (template === 'integration') {
        testType = 'integration';
      } else {
        testType = 'unit';
      }
      
      methods.push({
        methodName,
        displayName: ac.description,
        description: `Test for: ${ac.description}`,
        testType,
        acceptanceCriteria: `AC${index + 1}`,
        datasetPath: this.findDatasetForTest(story, template),
        expectedOutcome: ac.expected || 'Success',
        assertions: this.generateAssertions(ac),
        setupCode: [],
        teardownCode: []
      });
    }

    return methods;
  }

  /**
   * Parse acceptance criteria from markdown
   */
  private parseAcceptanceCriteria(content: string): Array<{description: string, expected: string}> {
    const criteria: Array<{description: string, expected: string}> = [];
    const lines = content.split('\n');

    let currentCriteria = '';
    let currentExpected = '';

    for (const line of lines) {
      const trimmed = line.trim();
      
      if (trimmed.match(/^(AC\d+|Given|When|Then|And):/i)) {
        if (currentCriteria) {
          criteria.push({
            description: currentCriteria,
            expected: currentExpected || 'Success'
          });
        }
        currentCriteria = trimmed.replace(/^(AC\d+|Given|When|Then|And):\s*/i, '');
        currentExpected = '';
      } else if (trimmed.match(/^Expected:/i)) {
        currentExpected = trimmed.replace(/^Expected:\s*/i, '');
      } else if (trimmed && currentCriteria) {
        currentCriteria += ' ' + trimmed;
      }
    }

    if (currentCriteria) {
      criteria.push({
        description: currentCriteria,
        expected: currentExpected || 'Success'
      });
    }

    return criteria.length > 0 ? criteria : [{
      description: 'Should pass basic validation',
      expected: 'Success'
    }];
  }

  /**
   * Build method name from description
   */
  private buildMethodName(description: string, index: number): string {
    // Extract key action words
    const cleaned = description
      .replace(/^(System|User|The|A|An)\s+/i, '')
      .replace(/[^\w\s]/g, '')
      .toLowerCase();
    
    const words = cleaned.split(/\s+/).slice(0, 6); // Limit to 6 words
    
    const camelCase = words.map((word, i) => 
      i === 0 ? word : word.charAt(0).toUpperCase() + word.slice(1)
    ).join('');
    
    return `should${camelCase.charAt(0).toUpperCase() + camelCase.slice(1)}`;
  }

  /**
   * Find appropriate dataset for test
   */
  private findDatasetForTest(story: StoryModel, template: BackendTestTemplate): string | undefined {
    if (!this.datasetRegistry?.datasets) {
      return undefined;
    }

    // Try to find dataset by story ID in usedBy array
    const matchingDataset = this.datasetRegistry.datasets.find((dataset: any) =>
      dataset.usedBy?.some((usage: string) => 
        usage.includes(story.storyId) || usage.includes(story.title)
      )
    );

    if (matchingDataset) {
      return matchingDataset.path;
    }

    // Fallback: find by type based on story content
    if (story.title.toLowerCase().includes('trade')) {
      return 'cds-trades/single-name-basic.json';
    } else if (story.title.toLowerCase().includes('pricing')) {
      return 'market-data/usd-ois-curve.json';
    }

    return undefined;
  }

  /**
   * Generate assertions for acceptance criteria
   */
  private generateAssertions(ac: {description: string, expected: string}): string[] {
    const assertions: string[] = [];

    // Analyze description for assertion hints
    const desc = ac.description.toLowerCase();

    if (desc.includes('persist') || desc.includes('save') || desc.includes('create')) {
      assertions.push('assertThat(result).isNotNull()');
      assertions.push('assertThat(result.getId()).isNotNull()');
    }

    if (desc.includes('retrieve') || desc.includes('fetch') || desc.includes('get')) {
      assertions.push('assertThat(result).isNotNull()');
    }

    if (desc.includes('update') || desc.includes('modify')) {
      assertions.push('assertThat(result.getUpdatedAt()).isAfter(result.getCreatedAt())');
    }

    if (desc.includes('delete') || desc.includes('remove')) {
      assertions.push('assertThat(result).isNull()');
    }

    if (desc.includes('validate') || desc.includes('valid')) {
      assertions.push('assertThat(result.isValid()).isTrue()');
    }

    // Default assertion if none generated
    if (assertions.length === 0) {
      assertions.push('assertThat(result).isNotNull()');
    }

    return assertions;
  }

  /**
   * Load dataset registry
   */
  private loadDatasetRegistry(): void {
    try {
      const registryPath = path.resolve(this.config.datasetRegistryPath);
      if (fs.existsSync(registryPath)) {
        const content = fs.readFileSync(registryPath, 'utf-8');
        this.datasetRegistry = JSON.parse(content);
      }
    } catch (error) {
      console.warn('Failed to load dataset registry:', error);
      this.datasetRegistry = null;
    }
  }

  /**
   * Generate imports
   */
  private generateImports(template: BackendTestTemplate, methods: TestMethod[]): string[] {
    const imports = new Set<string>([
      'org.junit.jupiter.api.*',
      'org.springframework.boot.test.context.SpringBootTest',
      'org.springframework.beans.factory.annotation.Autowired',
    ]);

    if (this.config.useAssertJ) {
      imports.add('static org.assertj.core.api.Assertions.*');
    }

    if (this.config.useAllure) {
      imports.add('io.qameta.allure.*');
    }

    // Add dataset loader if any method uses datasets
    if (methods.some(m => m.datasetPath)) {
      imports.add('com.cds.platform.test.data.DatasetLoader');
    }

    if (template === 'controller') {
      imports.add('org.springframework.boot.test.web.client.TestRestTemplate');
      imports.add('org.springframework.http.ResponseEntity');
    }

    return Array.from(imports).sort();
  }

  /**
   * Build Allure annotations
   */
  private buildAllureAnnotations(story: StoryModel): AllureAnnotations {
    return {
      epic: story.epicTitle || 'Unknown Epic',
      feature: story.title,
      story: story.storyId,
      severity: this.determineSeverity(story),
      tmsLink: story.normalizedId
    };
  }

  /**
   * Determine test severity based on epic number
   */
  private determineSeverity(story: StoryModel): 'blocker' | 'critical' | 'normal' | 'minor' | 'trivial' {
    // Infer severity from epic number (lower epic = more critical)
    const epicMatch = story.epicPath?.match(/epic_(\d+)/);
    const epicNum = epicMatch ? parseInt(epicMatch[1]) : 5;
    
    // Epic 1-4: Core functionality (CRITICAL)
    if (epicNum <= 4) return 'critical';
    
    // Epic 5-6: Important features (NORMAL)
    if (epicNum <= 6) return 'normal';
    
    // Epic 7+: Nice-to-have (MINOR)
    return 'minor';
  }

  /**
   * Build class JavaDoc
   */
  private buildClassJavadoc(story: StoryModel, template: BackendTestTemplate): string {
    return `/**
 * Test class for: ${story.title}
 * 
 * Epic: ${story.epicTitle || 'Unknown Epic'}
 * Story: ${story.storyId}
 * Template: ${template}
 * 
 * Auto-generated by Test Evidence Framework
 */`;
  }

  /**
   * Generate class fields
   */
  private generateFields(template: BackendTestTemplate, methods: TestMethod[]): TestField[] {
    const fields: TestField[] = [];

    // Add service under test
    const entityName = template === 'controller' ? 'Controller' : 'Service';
    fields.push({
      name: 'testService',
      type: `CDSTrade${entityName}`, // TODO: Extract from story
      annotation: '@Autowired'
    });

    // Add dataset field if needed
    if (methods.some(m => m.datasetPath)) {
      fields.push({
        name: 'testData',
        type: 'CDSTrade', // TODO: Determine from dataset
        annotation: undefined
      });
    }

    return fields;
  }

  /**
   * Generate setup method
   */
  private generateSetupMethod(template: BackendTestTemplate, methods: TestMethod[]): any {
    const hasDatasets = methods.some(m => m.datasetPath);
    
    if (!hasDatasets) {
      return undefined;
    }

    const datasetPath = methods.find(m => m.datasetPath)?.datasetPath;

    return {
      methodName: 'setUp',
      annotation: '@BeforeEach' as const,
      code: [
        `testData = DatasetLoader.load("${datasetPath}", CDSTrade.class);`
      ]
    };
  }

  /**
   * Generate teardown method
   */
  private generateTeardownMethod(template: BackendTestTemplate): any {
    // Only for integration tests that need cleanup
    if (template === 'integration') {
      return {
        methodName: 'tearDown',
        annotation: '@AfterEach' as const,
        code: ['// Cleanup test data']
      };
    }
    return undefined;
  }

  /**
   * Generate test method code
   */
  private generateTestMethodCode(methods: TestMethod[], story: StoryModel): GeneratedTestMethod[] {
    return methods.map((method, index) => ({
      config: method,
      annotations: {
        story: `AC${index + 1}: ${method.displayName}`,
        severity: 'critical' as const
      },
      javadoc: `/**\n     * ${method.description}\n     */`,
      code: this.renderTestMethod(method)
    }));
  }

  /**
   * Render test method code
   */
  private renderTestMethod(method: TestMethod): string {
    const lines: string[] = [];

    // Given
    lines.push('// Given');
    if (method.datasetPath) {
      lines.push('// Test data loaded in setUp()');
    }

    // When
    lines.push('');
    lines.push('// When');
    lines.push('var result = testService.process(testData);');

    // Then
    lines.push('');
    lines.push('// Then');
    method.assertions.forEach(assertion => {
      lines.push(`${assertion};`);
    });

    return lines.join('\n        ');
  }

  /**
   * Render complete test class
   */
  private renderTestClass(testClass: GeneratedTestClass): string {
    const lines: string[] = [];

    // Package
    lines.push(`package ${testClass.packageName};`);
    lines.push('');

    // Imports
    testClass.imports.forEach(imp => {
      lines.push(`import ${imp};`);
    });
    lines.push('');

    // Class JavaDoc
    lines.push(testClass.classJavadoc);

    // Allure annotations
    if (this.config.useAllure) {
      lines.push(`@Epic("${testClass.annotations.epic}")`);
      lines.push(`@Feature("${testClass.annotations.feature}")`);
    }

    // SpringBootTest annotation
    lines.push('@SpringBootTest');
    lines.push(`public class ${testClass.className} {`);
    lines.push('');

    // Fields
    testClass.fields.forEach(field => {
      if (field.annotation) {
        lines.push(`    ${field.annotation}`);
      }
      lines.push(`    private ${field.type} ${field.name};`);
      lines.push('');
    });

    // Setup method
    if (testClass.setupMethod) {
      lines.push(`    ${testClass.setupMethod.annotation}`);
      lines.push(`    void ${testClass.setupMethod.methodName}() {`);
      testClass.setupMethod.code.forEach(code => {
        lines.push(`        ${code}`);
      });
      lines.push(`    }`);
      lines.push('');
    }

    // Test methods
    testClass.testMethods.forEach(method => {
      lines.push(`    ${method.javadoc}`);
      lines.push(`    @Test`);
      if (this.config.useAllure && method.annotations.story) {
        lines.push(`    @Story("${method.annotations.story}")`);
        lines.push(`    @Severity(SeverityLevel.${method.annotations.severity?.toUpperCase()})`);
      }
      lines.push(`    void ${method.config.methodName}() {`);
      lines.push(`        ${method.code}`);
      lines.push(`    }`);
      lines.push('');
    });

    lines.push('}');

    return lines.join('\n');
  }

  /**
   * Write test file to disk
   */
  private writeTestFile(testClass: GeneratedTestClass, sourceCode: string): string {
    const packagePath = testClass.packageName.replace(/\./g, path.sep);
    const outputDir = path.join(this.config.outputDir, packagePath);
    
    // Create directory if it doesn't exist
    if (!fs.existsSync(outputDir)) {
      fs.mkdirSync(outputDir, { recursive: true });
    }

    const filePath = path.join(outputDir, `${testClass.className}.java`);
    fs.writeFileSync(filePath, sourceCode, 'utf-8');

    return filePath;
  }
}
