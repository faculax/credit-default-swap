/**
 * Evidence Exporter
 * 
 * Exports test evidence from ReportPortal to structured JSON files
 * for use by the static dashboard generator.
 * 
 * Story 20.9: Evidence Export & Static Dashboard
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import type { ReportPortalConfig } from '../models/reportportal-model.js';
import { ReportPortalQueryClient } from './reportportal-query-client.js';

/**
 * Export options
 */
export interface ExportOptions {
  /** Output directory for JSON files */
  outputDir: string;
  
  /** Filter by story ID */
  storyId?: string;
  
  /** Filter by services */
  services?: string[];
  
  /** Start time filter (Unix timestamp) */
  startTime?: number;
  
  /** End time filter (Unix timestamp) */
  endTime?: number;
  
  /** Maximum number of launches to query */
  limit?: number;
  
  /** Enable verbose logging */
  verbose?: boolean;
}

/**
 * Export result
 */
export interface ExportResult {
  success: boolean;
  storiesExported: number;
  filesCreated: string[];
  duration: number;
  error?: string;
}

/**
 * Evidence exporter
 */
export class EvidenceExporter {
  private readonly client: ReportPortalQueryClient;
  private readonly config: ReportPortalConfig;
  private verbose: boolean = false;

  constructor(config: ReportPortalConfig) {
    this.config = config;
    this.client = new ReportPortalQueryClient(config);
  }

  /**
   * Export all story evidence to JSON files
   */
  async exportAll(options: ExportOptions): Promise<ExportResult> {
    const startTime = Date.now();
    this.verbose = options.verbose || false;
    const filesCreated: string[] = [];

    try {
      // Ensure output directory exists
      await fs.mkdir(options.outputDir, { recursive: true });

      this.log('Querying ReportPortal for story evidence...');

      // Get all story evidence
      const stories = await this.client.getAllStoryEvidence({
        storyId: options.storyId,
        services: options.services,
        startTime: options.startTime,
        endTime: options.endTime,
        limit: options.limit,
      });

      this.log(`Found ${stories.length} stories with test evidence`);

      // Export stories summary
      const summaryFile = path.join(options.outputDir, 'stories.json');
      await fs.writeFile(summaryFile, JSON.stringify(stories, null, 2), 'utf-8');
      filesCreated.push(summaryFile);
      this.log(`✅ Exported stories summary: ${summaryFile}`);

      // Export detailed evidence for each story
      for (const story of stories) {
        this.log(`Exporting detailed evidence for ${story.storyId}...`);
        
        const detailedEvidence = await this.client.getStoryEvidence(story.storyId, {
          startTime: options.startTime,
          endTime: options.endTime,
          limit: options.limit,
        });

        if (detailedEvidence) {
          const storyFile = path.join(options.outputDir, `story-${this.sanitizeFilename(story.storyId)}.json`);
          await fs.writeFile(storyFile, JSON.stringify(detailedEvidence, null, 2), 'utf-8');
          filesCreated.push(storyFile);
          this.log(`✅ Exported story details: ${storyFile}`);
        }
      }

      // Export metadata
      const metadata = {
        exportDate: new Date().toISOString(),
        exportTimestamp: Date.now(),
        reportPortalEndpoint: this.config.endpoint,
        reportPortalProject: this.config.project,
        storiesExported: stories.length,
        filters: {
          storyId: options.storyId,
          services: options.services,
          startTime: options.startTime,
          endTime: options.endTime,
        },
      };

      const metadataFile = path.join(options.outputDir, 'export-metadata.json');
      await fs.writeFile(metadataFile, JSON.stringify(metadata, null, 2), 'utf-8');
      filesCreated.push(metadataFile);
      this.log(`✅ Exported metadata: ${metadataFile}`);

      return {
        success: true,
        storiesExported: stories.length,
        filesCreated,
        duration: Date.now() - startTime,
      };
    } catch (error) {
      return {
        success: false,
        storiesExported: 0,
        filesCreated,
        duration: Date.now() - startTime,
        error: (error as Error).message,
      };
    }
  }

  /**
   * Export single story evidence
   */
  async exportStory(storyId: string, options: Omit<ExportOptions, 'storyId'>): Promise<ExportResult> {
    return this.exportAll({ ...options, storyId });
  }

  /**
   * Sanitize filename for story ID
   */
  private sanitizeFilename(storyId: string): string {
    // eslint-disable-next-line unicorn/prefer-string-replace-all
    return storyId.replace(/[^\w.-]/g, '-').toLowerCase();
  }

  /**
   * Log message if verbose
   */
  private log(message: string): void {
    if (this.verbose) {
      console.log(`[Evidence Export] ${message}`);
    }
  }
}
