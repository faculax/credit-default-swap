/**
 * ReportPortal Uploader Service
 * 
 * Orchestrates the upload of test results to ReportPortal including
 * launch management, test item creation, log attachment, and finalization.
 * 
 * Story 20.8: ReportPortal Integration
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import type {
  ReportPortalConfig,
  ReportPortalUploadResult,
  ReportPortalUploadStats,
  ReportPortalBatchUploadOptions,
  StartLaunchRequest,
  FinishLaunchRequest,
} from '../models/reportportal-model.js';
import type {
  ValidationReport,
  ValidationResult,
} from '../models/validation-model.js';
import { ReportPortalClient } from './reportportal-client.js';
import { ReportPortalConverter } from './reportportal-converter.js';

/**
 * Progress callback function
 */
type ProgressCallback = (message: string, current: number, total: number) => void;

/**
 * ReportPortal uploader service
 */
export class ReportPortalUploader {
  private readonly client: ReportPortalClient;
  private readonly converter: ReportPortalConverter;
  private readonly config: ReportPortalConfig;
  private progressCallback?: ProgressCallback;

  constructor(config: ReportPortalConfig) {
    this.config = config;
    this.client = new ReportPortalClient(config);
    this.converter = new ReportPortalConverter();
  }

  /**
   * Set progress callback
   */
  onProgress(callback: ProgressCallback): void {
    this.progressCallback = callback;
  }

  /**
   * Upload validation report to ReportPortal
   */
  async uploadValidationReport(
    report: ValidationReport
  ): Promise<ReportPortalUploadResult> {
    const startTime = Date.now();
    let launchId: string | undefined;
    let launchUrl: string | undefined;

    try {
      this.reportProgress('Checking connection to ReportPortal...', 0, 5);
      const connected = await this.client.checkConnection();
      if (!connected) {
        throw new Error('Failed to connect to ReportPortal');
      }

      // Ensure project exists (create if needed)
      this.reportProgress('Ensuring project exists...', 1, 5);
      await this.client.ensureProject();

      // Start launch
      this.reportProgress('Starting launch...', 2, 5);
      const launchRequest: StartLaunchRequest = {
        name: this.config.launchName || 'Validation Report',
        description: this.config.launchDescription,
        startTime: typeof report.timestamp === 'number' ? report.timestamp : Date.now(),
        attributes: [
          ...(this.config.launchAttributes || []),
          { key: 'type', value: 'validation' },
        ],
        mode: this.config.mode,
      };

      const launchResponse = await this.client.startLaunch(launchRequest);
      launchId = launchResponse.id;
      launchUrl = this.client.getLaunchUrl(launchId);

      // Upload validation suite
      this.reportProgress('Creating validation suite...', 3, 5);
      const suiteItem = this.converter.convertValidationReport(report, launchId);
      const suiteResponse = await this.client.startTestItem({
        ...suiteItem,
        launchUuid: launchId,
      });
      const suiteId = suiteResponse.id;

      // Upload test results
      this.reportProgress('Uploading test results...', 4, 5);
      const stats = await this.uploadValidationResults(
        report.results,
        launchId,
        suiteId
      );

      // Finish suite
      await this.client.finishTestItem(suiteId, {
        endTime: Date.now(),
        status: stats.failed > 0 ? 'failed' : 'passed',
      });

      // Finish launch
      this.reportProgress('Finalizing launch...', 5, 5);
      const finishRequest: FinishLaunchRequest = {
        endTime: Date.now(),
        status: stats.failed > 0 ? 'failed' : 'passed',
      };

      const finishResponse = await this.client.finishLaunch(launchId, finishRequest);

      return {
        success: true,
        launchId: finishResponse.id,
        launchNumber: finishResponse.number,
        launchUrl: finishResponse.link || launchUrl,
        itemsUploaded: stats.total,
        logsUploaded: report.results.reduce((sum, r) => sum + r.issues.length, 0),
        attachmentsUploaded: 0,
        duration: Date.now() - startTime,
        stats,
      };
    } catch (error) {
      if (launchId) {
        // Try to finish launch with error status
        try {
          await this.client.finishLaunch(launchId, {
            endTime: Date.now(),
            status: 'failed',
            description: `Upload failed: ${(error as Error).message}`,
          });
        } catch {
          // Ignore error during cleanup
        }
      }

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

  /**
   * Upload validation results
   */
  // eslint-disable-next-line sonarjs/cognitive-complexity
  private async uploadValidationResults(
    results: ValidationResult[],
    launchId: string,
    parentId: string
  ): Promise<ReportPortalUploadStats> {
    const stats: ReportPortalUploadStats = {
      total: results.length,
      passed: 0,
      failed: 0,
      skipped: 0,
      suites: 0,
      steps: 0,
    };

    for (const result of results) {
      const testItem = this.converter.convertValidationResult(result, launchId, parentId);
      const testResponse = await this.client.startTestItem({
        ...testItem,
        launchUuid: launchId,
        parentUuid: parentId,
      });

      const testId = testResponse.id;

      // Upload issues as logs
      if (result.issues.length > 0) {
        const logs = this.converter.convertValidationIssues(
          result.issues,
          testId,
          launchId
        );

        for (const log of logs) {
          if (log.itemUuid) {
            await this.client.saveLog({
              itemUuid: log.itemUuid,
              time: log.time,
              message: log.message,
              level: log.level,
              launchUuid: launchId,
            });
          }
        }
      }

      // Finish test item
      await this.client.finishTestItem(testId, {
        endTime: Date.now(),
        status: testItem.status || 'passed',
      });

      // Update stats
      if (result.valid) {
        stats.passed++;
      } else {
        const hasErrors = result.issues.some((issue) => issue.severity === 'error');
        if (hasErrors) {
          stats.failed++;
        } else {
          stats.skipped++;
        }
      }
    }

    return stats;
  }

  /**
   * Upload batch of test results from directory
   */
  async uploadBatch(
    options: ReportPortalBatchUploadOptions
  ): Promise<ReportPortalUploadResult> {
    const startTime = Date.now();

    try {
      // Check connection
      this.reportProgress('Checking connection to ReportPortal...', 0, 6);
      const connected = await this.client.checkConnection();
      if (!connected) {
        throw new Error('Failed to connect to ReportPortal');
      }

      // Ensure project exists
      this.reportProgress('Ensuring project exists...', 1, 6);
      await this.client.ensureProject();

      // Find result files
      this.reportProgress('Scanning for result files...', 2, 6);
      const resultFiles = await this.findResultFiles(
        options.sourceDir,
        options.patterns || ['**/*.json']
      );

      if (resultFiles.length === 0) {
        throw new Error(`No result files found in ${options.sourceDir}`);
      }

      // Start launch
      this.reportProgress(`Starting launch for ${resultFiles.length} files...`, 3, 6);
      const launchRequest: StartLaunchRequest = {
        name: options.launchName,
        description: options.launchDescription,
        startTime: Date.now(),
        attributes: options.launchAttributes || [],
        mode: this.config.mode,
      };

      const launchResponse = await this.client.startLaunch(launchRequest);
      const launchId = launchResponse.id;
      const launchUrl = this.client.getLaunchUrl(launchId);

      // Process files
      this.reportProgress('Uploading test results...', 4, 6);
      let itemsUploaded = 0;
      let logsUploaded = 0;

      for (let i = 0; i < resultFiles.length; i++) {
        const file = resultFiles[i];
        this.reportProgress(
          `Processing ${path.basename(file)}...`,
          4 + (i / resultFiles.length),
          6
        );

        const result = await this.uploadResultFile(file, launchId);
        itemsUploaded += result.itemsUploaded;
        logsUploaded += result.logsUploaded;
      }

      // Finish launch
      this.reportProgress('Finalizing launch...', 5, 6);
      const finishRequest: FinishLaunchRequest = {
        endTime: Date.now(),
      };

      const finishResponse = await this.client.finishLaunch(launchId, finishRequest);

      this.reportProgress('Upload complete!', 6, 6);

      return {
        success: true,
        launchId: finishResponse.id,
        launchNumber: finishResponse.number,
        launchUrl: finishResponse.link || launchUrl,
        itemsUploaded,
        logsUploaded,
        attachmentsUploaded: 0,
        duration: Date.now() - startTime,
        stats: {
          total: itemsUploaded,
          passed: 0,
          failed: 0,
          skipped: 0,
          suites: resultFiles.length,
          steps: 0,
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

  /**
   * Upload single result file
   */
  private async uploadResultFile(
    filePath: string,
    launchId: string
  ): Promise<{ itemsUploaded: number; logsUploaded: number }> {
    try {
      const content = await fs.readFile(filePath, 'utf-8');
      const data = JSON.parse(content);

      // Try to detect file type and upload accordingly
      if (this.isValidationReport(data)) {
        const suiteItem = this.converter.convertValidationReport(data, launchId);
        const suiteResponse = await this.client.startTestItem({
          ...suiteItem,
          launchUuid: launchId,
        });

        const stats = await this.uploadValidationResults(
          data.results,
          launchId,
          suiteResponse.id
        );

        await this.client.finishTestItem(suiteResponse.id, {
          endTime: Date.now(),
          status: stats.failed > 0 ? 'failed' : 'passed',
        });

        return {
          itemsUploaded: stats.total + 1,
          logsUploaded: data.results.reduce((sum: number, r: ValidationResult) => sum + r.issues.length, 0),
        };
      }

      // Unknown format - skip
      return { itemsUploaded: 0, logsUploaded: 0 };
    } catch (error) {
      if (this.config.debug) {
        console.warn(`Failed to upload file ${filePath}:`, error);
      }
      return { itemsUploaded: 0, logsUploaded: 0 };
    }
  }

  /**
   * Find result files matching patterns
   */
  private async findResultFiles(
    dir: string,
    patterns: string[]
  ): Promise<string[]> {
    const files: string[] = [];

    const scanDir = async (currentDir: string): Promise<void> => {
      const entries = await fs.readdir(currentDir, { withFileTypes: true });

      for (const entry of entries) {
        const fullPath = path.join(currentDir, entry.name);

        if (entry.isDirectory()) {
          await scanDir(fullPath);
        } else if (entry.isFile()) {
          // Simple pattern matching (only supports *.ext and **/*.ext)
          for (const pattern of patterns) {
            if (this.matchesPattern(entry.name, pattern)) {
              files.push(fullPath);
              break;
            }
          }
        }
      }
    };

    await scanDir(dir);
    return files;
  }

  /**
   * Check if filename matches pattern
   */
  private matchesPattern(filename: string, pattern: string): boolean {
    // Convert glob pattern to regex
    // eslint-disable-next-line prefer-named-capture-group, unicorn/no-unsafe-regex, unicorn/prefer-string-replace-all
    const regexPattern = pattern
      .replace(/\./g, String.raw`\.`)
      .replace(/\*\*/g, '.*')
      .replace(/\*/g, String.raw`[^/\\]*`);

    const regex = new RegExp(`^${regexPattern}$`, 'i');
    return regex.test(filename);
  }

  /**
   * Check if data is a validation report
   */
  private isValidationReport(data: unknown): data is ValidationReport {
    return (
      typeof data === 'object' &&
      data !== null &&
      'id' in data &&
      'summary' in data &&
      'results' in data
    );
  }

  /**
   * Report progress
   */
  private reportProgress(message: string, current: number, total: number): void {
    if (this.progressCallback) {
      this.progressCallback(message, current, total);
    }
  }
}
