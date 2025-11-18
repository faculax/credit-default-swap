#!/usr/bin/env node
/**
 * ReportPortal Upload CLI
 * 
 * Command-line interface for uploading test results to ReportPortal.
 * Supports validation reports, Allure results, and batch uploads.
 * 
 * Story 20.8: ReportPortal Integration
 * 
 * Usage:
 *   npm run upload-reportportal -- --config reportportal.json --report test-report.json
 *   npm run upload-reportportal -- --batch --dir ./generated-tests --pattern "**\/*.json"
 */

import * as fs from 'node:fs/promises';
import type {
  ReportPortalConfig,
  ReportPortalBatchUploadOptions,
} from '../models/reportportal-model.js';
import type { ValidationReport } from '../models/validation-model.js';
import { ReportPortalUploader } from '../reportportal/reportportal-uploader.js';

/**
 * CLI arguments
 */
interface CliArgs {
  config?: string;
  report?: string;
  batch?: boolean;
  dir?: string;
  pattern?: string;
  endpoint?: string;
  token?: string;
  project?: string;
  launchName?: string;
  launchDescription?: string;
  mode?: 'DEFAULT' | 'DEBUG';
  verbose?: boolean;
  help?: boolean;
}

/**
 * Parse command-line arguments
 */
// eslint-disable-next-line sonarjs/cognitive-complexity
function parseArgs(args: string[]): CliArgs {
  const parsed: CliArgs = {};

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];

    switch (arg) {
      case '--config':
        // eslint-disable-next-line no-plusplus
        parsed.config = args[++i];
        break;
      case '--report':
        // eslint-disable-next-line no-plusplus
        parsed.report = args[++i];
        break;
      case '--batch':
        parsed.batch = true;
        break;
      case '--dir':
        // eslint-disable-next-line no-plusplus
        parsed.dir = args[++i];
        break;
      case '--pattern':
        // eslint-disable-next-line no-plusplus
        parsed.pattern = args[++i];
        break;
      case '--endpoint':
        // eslint-disable-next-line no-plusplus
        parsed.endpoint = args[++i];
        break;
      case '--token':
        // eslint-disable-next-line no-plusplus
        parsed.token = args[++i];
        break;
      case '--project':
        // eslint-disable-next-line no-plusplus
        parsed.project = args[++i];
        break;
      case '--launch-name':
        // eslint-disable-next-line no-plusplus
        parsed.launchName = args[++i];
        break;
      case '--launch-description':
        // eslint-disable-next-line no-plusplus
        parsed.launchDescription = args[++i];
        break;
      case '--mode':
        // eslint-disable-next-line no-plusplus
        parsed.mode = args[++i] as 'DEFAULT' | 'DEBUG';
        break;
      case '--verbose':
        parsed.verbose = true;
        break;
      case '--help':
      case '-h':
        parsed.help = true;
        break;
    }
  }

  return parsed;
}

/**
 * Show help message
 */
function showHelp(): void {
  console.log(`
ReportPortal Upload CLI

Upload test results to ReportPortal for centralized reporting.

Usage:
  npm run upload-reportportal -- [options]

Options:
  --config <path>              Path to ReportPortal config file (JSON)
  --report <path>              Path to validation report to upload
  --batch                      Upload all reports from directory
  --dir <path>                 Directory containing test results (for batch mode)
  --pattern <glob>             File pattern to match (default: **/*.json)
  --endpoint <url>             ReportPortal server endpoint
  --token <token>              ReportPortal API token
  --project <name>             ReportPortal project name
  --launch-name <name>         Launch name (default: "Test Evidence Framework")
  --launch-description <text>  Launch description
  --mode <mode>                Launch mode: DEFAULT or DEBUG (default: DEFAULT)
  --verbose                    Enable verbose logging
  --help, -h                   Show this help message

Examples:
  # Upload single validation report
  npm run upload-reportportal -- --report ./test-validation-report.json --config ./reportportal.json

  # Upload with command-line config
  npm run upload-reportportal -- \\
    --report ./test-validation-report.json \\
    --endpoint https://reportportal.example.com \\
    --token YOUR_API_TOKEN \\
    --project my-project \\
    --launch-name "Test Evidence Framework"

  # Batch upload all reports
  npm run upload-reportportal -- \\
    --batch \\
    --dir ./generated-tests \\
    --pattern "**/*.json" \\
    --config ./reportportal.json

Configuration File (reportportal.json):
  {
    "endpoint": "https://reportportal.example.com",
    "token": "YOUR_API_TOKEN",
    "project": "my-project",
    "launchName": "Test Evidence Framework",
    "launchDescription": "Automated test results",
    "mode": "DEFAULT",
    "debug": false
  }

Environment Variables:
  RP_ENDPOINT   ReportPortal server endpoint
  RP_TOKEN      ReportPortal API token
  RP_PROJECT    ReportPortal project name
`);
}

/**
 * Load configuration from file
 */
async function loadConfig(configPath: string): Promise<Partial<ReportPortalConfig>> {
  try {
    const content = await fs.readFile(configPath, 'utf-8');
    return JSON.parse(content) as Partial<ReportPortalConfig>;
  } catch (error) {
    throw new Error(`Failed to load config from ${configPath}: ${(error as Error).message}`);
  }
}

/**
 * Load validation report from file
 */
async function loadReport(reportPath: string): Promise<ValidationReport> {
  try {
    const content = await fs.readFile(reportPath, 'utf-8');
    return JSON.parse(content) as ValidationReport;
  } catch (error) {
    throw new Error(`Failed to load report from ${reportPath}: ${(error as Error).message}`);
  }
}

/**
 * Build ReportPortal configuration
 */
async function buildConfig(args: CliArgs): Promise<ReportPortalConfig> {
  let config: Partial<ReportPortalConfig> = {};

  // Load from config file if specified
  if (args.config) {
    config = await loadConfig(args.config);
  }

  // Override with environment variables
  if (process.env.RP_ENDPOINT) {
    config.endpoint = process.env.RP_ENDPOINT;
  }
  if (process.env.RP_TOKEN) {
    config.token = process.env.RP_TOKEN;
  }
  if (process.env.RP_PROJECT) {
    config.project = process.env.RP_PROJECT;
  }

  // Override with command-line arguments
  if (args.endpoint) config.endpoint = args.endpoint;
  if (args.token) config.token = args.token;
  if (args.project) config.project = args.project;
  if (args.launchName) config.launchName = args.launchName;
  if (args.launchDescription) config.launchDescription = args.launchDescription;
  if (args.mode) config.mode = args.mode;
  if (args.verbose) config.debug = true;

  // Validate required fields
  if (!config.endpoint) {
    throw new Error('ReportPortal endpoint is required (--endpoint or RP_ENDPOINT)');
  }
  if (!config.token) {
    throw new Error('ReportPortal API token is required (--token or RP_TOKEN)');
  }
  if (!config.project) {
    throw new Error('ReportPortal project is required (--project or RP_PROJECT)');
  }

  return config as ReportPortalConfig;
}

/**
 * Upload single report
 */
async function uploadReport(config: ReportPortalConfig, reportPath: string): Promise<void> {
  console.log(`\n📄 Loading report: ${reportPath}`);
  const report = await loadReport(reportPath);

  console.log(`\n🚀 Starting upload to ReportPortal...`);
  console.log(`   Endpoint: ${config.endpoint}`);
  console.log(`   Project: ${config.project}`);

  const uploader = new ReportPortalUploader(config);

  // Show progress
  uploader.onProgress((message, current, total) => {
    const percentage = Math.round((current / total) * 100);
    console.log(`   [${percentage}%] ${message}`);
  });

  const result = await uploader.uploadValidationReport(report);

  if (result.success) {
    console.log(`\n✅ Upload successful!`);
    console.log(`   Launch ID: ${result.launchId}`);
    if (result.launchNumber) {
      console.log(`   Launch #${result.launchNumber}`);
    }
    if (result.launchUrl) {
      console.log(`   URL: ${result.launchUrl}`);
    }
    console.log(`\n📊 Statistics:`);
    console.log(`   Items Uploaded: ${result.itemsUploaded}`);
    console.log(`   Logs Uploaded: ${result.logsUploaded}`);
    console.log(`   Duration: ${result.duration}ms`);
    console.log(`\n   Total Tests: ${result.stats.total}`);
    console.log(`   ✅ Passed: ${result.stats.passed}`);
    console.log(`   ❌ Failed: ${result.stats.failed}`);
    console.log(`   ⏭️  Skipped: ${result.stats.skipped}`);
  } else {
    console.error(`\n❌ Upload failed: ${result.error}`);
    process.exit(1);
  }
}

/**
 * Upload batch of reports
 */
async function uploadBatch(config: ReportPortalConfig, args: CliArgs): Promise<void> {
  if (!args.dir) {
    throw new Error('--dir is required for batch mode');
  }

  const options: ReportPortalBatchUploadOptions = {
    sourceDir: args.dir,
    patterns: args.pattern ? [args.pattern] : ['**/*.json'],
    launchName: config.launchName || 'Test Evidence Framework',
    launchDescription: config.launchDescription,
    launchAttributes: config.launchAttributes,
  };

  console.log(`\n📁 Scanning directory: ${options.sourceDir}`);
  console.log(`   Pattern: ${options.patterns?.join(', ') || 'N/A'}`);

  const uploader = new ReportPortalUploader(config);

  // Show progress
  uploader.onProgress((message, current, total) => {
    const percentage = Math.round((current / total) * 100);
    console.log(`   [${percentage}%] ${message}`);
  });

  const result = await uploader.uploadBatch(options);

  if (result.success) {
    console.log(`\n✅ Batch upload successful!`);
    console.log(`   Launch ID: ${result.launchId}`);
    if (result.launchNumber) {
      console.log(`   Launch #${result.launchNumber}`);
    }
    if (result.launchUrl) {
      console.log(`   URL: ${result.launchUrl}`);
    }
    console.log(`\n📊 Statistics:`);
    console.log(`   Items Uploaded: ${result.itemsUploaded}`);
    console.log(`   Logs Uploaded: ${result.logsUploaded}`);
    console.log(`   Duration: ${result.duration}ms`);
  } else {
    console.error(`\n❌ Batch upload failed: ${result.error}`);
    process.exit(1);
  }
}

/**
 * Main function
 */
async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));

  if (args.help) {
    showHelp();
    return;
  }

  try {
    const config = await buildConfig(args);

    if (args.batch) {
      await uploadBatch(config, args);
    } else if (args.report) {
      await uploadReport(config, args.report);
    } else {
      console.error('\n❌ Error: Either --report or --batch must be specified\n');
      showHelp();
      process.exit(1);
    }
  } catch (error) {
    console.error(`\n❌ Error: ${(error as Error).message}\n`);
    process.exit(1);
  }
}

// Run main function
// eslint-disable-next-line unicorn/prefer-top-level-await
main().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
