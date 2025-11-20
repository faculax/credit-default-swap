#!/usr/bin/env node

/**
 * Unified Test Generation CLI
 * 
 * Single command to analyze a story and generate complete, working tests
 * for all involved services with full acceptance criteria coverage.
 * 
 * Usage:
 *   generate-tests story_3_1
 *   generate-tests --story story_3_1
 *   generate-tests --story ../user-stories/epic_03/story_3_1.md
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve, join, dirname } from 'node:path';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import { ProductionTestGenerator } from '../generators/production-test-generator';
import { ServicesInvolvedStatus } from '../models/story-model';
import { WorkspaceAnalyzer } from '../inference/workspace-analyzer';
import type { WorkspaceContext } from '../models/workspace-context-model';

interface GenerateOptions {
  story: string;
  dryRun?: boolean;
  verbose?: boolean;
  scanWorkspace?: boolean;
}

const argv = yargs(hideBin(process.argv))
  .usage('Usage: $0 <story-id> [options]')
  .command('$0 <story>', 'Generate complete tests for a story', (yargs) => {
    return yargs.positional('story', {
      describe: 'Story ID (e.g., story_3_1) or path to story file',
      type: 'string'
    });
  })
  .option('dry-run', {
    alias: 'd',
    type: 'boolean',
    description: 'Show what would be generated without creating files'
  })
  .option('verbose', {
    alias: 'v',
    type: 'boolean',
    description: 'Show detailed generation logs'
  })
  .option('scan-workspace', {
    alias: 's',
    type: 'boolean',
    description: 'Scan workspace for actual classes/components to use EXACT names and packages',
    default: true
  })
  .example('$0 story_3_1', 'Generate tests for Story 3.1')
  .example('$0 --story story_3_2 --dry-run', 'Preview tests for Story 3.2')
  .example('$0 story_4_1 --scan-workspace', 'Generate tests with workspace context')
  .help()
  .alias('help', 'h')
  .parseSync();

const options: GenerateOptions = {
  story: argv.story as string,
  dryRun: argv.dryRun,
  verbose: argv.verbose,
  scanWorkspace: argv.scanWorkspace
};

async function main() {
  console.log('🚀 Test Evidence Framework - Intelligent Test Generator\n');
  
  try {
    // Step 0: Scan workspace if enabled
    let workspaceContext: WorkspaceContext | undefined;
    
    if (options.scanWorkspace) {
      console.log('🔍 Scanning workspace for actual classes and components...');
      // Use process.cwd() to get the directory where the command is run
      // User should run from workspace root: cd test-evidence-framework && npm run generate-tests
      // Then go up one level to get to the actual workspace root
      const projectRoot = resolve(process.cwd(), '..');
      const analyzer = new WorkspaceAnalyzer({
        workspaceRoot: projectRoot,
        scanBackend: true,
        scanFrontend: true,
        extractMethods: false, // Skip methods for performance
        extractEndpoints: true
      });
      
      const scanResult = await analyzer.scan();
      
      if (scanResult.success && scanResult.context) {
        workspaceContext = scanResult.context;
        console.log(`   ✅ Found ${scanResult.stats.backendClassesFound} backend classes`);
        console.log(`   ✅ Found ${scanResult.stats.frontendComponentsFound} frontend components`);
        console.log(`   ✅ Scanned in ${scanResult.scanDuration}ms\n`);
        
        if (options.verbose && workspaceContext) {
          console.log('   Backend classes sample:');
          workspaceContext.backendClasses.slice(0, 5).forEach(cls => {
            console.log(`     - ${cls.fullyQualifiedName} (${cls.type})`);
          });
          console.log('   Frontend components sample:');
          workspaceContext.frontendComponents.slice(0, 5).forEach(comp => {
            console.log(`     - ${comp.componentName} (${comp.relativePath})`);
          });
          console.log('');
        }
      } else {
        console.warn(`   ⚠️  Workspace scan failed: ${scanResult.errors.join(', ')}`);
        console.warn('   Continuing without workspace context...\n');
      }
    }
    
    // Step 1: Locate story file
    const storyPath = await locateStoryFile(options.story);
    console.log(`📖 Story: ${storyPath}\n`);
    
    // Step 2: Parse story (using AI-powered parser)
    console.log('🔍 Analyzing story content...');
    const story = await parseStory(storyPath);
    console.log(`   Title: ${story.title}`);
    console.log(`   Epic: ${story.epic}`);
    console.log(`   Acceptance Criteria: ${story.acceptanceCriteria.length}\n`);
    
    // Step 3: Auto-detect services (AI inference)
    console.log('🎯 Detecting involved services...');
    const services = await detectServices(story);
    console.log(`   Services: ${services.join(', ')}\n`);
    
    // Step 4: Generate complete tests (AI-powered implementation with workspace context)
    console.log('⚙️  Generating complete, working tests...\n');
    
    for (const service of services) {
      console.log(`📝 ${service.toUpperCase()}:`);
      const tests = await generateCompleteTests(story, service, options, workspaceContext);
      
      for (const test of tests) {
        if (options.dryRun) {
          console.log(`   [DRY-RUN] Would create: ${test.filePath}`);
          console.log(`   Test cases: ${test.testCases.length}`);
        } else {
          console.log(`   ✅ Created: ${test.filePath}`);
          console.log(`   Test cases: ${test.testCases.length}`);
        }
      }
      console.log('');
    }
    
    // Step 5: Export test plan for documentation
    if (!options.dryRun) {
      try {
        console.log('\n📋 Exporting test plan...');
        const { TestPlanExporter } = await import('../exporters/test-plan-exporter.js');
        const { TestPlanner } = await import('../planner/test-planner.js');
        const { StoryParser } = await import('../parser/story-parser.js');
        
        // Re-parse story with StoryParser for TestPlanner
        const parser = new StoryParser(false);
        const { story: parsedStory } = parser.parseStory(storyPath);
        
        const planner = new TestPlanner();
        const plan = planner.plan(parsedStory);
        
        const exporter = new TestPlanExporter({
          outputDir: resolve(process.cwd(), '..', 'test-plans'),
          format: 'all',
          includeTestData: true,
          includeScenarios: true,
          generateIndex: false
        });
        
        const jsonPath = exporter.exportToJson(plan);
        const mdPath = exporter.exportToMarkdown(plan);
        
        console.log(`   ✅ JSON: ${jsonPath}`);
        console.log(`   ✅ Markdown: ${mdPath}`);
      } catch (error) {
        console.log(`   ⚠️  Test plan export skipped: ${error instanceof Error ? error.message : 'unknown error'}`);
      }
    }
    
    // Step 6: Summary
    console.log('\n✨ Generation complete!\n');
    
    if (!options.dryRun) {
      console.log('Next steps:');
      console.log('  1. Review generated tests');
      console.log('  2. Run tests: ./scripts/test-unified-local.ps1');
      console.log('  3. View results: npm run export-evidence\n');
    }
    
  } catch (error) {
    console.error('❌ Error:', error instanceof Error ? error.message : error);
    process.exit(1);
  }
}

/**
 * Locate story file from ID or path
 */
async function locateStoryFile(input: string): Promise<string> {
  // If it's already a path, use it
  if (input.endsWith('.md')) {
    return resolve(input);
  }
  
  // Otherwise, search in user-stories directory
  const workspaceRoot = resolve(process.cwd(), '..');
  const storyId = input.replace(/^story_/, ''); // e.g., "3_3" from "story_3_3"
  const epicNumber = storyId.split('_')[0]; // e.g., "3" from "3_3"
  
  const glob = require('node:fs');
  const path = require('node:path');
  
  // Simple search in user-stories directory
  const userStoriesDir = path.join(workspaceRoot, 'user-stories');
  
  if (!glob.existsSync(userStoriesDir)) {
    throw new Error(`User stories directory not found: ${userStoriesDir}`);
  }
  
  const epicDirs = glob.readdirSync(userStoriesDir, { withFileTypes: true })
    .filter((d: any) => d.isDirectory() && (
      d.name.startsWith(`epic_${epicNumber}_`) || 
      d.name.startsWith(`epic_0${epicNumber}_`)
    ))
    .map((d: any) => d.name);
  
  for (const epicDir of epicDirs) {
    const epicPath = path.join(userStoriesDir, epicDir);
    const storyFiles = glob.readdirSync(epicPath)
      .filter((f: string) => {
        // Match story_3_3*.md for input "story_3_3" or "3_3"
        return f.startsWith(`story_${storyId}`) && f.endsWith('.md');
      });
    
    if (storyFiles.length > 0) {
      return path.join(epicPath, storyFiles[0]);
    }
  }
  
  throw new Error(`Story file not found for: ${input} (searched in ${userStoriesDir}/epic_${epicNumber}*)`);
}

/**
 * Parse story using AI-powered analysis
 */
async function parseStory(filePath: string): Promise<ParsedStory> {
  const content = readFileSync(filePath, 'utf-8');
  
  // Extract title
  const titleMatch = content.match(/^#\s+(.+)$/m);
  const title = titleMatch ? titleMatch[1] : 'Unknown Story';
  
  // Extract epic
  const epicMatch = filePath.match(/epic_(\d+)/);
  const epic = epicMatch ? `Epic ${epicMatch[1]}` : 'Unknown Epic';
  
  // Extract acceptance criteria (handle emoji markers like ✅)
  const acSection = content.match(/##\s+[^#\n]*Acceptance Criteria[^#\n]*\n([\s\S]+?)(?=\n##|$)/i);
  const acceptanceCriteria: string[] = [];
  
  if (acSection) {
    const lines = acSection[1].split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('-') || trimmed.startsWith('*') || /^\d+\./.test(trimmed)) {
        // Remove leading bullet/number and clean up
        const cleaned = trimmed.replace(/^[-*\d.)\s]+/, '').trim();
        if (cleaned.length > 0) {
          acceptanceCriteria.push(cleaned);
        }
      }
    }
  }
  
  return {
    title,
    epic,
    storyId: filePath.match(/story_\d+_\d+/)?.[0] || 'unknown',
    filePath,
    content,
    acceptanceCriteria
  };
}

/**
 * Auto-detect services from story content using AI
 */
async function detectServices(story: ParsedStory): Promise<string[]> {
  const content = story.content.toLowerCase();
  const services: string[] = [];
  
  // Frontend indicators
  const frontendKeywords = ['form', 'ui', 'display', 'render', 'button', 'input', 'component', 'page', 'modal'];
  if (frontendKeywords.some(kw => content.includes(kw))) {
    services.push('frontend');
  }
  
  // Backend indicators
  const backendKeywords = ['api', 'endpoint', 'service', 'persist', 'database', 'validate', 'controller', 'rest'];
  if (backendKeywords.some(kw => content.includes(kw))) {
    services.push('backend');
  }
  
  // Gateway indicators
  if (content.includes('gateway') || content.includes('routing') || content.includes('authentication')) {
    services.push('gateway');
  }
  
  // Risk engine indicators
  if (content.includes('risk') || content.includes('calculation') || content.includes('pricing')) {
    services.push('risk-engine');
  }
  
  // Default to backend + frontend if none detected
  if (services.length === 0) {
    services.push('backend', 'frontend');
  }
  
  return services;
}

/**
 * Generate complete, working tests (no TODOs)
 */
async function generateCompleteTests(
  story: ParsedStory,
  service: string,
  options: GenerateOptions,
  workspaceContext?: WorkspaceContext
): Promise<GeneratedTest[]> {
  if (service === 'frontend') {
    return generateFrontendTests(story, options, workspaceContext);
  } else if (service === 'backend') {
    return generateBackendTests(story, options, workspaceContext);
  } else {
    return [];
  }
}

/**
 * Generate complete frontend tests using ProductionTestGenerator
 */
async function generateFrontendTests(
  story: ParsedStory,
  options: GenerateOptions,
  workspaceContext?: WorkspaceContext
): Promise<GeneratedTest[]> {
  const generator = new ProductionTestGenerator();
  const projectRoot = resolve(process.cwd(), '..');
  
  // Infer component name from story title OR use workspace context
  let componentName = story.title
    .replace(/Story \d+\.\d+ – /, '')
    .replace(/[^a-zA-Z0-9\s]/g, '')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join('');
  
  // Override with actual component from workspace if available
  if (workspaceContext) {
    const storyText = story.title.toLowerCase();
    const matchingComponent = workspaceContext.frontendComponents.find(comp =>
      storyText.includes(comp.componentName.toLowerCase())
    );
    if (matchingComponent) {
      componentName = matchingComponent.componentName;
      if (options.verbose) {
        console.log(`   🎯 Using actual component: ${componentName} (${matchingComponent.relativePath})`);
      }
    }
  }
  
  // Generate test file path
  const testFilePath = join(
    projectRoot,
    'frontend',
    'src',
    '__tests__',
    'components',
    `${componentName}.Story${story.storyId.replace('story_', '').replace('_', '_')}.test.tsx`
  );
  
  // Convert to StoryModel format
  const storyModel = {
    storyId: story.storyId,
    normalizedId: story.storyId.toUpperCase(),
    title: story.title,
    filePath: story.filePath,
    acceptanceCriteria: story.acceptanceCriteria,
    testScenarios: [],
    servicesInvolved: ['frontend' as const],
    servicesInvolvedStatus: ServicesInvolvedStatus.PRESENT
  };
  
  // Generate complete test file
  const result = generator.generateFrontendTestFile(storyModel, componentName);
  
  if (!options.dryRun) {
    mkdirSync(dirname(testFilePath), { recursive: true });
    writeFileSync(testFilePath, result.testCode, 'utf-8');
  }
  
  return [{
    filePath: testFilePath,
    testCases: story.acceptanceCriteria,
    linesOfCode: result.testCode.split('\n').length
  }];
}

/**
 * Generate complete backend tests using ProductionTestGenerator
 */
async function generateBackendTests(
  story: ParsedStory,
  options: GenerateOptions,
  workspaceContext?: WorkspaceContext
): Promise<GeneratedTest[]> {
  const generator = new ProductionTestGenerator();
  const projectRoot = resolve(process.cwd(), '..');
  
  // Infer test class name from story OR use workspace context
  let className = story.title
    .replace(/Story \d+\.\d+ – /, '')
    .replace(/[^a-zA-Z0-9\s]/g, '')
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join('') + 'Story' + story.storyId.replace('story_', '').replace('_', '_') + 'IntegrationTest';
  
  let packagePath = 'com/cds/platform/trade';
  
  // Override with actual service/package from workspace if available
  if (workspaceContext) {
    const storyText = `${story.title} ${story.acceptanceCriteria.join(' ')}`.toLowerCase();
    const matchingService = workspaceContext.backendClasses.find(cls =>
      cls.type === 'Service' && storyText.includes(cls.className.replace('Service', '').toLowerCase())
    );
    if (matchingService) {
      className = `${matchingService.className}Test`;
      packagePath = matchingService.packageName.replace(/\./g, '/');
      if (options.verbose) {
        console.log(`   🎯 Using actual service: ${matchingService.fullyQualifiedName}`);
        console.log(`   📦 Package: ${matchingService.packageName}`);
      }
    }
  }
  
  // Generate test file path
  const testFilePath = join(
    projectRoot,
    'backend',
    'src',
    'test',
    'java',
    packagePath,
    `${className}.java`
  );
  
  // Convert to StoryModel format
  const storyModel = {
    storyId: story.storyId,
    normalizedId: story.storyId.toUpperCase(),
    title: story.title,
    filePath: story.filePath,
    acceptanceCriteria: story.acceptanceCriteria,
    testScenarios: [],
    servicesInvolved: ['backend' as const],
    servicesInvolvedStatus: ServicesInvolvedStatus.PRESENT
  };
  
  // Generate complete test file WITH workspace context
  const result = generator.generateBackendTestFile(storyModel, className, workspaceContext);
  
  if (!options.dryRun) {
    mkdirSync(dirname(testFilePath), { recursive: true });
    writeFileSync(testFilePath, result.testCode, 'utf-8');
  }
  
  return [{
    filePath: testFilePath,
    testCases: story.acceptanceCriteria,
    linesOfCode: result.testCode.split('\n').length
  }];
}

// Type definitions
interface ParsedStory {
  title: string;
  epic: string;
  storyId: string;
  filePath: string;
  content: string;
  acceptanceCriteria: string[];
}

interface GeneratedTest {
  filePath: string;
  testCases: string[];
  linesOfCode: number;
}

// Run main
main().catch(error => {
  console.error(error);
  process.exit(1);
});
