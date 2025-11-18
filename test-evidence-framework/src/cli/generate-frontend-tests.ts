#!/usr/bin/env node
/**
 * Frontend Test Generator CLI
 * 
 * Generates React Testing Library tests from user stories
 * 
 * Usage:
 *   npm run generate:frontend-tests -- --stories-dir=../user-stories --output-dir=../frontend/src/__tests__
 */

import { readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { StoryParser } from '../parser/story-parser.js';
import { TestPlanner } from '../planner/test-planner.js';
import { FrontendTestGenerator } from '../generators/frontend-test-generator.js';
import type { StoryModel } from '../models/story-model.js';
import type { TestPlan } from '../models/test-plan-model.js';

// Parse command line arguments
const args = process.argv.slice(2);
const storiesDir = args.find(arg => arg.startsWith('--stories-dir='))?.split('=')[1] || '../user-stories';
const outputDir = args.find(arg => arg.startsWith('--output-dir='))?.split('=')[1] || '../frontend/src/__tests__';

console.log('🧪 Frontend Test Generator');
console.log('==========================\n');
console.log(`📂 Stories directory: ${storiesDir}`);
console.log(`📝 Output directory: ${outputDir}\n`);

/**
 * Find all story markdown files recursively
 */
function findStoryFiles(dir: string): string[] {
  const files: string[] = [];
  
  try {
    const entries = readdirSync(dir);
    
    for (const entry of entries) {
      const fullPath = join(dir, entry);
      const stat = statSync(fullPath);
      
      if (stat.isDirectory()) {
        files.push(...findStoryFiles(fullPath));
      } else if (stat.isFile() && entry.endsWith('.md') && entry.includes('story_')) {
        files.push(fullPath);
      }
    }
  } catch (error) {
    console.error(`❌ Error reading directory ${dir}:`, error);
  }
  
  return files;
}

/**
 * Main execution
 */
async function main(): Promise<void> {
  try {
    // Find all story files
    console.log('🔍 Searching for story files...');
    const storyFiles = findStoryFiles(storiesDir);
    console.log(`   Found ${storyFiles.length} story files\n`);
    
    if (storyFiles.length === 0) {
      console.log('⚠️  No story files found. Please check the stories directory.');
      return;
    }
    
    // Parse all stories
    console.log('📖 Parsing stories...');
    const parser = new StoryParser();
    const stories: StoryModel[] = [];
    
    for (const storyFile of storyFiles) {
      try {
        const parsed = parser.parseStory(storyFile);
        stories.push(parsed.story);
        console.log(`   ✓ Parsed: ${parsed.story.storyId}`);
      } catch (error) {
        console.error(`   ✗ Failed to parse ${storyFile}:`, error);
      }
    }
    
    console.log(`   Successfully parsed ${stories.length} stories\n`);
    
    // Generate test plans
    console.log('📋 Generating test plans...');
    const planner = new TestPlanner();
    const plans: TestPlan[] = stories.map((story: StoryModel) => planner.plan(story));
    console.log(`   Generated ${plans.length} test plans\n`);
    
    // Filter for frontend tests
    console.log('🎨 Filtering frontend tests...');
    const frontendPlans = plans.filter((plan: TestPlan) => 
      plan.plannedServices.includes('frontend')
    );
    console.log(`   Found ${frontendPlans.length} stories requiring frontend tests\n`);
    
    if (frontendPlans.length === 0) {
      console.log('ℹ️  No frontend tests to generate.');
      return;
    }
    
    // Generate frontend tests
    console.log('⚛️  Generating frontend tests...');
    const generator = new FrontendTestGenerator(outputDir);
    
    let totalTestsGenerated = 0;
    let totalFilesGenerated = 0;
    
    for (const plan of frontendPlans) {
      try {
        const story = stories.find((s: StoryModel) => s.storyId === plan.storyId);
        if (!story) continue;
        
        const generatedTests = await generator.generateTestsForStory(story, plan);
        
        for (const test of generatedTests) {
          console.log(`   ✓ Generated: ${test.filePath}`);
          console.log(`      Test type: ${test.testType}`);
          console.log(`      Test count: ${test.testCount}`);
          totalTestsGenerated += test.testCount;
          totalFilesGenerated++;
        }
      } catch (error) {
        console.error(`   ✗ Failed to generate tests for ${plan.storyId}:`, error);
      }
    }
    
    // Summary
    console.log('\n✅ Frontend test generation complete!');
    console.log('=====================================\n');
    console.log(`📊 Summary:`);
    console.log(`   Stories processed: ${stories.length}`);
    console.log(`   Test plans created: ${plans.length}`);
    console.log(`   Frontend test plans: ${frontendPlans.length}`);
    console.log(`   Test files generated: ${totalFilesGenerated}`);
    console.log(`   Total tests generated: ${totalTestsGenerated}\n`);
    
    console.log('🚀 Next steps:');
    console.log(`   1. Review generated tests in ${outputDir}`);
    console.log('   2. Add specific assertions based on acceptance criteria');
    console.log('   3. Configure MSW handlers for API mocking');
    console.log('   4. Run tests: npm test\n');
    
  } catch (error) {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  }
}

// Run CLI
main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
