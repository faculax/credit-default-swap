/**
 * Test Planner - Maps stories to test plans based on services involved
 */

import {
  ServiceName,
  TestType,
  ServicesInvolvedStatus,
  StoryModel
} from '../models/story-model';
import {
  TestPlan,
  PlannedTest
} from '../models/test-plan-model';

/**
 * Rules for mapping services to test types
 */
const SERVICE_TEST_MAPPING: Record<ServiceName, TestType[]> = {
  'frontend': ['component', 'unit'],
  'backend': ['unit', 'integration', 'api'],
  'gateway': ['unit', 'api'],
  'risk-engine': ['unit', 'integration']
};

/**
 * Target paths for test generation
 */
const SERVICE_TEST_PATHS: Record<ServiceName, string> = {
  'frontend': 'frontend/src/__tests__',
  'backend': 'backend/src/test/java',
  'gateway': 'gateway/src/test/java',
  'risk-engine': 'risk-engine/src/test/java'
};

export class TestPlanner {
  /**
   * Create a test plan from a story model
   */
  plan(story: StoryModel): TestPlan {
    // Skip stories with invalid or missing services
    if (story.servicesInvolvedStatus !== ServicesInvolvedStatus.PRESENT) {
      return {
        storyId: story.storyId,
        normalizedId: story.normalizedId,
        title: story.title,
        plannedServices: [],
        plannedTests: [],
        requiresFlowTests: false,
        story
      };
    }
    
    // Determine if flow tests are required (multi-service stories)
    const requiresFlowTests = story.servicesInvolved.length > 1;
    
    // Create planned tests for each service
    const plannedTests: PlannedTest[] = [];
    
    for (const service of story.servicesInvolved) {
      const testTypes = SERVICE_TEST_MAPPING[service] || [];
      
      // Add flow test if required
      const finalTestTypes = requiresFlowTests
        ? [...testTypes, 'flow' as TestType]
        : testTypes;
      
      plannedTests.push({
        service,
        testTypes: finalTestTypes,
        targetPath: SERVICE_TEST_PATHS[service],
        acceptanceCriteria: story.acceptanceCriteria.map((_, idx) => idx),
        testScenarios: story.testScenarios.map((_, idx) => idx)
      });
    }
    
    return {
      storyId: story.storyId,
      normalizedId: story.normalizedId,
      title: story.title,
      plannedServices: story.servicesInvolved,
      plannedTests,
      requiresFlowTests,
      story
    };
  }
  
  /**
   * Create test plans for multiple stories
   */
  planMany(stories: StoryModel[]): TestPlan[] {
    return stories.map(story => this.plan(story));
  }
  
  /**
   * Get recommended test count for a plan
   */
  getRecommendedTestCount(plan: TestPlan): number {
    // Each acceptance criterion should have at least one test
    // Each test scenario should have at least one test
    const minTests = plan.story.acceptanceCriteria.length + plan.story.testScenarios.length;
    
    // Add flow tests if required (one per service combination)
    const flowTests = plan.requiresFlowTests ? 1 : 0;
    
    return minTests + flowTests;
  }
  
  /**
   * Estimate test generation complexity
   */
  estimateComplexity(plan: TestPlan): 'low' | 'medium' | 'high' {
    const serviceCount = plan.plannedServices.length;
    const criteriaCount = plan.story.acceptanceCriteria.length;
    const scenarioCount = plan.story.testScenarios.length;
    
    const totalComplexity = serviceCount * 10 + criteriaCount * 2 + scenarioCount * 3;
    
    if (totalComplexity < 20) return 'low';
    if (totalComplexity < 50) return 'medium';
    return 'high';
  }
}
