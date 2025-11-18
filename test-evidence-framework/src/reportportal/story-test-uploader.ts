/**
 * Story Test Uploader for ReportPortal
 * 
 * Uploads hierarchical story-based test results to ReportPortal
 * with proper Epic → Story → Acceptance Criteria → Test nesting.
 */

import type { ReportPortalConfig, ReportPortalUploadResult } from '../models/reportportal-model.js';
import { ReportPortalClient } from './reportportal-client.js';
import { StoryTestConverter, type StoryTestResult, type TestCase } from './story-test-converter.js';

/**
 * Upload story-based test results to ReportPortal
 */
export class StoryTestUploader {
  private readonly client: ReportPortalClient;
  private readonly converter: StoryTestConverter;
  private readonly config: ReportPortalConfig;

  constructor(config: ReportPortalConfig) {
    this.config = config;
    this.client = new ReportPortalClient(config);
    this.converter = new StoryTestConverter();
  }

  /**
   * Upload story test results with hierarchical structure
   */
  async upload(result: StoryTestResult): Promise<ReportPortalUploadResult> {
    const startTime = Date.now();

    try {
      // Check connection and ensure project exists
      console.log('🔌 Checking connection to ReportPortal...');
      const connected = await this.client.checkConnection();
      if (!connected) {
        throw new Error('Failed to connect to ReportPortal');
      }

      console.log('📋 Ensuring project exists...');
      await this.client.ensureProject();

      // Start launch
      console.log(`🚀 Starting launch: ${result.title}`);
      const launchRequest = this.converter.createLaunchRequest(result);
      const launchResponse = await this.client.startLaunch(launchRequest);
      const launchId = launchResponse.id;

      // Create epic suite
      console.log(`📂 Creating epic suite: ${result.epic.name}`);
      const epicSuiteRequest = this.converter.createEpicSuite(result, launchId);
      const epicSuiteResponse = await this.client.startTestItem(epicSuiteRequest);
      const epicSuiteId = epicSuiteResponse.id;

      let totalTestsUploaded = 0;
      let totalLogsUploaded = 0;

      // Process each story
      for (const story of result.stories) {
        console.log(`\n  📖 Processing story: ${story.storyId}`);
        const storyStartTime = Date.now();

        // Create story suite
        const storySuiteRequest = this.converter.createStorySuite(
          story,
          launchId,
          epicSuiteId,
          storyStartTime
        );
        const storySuiteResponse = await this.client.startTestItem(storySuiteRequest);
        const storySuiteId = storySuiteResponse.id;

        // Process each acceptance criterion
        for (const ac of story.acceptanceCriteria) {
          console.log(`    ✓ ${ac.id}: ${ac.tests.length} tests`);
          const acStartTime = Date.now();

          // Create acceptance criterion suite
          const acSuiteRequest = this.converter.createAcceptanceCriterionSuite(
            ac,
            launchId,
            storySuiteId,
            acStartTime
          );
          const acSuiteResponse = await this.client.startTestItem(acSuiteRequest);
          const acSuiteId = acSuiteResponse.id;

          // Process each test
          for (const test of ac.tests) {
            const testStartTime = Date.now();

            // Create test item
            const testItemRequest = this.converter.createTestItem(
              test,
              launchId,
              acSuiteId,
              testStartTime
            );
            const testItemResponse = await this.client.startTestItem(testItemRequest);
            const testItemId = testItemResponse.id;

            // Upload logs
            const logs = this.converter.createTestLogs(test, testItemId, launchId, testStartTime);
            for (const log of logs) {
              await this.client.saveLog(log);
              totalLogsUploaded++;
            }

            // Finish test item
            await this.client.finishTestItem(testItemId, {
              endTime: testStartTime + test.duration,
              status: this.converter.mapTestStatus(test.status),
            });

            totalTestsUploaded++;
          }

          // Finish acceptance criterion suite
          const acStatus = this.converter.calculateSuiteStatus(ac.tests);
          await this.client.finishTestItem(acSuiteId, {
            endTime: Date.now(),
            status: acStatus,
          });
        }

        // Finish story suite
        const storyTests: TestCase[] = story.acceptanceCriteria.flatMap(ac => ac.tests);
        const storyStatus = this.converter.calculateSuiteStatus(storyTests);
        await this.client.finishTestItem(storySuiteId, {
          endTime: Date.now(),
          status: storyStatus,
        });
      }

      // Finish epic suite
      const allTests: TestCase[] = result.stories.flatMap(s => 
        s.acceptanceCriteria.flatMap(ac => ac.tests)
      );
      const epicStatus = this.converter.calculateSuiteStatus(allTests);
      await this.client.finishTestItem(epicSuiteId, {
        endTime: Date.now(),
        status: epicStatus,
      });

      // Finish launch
      console.log('\n🏁 Finalizing launch...');
      const finishResponse = await this.client.finishLaunch(launchId, {
        endTime: Date.now(),
        status: epicStatus,
      });

      const duration = Date.now() - startTime;

      console.log('\n✅ Upload successful!');
      console.log(`   Launch #${finishResponse.number}`);
      console.log(`   URL: ${finishResponse.link || this.client.getLaunchUrl(launchId)}`);
      console.log(`\n📊 Statistics:`);
      console.log(`   Tests: ${totalTestsUploaded}`);
      console.log(`   Stories: ${result.stories.length}`);
      console.log(`   Acceptance Criteria: ${result.summary.testsByAcceptanceCriteria}`);
      console.log(`   Logs: ${totalLogsUploaded}`);
      console.log(`   Duration: ${duration}ms`);

      return {
        success: true,
        launchId: finishResponse.id,
        launchNumber: finishResponse.number,
        launchUrl: finishResponse.link || this.client.getLaunchUrl(launchId),
        itemsUploaded: totalTestsUploaded,
        logsUploaded: totalLogsUploaded,
        attachmentsUploaded: 0,
        duration,
        stats: {
          total: result.summary.totalTests,
          passed: result.summary.passed,
          failed: result.summary.failed,
          skipped: result.summary.skipped,
          suites: result.stories.length,
          steps: totalTestsUploaded,
        },
      };
    } catch (error) {
      return {
        success: false,
        itemsUploaded: 0,
        logsUploaded: 0,
        attachmentsUploaded: 0,
        duration: Date.now() - startTime,
        error: (error as Error).message,
        stats: {
          total: 0,
          passed: 0,
          failed: 0,
          skipped: 0,
          suites: 0,
          steps: 0,
        },
      };
    }
  }
}
