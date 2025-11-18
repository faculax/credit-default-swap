#!/usr/bin/env node
/**
 * CLI tool to parse user stories from /user-stories directory
 * 
 * Usage:
 *   npm run parse-stories -- --root ../user-stories
 *   npm run parse-stories -- --root ../user-stories --output parsed-stories.json
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import { StoryParser } from '../parser/story-parser';
import { StoryCatalog } from '../catalog/story-catalog';

interface CliArgs {
  root: string;
  output?: string;
  verbose?: boolean;
  infer?: boolean;
}

function parseArgs(): CliArgs {
  const args = process.argv.slice(2);
  const result: CliArgs = {
    root: '../user-stories',
    verbose: false,
    infer: false
  };
  
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--root' && i + 1 < args.length) {
      result.root = args[i + 1];
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
  
  // Resolve root path
  const rootPath = path.resolve(process.cwd(), args.root);
  
  if (!fs.existsSync(rootPath)) {
    console.error(`Error: Directory not found: ${rootPath}`);
    process.exit(1);
  }
  
  console.log(`Parsing stories from: ${rootPath}\n`);
  
  if (args.infer) {
    console.log('🔍 Service inference enabled - will infer services from content when section is missing\n');
  }
  
  // Parse all stories
  const parser = new StoryParser(args.infer);
  const results = parser.parseStoriesInDirectory(rootPath);
  
  // Build catalog
  const catalog = new StoryCatalog();
  const validStories: typeof results = [];
  const invalidStories: typeof results = [];
  
  for (const result of results) {
    if (result.validation.valid) {
      catalog.add(result.story);
      validStories.push(result);
    } else {
      invalidStories.push(result);
    }
  }
  
  // Report results
  console.log(`✅ Parsed ${validStories.length} valid stories`);
  console.log(`❌ Found ${invalidStories.length} stories with errors\n`);
  
  // Show statistics
  const stats = catalog.getStatistics();
  console.log('📊 Statistics:');
  console.log(`   Total stories: ${stats.totalStories}`);
  console.log(`   Multi-service: ${stats.multiService}`);
  console.log(`   With missing services: ${stats.withMissingServices}`);
  console.log(`   With invalid services: ${stats.withInvalidServices}`);
  console.log('\n   By service:');
  console.log(`     frontend:     ${stats.byService.frontend}`);
  console.log(`     backend:      ${stats.byService.backend}`);
  console.log(`     gateway:      ${stats.byService.gateway}`);
  console.log(`     risk-engine:  ${stats.byService['risk-engine']}`);
  
  // Show errors if verbose
  if (args.verbose && invalidStories.length > 0) {
    console.log('\n❌ Stories with errors:');
    for (const result of invalidStories) {
      console.log(`\n  ${result.story.storyId} (${result.story.filePath})`);
      for (const error of result.validation.errors) {
        console.log(`    - [${error.field}] ${error.message}`);
      }
      if (result.validation.warnings.length > 0) {
        console.log(`    Warnings:`);
        for (const warning of result.validation.warnings) {
          console.log(`      - [${warning.field}] ${warning.message}`);
        }
      }
    }
  }
  
  // Save output if requested
  if (args.output) {
    const outputPath = path.resolve(process.cwd(), args.output);
    const output = {
      parsedAt: new Date().toISOString(),
      rootPath,
      statistics: stats,
      stories: catalog.listAll(),
      errors: invalidStories.map(r => ({
        storyId: r.story.storyId,
        filePath: r.story.filePath,
        errors: r.validation.errors,
        warnings: r.validation.warnings
      }))
    };
    
    fs.writeFileSync(outputPath, JSON.stringify(output, null, 2));
    console.log(`\n💾 Saved output to: ${outputPath}`);
  }
  
  // Exit with error code if there are invalid stories
  if (invalidStories.length > 0) {
    console.log(`\n⚠️  Run with --verbose to see error details`);
    process.exit(1);
  }
}

main();
