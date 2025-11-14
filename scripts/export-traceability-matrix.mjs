#!/usr/bin/env node

import { promises as fs } from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import process from 'node:process';

const DEFAULT_RESULTS_DIR = path.join(process.cwd(), 'backend', 'allure-results');
const DEFAULT_OUTPUT_BASENAME = 'traceability-matrix';
const SUPPORTED_FORMATS = new Set(['json', 'csv']);

function parseArgs(argv) {
  const options = {
    resultsDir: DEFAULT_RESULTS_DIR,
    format: 'json',
    failOnMissing: false,
    failOnOrphans: false,
    quiet: false
  };

  for (let index = 0; index < argv.length;) {
    const arg = argv[index];
    index += 1;
    switch (arg) {
      case '--results-dir':
        options.resultsDir = path.resolve(requireValue(argv[index], arg));
        index += 1;
        break;
      case '--output':
        options.output = path.resolve(requireValue(argv[index], arg));
        index += 1;
        break;
      case '--format':
        options.format = requireValue(argv[index], arg);
        index += 1;
        break;
      case '--expected-catalog':
        options.expectedCatalog = path.resolve(requireValue(argv[index], arg));
        index += 1;
        break;
      case '--fail-on-missing':
        options.failOnMissing = true;
        break;
      case '--fail-on-orphans':
        options.failOnOrphans = true;
        break;
      case '--quiet':
        options.quiet = true;
        break;
      case '--help':
        throw new HelpRequestedError();
      default:
        throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!SUPPORTED_FORMATS.has(options.format)) {
    throw new Error(`Unsupported format: ${options.format}`);
  }

  return options;
}

class HelpRequestedError extends Error {}

function requireValue(value, flag) {
  if (value === undefined) {
    throw new Error(`Missing value for ${flag}`);
  }
  return value;
}

function labelLookup(labels = []) {
  const lookup = Object.create(null);
  for (const label of labels) {
    if (label && label.name) {
      lookup[label.name] = label.value ?? '';
    }
  }
  return lookup;
}

function summariseStatus(statuses) {
  if (statuses.includes('failed')) {
    return 'failed';
  }
  if (statuses.includes('broken')) {
    return 'broken';
  }
  if (statuses.includes('skipped')) {
    return 'skipped';
  }
  if (statuses.length === 0) {
    return 'unknown';
  }
  return 'passed';
}

function escapeCsvValue(value) {
  const text = String(value ?? '');
  if (text === '') {
    return '';
  }
  if (/[",\n]/.test(text)) {
    return '"' + text.replaceAll('"', '""') + '"';
  }
  return text;
}

export async function readStoryCatalog(catalogPath) {
  if (!catalogPath) {
    return null;
  }
  const raw = await fs.readFile(catalogPath, 'utf8');
  const parsed = JSON.parse(raw);
  if (!Array.isArray(parsed.stories)) {
    throw new TypeError('Story catalog must contain a "stories" array');
  }
  return parsed;
}

export async function collectAllureResults(resultsDir) {
  const dirExists = await fs
    .stat(resultsDir)
    .then(stat => stat.isDirectory())
    .catch(() => false);
  if (!dirExists) {
    throw new Error(`Results directory not found: ${resultsDir}`);
  }

  const files = await fs.readdir(resultsDir);
  const resultEntries = [];

  for (const fileName of files) {
    if (!fileName.endsWith('-result.json')) {
      continue;
    }
    const filePath = path.join(resultsDir, fileName);
    const raw = await fs.readFile(filePath, 'utf8');
    let parsed;
    try {
      parsed = JSON.parse(raw);
    } catch (error) {
      throw new Error(`Unable to parse Allure result ${fileName}: ${error.message}`);
    }
    const labels = labelLookup(parsed.labels);
    const storyId = labels.story;
    if (!storyId) {
      continue; // Skip tests that are not linked to a story
    }

    resultEntries.push({
      storyId,
      testCaseId: parsed.testCaseId ?? '',
      name: parsed.name ?? parsed.testCaseName ?? '',
      status: parsed.status ?? 'unknown',
      testType: labels.testType ?? '',
      service: labels.service ?? '',
      microservice: labels.microservice ?? '',
      suite: labels.suite ?? labels.testClass ?? '',
      start: parsed.start ?? null,
      stop: parsed.stop ?? null
    });
  }

  return resultEntries;
}

function aggregateByStory(entries, catalog) {
  const grouped = new Map();
  for (const entry of entries) {
    if (!grouped.has(entry.storyId)) {
      grouped.set(entry.storyId, []);
    }
    grouped.get(entry.storyId).push(entry);
  }

  const catalogStories = catalog ? catalog.stories : [];
  const storyOutputs = [];
  const missingStories = [];
  const orphanStories = [];

  const handledStories = new Set();

  for (const spec of catalogStories) {
    const tests = grouped.get(spec.id) ?? [];
    handledStories.add(spec.id);
    storyOutputs.push({
      id: spec.id,
      title: spec.title ?? null,
      epic: spec.epic ?? null,
      userStories: spec.userStories ?? [],
      microservice: spec.microservice ?? null,
      requiresCoverage: spec.requiresCoverage !== false,
      overallStatus: summariseStatus(tests.map(test => test.status)),
      testCount: tests.length,
      tests
    });
    if (spec.requiresCoverage !== false && tests.length === 0) {
      missingStories.push(spec.id);
    }
  }

  for (const [storyId, tests] of grouped.entries()) {
    if (handledStories.has(storyId)) {
      continue;
    }
    orphanStories.push({ id: storyId, testCount: tests.length });
    storyOutputs.push({
      id: storyId,
      title: null,
      epic: null,
      userStories: [],
      microservice: tests[0]?.microservice ?? null,
      requiresCoverage: true,
      overallStatus: summariseStatus(tests.map(test => test.status)),
      testCount: tests.length,
      tests
    });
  }

  storyOutputs.sort((a, b) => a.id.localeCompare(b.id));

  return { storyOutputs, missingStories, orphanStories };
}

export async function exportTraceabilityMatrix(options = {}) {
  const {
    resultsDir = DEFAULT_RESULTS_DIR,
    catalogPath = null,
    format = 'json',
    output,
    failOnMissing = false,
    failOnOrphans = false,
    quiet = false
  } = options;

  if (!SUPPORTED_FORMATS.has(format)) {
    throw new Error(`Unsupported format: ${format}`);
  }

  const entries = await collectAllureResults(resultsDir);
  const catalog = catalogPath ? await readStoryCatalog(catalogPath) : null;
  const aggregated = aggregateByStory(entries, catalog);
  const artifact = buildArtifact({
    resultsDir,
    catalogPath,
    aggregated
  });
  const destination = output ?? path.join(process.cwd(), `${DEFAULT_OUTPUT_BASENAME}.${format}`);

  await writeArtifact(destination, format, artifact);

  if (!quiet) {
    logSummary(aggregated, destination);
  }

  enforceThresholds({
    failOnMissing,
    failOnOrphans,
    missingStories: aggregated.missingStories,
    orphanStories: aggregated.orphanStories
  });

  return { artifact, destination };
}

function buildArtifact({ resultsDir, catalogPath, aggregated }) {
  const artifact = {
    formatVersion: 1,
    generatedAt: new Date().toISOString(),
    resultsDir,
    stories: aggregated.storyOutputs
  };
  if (catalogPath) {
    artifact.catalogPath = catalogPath;
  }
  if (aggregated.missingStories.length > 0) {
    artifact.missingStories = aggregated.missingStories;
  }
  if (aggregated.orphanStories.length > 0) {
    artifact.orphanStories = aggregated.orphanStories;
  }
  return artifact;
}

async function writeArtifact(destination, format, artifact) {
  if (format === 'json') {
    await fs.writeFile(destination, JSON.stringify(artifact, null, 2), 'utf8');
    return;
  }

  const header = [
    'storyId',
    'title',
    'epic',
    'userStories',
    'testCaseId',
    'testName',
    'status',
    'testType',
    'service',
    'microservice'
  ];
  const rows = [header];

  for (const story of artifact.stories) {
    const metadataCells = [
      story.id,
      story.title ?? '',
      story.epic ?? '',
      (story.userStories ?? []).join('; ')
    ];
    if (story.tests.length === 0) {
      rows.push([...metadataCells, '', '', story.overallStatus, '', '', story.microservice ?? '']);
      continue;
    }
    for (const test of story.tests) {
      rows.push([
        ...metadataCells,
        test.testCaseId,
        test.name,
        test.status,
        test.testType,
        test.service,
        test.microservice
      ]);
    }
  }

  const csv = rows.map(columns => columns.map(escapeCsvValue).join(',')).join('\n');
  await fs.writeFile(destination, csv, 'utf8');
}

function logSummary(aggregated, destination) {
  console.log(`[traceability] ${aggregated.storyOutputs.length} stories captured`);
  if (aggregated.missingStories.length > 0) {
    console.warn(`[traceability] missing story coverage: ${aggregated.missingStories.join(', ')}`);
  }
  if (aggregated.orphanStories.length > 0) {
    const orphanList = aggregated.orphanStories.map(orphan => orphan.id).join(', ');
    console.warn(`[traceability] story IDs without catalog entry: ${orphanList}`);
  }
  console.log(`[traceability] artifact written to ${destination}`);
}

function enforceThresholds({ failOnMissing, failOnOrphans, missingStories, orphanStories }) {
  if (failOnMissing && missingStories.length > 0) {
    const error = new Error(`Missing story coverage for: ${missingStories.join(', ')}`);
    error.code = 'TRACEABILITY_MISSING';
    throw error;
  }
  if (failOnOrphans && orphanStories.length > 0) {
    const error = new Error(
      `Found story IDs without catalog entry: ${orphanStories.map(orphan => orphan.id).join(', ')}`
    );
    error.code = 'TRACEABILITY_ORPHANS';
    throw error;
  }
}

function printHelp() {
  console.log(
    'Usage: node scripts/export-traceability-matrix.mjs [options]\n\n' +
      'Options:\n' +
      '  --results-dir <path>       Directory containing Allure *-result.json files (default: backend/allure-results)\n' +
      '  --expected-catalog <path>  Path to story catalog JSON file for coverage validation\n' +
      '  --output <path>            Destination file path (default: traceability-matrix.<format>)\n' +
      '  --format <json|csv>        Output format (default: json)\n' +
      '  --fail-on-missing          Exit with non-zero code if catalog stories lack coverage\n' +
      '  --fail-on-orphans          Exit with non-zero code if tests reference unknown story IDs\n' +
      '  --quiet                    Suppress console summary\n' +
      '  --help                     Show this help message\n'
  );
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const argv = process.argv.slice(2);
  try {
    const options = parseArgs(argv);
    await exportTraceabilityMatrix({
      resultsDir: options.resultsDir,
      catalogPath: options.expectedCatalog,
      format: options.format,
      output: options.output,
      failOnMissing: options.failOnMissing,
      failOnOrphans: options.failOnOrphans,
      quiet: options.quiet
    });
  } catch (error) {
    if (error instanceof HelpRequestedError) {
      printHelp();
      process.exit(0);
    }
    console.error(error.message);
    process.exit(1);
  }
}
