/**
 * StoryCatalog - In-memory store for parsed stories with query capabilities
 */

import { StoryModel, ServiceName, ServicesInvolvedStatus } from '../models/story-model';

export class StoryCatalog {
  private stories: Map<string, StoryModel>;
  
  constructor() {
    this.stories = new Map();
  }
  
  /**
   * Add a story to the catalog
   */
  add(story: StoryModel): void {
    this.stories.set(story.storyId, story);
  }
  
  /**
   * Add multiple stories
   */
  addMany(stories: StoryModel[]): void {
    for (const story of stories) {
      this.add(story);
    }
  }
  
  /**
   * Get all stories
   */
  listAll(): StoryModel[] {
    return Array.from(this.stories.values()).sort((a, b) => 
      a.storyId.localeCompare(b.storyId)
    );
  }
  
  /**
   * Get story by ID (e.g. "Story 3.2")
   */
  getById(storyId: string): StoryModel | undefined {
    return this.stories.get(storyId);
  }
  
  /**
   * Get story by normalized ID (e.g. "STORY_3_2")
   */
  getByNormalizedId(normalizedId: string): StoryModel | undefined {
    return Array.from(this.stories.values()).find(
      story => story.normalizedId === normalizedId
    );
  }
  
  /**
   * Filter stories that involve a specific service
   */
  filterByService(service: ServiceName): StoryModel[] {
    return Array.from(this.stories.values())
      .filter(story => story.servicesInvolved.includes(service))
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Filter stories that involve all specified services
   */
  filterByServices(services: ServiceName[]): StoryModel[] {
    return Array.from(this.stories.values())
      .filter(story => 
        services.every(service => story.servicesInvolved.includes(service))
      )
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Get stories with invalid services
   */
  getInvalidStories(): StoryModel[] {
    return Array.from(this.stories.values())
      .filter(story => story.servicesInvolvedStatus === ServicesInvolvedStatus.INVALID)
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Get stories with missing services section
   */
  getStoriesWithMissingServices(): StoryModel[] {
    return Array.from(this.stories.values())
      .filter(story => story.servicesInvolvedStatus === ServicesInvolvedStatus.MISSING)
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Get stories by epic
   */
  filterByEpic(epicPath: string): StoryModel[] {
    return Array.from(this.stories.values())
      .filter(story => story.epicPath === epicPath)
      .sort((a, b) => a.storyId.localeCompare(b.storyId));
  }
  
  /**
   * Get catalog statistics
   */
  getStatistics() {
    const stories = this.listAll();
    const byService: Record<ServiceName, number> = {
      'frontend': 0,
      'backend': 0,
      'gateway': 0,
      'risk-engine': 0
    };
    
    const byEpic: Map<string, number> = new Map();
    let multiService = 0;
    let withValidServices = 0;
    let withMissingServices = 0;
    let withInvalidServices = 0;
    
    for (const story of stories) {
      // Count by service
      for (const service of story.servicesInvolved) {
        byService[service]++;
      }
      
      // Count multi-service
      if (story.servicesInvolved.length > 1) {
        multiService++;
      }
      
      // Count by status
      if (story.servicesInvolvedStatus === ServicesInvolvedStatus.PRESENT) {
        withValidServices++;
      } else if (story.servicesInvolvedStatus === ServicesInvolvedStatus.MISSING) {
        withMissingServices++;
      } else if (story.servicesInvolvedStatus === ServicesInvolvedStatus.INVALID) {
        withInvalidServices++;
      }
      
      // Count by epic
      if (story.epicPath) {
        const count = byEpic.get(story.epicPath) || 0;
        byEpic.set(story.epicPath, count + 1);
      }
    }
    
    return {
      totalStories: stories.length,
      byService,
      byEpic: Object.fromEntries(byEpic),
      multiService,
      withValidServices,
      withMissingServices,
      withInvalidServices
    };
  }
  
  /**
   * Clear all stories
   */
  clear(): void {
    this.stories.clear();
  }
  
  /**
   * Get count of stories
   */
  size(): number {
    return this.stories.size;
  }
}
