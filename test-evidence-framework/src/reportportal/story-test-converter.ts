/**
 * Story Test Result Converter for ReportPortal
 * 
 * Converts hierarchical test results (Epic → Story → Acceptance Criteria → Tests)
 * into ReportPortal's nested suite structure.
 */

import type {
  StartTestItemRequest,
  StartLaunchRequest,
  SaveLogRequest,
} from '../models/reportportal-model.js';

/**
 * Story-based test result structure
 */
export interface StoryTestResult {
  id: string;
  title: string;
  timestamp: number;
  epic: {
    id: string;
    name: string;
    description: string;
  };
  stories: StoryTest[];
  summary: TestSummary;
}

export interface StoryTest {
  storyId: string;
  title: string;
  epic: string;
  services: string[];
  acceptanceCriteria: AcceptanceCriterion[];
}

export interface AcceptanceCriterion {
  id: string;
  description: string;
  tests: TestCase[];
}

export type TestStatus = 'passed' | 'failed' | 'skipped';

export interface TestCase {
  name: string;
  status: TestStatus;
  duration: number;
  assertions: number;
  error?: string;
  skipReason?: string;
}

export interface TestSummary {
  totalTests: number;
  passed: number;
  failed: number;
  skipped: number;
  totalDuration: number;
  testsByStory: Record<string, StoryStats>;
  testsByAcceptanceCriteria: number;
}

export interface StoryStats {
  total: number;
  passed: number;
  failed: number;
  skipped: number;
}

/**
 * Convert story test results to ReportPortal launch
 */
export class StoryTestConverter {
  /**
   * Create launch request from test results
   */
  createLaunchRequest(result: StoryTestResult): StartLaunchRequest {
    return {
      name: result.title,
      description: `${result.epic.name}: ${result.epic.description}`,
      startTime: result.timestamp,
      attributes: [
        { key: 'epic', value: result.epic.id },
        { key: 'type', value: 'story-test' },
        { key: 'totalTests', value: result.summary.totalTests.toString() },
      ],
      mode: 'DEFAULT',
    };
  }

  /**
   * Create epic suite (top-level suite for the epic)
   */
  createEpicSuite(result: StoryTestResult, launchId: string): StartTestItemRequest {
    return {
      name: result.epic.name,
      description: result.epic.description,
      startTime: result.timestamp,
      type: 'suite',
      launchUuid: launchId,
      attributes: [
        { key: 'epicId', value: result.epic.id },
        { key: 'storyCount', value: result.stories.length.toString() },
      ],
    };
  }

  /**
   * Create story suite (nested under epic)
   */
  createStorySuite(story: StoryTest, launchId: string, parentId: string, startTime: number): StartTestItemRequest {
    return {
      name: `${story.storyId}: ${story.title}`,
      description: `**Epic:** ${story.epic}\n\n**Services Involved:** ${story.services.join(', ')}\n\n**Acceptance Criteria:** ${story.acceptanceCriteria.length}`,
      startTime,
      type: 'suite',
      launchUuid: launchId,
      parentUuid: parentId,
      attributes: [
        { key: 'storyId', value: story.storyId },
        { key: 'epic', value: story.epic },
        ...story.services.map(service => ({ key: 'service', value: service })),
        { key: 'criteria', value: story.acceptanceCriteria.length.toString() },
        { key: 'type', value: 'story' },
      ],
    };
  }

  /**
   * Create acceptance criterion suite (nested under story)
   */
  createAcceptanceCriterionSuite(
    ac: AcceptanceCriterion,
    launchId: string,
    parentId: string,
    startTime: number
  ): StartTestItemRequest {
    return {
      name: `${ac.id}: ${ac.description}`,
      description: `**Tests:** ${ac.tests.length}`,
      startTime,
      type: 'suite',
      launchUuid: launchId,
      parentUuid: parentId,
      attributes: [
        { key: 'criterionId', value: ac.id },
        { key: 'type', value: 'acceptance-criterion' },
        { key: 'tests', value: ac.tests.length.toString() },
      ],
    };
  }

  /**
   * Create test item (nested under acceptance criterion)
   */
  createTestItem(
    test: TestCase,
    launchId: string,
    parentId: string,
    startTime: number
  ): StartTestItemRequest {
    let description: string;
    
    if (test.error) {
      description = `❌ **Error:** ${test.error}`;
    } else if (test.skipReason) {
      description = `⏭️ **Skipped:** ${test.skipReason}`;
    } else {
      description = `✓ Test executed successfully with ${test.assertions} assertions`;
    }

    return {
      name: test.name.split('_').join(' '),
      description,
      startTime,
      type: 'step',
      launchUuid: launchId,
      parentUuid: parentId,
      attributes: [
        { key: 'assertions', value: test.assertions.toString() },
        { key: 'duration', value: `${test.duration}ms` },
        { key: 'status', value: test.status },
        { key: 'type', value: 'test' },
      ],
    };
  }

  /**
   * Create log entries for test failures
   */
  createTestLogs(
    test: TestCase,
    testId: string,
    launchId: string,
    timestamp: number
  ): SaveLogRequest[] {
    const logs: SaveLogRequest[] = [];

    if (test.error) {
      logs.push({
        itemUuid: testId,
        time: timestamp,
        message: `❌ Test Failed: ${test.error}`,
        level: 'error',
        launchUuid: launchId,
      });
    }

    if (test.skipReason) {
      logs.push({
        itemUuid: testId,
        time: timestamp,
        message: `⏭️  Test Skipped: ${test.skipReason}`,
        level: 'warn',
        launchUuid: launchId,
      });
    }

    // Add assertion count info
    logs.push({
      itemUuid: testId,
      time: timestamp,
      message: `✓ Assertions executed: ${test.assertions}`,
      level: 'info',
      launchUuid: launchId,
    });

    return logs;
  }

  /**
   * Map test status to ReportPortal status
   */
  mapTestStatus(status: TestStatus): TestStatus {
    return status;
  }

  /**
   * Calculate suite status based on test results
   */
  calculateSuiteStatus(tests: TestCase[]): TestStatus {
    const hasFailures = tests.some(t => t.status === 'failed');
    if (hasFailures) return 'failed';

    const allSkipped = tests.every(t => t.status === 'skipped');
    if (allSkipped) return 'skipped';

    return 'passed';
  }
}
