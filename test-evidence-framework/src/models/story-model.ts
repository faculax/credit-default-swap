/**
 * Story model types representing parsed user stories from /user-stories
 */

export type ServiceName = 'frontend' | 'backend' | 'gateway' | 'risk-engine';

export type TestType = 'unit' | 'component' | 'api' | 'integration' | 'flow';

/**
 * Status of services involved parsing
 */
export enum ServicesInvolvedStatus {
  PRESENT = 'PRESENT',
  MISSING = 'MISSING',
  INVALID = 'INVALID'
}

/**
 * Validation result for a story
 */
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

export interface ValidationError {
  field: string;
  message: string;
  filePath?: string;
}

export interface ValidationWarning {
  field: string;
  message: string;
  filePath?: string;
}

/**
 * Core story model
 */
export interface StoryModel {
  storyId: string;                    // e.g. "Story 3.2"
  normalizedId: string;                // e.g. "STORY_3_2"
  title: string;
  filePath: string;
  
  // User story components
  actor?: string;                      // "As the system"
  capability?: string;                 // "I want to..."
  benefit?: string;                    // "So that..."
  
  // Acceptance criteria (ordered list)
  acceptanceCriteria: string[];
  
  // Test scenarios (ordered list)
  testScenarios: string[];
  
  // Services involved
  servicesInvolved: ServiceName[];
  servicesInvolvedStatus: ServicesInvolvedStatus;
  
  // Optional sections
  implementationGuidance?: string[];
  deliverables?: string[];
  dependencies?: string[];
  
  // Metadata
  epicPath?: string;                   // Path to epic folder
  epicTitle?: string;
}

/**
 * Story catalog for querying parsed stories
 */
export interface StoryCatalog {
  stories: Map<string, StoryModel>;
  
  /**
   * List all stories
   */
  listAll(): StoryModel[];
  
  /**
   * Get story by ID
   */
  getById(storyId: string): StoryModel | undefined;
  
  /**
   * Get story by normalized ID
   */
  getByNormalizedId(normalizedId: string): StoryModel | undefined;
  
  /**
   * Filter stories by service
   */
  filterByService(service: ServiceName): StoryModel[];
  
  /**
   * Filter stories by multiple services (stories that involve ALL specified services)
   */
  filterByServices(services: ServiceName[]): StoryModel[];
  
  /**
   * Get stories with validation issues
   */
  getInvalidStories(): StoryModel[];
  
  /**
   * Get stories with missing services
   */
  getStoriesWithMissingServices(): StoryModel[];
}
