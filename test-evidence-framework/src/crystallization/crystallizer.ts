/**
 * Crystallizer
 * 
 * Manages the crystallization workflow for generated tests:
 * generated → under-review → approved → crystallized (locked)
 * 
 * @module crystallizer
 */

import * as fs from 'node:fs/promises';
import * as crypto from 'node:crypto';
import type {
  CrystallizedTest,
  CrystallizationStatus,
  CrystallizationRegistry,
  CrystallizationConfig,
  CrystallizationOperation,
  Reviewer,
  ReviewDecision,
  CrystallizationStats,
} from '../models/crystallization-model.js';
import type { ValidationResult } from '../models/validation-model.js';
import { CodeValidator } from '../validators/code-validator.js';

/**
 * Crystallizer Class
 * 
 * Manages test crystallization lifecycle and registry
 */
export class Crystallizer {
  private readonly config: CrystallizationConfig;
  private readonly validator: CodeValidator;
  private readonly registry: CrystallizationRegistry;
  private readonly registryPath: string;

  constructor(config?: Partial<CrystallizationConfig>, registryPath?: string) {
    this.config = {
      requireValidation: true,
      minQualityScore: 70,
      requireManualReview: true,
      minReviewers: 1,
      lockAfterCrystallization: true,
      enableVersioning: true,
      maxVersions: 10,
      ...config,
    };
    
    this.validator = new CodeValidator();
    this.registryPath = registryPath || '.crystallization-registry.json';
    this.registry = this.createEmptyRegistry();
  }

  /**
   * Initialize crystallizer (load registry)
   */
  public async initialize(): Promise<void> {
    try {
      const content = await fs.readFile(this.registryPath, 'utf-8');
      const loaded = JSON.parse(content) as CrystallizationRegistry;
      Object.assign(this.registry, {
        ...loaded,
        tests: new Map(Object.entries(loaded.tests)),
        byStatus: new Map(Object.entries(loaded.byStatus)),
        byStory: new Map(Object.entries(loaded.byStory)),
        byFramework: new Map(Object.entries(loaded.byFramework)),
        byType: new Map(Object.entries(loaded.byType)),
      });
    } catch {
      // Registry doesn't exist yet, use empty
    }
  }

  /**
   * Save registry to disk
   */
  public async save(): Promise<void> {
    const serialized = {
      ...this.registry,
      tests: Object.fromEntries(this.registry.tests),
      byStatus: Object.fromEntries(this.registry.byStatus),
      byStory: Object.fromEntries(this.registry.byStory),
      byFramework: Object.fromEntries(this.registry.byFramework),
      byType: Object.fromEntries(this.registry.byType),
    };
    
    await fs.writeFile(this.registryPath, JSON.stringify(serialized, null, 2), 'utf-8');
  }

  /**
   * Register a generated test
   */
  public async registerTest(filePath: string, storyId: string): Promise<CrystallizedTest> {
    const content = await fs.readFile(filePath, 'utf-8');
    const contentHash = this.hashContent(content);
    
    // Validate if required
    let validationResult: ValidationResult | undefined;
    if (this.config.requireValidation) {
      validationResult = await this.validator.validate(filePath);
    }

    const test: CrystallizedTest = {
      id: crypto.randomUUID(),
      filePath,
      framework: validationResult?.framework || 'jest',
      testType: validationResult?.testType || 'unit',
      storyId,
      status: 'generated',
      review: {
        id: crypto.randomUUID(),
        filePath,
        status: 'generated',
        reviewers: [],
        createdAt: new Date(),
        lastModifiedAt: new Date(),
        validationResult,
        qualityScore: validationResult?.metrics ? this.calculateQualityScore(validationResult.metrics) : 0,
      },
      contentHash,
      locked: false,
      version: 1,
    };

    this.registry.tests.set(filePath, test);
    this.updateIndices(test);
    this.updateStats();
    await this.save();

    return test;
  }

  /**
   * Review a test
   */
  public async review(
    filePath: string,
    reviewer: string,
    decision: ReviewDecision,
    comments?: string
  ): Promise<CrystallizationOperation> {
    const test = this.registry.tests.get(filePath);
    if (!test) {
      throw new Error(`Test not found: ${filePath}`);
    }

    const reviewerInfo: Reviewer = {
      name: reviewer,
      timestamp: new Date(),
      decision,
      comments,
    };

    test.review.reviewers.push(reviewerInfo);
    test.review.lastModifiedAt = new Date();

    const previousStatus = test.status;
    let newStatus: CrystallizationStatus;

    if (decision === 'approve') {
      newStatus = test.review.reviewers.length >= this.config.minReviewers ? 'approved' : 'under-review';
    } else if (decision === 'request-changes') {
      newStatus = 'needs-changes';
    } else {
      newStatus = 'deprecated';
    }

    test.status = newStatus;
    test.review.status = newStatus;
    
    this.updateIndices(test);
    this.updateStats();
    await this.save();

    return {
      id: crypto.randomUUID(),
      type: 'review',
      filePath,
      operator: reviewer,
      timestamp: new Date(),
      success: true,
      message: `Test reviewed: ${decision}`,
      previousStatus,
      newStatus,
    };
  }

  /**
   * Crystallize an approved test
   */
  public async crystallize(filePath: string, operator: string): Promise<CrystallizationOperation> {
    const test = this.registry.tests.get(filePath);
    if (!test) {
      throw new Error(`Test not found: ${filePath}`);
    }

    if (test.status !== 'approved') {
      throw new Error(`Test must be approved before crystallization. Current status: ${test.status}`);
    }

    if (this.config.requireValidation && !test.review.validationResult) {
      throw new Error('Test must be validated before crystallization');
    }

    if (test.review.qualityScore < this.config.minQualityScore) {
      throw new Error(`Quality score ${test.review.qualityScore} below threshold ${this.config.minQualityScore}`);
    }

    const previousStatus = test.status;
    test.status = 'crystallized';
    test.review.status = 'crystallized';
    test.crystallizedAt = new Date();
    
    if (this.config.lockAfterCrystallization) {
      test.locked = true;
      test.lockReason = 'Automatically locked after crystallization';
    }

    this.updateIndices(test);
    this.updateStats();
    await this.save();

    return {
      id: crypto.randomUUID(),
      type: 'crystallize',
      filePath,
      operator,
      timestamp: new Date(),
      success: true,
      message: 'Test crystallized successfully',
      previousStatus,
      newStatus: 'crystallized',
    };
  }

  /**
   * Get test status
   */
  public getTest(filePath: string): CrystallizedTest | undefined {
    return this.registry.tests.get(filePath);
  }

  /**
   * Get all tests with specific status
   */
  public getTestsByStatus(status: CrystallizationStatus): CrystallizedTest[] {
    const paths = this.registry.byStatus.get(status) || [];
    return paths.map(p => this.registry.tests.get(p)).filter((t): t is CrystallizedTest => t !== undefined);
  }

  /**
   * Get registry statistics
   */
  public getStats(): CrystallizationStats {
    return this.registry.stats;
  }

  /**
   * Get full registry
   */
  public getRegistry(): CrystallizationRegistry {
    return this.registry;
  }

  /**
   * Calculate quality score from validation metrics
   */
  private calculateQualityScore(metrics: ValidationResult['metrics']): number {
    let score = 100;

    if (metrics.assertionCount === 0) score -= 30;
    else if (metrics.assertionsPerTest < 1) score -= 15;

    if (!metrics.hasSetup) score -= 5;
    if (!metrics.hasTeardown) score -= 5;

    score -= Math.min(metrics.incompleteCommentCount * 5, 20);

    if (metrics.testCaseCount === 0) score -= 40;

    return Math.max(0, score);
  }

  /**
   * Update indices after test change
   */
  private updateIndices(test: CrystallizedTest): void {
    // Update by status
    for (const [, paths] of this.registry.byStatus.entries()) {
      const index = paths.indexOf(test.filePath);
      if (index > -1) paths.splice(index, 1);
    }
    const statusPaths = this.registry.byStatus.get(test.status) || [];
    statusPaths.push(test.filePath);
    this.registry.byStatus.set(test.status, statusPaths);

    // Update by story
    const storyPaths = this.registry.byStory.get(test.storyId) || [];
    if (!storyPaths.includes(test.filePath)) {
      storyPaths.push(test.filePath);
      this.registry.byStory.set(test.storyId, storyPaths);
    }

    // Update by framework
    const frameworkPaths = this.registry.byFramework.get(test.framework) || [];
    if (!frameworkPaths.includes(test.filePath)) {
      frameworkPaths.push(test.filePath);
      this.registry.byFramework.set(test.framework, frameworkPaths);
    }

    // Update by type
    const typePaths = this.registry.byType.get(test.testType) || [];
    if (!typePaths.includes(test.filePath)) {
      typePaths.push(test.filePath);
      this.registry.byType.set(test.testType, typePaths);
    }
  }

  /**
   * Update statistics
   */
  private updateStats(): void {
    const tests = Array.from(this.registry.tests.values());
    
    this.registry.stats = {
      total: tests.length,
      byStatus: {
        generated: this.getTestsByStatus('generated').length,
        'under-review': this.getTestsByStatus('under-review').length,
        'needs-changes': this.getTestsByStatus('needs-changes').length,
        approved: this.getTestsByStatus('approved').length,
        crystallized: this.getTestsByStatus('crystallized').length,
        deprecated: this.getTestsByStatus('deprecated').length,
      },
      averageQualityScore: tests.reduce((sum, t) => sum + t.review.qualityScore, 0) / (tests.length || 1),
      crystallizedToday: tests.filter(t => 
        t.crystallizedAt && 
        new Date(t.crystallizedAt).toDateString() === new Date().toDateString()
      ).length,
      underReview: this.getTestsByStatus('under-review').length,
      needingChanges: this.getTestsByStatus('needs-changes').length,
      locked: tests.filter(t => t.locked).length,
      crystallizationRate: 0, // Would calculate from historical data
    };
  }

  /**
   * Create empty registry
   */
  private createEmptyRegistry(): CrystallizationRegistry {
    return {
      id: crypto.randomUUID(),
      version: '1.0.0',
      lastUpdated: new Date(),
      tests: new Map(),
      byStatus: new Map(),
      byStory: new Map(),
      byFramework: new Map(),
      byType: new Map(),
      stats: {
        total: 0,
        byStatus: {
          generated: 0,
          'under-review': 0,
          'needs-changes': 0,
          approved: 0,
          crystallized: 0,
          deprecated: 0,
        },
        averageQualityScore: 0,
        crystallizedToday: 0,
        underReview: 0,
        needingChanges: 0,
        locked: 0,
        crystallizationRate: 0,
      },
    };
  }

  /**
   * Hash content for change detection
   */
  private hashContent(content: string): string {
    return crypto.createHash('sha256').update(content).digest('hex');
  }
}
