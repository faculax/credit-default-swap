/**
 * ReportPortal Query Client
 * 
 * Queries ReportPortal REST API for test evidence including launches,
 * test items, and results. Filters by story ID, services, and time ranges.
 * 
 * Story 20.9: Evidence Export & Static Dashboard
 */

import * as https from 'node:https';
import * as http from 'node:http';
import type { ReportPortalConfig } from '../models/reportportal-model.js';

/**
 * ReportPortal launch data
 */
export interface RPLaunch {
  id: number;
  uuid: string;
  name: string;
  number: number;
  description?: string;
  startTime: number;
  endTime?: number;
  status: string;
  statistics: RPStatistics;
  attributes: RPAttribute[];
}

/**
 * ReportPortal test item data
 */
export interface RPTestItem {
  id: number;
  uuid: string;
  name: string;
  type: string;
  status: string;
  startTime: number;
  endTime?: number;
  description?: string;
  attributes: RPAttribute[];
  parameters?: RPParameter[];
  codeRef?: string;
  launchId: number;
  parentId?: number;
  hasChildren: boolean;
}

/**
 * ReportPortal attribute/tag
 */
export interface RPAttribute {
  key?: string;
  value: string;
  system?: boolean;
}

/**
 * ReportPortal parameter
 */
export interface RPParameter {
  key: string;
  value: string;
}

/**
 * ReportPortal statistics
 */
export interface RPStatistics {
  executions: {
    total: number;
    passed: number;
    failed: number;
    skipped: number;
  };
  defects: {
    product_bug: { total: number };
    automation_bug: { total: number };
    system_issue: { total: number };
    to_investigate: { total: number };
  };
}

/**
 * Story evidence summary
 */
export interface StoryEvidence {
  storyId: string;
  title: string;
  description?: string;
  servicesInvolved: string[];
  lastExecutionDate?: number;
  statusByService: Record<string, ServiceStatus>;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  skippedTests: number;
}

/**
 * Service test status
 */
export interface ServiceStatus {
  service: string;
  tested: boolean;
  lastExecution?: number;
  status: 'passed' | 'failed' | 'skipped' | 'not-tested';
  tests: number;
  passed: number;
  failed: number;
  skipped: number;
}

/**
 * Detailed story evidence
 */
export interface DetailedStoryEvidence extends StoryEvidence {
  acceptanceCriteria: string[];
  testScenarios: string[];
  testsByService: Record<string, TestExecution[]>;
  history: TestHistory[];
}

/**
 * Test execution record
 */
export interface TestExecution {
  testName: string;
  testType: string;
  status: 'passed' | 'failed' | 'skipped';
  startTime: number;
  endTime?: number;
  duration?: number;
  launchId: number;
  launchName: string;
  attributes: RPAttribute[];
}

/**
 * Test history entry
 */
export interface TestHistory {
  launchId: number;
  launchName: string;
  launchNumber: number;
  date: number;
  status: string;
  total: number;
  passed: number;
  failed: number;
  skipped: number;
}

/**
 * Query options
 */
export interface QueryOptions {
  storyId?: string;
  services?: string[];
  startTime?: number;
  endTime?: number;
  limit?: number;
}

/**
 * ReportPortal Query Client
 */
export class ReportPortalQueryClient {
  private readonly config: ReportPortalConfig;
  private readonly baseUrl: string;
  private readonly apiUrl: string;

  constructor(config: ReportPortalConfig) {
    this.config = config;
    this.baseUrl = config.endpoint.replace(/\/$/, '');
    this.apiUrl = `${this.baseUrl}/api/v1/${config.project}`;
  }

  /**
   * Query launches with filters
   */
  async queryLaunches(options: QueryOptions = {}): Promise<RPLaunch[]> {
    const params = new URLSearchParams();
    
    if (options.startTime) {
      params.append('filter.gte.startTime', options.startTime.toString());
    }
    if (options.endTime) {
      params.append('filter.lte.endTime', options.endTime.toString());
    }
    if (options.limit) {
      params.append('page.size', options.limit.toString());
    } else {
      params.append('page.size', '100');
    }

    // Filter by story ID if provided
    if (options.storyId) {
      params.append('filter.has.attributeKey', 'story');
      params.append('filter.eq.attributeValue', options.storyId);
    }

    // Filter by service if provided
    if (options.services && options.services.length > 0) {
      for (const service of options.services) {
        params.append('filter.has.attributeValue', service);
      }
    }

    const url = `/launch?${params.toString()}`;
    const response = await this.request<{ content: RPLaunch[] }>(url);
    return response.content || [];
  }

  /**
   * Query test items for a launch
   */
  async queryTestItems(launchId: number): Promise<RPTestItem[]> {
    const url = `/item?filter.eq.launchId=${launchId}&page.size=1000`;
    const response = await this.request<{ content: RPTestItem[] }>(url);
    return response.content || [];
  }

  /**
   * Get all story evidence summaries
   */
  async getAllStoryEvidence(options: QueryOptions = {}): Promise<StoryEvidence[]> {
    const launches = await this.queryLaunches(options);
    const evidenceMap = new Map<string, StoryEvidence>();

    for (const launch of launches) {
      const testItems = await this.queryTestItems(launch.id);
      this.aggregateEvidence(launch, testItems, evidenceMap);
    }

    return Array.from(evidenceMap.values());
  }

  /**
   * Get detailed evidence for a specific story
   */
  async getStoryEvidence(storyId: string, options: QueryOptions = {}): Promise<DetailedStoryEvidence | null> {
    const launches = await this.queryLaunches({ ...options, storyId });
    
    if (launches.length === 0) {
      return null;
    }

    const testsByService: Record<string, TestExecution[]> = {};
    const history: TestHistory[] = [];
    let totalTests = 0;
    let passedTests = 0;
    let failedTests = 0;
    let skippedTests = 0;

    for (const launch of launches) {
      const testItems = await this.queryTestItems(launch.id);
      
      // Add to history
      history.push({
        launchId: launch.id,
        launchName: launch.name,
        launchNumber: launch.number,
        date: launch.startTime,
        status: launch.status,
        total: launch.statistics.executions.total,
        passed: launch.statistics.executions.passed,
        failed: launch.statistics.executions.failed,
        skipped: launch.statistics.executions.skipped,
      });

      // Process test items
      for (const item of testItems) {
        if (item.type === 'test') {
          const service = this.extractService(item.attributes);
          if (service) {
            if (!testsByService[service]) {
              testsByService[service] = [];
            }

            testsByService[service].push({
              testName: item.name,
              testType: item.type,
              status: this.mapStatus(item.status),
              startTime: item.startTime,
              endTime: item.endTime,
              duration: item.endTime ? item.endTime - item.startTime : undefined,
              launchId: launch.id,
              launchName: launch.name,
              attributes: item.attributes,
            });

            totalTests++;
            if (item.status === 'passed') passedTests++;
            else if (item.status === 'failed') failedTests++;
            else if (item.status === 'skipped') skippedTests++;
          }
        }
      }
    }

    // Extract story metadata from first launch
    const firstLaunch = launches[0];
    const storyAttr = firstLaunch.attributes.find(a => a.key === 'story' || a.value === storyId);
    const services = this.extractServices(firstLaunch.attributes);

    const statusByService: Record<string, ServiceStatus> = {};
    for (const service of services) {
      const serviceTests = testsByService[service] || [];
      const passed = serviceTests.filter(t => t.status === 'passed').length;
      const failed = serviceTests.filter(t => t.status === 'failed').length;
      const skipped = serviceTests.filter(t => t.status === 'skipped').length;

      statusByService[service] = {
        service,
        tested: serviceTests.length > 0,
        lastExecution: serviceTests.length > 0 ? Math.max(...serviceTests.map(t => t.startTime)) : undefined,
        status: failed > 0 ? 'failed' : passed > 0 ? 'passed' : skipped > 0 ? 'skipped' : 'not-tested',
        tests: serviceTests.length,
        passed,
        failed,
        skipped,
      };
    }

    return {
      storyId,
      title: firstLaunch.name.replace(/^Story \d+\.\d+ - /, '') || storyId,
      description: firstLaunch.description,
      servicesInvolved: services,
      lastExecutionDate: Math.max(...launches.map(l => l.startTime)),
      statusByService,
      totalTests,
      passedTests,
      failedTests,
      skippedTests,
      acceptanceCriteria: [], // Would need to be fetched from story files
      testScenarios: [], // Would need to be fetched from story files
      testsByService,
      history: history.sort((a, b) => b.date - a.date),
    };
  }

  /**
   * Aggregate evidence from launch and test items
   */
  private aggregateEvidence(
    launch: RPLaunch,
    testItems: RPTestItem[],
    evidenceMap: Map<string, StoryEvidence>
  ): void {
    // Extract story ID from launch attributes
    const storyAttr = launch.attributes.find(a => a.key === 'story');
    if (!storyAttr) return;

    const storyId = storyAttr.value;
    const services = this.extractServices(launch.attributes);

    let evidence = evidenceMap.get(storyId);
    if (!evidence) {
      evidence = {
        storyId,
        title: launch.name.replace(/^Story \d+\.\d+ - /, '') || storyId,
        description: launch.description,
        servicesInvolved: services,
        statusByService: {},
        totalTests: 0,
        passedTests: 0,
        failedTests: 0,
        skippedTests: 0,
      };
      evidenceMap.set(storyId, evidence);
    }

    // Update last execution date
    if (!evidence.lastExecutionDate || launch.startTime > evidence.lastExecutionDate) {
      evidence.lastExecutionDate = launch.startTime;
    }

    // Aggregate test statistics
    evidence.totalTests += launch.statistics.executions.total;
    evidence.passedTests += launch.statistics.executions.passed;
    evidence.failedTests += launch.statistics.executions.failed;
    evidence.skippedTests += launch.statistics.executions.skipped;

    // Update service status
    for (const service of services) {
      if (!evidence.statusByService[service]) {
        evidence.statusByService[service] = {
          service,
          tested: false,
          status: 'not-tested',
          tests: 0,
          passed: 0,
          failed: 0,
          skipped: 0,
        };
      }

      const serviceStatus = evidence.statusByService[service];
      serviceStatus.tested = true;
      serviceStatus.lastExecution = launch.startTime;
      serviceStatus.tests += launch.statistics.executions.total;
      serviceStatus.passed += launch.statistics.executions.passed;
      serviceStatus.failed += launch.statistics.executions.failed;
      serviceStatus.skipped += launch.statistics.executions.skipped;

      if (serviceStatus.failed > 0) {
        serviceStatus.status = 'failed';
      } else if (serviceStatus.passed > 0) {
        serviceStatus.status = 'passed';
      } else if (serviceStatus.skipped > 0) {
        serviceStatus.status = 'skipped';
      }
    }
  }

  /**
   * Extract service from attributes
   */
  private extractService(attributes: RPAttribute[]): string | null {
    const serviceAttr = attributes.find(a => 
      a.key === 'service' || 
      ['frontend', 'backend', 'gateway', 'risk-engine'].includes(a.value.toLowerCase())
    );
    return serviceAttr ? serviceAttr.value : null;
  }

  /**
   * Extract all services from attributes
   */
  private extractServices(attributes: RPAttribute[]): string[] {
    return attributes
      .filter(a => 
        a.key === 'service' || 
        ['frontend', 'backend', 'gateway', 'risk-engine'].includes(a.value.toLowerCase())
      )
      .map(a => a.value)
      .filter((v, i, arr) => arr.indexOf(v) === i); // Unique
  }

  /**
   * Map status to standard format
   */
  private mapStatus(status: string): 'passed' | 'failed' | 'skipped' {
    const normalized = status.toLowerCase();
    if (normalized === 'passed') return 'passed';
    if (normalized === 'failed') return 'failed';
    return 'skipped';
  }

  /**
   * Make HTTP request to ReportPortal API
   */
  private request<T>(path: string): Promise<T> {
    return new Promise((resolve, reject) => {
      const url = new URL(`${this.apiUrl}${path}`);
      const isHttps = url.protocol === 'https:';
      const client = isHttps ? https : http;

      const options: http.RequestOptions = {
        hostname: url.hostname,
        port: url.port || (isHttps ? 443 : 80),
        path: url.pathname + url.search,
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.config.token}`,
          'Accept': 'application/json',
        },
        timeout: this.config.timeout || 30000,
      };

      const req = client.request(options, (res) => {
        let data = '';

        res.on('data', (chunk) => {
          data += chunk.toString();
        });

        res.on('end', () => {
          if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
            try {
              resolve(JSON.parse(data) as T);
            } catch (error) {
              reject(new Error(`Failed to parse response: ${(error as Error).message}`));
            }
          } else {
            reject(new Error(`HTTP ${res.statusCode}: ${res.statusMessage}`));
          }
        });
      });

      req.on('error', reject);
      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Request timeout'));
      });

      req.end();
    });
  }
}
