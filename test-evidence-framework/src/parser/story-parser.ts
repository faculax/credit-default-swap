/**
 * Story parser - parses markdown stories from /user-stories
 */

import * as fs from 'node:fs';
import * as path from 'node:path';
import {
  StoryModel,
  ServiceName,
  ServicesInvolvedStatus,
  ValidationResult,
  ValidationError,
  ValidationWarning
} from '../models/story-model';
import { ServiceInferenceHelper } from '../inference/service-inference';

const ALLOWED_SERVICES: ServiceName[] = ['frontend', 'backend', 'gateway', 'risk-engine'];

export class StoryParser {
  private inferenceHelper: ServiceInferenceHelper;
  private enableInference: boolean;
  
  constructor(enableInference: boolean = false) {
    this.inferenceHelper = new ServiceInferenceHelper();
    this.enableInference = enableInference;
  }
  /**
   * Parse a single story markdown file
   */
  parseStory(filePath: string): { story: StoryModel; validation: ValidationResult } {
    const content = fs.readFileSync(filePath, 'utf-8');
    const errors: ValidationError[] = [];
    const warnings: ValidationWarning[] = [];
    
    // Extract story ID from filename (e.g. "story_3_2_..." -> "Story 3.2")
    const filename = path.basename(filePath, '.md');
    const idMatch = filename.match(/story_(\d+)_(\d+)/i);
    const storyId = idMatch ? `Story ${idMatch[1]}.${idMatch[2]}` : 'Unknown';
    const normalizedId = idMatch ? `STORY_${idMatch[1]}_${idMatch[2]}` : 'UNKNOWN';
    
    // Parse markdown sections
    const lines = content.split('\n');
    
    // Extract title (first H1)
    const titleMatch = content.match(/^#\s+(.+)$/m);
    const title = titleMatch ? titleMatch[1].replace(/^Story \d+\.\d+\s*[-–]\s*/, '') : '';
    
    // Extract user story components
    const userStoryMatch = content.match(/\*\*As\s+(.+?)\*\*,?\s*\n\s*I\s+want\s+(.+?)\s*\n\s*So\s+that\s+(.+?)(?=\n\n|##)/is);
    const actor = userStoryMatch ? userStoryMatch[1].trim() : undefined;
    const capability = userStoryMatch ? userStoryMatch[2].trim() : undefined;
    const benefit = userStoryMatch ? userStoryMatch[3].trim() : undefined;
    
    // Extract Acceptance Criteria (flexible emoji matching)
    const acceptanceCriteria = this.extractBulletList(content, /##[^#\n]*Acceptance Criteria/i);
    
    // Extract Test Scenarios (flexible emoji matching)
    const testScenarios = this.extractNumberedList(content, /##[^#\n]*Test Scenarios/i);
    
    // Extract optional sections (flexible emoji matching)
    const implementationGuidance = this.extractBulletList(content, /##[^#\n]*Implementation Guidance/i);
    const deliverables = this.extractBulletList(content, /##[^#\n]*Deliverables/i);
    const dependencies = this.extractBulletList(content, /##[^#\n]*Dependencies/i);
    
    // Extract Services Involved (with inference fallback if enabled)
    const { services, status } = this.extractServicesInvolved(
      content,
      filePath,
      errors,
      warnings,
      title,
      acceptanceCriteria,
      implementationGuidance,
      deliverables
    );
    
    // Extract epic info from path
    const epicMatch = filePath.match(/epic_(\d+)_([^/\\]+)/);
    const epicPath = epicMatch ? epicMatch[0] : undefined;
    const epicTitle = epicMatch ? this.titleCase(epicMatch[2].replace(/_/g, ' ')) : undefined;
    
    // Validation
    if (acceptanceCriteria.length === 0 && testScenarios.length === 0) {
      errors.push({
        field: 'acceptanceCriteria|testScenarios',
        message: 'Story must have at least one acceptance criterion or test scenario',
        filePath
      });
    }
    
    const story: StoryModel = {
      storyId,
      normalizedId,
      title,
      filePath,
      actor,
      capability,
      benefit,
      acceptanceCriteria,
      testScenarios,
      servicesInvolved: services,
      servicesInvolvedStatus: status,
      implementationGuidance: implementationGuidance.length > 0 ? implementationGuidance : undefined,
      deliverables: deliverables.length > 0 ? deliverables : undefined,
      dependencies: dependencies.length > 0 ? dependencies : undefined,
      epicPath,
      epicTitle
    };
    
    return {
      story,
      validation: {
        valid: errors.length === 0,
        errors,
        warnings
      }
    };
  }
  
  /**
   * Parse all stories in a directory tree
   */
  parseStoriesInDirectory(rootPath: string): { story: StoryModel; validation: ValidationResult }[] {
    const results: { story: StoryModel; validation: ValidationResult }[] = [];
    
    const walk = (dir: string) => {
      const files = fs.readdirSync(dir);
      
      for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
          walk(fullPath);
        } else if (file.match(/^story_\d+_\d+.*\.md$/i) && !file.includes('TEMPLATE')) {
          try {
            const result = this.parseStory(fullPath);
            results.push(result);
          } catch (error) {
            console.error(`Error parsing ${fullPath}:`, error);
          }
        }
      }
    };
    
    walk(rootPath);
    return results;
  }
  
  /**
   * Extract bullet list from a section
   */
  private extractBulletList(content: string, sectionRegex: RegExp): string[] {
    const sectionMatch = content.match(new RegExp(sectionRegex.source + '([\\s\\S]*?)(?=\\n##|$)', 'i'));
    if (!sectionMatch) return [];
    
    const sectionContent = sectionMatch[1];
    const bullets: string[] = [];
    
    const lines = sectionContent.split('\n');
    for (const line of lines) {
      // Trim line to handle both Unix (\n) and Windows (\r\n) line endings
      const trimmedLine = line.trim();
      const bulletMatch = trimmedLine.match(/^[-*]\s+(.+)$/);
      if (bulletMatch) {
        bullets.push(bulletMatch[1].trim());
      }
    }
    
    return bullets;
  }
  
  /**
   * Extract numbered list from a section
   */
  private extractNumberedList(content: string, sectionRegex: RegExp): string[] {
    const sectionMatch = content.match(new RegExp(sectionRegex.source + '([\\s\\S]*?)(?=\\n##|$)', 'i'));
    if (!sectionMatch) return [];
    
    const sectionContent = sectionMatch[1];
    const items: string[] = [];
    
    const lines = sectionContent.split('\n');
    let currentItem = '';
    
    for (const line of lines) {
      // Trim line to handle both Unix (\n) and Windows (\r\n) line endings
      const trimmedLine = line.trim();
      const numberMatch = trimmedLine.match(/^\d+\.\s+(.+)$/);
      if (numberMatch) {
        if (currentItem) {
          items.push(currentItem.trim());
        }
        currentItem = numberMatch[1];
      } else if (currentItem && trimmedLine) {
        currentItem += ' ' + trimmedLine;
      }
    }
    
    if (currentItem) {
      items.push(currentItem.trim());
    }
    
    return items;
  }
  
  /**
   * Extract Services Involved section
   */
  private extractServicesInvolved(
    content: string,
    filePath: string,
    errors: ValidationError[],
    warnings: ValidationWarning[],
    title: string,
    acceptanceCriteria: string[],
    implementationGuidance?: string[],
    deliverables?: string[]
  ): { services: ServiceName[]; status: ServicesInvolvedStatus } {
    const sectionMatch = content.match(/##\s+🧱\s*Services Involved([\\s\\S]*?)(?=\n##|$)/i);
    
    if (!sectionMatch) {
      // Try inference if enabled
      if (this.enableInference) {
        const inferredServices = this.inferenceHelper.inferServices(
          title,
          acceptanceCriteria,
          implementationGuidance,
          deliverables
        );
        
        if (inferredServices.length > 0) {
          warnings.push({
            field: 'servicesInvolved',
            message: `Missing "## 🧱 Services Involved" section - inferred: ${inferredServices.join(', ')}`,
            filePath
          });
          return { services: inferredServices, status: ServicesInvolvedStatus.PRESENT };
        }
      }
      
      warnings.push({
        field: 'servicesInvolved',
        message: 'Missing "## 🧱 Services Involved" section',
        filePath
      });
      return { services: [], status: ServicesInvolvedStatus.MISSING };
    }
    
    const sectionContent = sectionMatch[1];
    const services: ServiceName[] = [];
    const invalidServices: string[] = [];
    
    const lines = sectionContent.split('\n');
    for (const line of lines) {
      const bulletMatch = line.match(/^[-*]\s+(.+)$/);
      if (bulletMatch) {
        const serviceName = bulletMatch[1].trim().toLowerCase();
        if (ALLOWED_SERVICES.includes(serviceName as ServiceName)) {
          services.push(serviceName as ServiceName);
        } else {
          invalidServices.push(serviceName);
        }
      }
    }
    
    if (invalidServices.length > 0) {
      errors.push({
        field: 'servicesInvolved',
        message: `Invalid service names: ${invalidServices.join(', ')}. Allowed: ${ALLOWED_SERVICES.join(', ')}`,
        filePath
      });
      return { services, status: ServicesInvolvedStatus.INVALID };
    }
    
    if (services.length === 0) {
      warnings.push({
        field: 'servicesInvolved',
        message: 'Services Involved section is empty',
        filePath
      });
      return { services: [], status: ServicesInvolvedStatus.MISSING };
    }
    
    return { services, status: ServicesInvolvedStatus.PRESENT };
  }
  
  /**
   * Convert snake_case to Title Case
   */
  private titleCase(str: string): string {
    return str
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }
}
