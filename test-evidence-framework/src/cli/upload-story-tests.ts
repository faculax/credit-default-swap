#!/usr/bin/env node
/**
 * Upload Story Test Results to ReportPortal
 * 
 * Uploads hierarchical story-based test results with proper Epic/Story/AC nesting.
 */

import * as fs from 'node:fs/promises';
import type { ReportPortalConfig } from '../models/reportportal-model.js';
import { StoryTestUploader } from '../reportportal/story-test-uploader.js';
import type { StoryTestResult } from '../reportportal/story-test-converter.js';

interface UploadOptions {
  config: string;
  results: string;
  help?: boolean;
}

/**
 * Parse command line arguments
 */
function parseArgs(args: string[]): UploadOptions {
  const options: UploadOptions = {
    config: 'reportportal.json',
    results: '',
  };

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    const next = args[i + 1];

    switch (arg) {
      case '--config':
      case '-c':
        if (next) {
          options.config = next;
        }
        break;

      case '--results':
      case '-r':
        if (next) {
          options.results = next;
        }
        break;

      case '--help':
      case '-h':
        options.help = true;
        break;
    }
  }

  return options;
}

/**
 * Show help message
 */
function showHelp(): void {
  console.log(`
📊 Upload Story Test Results to ReportPortal

Usage:
  upload-story-tests [options]

Options:
  --config, -c <path>    Path to ReportPortal config file (default: reportportal.json)
  --results, -r <path>   Path to story test results JSON file (required)
  --help, -h             Show this help message

Example:
  npm run upload-story-tests -- --config reportportal.json --results sample-story-test-results.json

ReportPortal Config Format (reportportal.json):
  {
    "endpoint": "http://localhost:8080",
    "token": "YOUR_API_TOKEN",
    "project": "your-project-name",
    "launchName": "Test Evidence Framework",
    "mode": "DEFAULT"
  }

Story Test Results Format:
  {
    "id": "epic-03-test-run-001",
    "title": "Epic 3: CDS Trade Capture - Test Execution",
    "timestamp": 1700337600000,
    "epic": {
      "id": "epic_03",
      "name": "CDS Trade Capture",
      "description": "..."
    },
    "stories": [
      {
        "storyId": "story_3_1",
        "title": "...",
        "acceptanceCriteria": [
          {
            "id": "AC-3.1.1",
            "description": "...",
            "tests": [
              {
                "name": "test_name",
                "status": "passed|failed|skipped",
                "duration": 245,
                "assertions": 15
              }
            ]
          }
        ]
      }
    ],
    "summary": { ... }
  }
`);
}

/**
 * Load configuration file
 */
async function loadConfig(configPath: string): Promise<ReportPortalConfig> {
  try {
    const content = await fs.readFile(configPath, 'utf-8');
    return JSON.parse(content) as ReportPortalConfig;
  } catch (error) {
    throw new Error(`Failed to load config from ${configPath}: ${(error as Error).message}`);
  }
}

/**
 * Load test results file
 */
async function loadTestResults(resultsPath: string): Promise<StoryTestResult> {
  try {
    const content = await fs.readFile(resultsPath, 'utf-8');
    return JSON.parse(content) as StoryTestResult;
  } catch (error) {
    throw new Error(`Failed to load test results from ${resultsPath}: ${(error as Error).message}`);
  }
}

/**
 * Main execution
 */
async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const options = parseArgs(args);

  if (options.help) {
    showHelp();
    return;
  }

  if (!options.results) {
    console.error('❌ Error: --results parameter is required\n');
    showHelp();
    process.exit(1);
  }

  try {
    // Load configuration
    console.log(`📄 Loading config: ${options.config}`);
    const config = await loadConfig(options.config);

    // Load test results
    console.log(`📄 Loading test results: ${options.results}\n`);
    const testResults = await loadTestResults(options.results);

    console.log(`🔍 Test Results Summary:`);
    console.log(`   Epic: ${testResults.epic.name}`);
    console.log(`   Stories: ${testResults.stories.length}`);
    console.log(`   Total Tests: ${testResults.summary.totalTests}`);
    console.log(`   Passed: ${testResults.summary.passed}`);
    console.log(`   Failed: ${testResults.summary.failed}`);
    console.log(`   Skipped: ${testResults.summary.skipped}\n`);

    // Upload to ReportPortal
    const uploader = new StoryTestUploader(config);
    const result = await uploader.upload(testResults);

    if (result.success) {
      process.exit(0);
    } else {
      console.error(`\n❌ Upload failed: ${result.error}`);
      process.exit(1);
    }
  } catch (error) {
    console.error(`\n❌ Error: ${(error as Error).message}`);
    process.exit(1);
  }
}

// Run main  
main();

