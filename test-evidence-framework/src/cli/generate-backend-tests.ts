#!/usr/bin/env node
/**
 * CLI tool for Backend Test Generation (Story 20.3)
 * 
 * Usage:
 *   npm run generate-backend-tests -- --root ../user-stories --service backend
 *   npm run generate-backend-tests -- --story 3.1 --output backend/src/test/java
 */

import * as path from 'node:path';
import * as fs from 'node:fs';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';
import { StoryParser } from '../parser/story-parser';
import { TestPlanner } from '../planner/test-planner';
import { BackendTestGenerator } from '../generators/backend-test-generator';
import { BackendTestGenerationConfig } from '../models/backend-test-model';
import { ServiceName, StoryModel } from '../models/story-model';
import { TestPlan } from '../models/test-plan-model';

interface CliArgs {
  root: string;
  output: string;
  service?: ServiceName;
  story?: string;
  basePackage: string;
  datasetRegistry: string;
  verbose: boolean;
  dryRun: boolean;
}

const argv = yargs(hideBin(process.argv))
  .option('root', {
    alias: 'r',
    type: 'string',
    description: 'Root directory containing user stories',
    demandOption: true
  })
  .option('output', {
    alias: 'o',
    type: 'string',
    description: 'Output directory for generated tests',
    default: '../backend/src/test/java'
  })
  .option('service', {
    alias: 's',
    type: 'string',
    description: 'Filter by service (frontend, backend, gateway, risk-engine)',
    choices: ['frontend', 'backend', 'gateway', 'risk-engine']
  })
  .option('story', {
    type: 'string',
    description: 'Generate tests for specific story ID (e.g., 3.1)'
  })
  .option('base-package', {
    alias: 'p',
    type: 'string',
    description: 'Base Java package name',
    default: 'com.cds.platform'
  })
  .option('dataset-registry', {
    alias: 'd',
    type: 'string',
    description: 'Path to dataset registry.json',
    default: '../backend/src/test/resources/datasets/registry.json'
  })
  .option('verbose', {
    alias: 'v',
    type: 'boolean',
    description: 'Verbose output',
    default: false
  })
  .option('dry-run', {
    type: 'boolean',
    description: 'Preview without writing files',
    default: false
  })
  .help()
  .parseSync() as CliArgs;

async function main() {
  console.log('🧪 Backend Test Generator\n');
  console.log(`📁 Scanning stories in: ${argv.root}`);
  console.log(`📤 Output directory: ${argv.output}`);
  console.log(`📦 Base package: ${argv.basePackage}`);
  
  if (argv.dryRun) {
    console.log('🔍 DRY RUN MODE - No files will be written\n');
  }

  // Step 1: Parse stories
  console.log('\n📖 Step 1: Parsing stories...');
  const parser = new StoryParser(true); // Enable inference
  
  // Find all story files recursively
  const storyFiles: string[] = [];
  function findStoryFiles(dir: string) {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        findStoryFiles(fullPath);
      } else if (entry.isFile() && entry.name.endsWith('.md') && entry.name.startsWith('story_')) {
        storyFiles.push(fullPath);
      }
    }
  }
  findStoryFiles(argv.root);
  
  // Parse all stories
  const stories: StoryModel[] = [];
  for (const storyFile of storyFiles) {
    const { story } = parser.parseStory(storyFile);
    stories.push(story);
  }
  console.log(`   ✓ Parsed ${stories.length} stories`);

  // Filter by service
  let filteredStories = stories;
  if (argv.service) {
    filteredStories = stories.filter((story: StoryModel) => 
      story.servicesInvolved.includes(argv.service as ServiceName)
    );
    console.log(`   ✓ Filtered to ${filteredStories.length} stories for service: ${argv.service}`);
  }

  // Filter by story ID
  if (argv.story) {
    filteredStories = filteredStories.filter((story: StoryModel) => 
      story.storyId?.includes(argv.story!) || story.title.includes(argv.story!)
    );
    console.log(`   ✓ Filtered to ${filteredStories.length} stories matching: ${argv.story}`);
  }

  if (filteredStories.length === 0) {
    console.log('\n⚠️  No stories found matching criteria');
    process.exit(0);
  }

  // Step 2: Plan tests
  console.log('\n📋 Step 2: Planning tests...');
  const planner = new TestPlanner();
  const allPlans: TestPlan[] = filteredStories.map((story: StoryModel) => planner.plan(story));
  
  // Filter plans for backend-compatible services
  const backendPlans = allPlans.filter((plan: TestPlan) => 
    plan.plannedServices.some(service => ['backend', 'gateway', 'risk-engine'].includes(service))
  );
  
  console.log(`   ✓ Generated ${backendPlans.length} test plans`);

  // Step 3: Generate tests
  console.log('\n🔨 Step 3: Generating tests...');
  
  const config: BackendTestGenerationConfig = {
    outputDir: path.resolve(argv.output),
    basePackage: argv.basePackage,
    testSuffix: 'Test',
    useAllure: true,
    useAssertJ: true,
    useMockito: true,
    generateJavadoc: true,
    datasetRegistryPath: path.resolve(argv.datasetRegistry)
  };

  const generator = new BackendTestGenerator(config);
  
  let totalGenerated = 0;
  let totalErrors = 0;

  for (const plan of backendPlans) {
    const story = filteredStories.find((s: StoryModel) => s.storyId === plan.storyId);
    if (!story) continue;

    if (argv.verbose) {
      const services = plan.plannedServices.join(', ');
      console.log(`\n   📝 Generating tests for: ${story.title} (${services})`);
    }

    try {
      const results = generator.generateTests(story, plan);
      
      for (const result of results) {
        if (result.success) {
          totalGenerated++;
          if (argv.verbose) {
            console.log(`      ✓ ${result.testClass.className} → ${result.filePath}`);
          }
          
          if (argv.dryRun) {
            console.log(`      [DRY RUN] Would write to: ${result.filePath}`);
          }
        } else {
          totalErrors++;
          console.log(`      ✗ Failed: ${result.errors.join(', ')}`);
        }
      }
    } catch (error) {
      totalErrors++;
      console.log(`      ✗ Error generating tests: ${error}`);
    }
  }

  // Summary
  console.log('\n' + '='.repeat(60));
  console.log('📊 Generation Summary');
  console.log('='.repeat(60));
  console.log(`✅ Stories processed: ${filteredStories.length}`);
  console.log(`✅ Test plans created: ${backendPlans.length}`);
  console.log(`✅ Test classes generated: ${totalGenerated}`);
  
  if (totalErrors > 0) {
    console.log(`❌ Errors encountered: ${totalErrors}`);
  }
  
  if (argv.dryRun) {
    console.log('\n🔍 DRY RUN COMPLETE - No files were written');
  } else {
    console.log(`\n📁 Test classes written to: ${argv.output}`);
  }
  
  console.log('\n🎉 Backend test generation complete!');
}

main().catch(error => {
  console.error('\n❌ Fatal error:', error);
  process.exit(1);
});
