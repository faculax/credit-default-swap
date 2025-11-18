/**
 * Test the CodeValidator and Crystallizer
 */

import { CodeValidator } from './src/validators/code-validator.js';
import { Crystallizer } from './src/crystallization/crystallizer.js';
import * as path from 'node:path';

async function testValidation() {
  console.log('🧪 Testing Code Validator and Crystallizer\n');
  console.log('='.repeat(60));
  
  // Create validator
  const validator = new CodeValidator({
    checkSyntax: true,
    checkCompilation: true,
    checkQuality: true,
    qualityThreshold: 70,
  });

  console.log('\n1️⃣  Testing Valid Test File');
  console.log('-'.repeat(60));
  
  const validTestPath = path.resolve('./test-samples/valid-test.spec.ts');
  try {
    const validResult = await validator.validate(validTestPath);
    console.log(`✅ File: ${path.basename(validResult.filePath)}`);
    console.log(`   Framework: ${validResult.framework}`);
    console.log(`   Test Type: ${validResult.testType}`);
    console.log(`   Valid: ${validResult.valid ? '✅ YES' : '❌ NO'}`);
    console.log(`   Issues: ${validResult.issues.length}`);
    console.log(`   Metrics:`);
    console.log(`     - Lines of Code: ${validResult.metrics.linesOfCode}`);
    console.log(`     - Test Cases: ${validResult.metrics.testCaseCount}`);
    console.log(`     - Assertions: ${validResult.metrics.assertionCount}`);
    console.log(`     - Assertions/Test: ${validResult.metrics.assertionsPerTest.toFixed(2)}`);
    console.log(`     - Has Setup: ${validResult.metrics.hasSetup ? '✅' : '❌'}`);
    console.log(`     - Has Teardown: ${validResult.metrics.hasTeardown ? '✅' : '❌'}`);
    
    if (validResult.issues.length > 0) {
      console.log(`\n   Issues Found:`);
      for (const issue of validResult.issues) {
        console.log(`     - [${issue.severity.toUpperCase()}] ${issue.message}`);
      }
    }
  } catch (error) {
    console.error(`❌ Error validating valid test:`, error.message);
  }

  console.log('\n2️⃣  Testing Invalid Test File');
  console.log('-'.repeat(60));
  
  const invalidTestPath = path.resolve('./test-samples/invalid-test.spec.ts');
  try {
    const invalidResult = await validator.validate(invalidTestPath);
    console.log(`📄 File: ${path.basename(invalidResult.filePath)}`);
    console.log(`   Framework: ${invalidResult.framework}`);
    console.log(`   Valid: ${invalidResult.valid ? '✅ YES' : '❌ NO'}`);
    console.log(`   Issues: ${invalidResult.issues.length}`);
    
    if (invalidResult.issues.length > 0) {
      console.log(`\n   Issues Found:`);
      for (const issue of invalidResult.issues) {
        console.log(`     - [${issue.severity.toUpperCase()}] ${issue.message}`);
        if (issue.line) {
          console.log(`       Line ${issue.line}: ${issue.snippet || ''}`);
        }
      }
    }
  } catch (error) {
    console.error(`❌ Error validating invalid test:`, error.message);
  }

  console.log('\n3️⃣  Testing Validation Report Generation');
  console.log('-'.repeat(60));
  
  try {
    const allFiles = [validTestPath, invalidTestPath];
    const report = await validator.validateAll(allFiles);
    
    console.log(`📊 Validation Report:`);
    console.log(`   Total Tests: ${report.summary.totalTests}`);
    console.log(`   ✅ Valid: ${report.summary.validTests}`);
    console.log(`   ❌ Invalid: ${report.summary.invalidTests}`);
    console.log(`   ⚠️  Total Issues: ${report.summary.totalIssues}`);
    console.log(`\n   Issues by Severity:`);
    console.log(`     Errors: ${report.summary.issuesBySeverity.error}`);
    console.log(`     Warnings: ${report.summary.issuesBySeverity.warning}`);
    console.log(`     Info: ${report.summary.issuesBySeverity.info}`);
    console.log(`     Suggestions: ${report.summary.issuesBySeverity.suggestion}`);
    console.log(`\n   Quality Assessment:`);
    console.log(`     Score: ${report.quality.score}/100`);
    console.log(`     Grade: ${report.quality.grade}`);
    console.log(`     Breakdown:`);
    console.log(`       - Syntax: ${report.quality.breakdown.syntax}`);
    console.log(`       - Structure: ${report.quality.breakdown.structure}`);
    console.log(`       - Assertions: ${report.quality.breakdown.assertions}`);
    console.log(`       - Best Practices: ${report.quality.breakdown.bestPractices}`);
  } catch (error) {
    console.error(`❌ Error generating report:`, error.message);
  }

  console.log('\n4️⃣  Testing Crystallization Workflow');
  console.log('-'.repeat(60));
  
  try {
    const crystallizer = new Crystallizer({
      requireValidation: true,
      minQualityScore: 60,
      requireManualReview: true,
      minReviewers: 1,
      lockAfterCrystallization: true,
    }, './test-samples/.crystallization-registry.json');
    
    await crystallizer.initialize();
    
    // Register test
    console.log(`\n   📝 Registering test...`);
    const registeredTest = await crystallizer.registerTest(validTestPath, 'story-03-001');
    console.log(`   ✅ Registered: ${registeredTest.id}`);
    console.log(`   Status: ${registeredTest.status}`);
    console.log(`   Quality Score: ${registeredTest.review.qualityScore}`);
    
    // Review test
    console.log(`\n   👀 Reviewing test...`);
    const reviewOp = await crystallizer.review(validTestPath, 'test-reviewer', 'approve', 'Looks good!');
    console.log(`   ✅ Reviewed: ${reviewOp.message}`);
    console.log(`   Status: ${reviewOp.previousStatus} → ${reviewOp.newStatus}`);
    
    // Crystallize test
    console.log(`\n   💎 Crystallizing test...`);
    const crystallizeOp = await crystallizer.crystallize(validTestPath, 'test-reviewer');
    console.log(`   ✅ Crystallized: ${crystallizeOp.message}`);
    console.log(`   Status: ${crystallizeOp.previousStatus} → ${crystallizeOp.newStatus}`);
    
    // Get stats
    const stats = crystallizer.getStats();
    console.log(`\n   📊 Registry Statistics:`);
    console.log(`   Total Tests: ${stats.total}`);
    console.log(`   Crystallized: ${stats.byStatus.crystallized}`);
    console.log(`   Locked: ${stats.locked}`);
    console.log(`   Average Quality: ${stats.averageQualityScore.toFixed(2)}`);
    
  } catch (error) {
    console.error(`❌ Error in crystallization:`, error.message);
  }

  console.log('\n' + '='.repeat(60));
  console.log('✅ All tests completed!\n');
}

// Run tests
testValidation().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
