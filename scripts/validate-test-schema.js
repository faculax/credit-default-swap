#!/usr/bin/env node

/**
 * Test Type Schema Validator
 * 
 * Validates that all test files in the repository use labels defined in the shared schema.
 * Runs as part of CI pipeline to enforce consistent test labeling.
 * 
 * Usage:
 *   node scripts/validate-test-schema.js
 *   node scripts/validate-test-schema.js --fix  (auto-fix issues where possible)
 *   node scripts/validate-test-schema.js --verbose (detailed output)
 */

const fs = require('fs');
const path = require('path');

// Load schema
const schemaPath = path.join(__dirname, '..', 'schema', 'test-type-schema.json');
const schema = JSON.parse(fs.readFileSync(schemaPath, 'utf8'));

// Extract valid values from schema
const validTestTypes = schema.testTypes.map(t => t.id);
const validServices = schema.microservices.map(m => m.id);
const validSeverities = schema.validationRules.severityEnum;
const storyIdPattern = new RegExp(schema.validationRules.storyIdPattern);

// Configuration
const config = {
  verbose: process.argv.includes('--verbose'),
  fix: process.argv.includes('--fix'),
  rootDir: path.join(__dirname, '..'),
  frontendTestDirs: [
    'frontend/src/__tests__',
    'frontend/src/components/**/__tests__'
  ],
  backendTestDirs: [
    'backend/src/test/java',
    'gateway/src/test/java',
    'risk-engine/src/test/java'
  ]
};

// Validation results
const results = {
  totalFiles: 0,
  validFiles: 0,
  invalidFiles: 0,
  errors: [],
  warnings: []
};

/**
 * Extract test metadata from test name or suite name
 * Looks for patterns like [story:UTS-2.3] [testType:unit] [service:frontend]
 */
function extractMetadata(line) {
  const metadata = {};
  
  const storyMatch = line.match(/\[story:([^\]]+)\]/);
  if (storyMatch) metadata.storyId = storyMatch[1];
  
  const testTypeMatch = line.match(/\[testType:([^\]]+)\]/);
  if (testTypeMatch) metadata.testType = testTypeMatch[1];
  
  const serviceMatch = line.match(/\[service:([^\]]+)\]/);
  if (serviceMatch) metadata.service = serviceMatch[1];
  
  const severityMatch = line.match(/\[severity:([^\]]+)\]/);
  if (severityMatch) metadata.severity = severityMatch[1];
  
  return metadata;
}

/**
 * Validate frontend test file (TypeScript/JavaScript)
 */
function validateFrontendTest(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const fileName = path.relative(config.rootDir, filePath);
  
  // Check if file uses testHelpers
  const usesWithStoryId = content.includes('withStoryId');
  const usesDescribeStory = content.includes('describeStory');
  
  if (!usesWithStoryId && !usesDescribeStory) {
    results.warnings.push({
      file: fileName,
      message: 'Test file does not use withStoryId() or describeStory() helpers',
      severity: 'warning'
    });
    return false;
  }
  
  // Extract metadata from test calls
  const lines = content.split('\n');
  let hasValidMetadata = false;
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const metadata = extractMetadata(line);
    
    if (Object.keys(metadata).length > 0) {
      hasValidMetadata = true;
      
      // Validate story ID
      if (metadata.storyId && !storyIdPattern.test(metadata.storyId)) {
        results.errors.push({
          file: fileName,
          line: i + 1,
          message: `Invalid story ID format: "${metadata.storyId}". Expected pattern: ${schema.validationRules.storyIdPattern}`,
          severity: 'error'
        });
      }
      
      // Validate test type
      if (metadata.testType && !validTestTypes.includes(metadata.testType)) {
        results.errors.push({
          file: fileName,
          line: i + 1,
          message: `Invalid test type: "${metadata.testType}". Valid types: ${validTestTypes.join(', ')}`,
          severity: 'error'
        });
      }
      
      // Validate service
      if (metadata.service && !validServices.includes(metadata.service)) {
        results.errors.push({
          file: fileName,
          line: i + 1,
          message: `Invalid service: "${metadata.service}". Valid services: ${validServices.join(', ')}`,
          severity: 'error'
        });
      }
      
      // Validate severity (if present)
      if (metadata.severity && !validSeverities.includes(metadata.severity)) {
        results.errors.push({
          file: fileName,
          line: i + 1,
          message: `Invalid severity: "${metadata.severity}". Valid severities: ${validSeverities.join(', ')}`,
          severity: 'error'
        });
      }
    }
  }
  
  if (!hasValidMetadata) {
    results.warnings.push({
      file: fileName,
      message: 'No test metadata found in file. Tests may not appear in Allure reports correctly.',
      severity: 'warning'
    });
  }
  
  return hasValidMetadata;
}

/**
 * Validate backend test file (Java)
 */
function validateBackendTest(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const fileName = path.relative(config.rootDir, filePath);
  
  // Check for @DisplayName annotations with metadata
  const displayNamePattern = /@DisplayName\("([^"]+)"\)/g;
  let match;
  let hasValidMetadata = false;
  
  while ((match = displayNamePattern.exec(content)) !== null) {
    const displayName = match[1];
    const metadata = extractMetadata(displayName);
    
    if (Object.keys(metadata).length > 0) {
      hasValidMetadata = true;
      
      // Find line number
      const lineNum = content.substring(0, match.index).split('\n').length;
      
      // Validate story ID
      if (metadata.storyId && !storyIdPattern.test(metadata.storyId)) {
        results.errors.push({
          file: fileName,
          line: lineNum,
          message: `Invalid story ID format: "${metadata.storyId}". Expected pattern: ${schema.validationRules.storyIdPattern}`,
          severity: 'error'
        });
      }
      
      // Validate test type
      if (metadata.testType && !validTestTypes.includes(metadata.testType)) {
        results.errors.push({
          file: fileName,
          line: lineNum,
          message: `Invalid test type: "${metadata.testType}". Valid types: ${validTestTypes.join(', ')}`,
          severity: 'error'
        });
      }
      
      // Validate service
      if (metadata.service && !validServices.includes(metadata.service)) {
        results.errors.push({
          file: fileName,
          line: lineNum,
          message: `Invalid service: "${metadata.service}". Valid services: ${validServices.join(', ')}`,
          severity: 'error'
        });
      }
    }
  }
  
  if (!hasValidMetadata) {
    results.warnings.push({
      file: fileName,
      message: 'No test metadata found in @DisplayName annotations. Tests may not appear in Allure reports correctly.',
      severity: 'warning'
    });
  }
  
  return hasValidMetadata;
}

/**
 * Find all test files recursively
 */
function findTestFiles(dir, pattern) {
  const files = [];
  
  function walk(currentDir) {
    try {
      const entries = fs.readdirSync(currentDir, { withFileTypes: true });
      
      for (const entry of entries) {
        const fullPath = path.join(currentDir, entry.name);
        
        if (entry.isDirectory()) {
          // Skip node_modules, build, target, dist directories
          if (!['node_modules', 'build', 'target', 'dist', '.git'].includes(entry.name)) {
            walk(fullPath);
          }
        } else if (entry.isFile() && pattern.test(entry.name)) {
          files.push(fullPath);
        }
      }
    } catch (err) {
      // Directory doesn't exist, skip
    }
  }
  
  walk(dir);
  return files;
}

/**
 * Main validation function
 */
function validate() {
  console.log('🔍 Validating test files against schema...\n');
  console.log(`Schema version: ${schema.version}`);
  console.log(`Last updated: ${schema.lastUpdated}\n`);
  
  // Validate frontend tests
  console.log('📱 Validating frontend tests...');
  const frontendPattern = /\.(test|spec)\.(ts|tsx|js|jsx)$/;
  const frontendTestFiles = findTestFiles(
    path.join(config.rootDir, 'frontend'),
    frontendPattern
  );
  
  frontendTestFiles.forEach(file => {
    results.totalFiles++;
    if (validateFrontendTest(file)) {
      results.validFiles++;
    } else {
      results.invalidFiles++;
    }
  });
  
  console.log(`  Found ${frontendTestFiles.length} frontend test files\n`);
  
  // Validate backend tests
  console.log('⚙️  Validating backend tests...');
  const backendPattern = /Test\.java$/;
  const backendDirs = ['backend', 'gateway', 'risk-engine'].map(d => 
    path.join(config.rootDir, d)
  );
  
  let backendTestFiles = [];
  backendDirs.forEach(dir => {
    const files = findTestFiles(dir, backendPattern);
    backendTestFiles = backendTestFiles.concat(files);
  });
  
  backendTestFiles.forEach(file => {
    results.totalFiles++;
    if (validateBackendTest(file)) {
      results.validFiles++;
    } else {
      results.invalidFiles++;
    }
  });
  
  console.log(`  Found ${backendTestFiles.length} backend test files\n`);
  
  // Print results
  console.log('=' .repeat(80));
  console.log('📊 Validation Results\n');
  console.log(`Total files scanned: ${results.totalFiles}`);
  console.log(`Valid files: ${results.validFiles}`);
  console.log(`Files with issues: ${results.invalidFiles}\n`);
  
  if (results.errors.length > 0) {
    console.log(`❌ Errors (${results.errors.length}):`);
    results.errors.forEach(err => {
      console.log(`  ${err.file}:${err.line || '?'} - ${err.message}`);
    });
    console.log('');
  }
  
  if (results.warnings.length > 0 && config.verbose) {
    console.log(`⚠️  Warnings (${results.warnings.length}):`);
    results.warnings.forEach(warn => {
      console.log(`  ${warn.file} - ${warn.message}`);
    });
    console.log('');
  }
  
  console.log('=' .repeat(80));
  
  // Exit code
  if (results.errors.length > 0) {
    console.log('\n❌ Validation failed. Please fix errors above.\n');
    process.exit(1);
  } else if (results.warnings.length > 0) {
    console.log(`\n⚠️  Validation passed with ${results.warnings.length} warnings.\n`);
    process.exit(0);
  } else {
    console.log('\n✅ All tests validated successfully!\n');
    process.exit(0);
  }
}

// Run validation
validate();
