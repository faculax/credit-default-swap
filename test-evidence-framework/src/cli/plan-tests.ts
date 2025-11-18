#!/usr/bin/env node
/**
 * CLI tool to generate test plans from parsed stories
 * 
 * Usage:
 *   npm run plan-tests -- --root ../user-stories
 *   npm run plan-tests -- --story "Story 20.1"
 *   npm run plan-tests -- --service backend
 *   npm run plan-tests -- --output test-plans.json
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import { StoryParser } from '../parser/story-parser';
import { StoryCatalog } from '../catalog/story-catalog';
import { TestPlanner } from '../planner/test-planner';
import { TestPlanCatalog } from '../catalog/test-plan-catalog';
import { ServiceName } from '../models/story-model';

interface CliArgs {
  root?: string;
  story?: string;
  service?: ServiceName;
  output?: string;
  verbose?: boolean;
  infer?: boolean;
}

function parseArgs(): CliArgs {
  const args = process.argv.slice(2);
  const result: CliArgs = {
    verbose: false,
    infer: false
  };
  
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--root' && i + 1 < args.length) {
      result.root = args[i + 1];
      i++;
    } else if (args[i] === '--story' && i + 1 < args.length) {
      result.story = args[i + 1];
      i++;
    } else if (args[i] === '--service' && i + 1 < args.length) {
      result.service = args[i + 1] as ServiceName;
      i++;
    } else if (args[i] === '--output' && i + 1 < args.length) {
      result.output = args[i + 1];
      i++;
    } else if (args[i] === '--verbose' || args[i] === '-v') {
      result.verbose = true;
    } else if (args[i] === '--infer' || args[i] === '-i') {
      result.infer = true;
    }
  }
  
  return result;
}

function main() {
  const args = parseArgs();
  
  // Parse stories
  const parser = new StoryParser(args.infer);
  let storyCatalog: StoryCatalog;
  
  if (args.root) {
    const rootPath = path.resolve(process.cwd(), args.root);
    
    if (!fs.existsSync(rootPath)) {
      console.error(`Error: Directory not found: ${rootPath}`);
      process.exit(1);
    }
    
    console.log(`Parsing stories from: ${rootPath}\n`);
    
    if (args.infer) {
      console.log('🔍 Service inference enabled\n');
    }
    
    const results = parser.parseStoriesInDirectory(rootPath);
    storyCatalog = new StoryCatalog();
    
    for (const result of results) {
      if (result.validation.valid) {
        storyCatalog.add(result.story);
      }
    }
    
    console.log(`✅ Loaded ${storyCatalog.size()} valid stories\n`);
  } else {
    console.error('Error: --root is required to load stories');
    process.exit(1);
  }
  
  // Create test plans
  const planner = new TestPlanner();
  const planCatalog = new TestPlanCatalog();
  
  let plansToDisplay: ReturnType<typeof planner.plan>[] = [];
  
  if (args.story) {
    // Plan for specific story
    const story = storyCatalog.getById(args.story);
    if (!story) {
      console.error(`Error: Story not found: ${args.story}`);
      process.exit(1);
    }
    
    const plan = planner.plan(story);
    planCatalog.add(plan);
    plansToDisplay = [plan];
    
    console.log(`📋 Test Plan for ${story.storyId}: ${story.title}\n`);
  } else if (args.service) {
    // Plan for all stories involving a service
    const stories = storyCatalog.filterByService(args.service);
    const plans = planner.planMany(stories);
    planCatalog.addMany(plans);
    plansToDisplay = plans;
    
    console.log(`📋 Test Plans for service: ${args.service} (${plans.length} stories)\n`);
  } else {
    // Plan for all stories
    const stories = storyCatalog.listAll();
    const plans = planner.planMany(stories);
    planCatalog.addMany(plans);
    plansToDisplay = plans;
    
    console.log(`📋 Test Plans for all stories (${plans.length} total)\n`);
  }
  
  // Display plans
  for (const plan of plansToDisplay) {
    console.log(`${plan.storyId}: ${plan.title}`);
    console.log(`  Services: ${plan.plannedServices.join(', ') || 'none'}`);
    console.log(`  Flow tests: ${plan.requiresFlowTests ? 'Yes' : 'No'}`);
    console.log(`  Recommended tests: ${planner.getRecommendedTestCount(plan)}`);
    console.log(`  Complexity: ${planner.estimateComplexity(plan)}`);
    
    if (args.verbose && plan.plannedTests.length > 0) {
      console.log(`  Planned tests:`);
      for (const test of plan.plannedTests) {
        console.log(`    - ${test.service}: ${test.testTypes.join(', ')}`);
        console.log(`      Path: ${test.targetPath}`);
        console.log(`      Acceptance criteria: ${test.acceptanceCriteria.length}`);
        console.log(`      Test scenarios: ${test.testScenarios.length}`);
      }
    }
    
    console.log();
  }
  
  // Show statistics
  const stats = planCatalog.getStatistics();
  console.log('📊 Statistics:');
  console.log(`   Total plans: ${stats.totalPlans}`);
  console.log(`   Multi-service plans: ${stats.multiServicePlans}`);
  console.log(`   Plans requiring flow tests: ${stats.flowTestsRequired}`);
  console.log('\n   Plans by service:');
  console.log(`     frontend:     ${stats.byService.frontend}`);
  console.log(`     backend:      ${stats.byService.backend}`);
  console.log(`     gateway:      ${stats.byService.gateway}`);
  console.log(`     risk-engine:  ${stats.byService['risk-engine']}`);
  
  // Save output if requested
  if (args.output) {
    const outputPath = path.resolve(process.cwd(), args.output);
    const output = {
      generatedAt: new Date().toISOString(),
      statistics: stats,
      plans: planCatalog.listAll().map(plan => ({
        storyId: plan.storyId,
        normalizedId: plan.normalizedId,
        title: plan.title,
        plannedServices: plan.plannedServices,
        plannedTests: plan.plannedTests,
        requiresFlowTests: plan.requiresFlowTests,
        recommendedTestCount: planner.getRecommendedTestCount(plan),
        complexity: planner.estimateComplexity(plan)
      }))
    };
    
    fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));
    console.log(`\n💾 Saved test plans to: ${outputPath}`);
  }
}

main();
