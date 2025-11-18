/**
 * Test plan model types
 */

import { ServiceName, StoryModel, TestType } from './story-model';

/**
 * Planned test for a specific service
 */
export interface PlannedTest {
  service: ServiceName;
  testTypes: TestType[];
  targetPath: string;               // e.g. "/backend/src/test/java/..."
  acceptanceCriteria: number[];     // Indices of criteria this service covers
  testScenarios: number[];          // Indices of scenarios this service covers
}

/**
 * Test plan derived from a story
 */
export interface TestPlan {
  storyId: string;
  normalizedId: string;
  title: string;
  
  // Planned services and their test types
  plannedServices: ServiceName[];
  plannedTests: PlannedTest[];
  
  // Indicates if cross-service flow tests are needed
  requiresFlowTests: boolean;
  
  // Reference to source story
  story: StoryModel;
}

/**
 * Test plan catalog for querying plans
 */
export interface TestPlanCatalog {
  plans: Map<string, TestPlan>;
  
  /**
   * List all plans
   */
  listAll(): TestPlan[];
  
  /**
   * Get plan by story ID
   */
  getByStoryId(storyId: string): TestPlan | undefined;
  
  /**
   * List all stories requiring tests in a given service
   */
  listStoriesForService(service: ServiceName): TestPlan[];
  
  /**
   * List all plans requiring flow tests
   */
  listFlowTestPlans(): TestPlan[];
  
  /**
   * Get statistics
   */
  getStatistics(): TestPlanStatistics;
}

export interface TestPlanStatistics {
  totalPlans: number;
  byService: Record<ServiceName, number>;
  flowTestsRequired: number;
  multiServicePlans: number;
}
