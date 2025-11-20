/**
 * CLI Enhancement: Export Test Plans
 * 
 * Command to export test plans to JSON/Markdown for documentation and ReportPortal integration
 * 
 * Usage: node dist/cli/export-test-plans.js [options]
 */

import * as fs from 'fs';
import * as path from 'path';
import { TestPlanCatalog } from '../catalog/test-plan-catalog.js';
import { TestPlanExporter, TestPlanExportOptions } from '../exporters/test-plan-exporter.js';
import { StoryParser } from '../parser/story-parser.js';
import { TestPlanner } from '../planner/test-planner.js';

interface ExportOptions {
  /** Output directory */
  outputDir?: string;
  
  /** Export format */
  format?: 'json' | 'markdown' | 'html' | 'all';
  
  /** Story ID filter (export single story) */
  storyId?: string;
  
  /** Include test data samples */
  includeTestData?: boolean;
  
  /** Generate index */
  generateIndex?: boolean;
}

async function exportTestPlans(options: ExportOptions = {}) {
  console.log('🚀 Test Plan Exporter\n');
  
  // Default options
  const outputDir = options.outputDir || path.join(process.cwd(), 'test-plans');
  const format = options.format || 'all';
  
  console.log(`📁 Output Directory: ${outputDir}`);
  console.log(`📝 Format: ${format}\n`);
  
  // Build catalog
  console.log('📖 Scanning user stories...');
  const catalog = await buildTestPlanCatalog();
  
  const stats = catalog.getStatistics();
  console.log(`   ✅ Found ${stats.totalPlans} test plans`);
  console.log(`   ✅ Frontend: ${stats.byService.frontend}`);
  console.log(`   ✅ Backend: ${stats.byService.backend}`);
  console.log(`   ✅ Gateway: ${stats.byService.gateway}`);
  console.log(`   ✅ Risk Engine: ${stats.byService['risk-engine']}\n`);
  
  // Export
  const exportOptions: TestPlanExportOptions = {
    outputDir,
    format,
    includeTestData: options.includeTestData !== false,
    includeAcceptanceCriteria: true,
    includeScenarios: true,
    generateIndex: options.generateIndex !== false
  };
  
  const exporter = new TestPlanExporter(exportOptions);
  
  if (options.storyId) {
    // Export single plan - try both storyId and normalizedId
    console.log(`📝 Exporting test plan for ${options.storyId}...`);
    
    // Try exact story ID first (e.g., "Story 3.1")
    let plan = catalog.getByStoryId(options.storyId);
    
    // If not found, try converting filename to story ID (e.g., "story_3_1" -> "Story 3.1")
    if (!plan) {
      const match = options.storyId.match(/story[_\s](\d+)[_\s](\d+)/i);
      if (match) {
        const storyId = `Story ${match[1]}.${match[2]}`;
        plan = catalog.getByStoryId(storyId);
      }
    }
    
    // If still not found, try normalized ID (e.g., "STORY_3_1")
    if (!plan) {
      const normalizedId = options.storyId.toUpperCase().replace(/[_\s-]+/g, '_');
      plan = catalog.getByNormalizedId(normalizedId);
    }
    
    if (!plan) {
      console.error(`❌ Test plan not found for story: ${options.storyId}`);
      console.error(`   Tried: story ID, filename pattern, and normalized ID`);
      process.exit(1);
    }
    
    if (format === 'json' || format === 'all') {
      const jsonPath = exporter.exportToJson(plan);
      console.log(`   ✅ JSON: ${jsonPath}`);
    }
    
    if (format === 'markdown' || format === 'all') {
      const mdPath = exporter.exportToMarkdown(plan);
      console.log(`   ✅ Markdown: ${mdPath}`);
    }
  } else {
    // Export entire catalog
    console.log('📝 Exporting all test plans...');
    const result = exporter.exportCatalog(catalog);
    
    console.log(`   ✅ Exported ${result.plans.length} files`);
    
    if (result.index) {
      console.log(`   ✅ Index: ${result.index}`);
    }
  }
  
  console.log('\n✨ Export complete!\n');
  console.log('Next steps:');
  console.log('  1. Review exported test plans');
  console.log('  2. Share with team for documentation');
  console.log('  3. Use in ReportPortal integration');
}

async function buildTestPlanCatalog(): Promise<TestPlanCatalog> {
  const catalog = new TestPlanCatalog();
  const storiesDir = path.join(process.cwd(), 'user-stories');
  
  if (!fs.existsSync(storiesDir)) {
    console.error(`❌ Stories directory not found: ${storiesDir}`);
    process.exit(1);
  }
  
  // Scan all story files
  const parser = new StoryParser(false);
  const planner = new TestPlanner();
  
  const storyFiles = findStoryFiles(storiesDir);
  
  for (const storyFile of storyFiles) {
    try {
      const { story } = parser.parseStory(storyFile);
      const plan = planner.plan(story);
      catalog.add(plan);
    } catch (error) {
      console.warn(`⚠️  Warning: Could not parse ${storyFile}: ${error}`);
    }
  }
  
  return catalog;
}

function findStoryFiles(dir: string): string[] {
  const files: string[] = [];
  
  function scan(currentDir: string) {
    const entries = fs.readdirSync(currentDir, { withFileTypes: true });
    
    for (const entry of entries) {
      const fullPath = path.join(currentDir, entry.name);
      
      if (entry.isDirectory()) {
        scan(fullPath);
      } else if (entry.isFile() && /story_\d+_\d+.*\.md$/i.test(entry.name)) {
        files.push(fullPath);
      }
    }
  }
  
  scan(dir);
  return files;
}

// Parse CLI arguments
function parseArgs(): ExportOptions {
  const args = process.argv.slice(2);
  const options: ExportOptions = {};
  
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    
    switch (arg) {
      case '--output':
      case '-o':
        options.outputDir = args[++i];
        break;
      case '--format':
      case '-f':
        options.format = args[++i] as any;
        break;
      case '--story':
      case '-s':
        options.storyId = args[++i];
        break;
      case '--no-test-data':
        options.includeTestData = false;
        break;
      case '--no-index':
        options.generateIndex = false;
        break;
      case '--help':
      case '-h':
        printHelp();
        process.exit(0);
        break;
    }
  }
  
  return options;
}

function printHelp() {
  console.log(`
Test Plan Exporter

Usage: node dist/cli/export-test-plans.js [options]

Options:
  -o, --output <dir>      Output directory (default: ./test-plans)
  -f, --format <format>   Export format: json, markdown, html, all (default: all)
  -s, --story <id>        Export single story (e.g., story_3_1)
  --no-test-data          Exclude test data samples
  --no-index              Skip index file generation
  -h, --help              Show this help message

Examples:
  # Export all plans to default location
  node dist/cli/export-test-plans.js
  
  # Export single story to custom location
  node dist/cli/export-test-plans.js --story story_3_1 --output ./docs/test-plans
  
  # Export as JSON only
  node dist/cli/export-test-plans.js --format json
`);
}

// Run CLI
const options = parseArgs();
exportTestPlans(options).catch(error => {
  console.error('❌ Error:', error.message);
  process.exit(1);
});
