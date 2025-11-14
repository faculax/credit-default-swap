#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SCHEMA_PATH = path.join(
  __dirname,
  '..',
  'unified-testing-config',
  'label-schema.json'
);

/**
 * Loads and validates the label schema configuration.
 * @returns {Object} The validated label schema
 */
function loadLabelSchema() {
  try {
    const raw = fs.readFileSync(SCHEMA_PATH, 'utf8');
    const schema = JSON.parse(raw);

    if (!Array.isArray(schema.testTypes)) {
      throw new Error('Missing or invalid "testTypes" array');
    }
    if (!Array.isArray(schema.services)) {
      throw new Error('Missing or invalid "services" array');
    }
    if (!Array.isArray(schema.microservices)) {
      throw new Error('Missing or invalid "microservices" array');
    }

    return schema;
  } catch (error) {
    console.error(`Failed to load label schema at ${SCHEMA_PATH}: ${error.message}`);
    process.exit(2);
  }
}

/**
 * Validates labels extracted from Allure results.
 * @param {Array} allureResults - Array of Allure test result objects
 * @param {Object} schema - Label schema configuration
 * @returns {Object} Validation results with errors and warnings
 */
function validateAllureLabels(allureResults, schema) {
  const validTestTypes = new Set(schema.testTypes.map(t => t.name));
  const validServices = new Set(schema.services.map(s => s.id));
  const validMicroservices = new Set(schema.microservices.map(m => m.id));

  const errors = [];
  const warnings = [];
  const stats = {
    totalTests: 0,
    validLabels: 0,
    invalidTestTypes: 0,
    invalidServices: 0,
    invalidMicroservices: 0,
    missingStoryIds: 0
  };

  for (const result of allureResults) {
    stats.totalTests++;
    const labels = result.labels || [];
    const labelMap = new Map(labels.map(l => [l.name, l.value]));

    // Validate story ID
    const storyId = labelMap.get('story');
    if (!storyId) {
      stats.missingStoryIds++;
      warnings.push({
        test: result.name || result.fullName,
        message: 'Missing story label'
      });
    }

    // Validate testType
    const testType = labelMap.get('testType');
    if (testType && !validTestTypes.has(testType)) {
      stats.invalidTestTypes++;
      errors.push({
        test: result.name || result.fullName,
        label: 'testType',
        value: testType,
        message: `Invalid testType "${testType}". Valid values: ${Array.from(validTestTypes).join(', ')}`
      });
    }

    // Validate service
    const service = labelMap.get('service');
    if (service && !validServices.has(service)) {
      stats.invalidServices++;
      errors.push({
        test: result.name || result.fullName,
        label: 'service',
        value: service,
        message: `Invalid service "${service}". Valid values: ${Array.from(validServices).join(', ')}`
      });
    }

    // Validate microservice (optional but must be valid if present)
    const microservice = labelMap.get('microservice');
    if (microservice && !validMicroservices.has(microservice)) {
      stats.invalidMicroservices++;
      errors.push({
        test: result.name || result.fullName,
        label: 'microservice',
        value: microservice,
        message: `Invalid microservice "${microservice}". Valid values: ${Array.from(validMicroservices).join(', ')}`
      });
    }

    if (errors.length === 0 && warnings.length === 0) {
      stats.validLabels++;
    }
  }

  return { errors, warnings, stats };
}

/**
 * Recursively collects all Allure result JSON files from a directory.
 * @param {string} resultsDir - Path to the Allure results directory
 * @returns {Array} Array of parsed Allure result objects
 */
function collectAllureResults(resultsDir) {
  if (!fs.existsSync(resultsDir)) {
    return [];
  }

  const results = [];
  const files = fs.readdirSync(resultsDir);

  for (const file of files) {
    if (file.endsWith('-result.json')) {
      const filePath = path.join(resultsDir, file);
      try {
        const content = fs.readFileSync(filePath, 'utf8');
        const result = JSON.parse(content);
        results.push(result);
      } catch (error) {
        console.warn(`Warning: Failed to parse ${file}: ${error.message}`);
      }
    }
  }

  return results;
}

/**
 * Validates labels in Jest test names (frontend pattern).
 * @param {string} testOutput - Jest JSON output
 * @param {Object} schema - Label schema configuration
 * @returns {Object} Validation results
 */
function validateJestLabels(testOutput, schema) {
  const validTestTypes = new Set(schema.testTypes.map(t => t.name));
  const validServices = new Set(schema.services.map(s => s.id));
  const validMicroservices = new Set(schema.microservices.map(m => m.id));

  const errors = [];
  const warnings = [];
  const stats = {
    totalTests: 0,
    validLabels: 0,
    invalidTestTypes: 0,
    invalidServices: 0,
    invalidMicroservices: 0,
    missingStoryIds: 0
  };

  try {
    const output = JSON.parse(testOutput);
    const testResults = output.testResults || [];

    for (const suite of testResults) {
      for (const test of suite.assertionResults || []) {
        stats.totalTests++;
        const testName = test.fullName || test.title;

        // Extract labels from test name using tag pattern
        const storyMatch = testName.match(/\[story:([^\]]+)\]/);
        const testTypeMatch = testName.match(/\[testType:([^\]]+)\]/);
        const serviceMatch = testName.match(/\[service:([^\]]+)\]/);
        const microserviceMatch = testName.match(/\[microservice:([^\]]+)\]/);

        // Validate story ID
        if (!storyMatch) {
          stats.missingStoryIds++;
          warnings.push({
            test: testName,
            message: 'Missing story tag in test name'
          });
        }

        // Validate testType
        if (testTypeMatch) {
          const testType = testTypeMatch[1];
          if (!validTestTypes.has(testType)) {
            stats.invalidTestTypes++;
            errors.push({
              test: testName,
              label: 'testType',
              value: testType,
              message: `Invalid testType "${testType}". Valid values: ${Array.from(validTestTypes).join(', ')}`
            });
          }
        }

        // Validate service
        if (serviceMatch) {
          const service = serviceMatch[1];
          if (!validServices.has(service)) {
            stats.invalidServices++;
            errors.push({
              test: testName,
              label: 'service',
              value: service,
              message: `Invalid service "${service}". Valid values: ${Array.from(validServices).join(', ')}`
            });
          }
        }

        // Validate microservice
        if (microserviceMatch) {
          const microservice = microserviceMatch[1];
          if (!validMicroservices.has(microservice)) {
            stats.invalidMicroservices++;
            errors.push({
              test: testName,
              label: 'microservice',
              value: microservice,
              message: `Invalid microservice "${microservice}". Valid values: ${Array.from(validMicroservices).join(', ')}`
            });
          }
        }

        if (errors.length === 0 && warnings.length === 0) {
          stats.validLabels++;
        }
      }
    }
  } catch (error) {
    console.error(`Failed to parse Jest output: ${error.message}`);
    process.exit(2);
  }

  return { errors, warnings, stats };
}

/**
 * Main execution function
 */
function main() {
  const args = process.argv.slice(2);
  
  if (args.includes('--help') || args.includes('-h')) {
    console.log(`
Usage: node validate-labels.mjs [OPTIONS]

Validates test labels against the unified label schema.

Options:
  --allure-results <dir>    Path to Allure results directory (backend)
  --jest-output <file>      Path to Jest JSON output file (frontend)
  --fail-on-error           Exit with code 1 if validation errors found
  --fail-on-warning         Exit with code 1 if warnings found
  --verbose                 Show detailed validation results
  -h, --help                Show this help message

Examples:
  # Validate backend Allure results
  node validate-labels.mjs --allure-results backend/allure-results

  # Validate frontend Jest output
  npm test -- --json --outputFile=test-output.json
  node validate-labels.mjs --jest-output test-output.json

  # Fail build on any errors
  node validate-labels.mjs --allure-results backend/allure-results --fail-on-error
`);
    process.exit(0);
  }

  const allureResultsDir = args[args.indexOf('--allure-results') + 1];
  const jestOutputFile = args[args.indexOf('--jest-output') + 1];
  const failOnError = args.includes('--fail-on-error');
  const failOnWarning = args.includes('--fail-on-warning');
  const verbose = args.includes('--verbose');

  const schema = loadLabelSchema();
  console.log('✓ Loaded label schema from', SCHEMA_PATH);
  console.log(`  Valid testTypes: ${schema.testTypes.map(t => t.name).join(', ')}`);
  console.log(`  Valid services: ${schema.services.map(s => s.id).join(', ')}`);
  console.log(`  Valid microservices: ${schema.microservices.map(m => m.id).join(', ')}`);
  console.log();

  let allErrors = [];
  let allWarnings = [];
  let totalStats = {
    totalTests: 0,
    validLabels: 0,
    invalidTestTypes: 0,
    invalidServices: 0,
    invalidMicroservices: 0,
    missingStoryIds: 0
  };

  // Validate Allure results if provided
  if (allureResultsDir && allureResultsDir !== '--jest-output') {
    console.log('Validating Allure results from:', allureResultsDir);
    const allureResults = collectAllureResults(allureResultsDir);
    console.log(`Found ${allureResults.length} Allure test results`);
    
    if (allureResults.length > 0) {
      const { errors, warnings, stats } = validateAllureLabels(allureResults, schema);
      allErrors = allErrors.concat(errors);
      allWarnings = allWarnings.concat(warnings);
      totalStats.totalTests += stats.totalTests;
      totalStats.validLabels += stats.validLabels;
      totalStats.invalidTestTypes += stats.invalidTestTypes;
      totalStats.invalidServices += stats.invalidServices;
      totalStats.invalidMicroservices += stats.invalidMicroservices;
      totalStats.missingStoryIds += stats.missingStoryIds;
    }
    console.log();
  }

  // Validate Jest output if provided
  if (jestOutputFile && jestOutputFile !== '--fail-on-error' && jestOutputFile !== '--fail-on-warning' && jestOutputFile !== '--verbose') {
    console.log('Validating Jest output from:', jestOutputFile);
    if (!fs.existsSync(jestOutputFile)) {
      console.error(`Error: Jest output file not found: ${jestOutputFile}`);
      process.exit(2);
    }
    
    const jestOutput = fs.readFileSync(jestOutputFile, 'utf8');
    const { errors, warnings, stats } = validateJestLabels(jestOutput, schema);
    allErrors = allErrors.concat(errors);
    allWarnings = allWarnings.concat(warnings);
    totalStats.totalTests += stats.totalTests;
    totalStats.validLabels += stats.validLabels;
    totalStats.invalidTestTypes += stats.invalidTestTypes;
    totalStats.invalidServices += stats.invalidServices;
    totalStats.invalidMicroservices += stats.invalidMicroservices;
    totalStats.missingStoryIds += stats.missingStoryIds;
    console.log();
  }

  // Display results
  console.log('=== Validation Results ===');
  console.log(`Total tests: ${totalStats.totalTests}`);
  console.log(`Valid labels: ${totalStats.validLabels}`);
  console.log(`Errors: ${allErrors.length}`);
  console.log(`Warnings: ${allWarnings.length}`);
  console.log();

  if (totalStats.invalidTestTypes > 0) {
    console.log(`⚠ Invalid testType labels: ${totalStats.invalidTestTypes}`);
  }
  if (totalStats.invalidServices > 0) {
    console.log(`⚠ Invalid service labels: ${totalStats.invalidServices}`);
  }
  if (totalStats.invalidMicroservices > 0) {
    console.log(`⚠ Invalid microservice labels: ${totalStats.invalidMicroservices}`);
  }
  if (totalStats.missingStoryIds > 0) {
    console.log(`⚠ Missing story IDs: ${totalStats.missingStoryIds}`);
  }
  console.log();

  // Show detailed errors
  if (verbose && allErrors.length > 0) {
    console.log('=== Errors ===');
    for (const error of allErrors) {
      console.log(`✗ ${error.test}`);
      console.log(`  ${error.message}`);
    }
    console.log();
  }

  // Show detailed warnings
  if (verbose && allWarnings.length > 0) {
    console.log('=== Warnings ===');
    for (const warning of allWarnings) {
      console.log(`⚠ ${warning.test}`);
      console.log(`  ${warning.message}`);
    }
    console.log();
  }

  // Exit based on validation results
  if (allErrors.length > 0) {
    console.error('✗ Label validation failed with errors');
    if (failOnError) {
      process.exit(1);
    }
  } else if (allWarnings.length > 0) {
    console.warn('⚠ Label validation completed with warnings');
    if (failOnWarning) {
      process.exit(1);
    }
  } else {
    console.log('✓ All labels validated successfully');
  }
}

// Export functions for testing
export {
  loadLabelSchema,
  validateAllureLabels,
  validateJestLabels,
  collectAllureResults
};

// Run if executed directly
if (import.meta.url === `file://${process.argv[1]}`) {
  main();
}
