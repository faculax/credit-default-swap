/**
 * TestPlanCatalog - In-memory store for test plans with query capabilities
 */

import { ServiceName } from '../models/story-model';
import { TestPlan, TestPlanStatistics } from '../models/test-plan-model';

export class TestPlanCatalog {
  private plans: Map<string, TestPlan>;
  
  constructor() {
    this.plans = new Map();
  }
  
  /**
   * Add a test plan to the catalog
   */
  add(plan: TestPlan): void {
    this.plans.set(plan.storyId, plan);
  }
  
  /**
   * Add multiple test plans
   */
  addMany(plans: TestPlan[]): void {
    for (const plan of plans) {
      this.add(plan);
    }
  }
  
  /**
   * Get all test plans
   */
  listAll(): TestPlan[] {
    return Array.from(this.plans.values()).sort((a, b) => 
      a.storyId.localeCompare(b.storyId)
    );
  }
  
  /**
   * Get test plan by story ID
   */
  getByStoryId(storyId: string): TestPlan | undefined {
    return this.plans.get(storyId);
  }
  
  /**
   * Get test plan by normalized ID
   */
  getByNormalizedId(normalizedId: string): TestPlan | undefined {
    return Array.from(this.plans.values()).find(
      plan => plan.normalizedId === normalizedId
    );
  }
  
  /**
   * List all stories that require tests for a specific service
   */
  listStoriesForService(service: ServiceName): TestPlan[] {
    return Array.from(this.plans.values())
      .filter(plan => plan.plannedServices.includes(service))
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * List all plans that require flow tests
   */
  listFlowTestPlans(): TestPlan[] {
    return Array.from(this.plans.values())
      .filter(plan => plan.requiresFlowTests)
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Get catalog statistics
   */
  getStatistics(): TestPlanStatistics {
    const plans = this.listAll();
    
    const byService: Record<ServiceName, number> = {
      'frontend': 0,
      'backend': 0,
      'gateway': 0,
      'risk-engine': 0
    };
    
    let flowTestsRequired = 0;
    let multiServicePlans = 0;
    
    for (const plan of plans) {
      // Count by service
      for (const service of plan.plannedServices) {
        byService[service]++;
      }
      
      // Count flow tests
      if (plan.requiresFlowTests) {
        flowTestsRequired++;
      }
      
      // Count multi-service
      if (plan.plannedServices.length > 1) {
        multiServicePlans++;
      }
    }
    
    return {
      totalPlans: plans.length,
      byService,
      flowTestsRequired,
      multiServicePlans
    };
  }
  
  /**
   * Clear all plans
   */
  clear(): void {
    this.plans.clear();
  }
  
  /**
   * Get count of plans
   */
  size(): number {
    return this.plans.size;
  }
}
