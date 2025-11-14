#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const CONFIG_PATH = path.join(
  __dirname,
  '..',
  'unified-testing-config',
  'story-id-config.json'
);

function loadConfig() {
  try {
    const raw = fs.readFileSync(CONFIG_PATH, 'utf8');
    const parsed = JSON.parse(raw);

    if (typeof parsed.pattern !== 'string' || parsed.pattern.trim() === '') {
      throw new Error('Missing or empty "pattern" property.');
    }

    return parsed;
  } catch (error) {
    console.error(`Failed to load story ID config at ${CONFIG_PATH}: ${error.message}`);
    process.exit(2);
  }
}

function requireValue(flag, value) {
  if (value === undefined || (typeof value === 'string' && value.startsWith('-'))) {
    console.error(`Missing value for ${flag}.`);
    process.exit(1);
  }

  return value;
}

function parseArgs(argv) {
  const args = {
    branch: null,
    prTitle: null,
    commitsFile: null,
    output: null,
    allowEmpty: false,
    verbose: false,
    help: false,
    unknown: [],
  };

  let index = 2;
  while (index < argv.length) {
    const token = argv[index];

    switch (token) {
      case '--branch':
      case '-b':
        args.branch = requireValue(token, argv[index + 1]);
        index += 2;
        continue;
      case '--pr-title':
        args.prTitle = requireValue(token, argv[index + 1]);
        index += 2;
        continue;
      case '--commits-file':
      case '-c':
        args.commitsFile = requireValue(token, argv[index + 1]);
        index += 2;
        continue;
      case '--output':
      case '-o':
        args.output = requireValue(token, argv[index + 1]);
        index += 2;
        continue;
      case '--allow-empty':
        args.allowEmpty = true;
        index += 1;
        continue;
      case '--verbose':
      case '-v':
        args.verbose = true;
        index += 1;
        continue;
      case '--help':
      case '-h':
        args.help = true;
        index += 1;
        continue;
      default:
        args.unknown.push(token);
        index += 1;
    }
  }

  return args;
}

function readCommitMessages(commitsFile) {
  if (commitsFile) {
    try {
      const content = fs.readFileSync(path.resolve(commitsFile), 'utf8');
      return content
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
    } catch (error) {
      console.error(`Failed to read commit messages from ${commitsFile}: ${error.message}`);
      process.exit(1);
    }
  }

  if (!process.stdin.isTTY) {
    try {
      const content = fs.readFileSync(0, 'utf8');
      return content
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
    } catch (error) {
      console.error(`Failed to read commit messages from stdin: ${error.message}`);
      process.exit(1);
    }
  }

  return [];
}

function buildSearchRegex(pattern) {
  let source = pattern;

  if (source.startsWith('^')) {
    source = source.slice(1);
  }

  if (source.endsWith('$')) {
    source = source.slice(0, -1);
  }

  if (source === '') {
    return new RegExp(pattern, 'g');
  }

  return new RegExp(source, 'g');
}

function compileBranchMatchers(entries) {
  if (!Array.isArray(entries)) {
    return [];
  }

  return entries
    .map((entry) => {
      if (typeof entry !== 'string' || entry.length === 0) {
        return null;
      }

      const pattern = entry.startsWith('^') || entry.endsWith('$') ? entry : `^${entry}$`;
      return new RegExp(pattern);
    })
    .filter(Boolean);
}

function branchIsReserved(branch, matchers) {
  if (!branch) {
    return false;
  }

  return matchers.some((regex) => regex.test(branch));
}

function collectMatches(texts, searchRegex, validationRegex) {
  const found = new Set();

  for (const text of texts) {
    if (!text) {
      continue;
    }

    searchRegex.lastIndex = 0;
    let match;

    while ((match = searchRegex.exec(text)) !== null) {
      const candidate = match[0];
      if (validationRegex.test(candidate)) {
        found.add(candidate);
      }
    }
  }

  return Array.from(found).sort();
}

function warnUnknownArguments(entries) {
  if (!entries || entries.length === 0) {
    return;
  }

  console.error(`Warning: ignoring unknown argument(s): ${entries.join(', ')}`);
}

function gatherSources(args, commitMessages) {
  const sources = [];

  if (args.branch) {
    sources.push(args.branch);
  }

  if (args.prTitle) {
    sources.push(args.prTitle);
  }

  return sources.concat(commitMessages);
}

function buildSummary(args, config, storyIds, branchReserved, shouldRequireStoryId, commitCount) {
  return {
    branch: args.branch,
    prTitle: args.prTitle,
    storyIds,
    storyPattern: config.pattern,
    reservedBranch: branchReserved,
    allowEmpty: !shouldRequireStoryId,
    commitMessagesScanned: commitCount,
  };
}

function handleValidationResult(storyIds, shouldRequireStoryId, config) {
  if (storyIds.length === 0) {
    if (shouldRequireStoryId) {
      console.error('Story ID validation failed. No valid identifiers found in branch, PR title, or commit messages.');
      console.error(`Expected pattern: ${config.pattern}`);
      if (Array.isArray(config.examples) && config.examples.length > 0) {
        console.error(`Example identifiers: ${config.examples.join(', ')}`);
      }
      return 1;
    }

    console.log('No story IDs found, but validation is optional for this branch or run.');
    return 0;
  }

  console.log(`Story ID validation passed. Found ${storyIds.length} identifier(s).`);
  return 0;
}

function printUsage() {
  console.log(`Usage: validate-story-ids [options]

Options:
  --branch <name>         Branch name to inspect.
  --pr-title <title>      Pull request title to scan.
  --commits-file <path>   File containing commit subjects (one per line).
  --output <path>         Write JSON summary to the given file.
  --allow-empty           Skip failure when no story IDs are found.
  --verbose               Print the JSON summary to stdout.
  --help                  Show this message.

Examples:
  validate-story-ids --branch feature/UTS-101 --pr-title "UTS-101 add validator"
  git log -5 --pretty=%s | validate-story-ids --branch feature/UTS-101
`);
}

function main() {
  const config = loadConfig();
  const args = parseArgs(process.argv);

  if (args.help) {
    printUsage();
    process.exit(0);
  }

  warnUnknownArguments(args.unknown);

  const branchMatchers = compileBranchMatchers(config.reservedBranches);
  const branchReserved = branchIsReserved(args.branch, branchMatchers);
  const shouldRequireStoryId = !args.allowEmpty && !branchReserved;

  const commitMessages = readCommitMessages(args.commitsFile);
  const sources = gatherSources(args, commitMessages);
  const searchRegex = buildSearchRegex(config.pattern);
  const validationRegex = new RegExp(config.pattern);
  const storyIds = collectMatches(sources, searchRegex, validationRegex);

  const summary = buildSummary(
    args,
    config,
    storyIds,
    branchReserved,
    shouldRequireStoryId,
    commitMessages.length
  );

  const exitCode = handleValidationResult(storyIds, shouldRequireStoryId, config);

  if (args.verbose) {
    console.log(JSON.stringify(summary, null, 2));
  }

  if (args.output) {
    try {
      fs.writeFileSync(path.resolve(args.output), `${JSON.stringify(summary, null, 2)}\n`);
    } catch (error) {
      console.error(`Failed to write summary to ${args.output}: ${error.message}`);
      process.exit(1);
    }
  }

  process.exit(exitCode);
}

main();
