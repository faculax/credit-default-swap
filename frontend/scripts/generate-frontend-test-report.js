#!/usr/bin/env node
/**
 * Generate TEST_REPORT.md with breakdown of unit, contract, and integration tests + coverage summary.
 * Assumes Jest run produced coverage/coverage-summary.json and coverage/test-results.json.
 */
const fs = require('node:fs');
const path = require('node:path');

const COVERAGE_SUMMARY = path.join(process.cwd(), 'coverage', 'coverage-summary.json');
const TEST_RESULTS = path.join(process.cwd(), 'coverage', 'test-results.json');
const REPORT_PATH = path.join(process.cwd(), 'TEST_REPORT.md');

function safeReadJSON(p) {
  try { return JSON.parse(fs.readFileSync(p, 'utf8')); } catch { return null; }
}

const coverage = safeReadJSON(COVERAGE_SUMMARY);
const results = safeReadJSON(TEST_RESULTS);

let totalTests = 0;
let unitCount = 0;
let contractCount = 0;
let integrationCount = 0;
let failedCount = 0;
let passedCount = 0;

if (results && results.testResults) {
  for (const tr of results.testResults) {
    const filePath = tr.name || '';
    const numTests = tr.assertionResults.length;
    totalTests += numTests;
    const isContract = /__tests__[/\\]contract[/\\]/.test(filePath);
    const isIntegration = /__tests__[/\\]integration[/\\]/.test(filePath);
    if (isContract) contractCount += numTests; else if (isIntegration) integrationCount += numTests; else unitCount += numTests;
    for (const a of tr.assertionResults) {
      if (a.status === 'failed') failedCount++; else if (a.status === 'passed') passedCount++; // skip pending
    }
  }
}

const linePct = coverage ? coverage.total?.lines?.pct?.toFixed(2) : 'N/A';
const branchPct = coverage ? coverage.total?.branches?.pct?.toFixed(2) : 'N/A';
const funcPct = coverage ? coverage.total?.functions?.pct?.toFixed(2) : 'N/A';
const stmtPct = coverage ? coverage.total?.statements?.pct?.toFixed(2) : 'N/A';

const timestamp = new Date().toISOString();

const md = `## Frontend Test Report\n\nGenerated: ${timestamp}\n\n### Summary\n\n| Metric | Value |\n|--------|-------|\n| Total Tests | ${totalTests} |\n| Passed | ${passedCount} |\n| Failed | ${failedCount} |\n| Unit Tests | ${unitCount} |\n| Contract Tests | ${contractCount} |\n| Integration Tests | ${integrationCount} |\n\n### Coverage\n\n| Type | Percent |\n|------|---------|\n| Lines | ${linePct}% |\n| Statements | ${stmtPct}% |\n| Branches | ${branchPct}% |\n| Functions | ${funcPct}% |\n\n### Classification Rules\n- Contract: path contains \`__tests__/contract/\`\n- Integration: path contains \`__tests__/integration/\`\n- Unit: all other jest test files\n\n### Next Improvements\n- Add negative/error path tests for services (non-200 responses)\n- Expand integration tests to include cancellation & retry flows\n- Cover edge cases (empty horizons, coupon schedule generation failure)\n`; 

fs.writeFileSync(REPORT_PATH, md, 'utf8');
console.log(`Test report written to ${REPORT_PATH}`);
