/**
 * Static Site Generator for Test Evidence Dashboard
 * 
 * Generates HTML pages from exported evidence JSON files:
 * - index.html: Story list with coverage badges and summary statistics
 * - story-{id}.html: Story detail page with acceptance criteria, test scenarios,
 *   per-service test results, and test execution history
 * 
 * @module evidence/static-site-generator
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import type { StoryEvidence, DetailedStoryEvidence, TestHistory } from './reportportal-query-client.js';

/**
 * Options for static site generation
 */
export interface SiteGenerationOptions {
  /**
   * Directory containing exported JSON files (stories.json, story-*.json)
   */
  inputDir: string;

  /**
   * Directory to generate HTML files
   */
  outputDir: string;

  /**
   * Project name for page titles
   */
  projectName?: string;

  /**
   * Base URL for GitHub Pages (e.g., /credit-default-swap/)
   */
  baseUrl?: string;

  /**
   * Enable verbose logging
   */
  verbose?: boolean;
}

/**
 * Result of site generation
 */
export interface SiteGenerationResult {
  /**
   * Whether generation succeeded
   */
  success: boolean;

  /**
   * Number of pages generated
   */
  pagesGenerated: number;

  /**
   * List of generated files
   */
  filesCreated: string[];

  /**
   * Time taken in milliseconds
   */
  duration: number;

  /**
   * Error message if failed
   */
  error?: string;
}

/**
 * Metadata from export-metadata.json
 */
interface ExportMetadata {
  exportDate: string;
  exportTimestamp: number;
  reportPortalEndpoint: string;
  reportPortalProject: string;
  storiesExported: number;
  filters?: {
    storyId?: string;
    services?: string[];
    startTime?: number;
    endTime?: number;
    limit?: number;
  };
}

/**
 * Static Site Generator
 */
export class StaticSiteGenerator {
  private readonly verbose: boolean;

  constructor(verbose = false) {
    this.verbose = verbose;
  }

  /**
   * Generate complete static site
   */
  async generate(options: SiteGenerationOptions): Promise<SiteGenerationResult> {
    const startTime = Date.now();
    const filesCreated: string[] = [];

    try {
      this.log('Starting static site generation...');

      // Create output directory
      await fs.mkdir(options.outputDir, { recursive: true });

      // Read stories.json
      const storiesPath = path.join(options.inputDir, 'stories.json');
      const storiesContent = await fs.readFile(storiesPath, 'utf8');
      const stories: StoryEvidence[] = JSON.parse(storiesContent);
      this.log(`Loaded ${stories.length} stories from ${storiesPath}`);

      // Read export metadata
      let metadata: ExportMetadata | undefined;
      try {
        const metadataPath = path.join(options.inputDir, 'export-metadata.json');
        const metadataContent = await fs.readFile(metadataPath, 'utf8');
        metadata = JSON.parse(metadataContent);
        this.log(`Loaded export metadata from ${metadataPath}`);
      } catch (error) {
        this.log(`No export metadata found (optional): ${error instanceof Error ? error.message : String(error)}`);
      }

      // Generate index.html
      const indexPath = path.join(options.outputDir, 'index.html');
      const indexHtml = this.generateIndexPage(stories, metadata, options);
      await fs.writeFile(indexPath, indexHtml, 'utf8');
      filesCreated.push(indexPath);
      this.log(`Generated index page: ${indexPath}`);

      // Generate story detail pages
      for (const story of stories) {
        const storyPath = path.join(options.inputDir, `story-${this.sanitizeFilename(story.storyId)}.json`);
        try {
          const storyContent = await fs.readFile(storyPath, 'utf8');
          const detailedStory: DetailedStoryEvidence = JSON.parse(storyContent);

          const storyPagePath = path.join(options.outputDir, `story-${this.sanitizeFilename(story.storyId)}.html`);
          const storyHtml = this.generateStoryPage(detailedStory, metadata, options);
          await fs.writeFile(storyPagePath, storyHtml, 'utf8');
          filesCreated.push(storyPagePath);
          this.log(`Generated story page: ${storyPagePath}`);
        } catch (error) {
          this.log(`Warning: Could not generate page for story ${story.storyId}: ${error instanceof Error ? error.message : String(error)}`);
        }
      }

      const duration = Date.now() - startTime;
      this.log(`Site generation completed in ${duration}ms`);

      return {
        success: true,
        pagesGenerated: stories.length + 1,
        filesCreated,
        duration,
      };
    } catch (error) {
      const duration = Date.now() - startTime;
      const errorMessage = error instanceof Error ? error.message : String(error);
      this.log(`Site generation failed: ${errorMessage}`);

      return {
        success: false,
        pagesGenerated: 0,
        filesCreated,
        duration,
        error: errorMessage,
      };
    }
  }

  /**
   * Generate index.html page
   */
  private generateIndexPage(
    stories: StoryEvidence[],
    metadata: ExportMetadata | undefined,
    options: SiteGenerationOptions
  ): string {
    const projectName = options.projectName || 'Test Evidence Dashboard';
    const baseUrl = options.baseUrl || '/';

    // Calculate overall statistics
    const totalStories = stories.length;
    const totalTests = stories.reduce((sum, s) => sum + s.totalTests, 0);
    const passedTests = stories.reduce((sum, s) => sum + s.passedTests, 0);
    const failedTests = stories.reduce((sum, s) => sum + s.failedTests, 0);
    const skippedTests = stories.reduce((sum, s) => sum + s.skippedTests, 0);
    const passRate = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(1) : '0.0';

    // Sort stories by ID
    const sortedStories = [...stories].sort((a, b) => a.storyId.localeCompare(b.storyId));

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${projectName} - Test Evidence Dashboard</title>
  <link rel="stylesheet" href="${baseUrl}dashboard.css">
</head>
<body>
  <header class="header">
    <h1>${projectName}</h1>
    <p class="subtitle">Test Evidence Dashboard</p>
  </header>

  <main class="main">
    <section class="summary">
      <h2>Overview</h2>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Stories</div>
          <div class="stat-value">${totalStories}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Total Tests</div>
          <div class="stat-value">${totalTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Passed</div>
          <div class="stat-value stat-passed">${passedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Failed</div>
          <div class="stat-value stat-failed">${failedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Skipped</div>
          <div class="stat-value stat-skipped">${skippedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Pass Rate</div>
          <div class="stat-value">${passRate}%</div>
        </div>
      </div>
      ${metadata ? this.generateMetadataSection(metadata) : ''}
    </section>

    <section class="stories">
      <h2>Stories</h2>
      <div class="story-list">
        ${sortedStories.map(story => this.generateStoryCard(story, baseUrl)).join('\n        ')}
      </div>
    </section>
  </main>

  <footer class="footer">
    <p>Generated on ${new Date().toLocaleString()}</p>
  </footer>
</body>
</html>`;
  }

  /**
   * Generate metadata section
   */
  private generateMetadataSection(metadata: ExportMetadata): string {
    let html = `
      <div class="export-metadata">
        <p><strong>Export Date:</strong> ${metadata.exportDate}</p>
        <p><strong>ReportPortal:</strong> ${metadata.reportPortalEndpoint} / ${metadata.reportPortalProject}</p>`;
    
    if (metadata.filters?.storyId) {
      html += `\n        <p><strong>Filtered by Story:</strong> ${metadata.filters.storyId}</p>`;
    }
    
    if (metadata.filters?.services) {
      html += `\n        <p><strong>Filtered by Services:</strong> ${metadata.filters.services.join(', ')}</p>`;
    }
    
    html += '\n      </div>\n      ';
    return html;
  }

  /**
   * Generate story card HTML for index page
   */
  private generateStoryCard(story: StoryEvidence, baseUrl: string): string {
    const passRate = story.totalTests > 0 ? ((story.passedTests / story.totalTests) * 100).toFixed(1) : '0.0';
    let statusClass: string;
    if (story.failedTests > 0) {
      statusClass = 'failed';
    } else if (story.totalTests === 0) {
      statusClass = 'not-tested';
    } else {
      statusClass = 'passed';
    }
    const storyUrl = `${baseUrl}story-${this.sanitizeFilename(story.storyId)}.html`;

    return `<div class="story-card story-card-${statusClass}">
          <div class="story-header">
            <h3><a href="${storyUrl}">${story.storyId}</a></h3>
            <span class="badge badge-${statusClass}">${statusClass.toUpperCase()}</span>
          </div>
          <div class="story-title">${story.title || 'No title'}</div>
          <div class="story-stats">
            <div class="story-stat">
              <span class="story-stat-label">Tests:</span>
              <span class="story-stat-value">${story.totalTests}</span>
            </div>
            <div class="story-stat">
              <span class="story-stat-label">Passed:</span>
              <span class="story-stat-value stat-passed">${story.passedTests}</span>
            </div>
            <div class="story-stat">
              <span class="story-stat-label">Failed:</span>
              <span class="story-stat-value stat-failed">${story.failedTests}</span>
            </div>
            <div class="story-stat">
              <span class="story-stat-label">Pass Rate:</span>
              <span class="story-stat-value">${passRate}%</span>
            </div>
          </div>
          <div class="story-services">
            <strong>Services:</strong> ${story.servicesInvolved.join(', ') || 'None'}
          </div>
          <div class="story-last-execution">
            <strong>Last Execution:</strong> ${story.lastExecutionDate || 'Never'}
          </div>
        </div>`;
  }

  /**
   * Generate story detail page
   */
  private generateStoryPage(
    story: DetailedStoryEvidence,
    metadata: ExportMetadata | undefined,
    options: SiteGenerationOptions
  ): string {
    const projectName = options.projectName || 'Test Evidence Dashboard';
    const baseUrl = options.baseUrl || '/';
    const passRate = story.totalTests > 0 ? ((story.passedTests / story.totalTests) * 100).toFixed(1) : '0.0';
    let statusClass: string;
    if (story.failedTests > 0) {
      statusClass = 'failed';
    } else if (story.totalTests === 0) {
      statusClass = 'not-tested';
    } else {
      statusClass = 'passed';
    }

    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${story.storyId} - ${projectName}</title>
  <link rel="stylesheet" href="${baseUrl}dashboard.css">
</head>
<body>
  <header class="header">
    <h1>${projectName}</h1>
    <nav class="breadcrumb">
      <a href="${baseUrl}index.html">Home</a> / ${story.storyId}
    </nav>
  </header>

  <main class="main">
    <section class="story-detail">
      <div class="story-detail-header">
        <h2>${story.storyId}</h2>
        <span class="badge badge-${statusClass}">${statusClass.toUpperCase()}</span>
      </div>
      <div class="story-detail-title">${story.title || 'No title'}</div>

      <div class="story-detail-stats">
        <div class="stat-card">
          <div class="stat-label">Total Tests</div>
          <div class="stat-value">${story.totalTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Passed</div>
          <div class="stat-value stat-passed">${story.passedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Failed</div>
          <div class="stat-value stat-failed">${story.failedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Skipped</div>
          <div class="stat-value stat-skipped">${story.skippedTests}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Pass Rate</div>
          <div class="stat-value">${passRate}%</div>
        </div>
      </div>

      ${story.acceptanceCriteria && story.acceptanceCriteria.length > 0 ? `
      <div class="story-section">
        <h3>Acceptance Criteria</h3>
        <ul class="acceptance-criteria">
          ${story.acceptanceCriteria.map(ac => `<li>${this.escapeHtml(ac)}</li>`).join('\n          ')}
        </ul>
      </div>
      ` : ''}

      ${story.testScenarios && story.testScenarios.length > 0 ? `
      <div class="story-section">
        <h3>Test Scenarios</h3>
        <ul class="test-scenarios">
          ${story.testScenarios.map(scenario => `<li>${this.escapeHtml(scenario)}</li>`).join('\n          ')}
        </ul>
      </div>
      ` : ''}

      <div class="story-section">
        <h3>Test Results by Service</h3>
        ${this.generateServiceTables(story)}
      </div>

      ${story.history && story.history.length > 0 ? `
      <div class="story-section">
        <h3>Test Execution History</h3>
        ${this.generateHistoryTable(story.history)}
      </div>
      ` : ''}
    </section>
  </main>

  <footer class="footer">
    <p>Generated on ${new Date().toLocaleString()}</p>
  </footer>
</body>
</html>`;
  }

  /**
   * Generate service status tables
   */
  private generateServiceTables(story: DetailedStoryEvidence): string {
    if (!story.statusByService || Object.keys(story.statusByService).length === 0) {
      return '<p>No service status available.</p>';
    }

    // Sort by service name
    const sortedServices = Object.values(story.statusByService).sort((a, b) => a.service.localeCompare(b.service));

    let html = '<div class="service-tables">';

    for (const serviceStatus of sortedServices) {
      const statusClass = serviceStatus.status;
      const passRate = serviceStatus.tests > 0
        ? ((serviceStatus.passed / serviceStatus.tests) * 100).toFixed(1)
        : '0.0';

      html += `
      <div class="service-table">
        <h4>${serviceStatus.service} <span class="badge badge-${statusClass}">${statusClass.toUpperCase()}</span></h4>
        <div class="service-stats">
          <span>Tests: ${serviceStatus.tests}</span>
          <span>Passed: <span class="stat-passed">${serviceStatus.passed}</span></span>
          <span>Failed: <span class="stat-failed">${serviceStatus.failed}</span></span>
          <span>Skipped: <span class="stat-skipped">${serviceStatus.skipped}</span></span>
          <span>Pass Rate: ${passRate}%</span>
          ${serviceStatus.lastExecution ? `<span>Last: ${new Date(serviceStatus.lastExecution).toLocaleString()}</span>` : ''}
        </div>
        ${this.generateServiceTestTable(story, serviceStatus.service)}
      </div>`;
    }

    html += '</div>';
    return html;
  }

  /**
   * Generate test table for a service
   */
  private generateServiceTestTable(story: DetailedStoryEvidence, service: string): string {
    const tests = story.testsByService?.[service];
    if (!tests || tests.length === 0) {
      return '<p class="no-tests">No tests executed for this service.</p>';
    }

    // Sort by test name
    const sortedTests = [...tests].sort((a, b) => a.testName.localeCompare(b.testName));

    let html = `
        <table class="test-table">
          <thead>
            <tr>
              <th>Test Name</th>
              <th>Type</th>
              <th>Status</th>
              <th>Duration</th>
              <th>Launch</th>
            </tr>
          </thead>
          <tbody>`;

    for (const test of sortedTests) {
      const statusClass = test.status;
      const duration = test.duration ? `${(test.duration / 1000).toFixed(2)}s` : 'N/A';

      html += `
            <tr class="test-row-${statusClass}">
              <td class="test-name">${this.escapeHtml(test.testName)}</td>
              <td class="test-type">${this.escapeHtml(test.testType)}</td>
              <td class="test-status"><span class="badge badge-${statusClass}">${statusClass.toUpperCase()}</span></td>
              <td class="test-duration">${duration}</td>
              <td class="test-launch">${this.escapeHtml(test.launchName)}</td>
            </tr>`;
    }

    html += `
          </tbody>
        </table>`;

    return html;
  }

  /**
   * Generate test execution history table
   */
  private generateHistoryTable(history: TestHistory[]): string {
    // Sort by date descending (most recent first)
    const sortedHistory = [...history].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    let html = `
        <table class="history-table">
          <thead>
            <tr>
              <th>Launch</th>
              <th>Date</th>
              <th>Status</th>
              <th>Total</th>
              <th>Passed</th>
              <th>Failed</th>
              <th>Skipped</th>
            </tr>
          </thead>
          <tbody>`;

    for (const entry of sortedHistory) {
      const statusClass = entry.status;
      const date = new Date(entry.date).toLocaleString();

      html += `
            <tr class="history-row-${statusClass}">
              <td class="history-launch">${this.escapeHtml(entry.launchName)} #${entry.launchNumber}</td>
              <td class="history-date">${date}</td>
              <td class="history-status"><span class="badge badge-${statusClass}">${statusClass.toUpperCase()}</span></td>
              <td class="history-total">${entry.total}</td>
              <td class="history-passed stat-passed">${entry.passed}</td>
              <td class="history-failed stat-failed">${entry.failed}</td>
              <td class="history-skipped stat-skipped">${entry.skipped}</td>
            </tr>`;
    }

    html += `
          </tbody>
        </table>`;

    return html;
  }

  /**
   * Escape HTML special characters
   */
  private escapeHtml(text: string): string {
    const escapeMap: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;',
    };
    // eslint-disable-next-line unicorn/prefer-string-replace-all
    return text.replace(/[&<>"']/g, match => escapeMap[match] || match);
  }

  /**
   * Sanitize story ID for use as filename
   */
  private sanitizeFilename(storyId: string): string {
    // eslint-disable-next-line unicorn/prefer-string-replace-all
    return storyId.replace(/[^\w.-]/g, '-').toLowerCase();
  }

  /**
   * Log message if verbose enabled
   */
  private log(message: string): void {
    if (this.verbose) {
      console.log(`[Static Site Generator] ${message}`);
    }
  }
}
