/**
 * ReportPortal HTTP Client
 * 
 * Handles communication with ReportPortal API including authentication,
 * request/response handling, retries, and error management.
 * 
 * Story 20.8: ReportPortal Integration
 */

import * as https from 'node:https';
import * as http from 'node:http';
import type {
  ReportPortalConfig,
  ReportPortalErrorResponse,
  StartLaunchRequest,
  StartLaunchResponse,
  FinishLaunchRequest,
  FinishLaunchResponse,
  StartTestItemRequest,
  StartTestItemResponse,
  FinishTestItemRequest,
  FinishTestItemResponse,
  SaveLogRequest,
  SaveLogResponse,
} from '../models/reportportal-model.js';

/**
 * HTTP request options
 */
interface RequestOptions {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  path: string;
  body?: unknown;
  headers?: Record<string, string>;
}

/**
 * ReportPortal API client
 */
export class ReportPortalClient {
  private readonly config: Required<ReportPortalConfig>;
  private readonly baseUrl: string;
  private readonly apiUrl: string;

  constructor(config: ReportPortalConfig) {
    this.config = {
      endpoint: config.endpoint,
      token: config.token,
      project: config.project,
      launchName: config.launchName || 'Test Evidence Framework',
      launchDescription: config.launchDescription || '',
      launchAttributes: config.launchAttributes || [],
      mode: config.mode || 'DEFAULT',
      debug: config.debug || false,
      timeout: config.timeout || 30000,
      maxRetries: config.maxRetries || 3,
      uploadAttachments: config.uploadAttachments !== false,
      maxAttachmentSize: config.maxAttachmentSize || 10 * 1024 * 1024, // 10MB
    };

    this.baseUrl = this.config.endpoint.replace(/\/$/, '');
    this.apiUrl = `${this.baseUrl}/api/v1/${this.config.project}`;

    if (this.config.debug) {
      console.log('[ReportPortal] Client initialized:', {
        endpoint: this.baseUrl,
        project: this.config.project,
        mode: this.config.mode,
      });
    }
  }

  /**
   * Start a new launch
   */
  async startLaunch(request: StartLaunchRequest): Promise<StartLaunchResponse> {
    if (this.config.debug) {
      console.log('[ReportPortal] Starting launch:', request.name);
    }

    const response = await this.request<StartLaunchResponse>({
      method: 'POST',
      path: '/launch',
      body: request,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Launch started:', response.id);
    }

    return response;
  }

  /**
   * Finish a launch
   */
  async finishLaunch(
    launchId: string,
    request: FinishLaunchRequest
  ): Promise<FinishLaunchResponse> {
    if (this.config.debug) {
      console.log('[ReportPortal] Finishing launch:', launchId);
    }

    const response = await this.request<FinishLaunchResponse>({
      method: 'PUT',
      path: `/launch/${launchId}/finish`,
      body: request,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Launch finished:', {
        id: response.id,
        link: response.link,
      });
    }

    return response;
  }

  /**
   * Start a test item
   */
  async startTestItem(request: StartTestItemRequest): Promise<StartTestItemResponse> {
    if (this.config.debug) {
      console.log('[ReportPortal] Starting test item:', {
        name: request.name,
        type: request.type,
        parent: request.parentUuid,
      });
    }

    const path = request.parentUuid
      ? `/item/${request.parentUuid}`
      : '/item';

    const response = await this.request<StartTestItemResponse>({
      method: 'POST',
      path,
      body: request,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Test item started:', response.id);
    }

    return response;
  }

  /**
   * Finish a test item
   */
  async finishTestItem(
    itemId: string,
    request: FinishTestItemRequest
  ): Promise<FinishTestItemResponse> {
    if (this.config.debug) {
      console.log('[ReportPortal] Finishing test item:', {
        id: itemId,
        status: request.status,
      });
    }

    const response = await this.request<FinishTestItemResponse>({
      method: 'PUT',
      path: `/item/${itemId}`,
      body: request,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Test item finished:', response.id);
    }

    return response;
  }

  /**
   * Save a log entry
   */
  async saveLog(request: SaveLogRequest): Promise<SaveLogResponse> {
    if (this.config.debug) {
      console.log('[ReportPortal] Saving log:', {
        item: request.itemUuid,
        level: request.level,
        hasFile: !!request.file,
      });
    }

    const response = await this.request<SaveLogResponse>({
      method: 'POST',
      path: '/log',
      body: request,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Log saved:', response.id);
    }

    return response;
  }

  /**
   * Save multiple log entries in batch
   */
  async saveLogBatch(requests: SaveLogRequest[]): Promise<SaveLogResponse[]> {
    if (this.config.debug) {
      console.log('[ReportPortal] Saving log batch:', requests.length);
    }

    const response = await this.request<SaveLogResponse[]>({
      method: 'POST',
      path: '/log',
      body: requests,
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Log batch saved:', response.length);
    }

    return response;
  }

  /**
   * Check connection to ReportPortal (lenient check)
   */
  async checkConnection(): Promise<boolean> {
    try {
      // Try to access project endpoint - if project doesn't exist, we still have connection
      await this.request({
        method: 'GET',
        path: '/launch?page.size=1',
      });
      return true;
    } catch (error) {
      const message = (error as Error).message;
      // "Project not found" means ReportPortal is accessible, just project doesn't exist yet
      if (message.includes('not found') || message.includes('404')) {
        return true;
      }
      if (this.config.debug) {
        console.warn('[ReportPortal] Connection check failed:', error);
      }
      return false;
    }
  }

  /**
   * Check if project exists
   */
  async projectExists(): Promise<boolean> {
    try {
      await this.request({
        method: 'GET',
        path: '/launch?page.size=1',
      });
      return true;
    } catch (error) {
      const message = (error as Error).message;
      // Project not found returns specific error
      if (message.includes('not found') || message.includes('404')) {
        return false;
      }
      throw error;
    }
  }

  /**
   * Create a new project
   */
  async createProject(projectName?: string): Promise<void> {
    const name = projectName || this.config.project;
    
    if (this.config.debug) {
      console.log('[ReportPortal] Creating project:', name);
    }

    // Use base API URL without project name for project creation
    const createUrl = `${this.baseUrl}/api/v1/project`;
    
    await this.executeRequestWithCustomUrl(createUrl, {
      method: 'POST',
      path: '',
      body: {
        projectName: name,
        entryType: 'INTERNAL',
      },
    });

    if (this.config.debug) {
      console.log('[ReportPortal] Project created successfully');
    }
  }

  /**
   * Ensure project exists (create if needed)
   */
  async ensureProject(): Promise<void> {
    const exists = await this.projectExists();
    
    if (!exists) {
      if (this.config.debug) {
        console.log('[ReportPortal] Project does not exist, creating...');
      }
      await this.createProject();
    }
  }

  /**
   * Get launch URL
   */
  getLaunchUrl(launchId: string): string {
    return `${this.baseUrl}/ui/#${this.config.project}/launches/all/${launchId}`;
  }

  /**
   * Get test item URL
   */
  getTestItemUrl(launchId: string, itemId: string): string {
    return `${this.baseUrl}/ui/#${this.config.project}/launches/all/${launchId}/${itemId}`;
  }

  /**
   * Make HTTP request to ReportPortal API
   */
  private async request<T>(options: RequestOptions): Promise<T> {
    let lastError: Error | undefined;

    for (let attempt = 1; attempt <= this.config.maxRetries; attempt++) {
      try {
        return await this.executeRequest<T>(options);
      } catch (error) {
        lastError = error as Error;

        if (this.config.debug) {
          console.warn(`[ReportPortal] Request failed (attempt ${attempt}/${this.config.maxRetries}):`, error);
        }

        if (attempt < this.config.maxRetries) {
          // Exponential backoff: 1s, 2s, 4s, etc.
          const delay = 1000 * Math.pow(2, attempt - 1);
          await new Promise(resolve => setTimeout(resolve, delay));
        }
      }
    }

    throw new Error(
      `ReportPortal request failed after ${this.config.maxRetries} attempts: ${lastError?.message}`
    );
  }

  /**
   * Execute a single HTTP request
   */
  private executeRequest<T>(options: RequestOptions): Promise<T> {
    return this.executeRequestWithCustomUrl(`${this.apiUrl}${options.path}`, options);
  }

  /**
   * Execute a single HTTP request with custom URL
   */
  private executeRequestWithCustomUrl<T>(fullUrl: string, options: RequestOptions): Promise<T> {
    return new Promise((resolve, reject) => {
      const url = new URL(fullUrl);
      const isHttps = url.protocol === 'https:';
      const client = isHttps ? https : http;

      const body = options.body ? JSON.stringify(options.body) : undefined;

      const headers: Record<string, string> = {
        'Authorization': `Bearer ${this.config.token}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...options.headers,
      };

      if (body) {
        headers['Content-Length'] = Buffer.byteLength(body).toString();
      }

      const requestOptions: http.RequestOptions = {
        hostname: url.hostname,
        port: url.port || (isHttps ? 443 : 80),
        path: url.pathname + url.search,
        method: options.method,
        headers,
        timeout: this.config.timeout,
      };

      const req = client.request(requestOptions, (res) => {
        let data = '';

        res.on('data', (chunk) => {
          data += chunk.toString();
        });

        res.on('end', () => {
          if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const result = data ? JSON.parse(data) : {};
              resolve(result as T);
            } catch (error) {
              reject(new Error(`Failed to parse response: ${(error as Error).message}`));
            }
          } else {
            let errorMessage = `HTTP ${res.statusCode}: ${res.statusMessage}`;
            try {
              const errorResponse: ReportPortalErrorResponse = JSON.parse(data);
              errorMessage = errorResponse.message || errorMessage;
              if (errorResponse.details) {
                errorMessage += ` - ${errorResponse.details}`;
              }
            } catch {
              // Use default error message
            }
            reject(new Error(errorMessage));
          }
        });
      });

      req.on('error', (error) => {
        reject(error);
      });

      req.on('timeout', () => {
        req.destroy();
        reject(new Error(`Request timeout after ${this.config.timeout}ms`));
      });

      if (body) {
        req.write(body);
      }

      req.end();
    });
  }
}
