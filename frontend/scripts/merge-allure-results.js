#!/usr/bin/env node
/**
 * Allure Results Merge Script
 * 
 * Merges Jest (unit/integration) and Cypress (E2E) Allure results into a single
 * allure-results directory for unified reporting.
 * 
 * Usage: node scripts/merge-allure-results.js
 */

const fs = require('fs');
const path = require('path');

// Directories
const JEST_RESULTS = path.join(__dirname, '..', 'allure-results');
const CYPRESS_RESULTS = path.join(__dirname, '..', 'cypress', 'allure-results');
const MERGED_RESULTS = path.join(__dirname, '..', 'allure-results-merged');

console.log('🔄 Merging Allure results...\n');

// Create merged directory
if (!fs.existsSync(MERGED_RESULTS)) {
  fs.mkdirSync(MERGED_RESULTS, { recursive: true });
  console.log(`✅ Created ${MERGED_RESULTS}`);
}

let copiedFiles = 0;

// Function to copy files from source to destination
function copyResults(sourceDir, label) {
  if (!fs.existsSync(sourceDir)) {
    console.log(`⚠️  ${label} results not found at ${sourceDir}`);
    return 0;
  }

  const files = fs.readdirSync(sourceDir);
  let count = 0;

  files.forEach(file => {
    const sourcePath = path.join(sourceDir, file);
    const destPath = path.join(MERGED_RESULTS, file);

    // Only copy JSON files (Allure results)
    if (file.endsWith('.json') && fs.statSync(sourcePath).isFile()) {
      fs.copyFileSync(sourcePath, destPath);
      count++;
    }
  });

  console.log(`✅ Copied ${count} ${label} result files`);
  return count;
}

// Copy Jest results
copiedFiles += copyResults(JEST_RESULTS, 'Jest');

// Copy Cypress results
copiedFiles += copyResults(CYPRESS_RESULTS, 'Cypress');

// Create environment.json if it doesn't exist
const envFilePath = path.join(MERGED_RESULTS, 'environment.json');
if (!fs.existsSync(envFilePath)) {
  const environment = {
    'Test Framework': 'Jest + Cypress',
    'Node Version': process.version,
    'Platform': process.platform,
    'Merged': new Date().toISOString()
  };
  fs.writeFileSync(envFilePath, JSON.stringify(environment, null, 2));
  console.log('✅ Created environment.json');
}

console.log(`\n📊 Total: ${copiedFiles} result files merged`);
console.log(`📁 Output: ${MERGED_RESULTS}\n`);

if (copiedFiles === 0) {
  console.log('⚠️  No results found to merge. Run tests first:');
  console.log('   npm run test:all     # Jest tests');
  console.log('   npm run test:e2e     # Cypress tests');
  process.exit(1);
}

console.log('✅ Merge complete! Generate report with:');
console.log('   npm run allure:generate:merged\n');
