/**
 * Test Plan Exporter
 * 
 * Exports test plans to various formats (JSON, Markdown, HTML) for easy reference
 * and documentation. Supports exporting individual plans or entire catalog.
 * 
 * Story 20.9: Test Plan Export and Documentation
 */

import * as fs from 'fs';
import * as path from 'path';
import { TestPlan, TestPlanStatistics } from '../models/test-plan-model';
import { ServiceName, TestType } from '../models/story-model';
import { TestPlanCatalog } from '../catalog/test-plan-catalog';

export interface TestPlanExportOptions {
  /** Output directory for exported plans */
  outputDir: string;
  
  /** Export format */
  format: 'json' | 'markdown' | 'html' | 'all';
  
  /** Include test data samples */
  includeTestData?: boolean;
  
  /** Include acceptance criteria details */
  includeAcceptanceCriteria?: boolean;
  
  /** Include test scenarios */
  includeScenarios?: boolean;
  
  /** Generate index/catalog file */
  generateIndex?: boolean;
}

export interface ExportedTestPlan {
  /** Story ID */
  storyId: string;
  
  /** Story title */
  title: string;
  
  /** Epic */
  epic?: string;
  
  /** Planned services */
  services: ServiceName[];
  
  /** Test types by service */
  testsByService: Record<ServiceName, TestType[]>;
  
  /** Acceptance criteria coverage */
  acceptanceCriteria: string[];
  
  /** Test scenarios */
  scenarios?: string[];
  
  /** Test data samples */
  testDataSamples?: Record<string, any>;
  
  /** Generated files */
  generatedFiles: {
    path: string;
    type: 'frontend' | 'backend' | 'gateway' | 'risk-engine';
    testCases: number;
  }[];
  
  /** Export timestamp */
  exportedAt: string;
}

export class TestPlanExporter {
  constructor(private options: TestPlanExportOptions) {
    this.ensureOutputDir();
  }
  
  /**
   * Ensure output directory exists
   */
  private ensureOutputDir(): void {
    if (!fs.existsSync(this.options.outputDir)) {
      fs.mkdirSync(this.options.outputDir, { recursive: true });
    }
  }
  
  /**
   * Export single test plan
   */
  exportPlan(plan: TestPlan): ExportedTestPlan {
    const exported: ExportedTestPlan = {
      storyId: plan.storyId,
      title: plan.title,
      epic: plan.story.epicTitle || plan.story.epicPath || 'Unknown Epic',
      services: plan.plannedServices,
      testsByService: this.groupTestsByService(plan),
      acceptanceCriteria: plan.story.acceptanceCriteria,
      exportedAt: new Date().toISOString(),
      generatedFiles: []
    };
    
    if (this.options.includeScenarios && plan.story.testScenarios) {
      exported.scenarios = plan.story.testScenarios;
    }
    
    if (this.options.includeTestData) {
      exported.testDataSamples = this.generateTestDataSamples(plan);
    }
    
    return exported;
  }
  
  /**
   * Group tests by service
   */
  private groupTestsByService(plan: TestPlan): Record<ServiceName, TestType[]> {
    const grouped: Record<ServiceName, TestType[]> = {} as any;
    
    for (const plannedTest of plan.plannedTests) {
      grouped[plannedTest.service] = plannedTest.testTypes;
    }
    
    return grouped;
  }
  
  /**
   * Generate test data samples
   */
  private generateTestDataSamples(plan: TestPlan): Record<string, any> {
    // Extract sample data from story content or generate based on entity types
    const samples: Record<string, any> = {};
    
    // Add story-specific samples
    samples.storyId = plan.storyId;
    samples.title = plan.title;
    samples.acceptanceCriteriaCount = plan.story.acceptanceCriteria.length;
    samples.testScenariosCount = plan.story.testScenarios?.length || 0;
    
    // Add test strategy summary
    samples.testStrategy = {
      services: plan.plannedServices,
      plannedTests: plan.plannedTests.map(pt => ({
        service: pt.service,
        testTypes: pt.testTypes
      })),
      requiresFlowTests: plan.requiresFlowTests
    };
    
    // Add example test scenarios if available
    if (plan.story.testScenarios && plan.story.testScenarios.length > 0) {
      samples.exampleScenarios = plan.story.testScenarios.slice(0, 3);
    }
    
    // Add acceptance criteria examples
    if (plan.story.acceptanceCriteria.length > 0) {
      samples.exampleAcceptanceCriteria = plan.story.acceptanceCriteria.slice(0, 3);
    }
    
    return samples;
  }
  
  /**
   * Export plan to JSON
   */
  exportToJson(plan: TestPlan): string {
    const exported = this.exportPlan(plan);
    const filename = `test-plan-${plan.normalizedId}.json`;
    const filepath = path.join(this.options.outputDir, filename);
    
    fs.writeFileSync(filepath, JSON.stringify(exported, null, 2), 'utf-8');
    
    return filepath;
  }
  
  /**
   * Export plan to Markdown
   */
  exportToMarkdown(plan: TestPlan): string {
    const exported = this.exportPlan(plan);
    const filename = `test-plan-${plan.normalizedId}.md`;
    const filepath = path.join(this.options.outputDir, filename);
    
    const markdown = this.generateMarkdown(exported);
    fs.writeFileSync(filepath, markdown, 'utf-8');
    
    return filepath;
  }
  
  /**
   * Generate Markdown content
   */
  private generateMarkdown(exported: ExportedTestPlan): string {
    const lines: string[] = [];
    
    lines.push(`# Test Plan: ${exported.storyId}`);
    lines.push('');
    lines.push(`**Title**: ${exported.title}`);
    if (exported.epic) {
      lines.push(`**Epic**: ${exported.epic}`);
    }
    lines.push(`**Exported**: ${exported.exportedAt}`);
    lines.push('');
    
    lines.push('## Services Involved');
    lines.push('');
    for (const service of exported.services) {
      const testTypes = exported.testsByService[service] || [];
      lines.push(`- **${service}**: ${testTypes.join(', ')}`);
    }
    lines.push('');
    
    lines.push('## Acceptance Criteria');
    lines.push('');
    exported.acceptanceCriteria.forEach((ac, idx) => {
      lines.push(`${idx + 1}. ${ac}`);
    });
    lines.push('');
    
    if (exported.scenarios && exported.scenarios.length > 0) {
      lines.push('## Test Scenarios');
      lines.push('');
      exported.scenarios.forEach((scenario, idx) => {
        lines.push(`${idx + 1}. ${scenario}`);
      });
      lines.push('');
    }
    
    if (exported.testDataSamples) {
      lines.push('## Test Data Samples');
      lines.push('');
      lines.push('```json');
      lines.push(JSON.stringify(exported.testDataSamples, null, 2));
      lines.push('```');
      lines.push('');
    }
    
    if (exported.generatedFiles.length > 0) {
      lines.push('## Generated Test Files');
      lines.push('');
      for (const file of exported.generatedFiles) {
        lines.push(`- **${file.type}**: \`${file.path}\` (${file.testCases} test cases)`);
      }
      lines.push('');
    }
    
    return lines.join('\n');
  }
  
  /**
   * Export entire catalog
   */
  exportCatalog(catalog: TestPlanCatalog): {
    plans: string[];
    index: string;
  } {
    const plans = catalog.listAll();
    const exported: string[] = [];
    
    for (const plan of plans) {
      if (this.options.format === 'json' || this.options.format === 'all') {
        exported.push(this.exportToJson(plan));
      }
      if (this.options.format === 'markdown' || this.options.format === 'all') {
        exported.push(this.exportToMarkdown(plan));
      }
    }
    
    let indexPath = '';
    if (this.options.generateIndex) {
      indexPath = this.generateIndex(catalog);
    }
    
    return { plans: exported, index: indexPath };
  }
  
  /**
   * Generate catalog index
   */
  private generateIndex(catalog: TestPlanCatalog): string {
    const stats = catalog.getStatistics();
    const plans = catalog.listAll();
    
    const lines: string[] = [];
    
    lines.push('# Test Plan Catalog');
    lines.push('');
    lines.push(`**Generated**: ${new Date().toISOString()}`);
    lines.push('');
    
    lines.push('## Statistics');
    lines.push('');
    lines.push(`- **Total Plans**: ${stats.totalPlans}`);
    lines.push(`- **Frontend Tests**: ${stats.byService.frontend}`);
    lines.push(`- **Backend Tests**: ${stats.byService.backend}`);
    lines.push(`- **Gateway Tests**: ${stats.byService.gateway}`);
    lines.push(`- **Risk Engine Tests**: ${stats.byService['risk-engine']}`);
    lines.push(`- **Multi-Service Plans**: ${stats.multiServicePlans}`);
    lines.push(`- **Flow Tests Required**: ${stats.flowTestsRequired}`);
    lines.push('');
    
    lines.push('## All Test Plans');
    lines.push('');
    lines.push('| Story ID | Title | Services | ACs | Files |');
    lines.push('|----------|-------|----------|-----|-------|');
    
    for (const plan of plans) {
      const services = plan.plannedServices.join(', ');
      const acCount = plan.story.acceptanceCriteria.length;
      const fileLinks = this.options.format === 'json' || this.options.format === 'all'
        ? `[JSON](test-plan-${plan.normalizedId}.json)`
        : `[MD](test-plan-${plan.normalizedId}.md)`;
      
      lines.push(`| ${plan.storyId} | ${plan.title} | ${services} | ${acCount} | ${fileLinks} |`);
    }
    lines.push('');
    
    const indexPath = path.join(this.options.outputDir, 'INDEX.md');
    fs.writeFileSync(indexPath, lines.join('\n'), 'utf-8');
    
    return indexPath;
  }
}
