#!/usr/bin/env node
/**
 * Flow Test Generator CLI
 * 
 * Command-line tool for generating E2E flow tests from user stories.
 * Supports Playwright and Cypress frameworks.
 * 
 * Usage:
 *   npm run generate:flow-tests -- [options]
 * 
 * Options:
 *   --framework <playwright|cypress>  E2E framework to use (default: playwright)
 *   --stories <path>                  Path to user stories directory
 *   --output <path>                   Output directory for generated tests
 *   --epic <name>                     Filter by epic name
 *   --verbose                         Enable verbose logging
 * 
 * @module cli/generate-flow-tests
 */

import { readdir, readFile, writeFile, mkdir } from 'node:fs/promises';
import { join, dirname, resolve } from 'node:path';
import { StoryParser } from '../parser/story-parser.js';
import { TestPlanner } from '../planner/test-planner.js';
import { FlowTestGenerator } from '../generators/flow-test-generator.js';
import type { E2EFramework } from '../models/flow-test-model.js';

const __dirname = resolve();

/**
 * CLI configuration
 */
interface CliConfig {
  framework: E2EFramework;
  storiesPath: string;
  outputPath: string;
  epicFilter?: string;
  verbose: boolean;
}

/**
 * Parse command-line arguments
 */
function parseArgs(): CliConfig {
  const args = process.argv.slice(2);
  const config: CliConfig = {
    framework: 'playwright',
    storiesPath: join(__dirname, '../../../user-stories'),
    outputPath: join(__dirname, '../../../e2e-tests'),
    verbose: false
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    
    switch (arg) {
      case '--framework': {
        i++;
        const framework = args[i] as E2EFramework;
        if (framework !== 'playwright' && framework !== 'cypress') {
          console.error('❌ Invalid framework. Must be "playwright" or "cypress"');
          process.exit(1);
        }
        config.framework = framework;
        break;
      }
      case '--stories': {
        i++;
        config.storiesPath = args[i];
        break;
      }
      case '--output': {
        i++;
        config.outputPath = args[i];
        break;
      }
      case '--epic': {
        i++;
        config.epicFilter = args[i];
        break;
      }
      case '--verbose': {
        config.verbose = true;
        break;
      }
      case '--help': {
        printHelp();
        process.exit(0);
        break;
      }
      default: {
        console.error(`❌ Unknown option: ${arg}`);
        printHelp();
        process.exit(1);
      }
    }
  }

  return config;
}

/**
 * Print help message
 */
function printHelp(): void {
  console.log(`
📋 Flow Test Generator CLI

Generate E2E flow tests from user stories for Playwright or Cypress.

Usage:
  npm run generate:flow-tests -- [options]

Options:
  --framework <framework>  E2E framework: "playwright" or "cypress" (default: playwright)
  --stories <path>         Path to user stories directory (default: user-stories/)
  --output <path>          Output directory for tests (default: e2e-tests/)
  --epic <name>            Filter by epic name (e.g., "epic_03")
  --verbose                Enable verbose logging
  --help                   Show this help message

Examples:
  # Generate Playwright tests for all stories
  npm run generate:flow-tests

  # Generate Cypress tests for a specific epic
  npm run generate:flow-tests -- --framework cypress --epic epic_03

  # Generate tests with custom output directory
  npm run generate:flow-tests -- --output ./tests/e2e --verbose
  `);
}

/**
 * Find all story files recursively
 */
async function findStoryFiles(dir: string, epicFilter?: string): Promise<string[]> {
  const files: string[] = [];
  
  try {
    const entries = await readdir(dir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = join(dir, entry.name);
      
      if (entry.isDirectory()) {
        // Skip if epic filter doesn't match
        if (epicFilter && entry.name.startsWith('epic_') && entry.name !== epicFilter) {
          continue;
        }
        
        const subFiles = await findStoryFiles(fullPath, epicFilter);
        files.push(...subFiles);
      } else if (entry.isFile() && entry.name.endsWith('.md')) {
        files.push(fullPath);
      }
    }
  } catch (error) {
    console.error(`⚠️  Could not read directory: ${dir}`);
    if (error instanceof Error) {
      console.error(`   ${error.message}`);
    }
  }
  
  return files;
}

/**
 * Generate flow tests
 */
async function generateFlowTests(config: CliConfig): Promise<void> {
  console.log('🚀 Flow Test Generator');
  console.log('─'.repeat(50));
  console.log(`📁 Stories: ${config.storiesPath}`);
  console.log(`📝 Framework: ${config.framework}`);
  console.log(`📂 Output: ${config.outputPath}`);
  if (config.epicFilter) {
    console.log(`🎯 Epic filter: ${config.epicFilter}`);
  }
  console.log('─'.repeat(50));
  console.log('');

  // Find story files
  console.log('🔍 Discovering story files...');
  const storyFiles = await findStoryFiles(config.storiesPath, config.epicFilter);
  
  if (storyFiles.length === 0) {
    console.log('⚠️  No story files found');
    return;
  }
  
  console.log(`✅ Found ${storyFiles.length} story files`);
  console.log('');

  // Parse stories
  console.log('📖 Parsing stories...');
  const parser = new StoryParser();
  const stories = [];
  
  for (const file of storyFiles) {
    try {
      const { story } = parser.parseStory(file);
      stories.push(story);
      
      if (config.verbose) {
        console.log(`   ✓ ${story.storyId}: ${story.title}`);
      }
    } catch (error) {
      console.error(`   ✗ Failed to parse ${file}`);
      if (config.verbose && error instanceof Error) {
        console.error(`     ${error.message}`);
      }
    }
  }
  
  console.log(`✅ Parsed ${stories.length} stories`);
  console.log('');

  // Create test plans
  console.log('📋 Creating test plans...');
  const planner = new TestPlanner();
  const plans = stories
    .map(story => planner.plan(story))
    .filter(plan => plan.requiresFlowTests); // Only generate for stories requiring flow tests
  
  console.log(`✅ Created ${plans.length} test plans (${plans.length} require flow tests)`);
  console.log('');

  // Generate flow tests
  console.log(`🧪 Generating ${config.framework} flow tests...`);
  const generator = new FlowTestGenerator(config.framework);
  let generatedCount = 0;
  let errorCount = 0;

  for (const plan of plans) {
    try {
      const tests = generator.generateFromPlan(plan.story, plan);
      
      for (const test of tests) {
        const outputFile = join(config.outputPath, test.filePath);
        const outputDir = dirname(outputFile);
        
        // Create directory if it doesn't exist
        await mkdir(outputDir, { recursive: true });
        
        // Write test file
        await writeFile(outputFile, test.code, 'utf-8');
        
        generatedCount++;
        
        if (config.verbose) {
          console.log(`   ✓ ${test.storyId} → ${test.filePath}`);
        }
      }
    } catch (error) {
      errorCount++;
      console.error(`   ✗ Failed to generate tests for ${plan.storyId}`);
      if (config.verbose && error instanceof Error) {
        console.error(`     ${error.message}`);
      }
    }
  }

  console.log('');
  console.log('─'.repeat(50));
  console.log('📊 Summary');
  console.log('─'.repeat(50));
  console.log(`✅ Generated: ${generatedCount} test files`);
  if (errorCount > 0) {
    console.log(`❌ Errors: ${errorCount}`);
  }
  console.log(`📂 Location: ${config.outputPath}`);
  console.log('');

  if (generatedCount > 0) {
    console.log('🎉 Flow test generation complete!');
    console.log('');
    console.log('Next steps:');
    if (config.framework === 'playwright') {
      console.log('  1. Install Playwright: npm install -D @playwright/test');
      console.log('  2. Run tests: npx playwright test');
    } else {
      console.log('  1. Install Cypress: npm install -D cypress');
      console.log('  2. Run tests: npx cypress run');
    }
    console.log('');
  }
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  try {
    const config = parseArgs();
    await generateFlowTests(config);
  } catch (error) {
    console.error('❌ Fatal error:');
    console.error(error);
    process.exit(1);
  }
}

// Run CLI
// eslint-disable-next-line unicorn/prefer-top-level-await
void main();
