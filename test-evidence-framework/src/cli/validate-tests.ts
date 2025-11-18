/**
 * Validate Tests CLI
 * 
 * Command-line tool for validating and crystallizing generated test files.
 * 
 * Usage:
 *   npm run validate-tests -- --dir <tests-dir> [options]
 * 
 * @module validate-tests-cli
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { CodeValidator } from '../validators/code-validator.js';
import { Crystallizer } from '../crystallization/crystallizer.js';

interface CliOptions {
  dir: string;
  pattern?: string;
  report?: boolean;
  crystallize?: boolean;
  storyId?: string;
  output?: string;
  verbose?: boolean;
}

/**
 * Parse command-line arguments
 */
function parseArgs(): CliOptions {
  const args = process.argv.slice(2);
  const options: CliOptions = {
    dir: 'generated-tests/',
    pattern: '**/*.{ts,java}',
    report: false,
    crystallize: false,
    verbose: false,
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    const next = args[i + 1];

    switch (arg) {
      case '--dir':
        options.dir = next;
        i++;
        break;
      case '--pattern':
        options.pattern = next;
        i++;
        break;
      case '--story-id':
        options.storyId = next;
        i++;
        break;
      case '--output':
        options.output = next;
        i++;
        break;
      case '--report':
        options.report = true;
        break;
      case '--crystallize':
        options.crystallize = true;
        break;
      case '--verbose':
        options.verbose = true;
        break;
      case '--help':
        printHelp();
        process.exit(0);
        break;
    }
  }

  return options;
}

/**
 * Print help message
 */
function printHelp(): void {
  console.log(`
🧪 Test Validation & Crystallization Tool

Usage:
  npm run validate-tests -- --dir <directory> [options]

Options:
  --dir <path>          Directory containing test files (default: generated-tests/)
  --pattern <glob>      File pattern to match (default: **/*.{ts,java})
  --story-id <id>       Story ID for crystallization
  --output <path>       Output path for validation report
  --report              Generate detailed HTML report
  --crystallize         Crystallize validated tests
  --verbose             Enable verbose logging
  --help                Show this help message

Examples:
  # Validate all tests in directory
  npm run validate-tests -- --dir ./backend-tests

  # Validate and generate report
  npm run validate-tests -- --dir ./frontend-tests --report --output validation-report.html

  # Validate and crystallize
  npm run validate-tests -- --dir ./tests --crystallize --story-id story-03-001

  # Verbose validation
  npm run validate-tests -- --dir ./tests --verbose
  `);
}

/**
 * Find test files recursively
 */
async function findTestFiles(dir: string, pattern: string): Promise<string[]> {
  const files: string[] = [];
  
  async function scan(currentDir: string): Promise<void> {
    try {
      const entries = await fs.readdir(currentDir, { withFileTypes: true });
      
      for (const entry of entries) {
        const fullPath = path.join(currentDir, entry.name);
        
        if (entry.isDirectory()) {
          await scan(fullPath);
        } else if (entry.isFile() && matchesPattern(entry.name, pattern)) {
          files.push(fullPath);
        }
      }
    } catch (error) {
      // Ignore permission errors
    }
  }

  await scan(dir);
  return files;
}

/**
 * Check if filename matches pattern
 */
function matchesPattern(filename: string, pattern: string): boolean {
  // Simple pattern matching
  if (pattern.includes('**')) {
    const extension = pattern.split('.').pop();
    return filename.endsWith(`.${extension}`);
  }
  if (pattern.startsWith('*')) {
    const extension = pattern.slice(1);
    return filename.endsWith(extension);
  }
  return filename.includes(pattern);
}

/**
 * Validate tests
 */
// eslint-disable-next-line sonarjs/cognitive-complexity
async function validateTests(options: CliOptions): Promise<void> {
  console.log('🧪 Test Validation Tool');
  console.log('─'.repeat(60));
  console.log(`📁 Directory: ${options.dir}`);
  console.log(`📝 Pattern: ${options.pattern}`);
  console.log('─'.repeat(60));
  console.log('');

  // Find test files
  console.log('🔍 Discovering test files...');
  const files = await findTestFiles(options.dir, options.pattern || '**/*.{ts,java}');
  console.log(`✅ Found ${files.length} test files\n`);

  if (files.length === 0) {
    console.log('❌ No test files found');
    return;
  }

  // Create validator
  const validator = new CodeValidator({
    checkSyntax: true,
    checkCompilation: true,
    checkQuality: true,
    qualityThreshold: 70,
  });

  // Set up progress reporting
  validator.onProgress((progress) => {
    if (options.verbose) {
      console.log(`   Validating ${path.basename(progress.currentFile)}...`);
    }
  });

  // Validate all files
  console.log('🔍 Validating tests...\n');
  const report = await validator.validateAll(files);

  // Print summary
  console.log('─'.repeat(60));
  console.log('📊 Validation Summary');
  console.log('─'.repeat(60));
  console.log(`Total Tests:     ${report.summary.totalTests}`);
  console.log(`✅ Valid Tests:   ${report.summary.validTests}`);
  console.log(`❌ Invalid Tests: ${report.summary.invalidTests}`);
  console.log(`⚠️  Total Issues:  ${report.summary.totalIssues}`);
  console.log('');
  console.log('Issues by Severity:');
  console.log(`  Errors:      ${report.summary.issuesBySeverity.error}`);
  console.log(`  Warnings:    ${report.summary.issuesBySeverity.warning}`);
  console.log(`  Info:        ${report.summary.issuesBySeverity.info}`);
  console.log(`  Suggestions: ${report.summary.issuesBySeverity.suggestion}`);
  console.log('');
  console.log(`📈 Quality Score: ${Math.round(report.quality.score)}/100 (Grade: ${report.quality.grade})`);
  console.log('─'.repeat(60));

  // Print failed tests
  if (report.summary.invalidTests > 0) {
    console.log('\n❌ Failed Tests:');
    const failedTests = report.results.filter(r => !r.valid);
    for (const r of failedTests) {
      console.log(`\n  ${path.basename(r.filePath)}`);
      for (const issue of r.issues) {
        console.log(`    ${issue.severity.toUpperCase()}: ${issue.message}`);
      }
    }
  }

  // Crystallization
  if (options.crystallize && options.storyId) {
    console.log('\n🔮 Crystallizing tests...');
    const crystallizer = new Crystallizer();
    await crystallizer.initialize();

    let crystallizedCount = 0;
    for (const result of report.results) {
      if (result.valid) {
        try {
          await crystallizer.registerTest(result.filePath, options.storyId);
          await crystallizer.review(result.filePath, 'auto-validator', 'approve', 'Auto-approved after validation');
          await crystallizer.crystallize(result.filePath, 'auto-validator');
          crystallizedCount++;
          if (options.verbose) {
            console.log(`  ✅ Crystallized: ${path.basename(result.filePath)}`);
          }
        } catch (error) {
          console.error(`  ❌ Failed to crystallize ${path.basename(result.filePath)}:`, (error as Error).message);
        }
      }
    }

    console.log(`\n✅ Crystallized ${crystallizedCount}/${report.summary.validTests} valid tests`);
  }

  // Generate report
  if (options.report) {
    const outputPath = options.output || 'validation-report.json';
    await fs.writeFile(outputPath, JSON.stringify(report, null, 2), 'utf-8');
    console.log(`\n📄 Report saved to: ${outputPath}`);
  }

  console.log('\n🎉 Validation complete!\n');

  // Exit with error code if validation failed
  if (report.summary.invalidTests > 0) {
    process.exit(1);
  }
}

/**
 * Main function
 */
async function main(): Promise<void> {
  try {
    const options = parseArgs();
    await validateTests(options);
  } catch (error) {
    console.error('\n❌ Error:', (error as Error).message);
    process.exit(1);
  }
}

// Run CLI
// eslint-disable-next-line unicorn/prefer-top-level-await
void main();
