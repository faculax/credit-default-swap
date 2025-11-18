#!/usr/bin/env node
/**
 * CLI Tool for Evidence Export
 * 
 * Export test evidence from ReportPortal and generate static dashboard.
 * 
 * Usage:
 *   export-evidence --output-dir ./evidence --story-id STORY-001
 *   export-evidence --output-dir ./evidence --services backend,gateway
 *   export-evidence --output-dir ./evidence --start-time 1640000000000
 * 
 * @module cli/export-evidence
 */

import * as path from 'node:path';
import * as fs from 'node:fs/promises';
import type { ReportPortalConfig } from '../models/reportportal-model.js';
import { EvidenceExporter } from '../evidence/evidence-exporter.js';
import { StaticSiteGenerator } from '../evidence/static-site-generator.js';

/**
 * Parse command-line arguments
 */
function parseArgs(): {
  config?: string;
  outputDir?: string;
  storyId?: string;
  services?: string[];
  startTime?: number;
  endTime?: number;
  limit?: number;
  projectName?: string;
  baseUrl?: string;
  verbose?: boolean;
  help?: boolean;
} {
  const args: Record<string, string | boolean> = {};
  const processArguments = process.argv.slice(2);

  // eslint-disable-next-line no-plusplus
  for (let index = 0; index < processArguments.length; index++) {
    const arg = processArguments[index];

    if (arg === '--help' || arg === '-h') {
      args.help = true;
    } else if (arg === '--verbose' || arg === '-v') {
      args.verbose = true;
    } else if (arg.startsWith('--')) {
      const key = arg.slice(2);
      // eslint-disable-next-line no-plusplus
      const value = processArguments[++index];
      args[key] = value;
    }
  }

  return {
    config: args.config as string | undefined,
    outputDir: args['output-dir'] as string | undefined,
    storyId: args['story-id'] as string | undefined,
    services: args.services ? (args.services as string).split(',').map(s => s.trim()) : undefined,
    startTime: args['start-time'] ? Number.parseInt(args['start-time'] as string, 10) : undefined,
    endTime: args['end-time'] ? Number.parseInt(args['end-time'] as string, 10) : undefined,
    limit: args.limit ? Number.parseInt(args.limit as string, 10) : undefined,
    projectName: args['project-name'] as string | undefined,
    baseUrl: args['base-url'] as string | undefined,
    verbose: Boolean(args.verbose),
    help: Boolean(args.help),
  };
}

/**
 * Show help message
 */
function showHelp(): void {
  console.log(`
Evidence Export CLI

Export test evidence from ReportPortal and generate static dashboard.

USAGE:
  export-evidence [OPTIONS]

OPTIONS:
  --config <path>          Path to ReportPortal config file (default: reportportal.json)
  --output-dir <path>      Output directory for evidence (required)
  --story-id <id>          Filter by story ID
  --services <list>        Comma-separated list of services to filter
  --start-time <timestamp> Start time filter (Unix timestamp in ms)
  --end-time <timestamp>   End time filter (Unix timestamp in ms)
  --limit <number>         Maximum number of launches to query
  --project-name <name>    Project name for dashboard (default: Test Evidence Dashboard)
  --base-url <url>         Base URL for GitHub Pages (e.g., /my-project/)
  --verbose, -v            Enable verbose logging
  --help, -h               Show this help message

CONFIGURATION:
  ReportPortal configuration can be provided via:
  1. Config file (--config or reportportal.json)
  2. Environment variables (REPORTPORTAL_ENDPOINT, REPORTPORTAL_TOKEN, etc.)

EXAMPLES:
  # Export all stories
  export-evidence --output-dir ./evidence

  # Export specific story
  export-evidence --output-dir ./evidence --story-id STORY-20.1

  # Export specific services
  export-evidence --output-dir ./evidence --services backend,gateway,risk-engine

  # Export with time range
  export-evidence --output-dir ./evidence --start-time 1640000000000 --end-time 1640100000000

  # Export with custom project name and base URL
  export-evidence --output-dir ./evidence --project-name "CDS Platform" --base-url /credit-default-swap/

  # Verbose mode
  export-evidence --output-dir ./evidence --verbose
`);
}

/**
 * Load ReportPortal configuration
 */
async function loadConfig(configPath = 'reportportal.json'): Promise<ReportPortalConfig> {
  // Try config file
  try {
    const configContent = await fs.readFile(configPath, 'utf8');
    const config = JSON.parse(configContent) as ReportPortalConfig;
    console.log(`Loaded config from ${configPath}`);
    return config;
  } catch {
    // Try environment variables
    const endpoint = process.env.REPORTPORTAL_ENDPOINT;
    const token = process.env.REPORTPORTAL_TOKEN;
    const project = process.env.REPORTPORTAL_PROJECT;

    if (!endpoint || !token || !project) {
      throw new Error(
        'ReportPortal configuration not found. Provide config file or set environment variables (REPORTPORTAL_ENDPOINT, REPORTPORTAL_TOKEN, REPORTPORTAL_PROJECT).'
      );
    }

    console.log('Loaded config from environment variables');
    return {
      endpoint,
      token,
      project,
    };
  }
}

/**
 * Main execution
 */
async function main(): Promise<void> {
  const args = parseArgs();

  if (args.help) {
    showHelp();
    process.exit(0);
  }

  if (!args.outputDir) {
    console.error('Error: --output-dir is required');
    showHelp();
    process.exit(1);
  }

  try {
    // Load configuration
    const config = await loadConfig(args.config);

    // Create output directories
    const jsonDir = path.join(args.outputDir, 'json');
    const htmlDir = path.join(args.outputDir, 'html');
    await fs.mkdir(jsonDir, { recursive: true });
    await fs.mkdir(htmlDir, { recursive: true });

    console.log('\n=== Evidence Export ===\n');

    // Export evidence to JSON
    console.log('Exporting evidence from ReportPortal...');
    const exporter = new EvidenceExporter(config);
    const exportResult = await exporter.exportAll({
      outputDir: jsonDir,
      storyId: args.storyId,
      services: args.services,
      startTime: args.startTime,
      endTime: args.endTime,
      limit: args.limit,
      verbose: args.verbose,
    });

    if (!exportResult.success) {
      console.error(`\nExport failed: ${exportResult.error}`);
      process.exit(1);
    }

    console.log(`\nExport successful:`);
    console.log(`  Stories exported: ${exportResult.storiesExported}`);
    console.log(`  Files created: ${exportResult.filesCreated.length}`);
    console.log(`  Duration: ${exportResult.duration}ms`);

    // Generate static site
    console.log('\nGenerating static dashboard...');
    const generator = new StaticSiteGenerator(args.verbose);
    const siteResult = await generator.generate({
      inputDir: jsonDir,
      outputDir: htmlDir,
      projectName: args.projectName,
      baseUrl: args.baseUrl,
      verbose: args.verbose,
    });

    if (!siteResult.success) {
      console.error(`\nSite generation failed: ${siteResult.error}`);
      process.exit(1);
    }

    console.log(`\nSite generation successful:`);
    console.log(`  Pages generated: ${siteResult.pagesGenerated}`);
    console.log(`  Files created: ${siteResult.filesCreated.length}`);
    console.log(`  Duration: ${siteResult.duration}ms`);

    // Copy CSS file
    const cssSource = path.resolve(process.cwd(), 'dashboard.css');
    const cssTarget = path.join(htmlDir, 'dashboard.css');
    await fs.copyFile(cssSource, cssTarget);
    console.log(`\nCopied dashboard.css to ${cssTarget}`);

    console.log(`\n✓ Evidence export complete!`);
    console.log(`  JSON files: ${jsonDir}`);
    console.log(`  HTML files: ${htmlDir}`);
    console.log(`\nOpen ${path.join(htmlDir, 'index.html')} in a browser to view the dashboard.`);
  } catch (error) {
    console.error(`\nError: ${error instanceof Error ? error.message : String(error)}`);
    if (args.verbose && error instanceof Error) {
      console.error(error.stack);
    }
    process.exit(1);
  }
}

// Execute (top-level await not supported in current module config)
// eslint-disable-next-line unicorn/prefer-top-level-await
void main().catch((error: unknown) => {
  console.error('Unhandled error:', error);
  process.exit(1);
});
