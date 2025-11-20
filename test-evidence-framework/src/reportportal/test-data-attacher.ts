/**
 * ReportPortal Test Data Attacher
 * 
 * Attaches test data (request payloads, response data, entity snapshots) to
 * ReportPortal test results for better debugging and traceability.
 * 
 * Story 20.10: ReportPortal Test Data Attachments
 */

import {
  ReportPortalLog,
  ReportPortalLogLevel,
  ReportPortalFile,
  SaveLogRequest
} from '../models/reportportal-model';

export interface TestDataAttachment {
  /** Attachment name */
  name: string;
  
  /** Attachment type */
  type: 'request' | 'response' | 'entity' | 'screenshot' | 'log' | 'other';
  
  /** Data content */
  data: any;
  
  /** MIME type */
  mimeType?: string;
  
  /** Attachment timestamp */
  timestamp?: number;
}

export interface AttachmentOptions {
  /** Pretty print JSON */
  prettyPrint?: boolean;
  
  /** Maximum data size (bytes) */
  maxSize?: number;
  
  /** Truncate large data */
  truncate?: boolean;
  
  /** Include metadata */
  includeMetadata?: boolean;
}

export class TestDataAttacher {
  private static readonly DEFAULT_MAX_SIZE = 1024 * 1024; // 1MB
  private static readonly DEFAULT_OPTIONS: AttachmentOptions = {
    prettyPrint: true,
    maxSize: TestDataAttacher.DEFAULT_MAX_SIZE,
    truncate: true,
    includeMetadata: true
  };
  
  /**
   * Create attachment log entry for ReportPortal
   */
  static createAttachment(
    itemUuid: string,
    launchUuid: string,
    attachment: TestDataAttachment,
    options: AttachmentOptions = {}
  ): SaveLogRequest {
    const opts = { ...TestDataAttacher.DEFAULT_OPTIONS, ...options };
    
    const file = this.prepareFile(attachment, opts);
    const message = this.generateAttachmentMessage(attachment, opts);
    
    return {
      itemUuid,
      launchUuid,
      time: attachment.timestamp || Date.now(),
      message,
      level: this.getLogLevel(attachment.type),
      file
    };
  }
  
  /**
   * Prepare file for attachment
   */
  private static prepareFile(
    attachment: TestDataAttachment,
    options: AttachmentOptions
  ): ReportPortalFile {
    let content: string;
    let contentType: string;
    
    // Determine content type
    if (attachment.mimeType) {
      contentType = attachment.mimeType;
    } else {
      contentType = this.inferContentType(attachment);
    }
    
    // Serialize data
    if (typeof attachment.data === 'string') {
      content = attachment.data;
    } else if (contentType.includes('json')) {
      content = options.prettyPrint
        ? JSON.stringify(attachment.data, null, 2)
        : JSON.stringify(attachment.data);
    } else {
      content = String(attachment.data);
    }
    
    // Truncate if needed
    if (options.truncate && options.maxSize && content.length > options.maxSize) {
      const truncated = content.substring(0, options.maxSize);
      const suffix = `\n\n... (truncated ${content.length - options.maxSize} bytes)`;
      content = truncated + suffix;
    }
    
    // Base64 encode
    const encoded = Buffer.from(content, 'utf-8').toString('base64');
    
    return {
      name: attachment.name,
      content: encoded,
      contentType
    };
  }
  
  /**
   * Infer content type from attachment
   */
  private static inferContentType(attachment: TestDataAttachment): string {
    if (attachment.type === 'screenshot') {
      return 'image/png';
    }
    
    if (typeof attachment.data === 'object') {
      return 'application/json';
    }
    
    return 'text/plain';
  }
  
  /**
   * Generate attachment message
   */
  private static generateAttachmentMessage(
    attachment: TestDataAttachment,
    options: AttachmentOptions
  ): string {
    const lines: string[] = [];
    
    lines.push(`📎 ${attachment.name}`);
    
    if (options.includeMetadata) {
      lines.push(`Type: ${attachment.type}`);
      
      if (attachment.timestamp) {
        const date = new Date(attachment.timestamp).toISOString();
        lines.push(`Timestamp: ${date}`);
      }
      
      const size = this.getDataSize(attachment.data);
      lines.push(`Size: ${this.formatBytes(size)}`);
    }
    
    return lines.join('\n');
  }
  
  /**
   * Get log level based on attachment type
   */
  private static getLogLevel(type: TestDataAttachment['type']): ReportPortalLogLevel {
    switch (type) {
      case 'request':
      case 'response':
        return 'info';
      case 'entity':
        return 'debug';
      case 'screenshot':
        return 'info';
      case 'log':
        return 'trace';
      default:
        return 'info';
    }
  }
  
  /**
   * Get data size in bytes
   */
  private static getDataSize(data: any): number {
    if (typeof data === 'string') {
      return Buffer.byteLength(data, 'utf-8');
    }
    
    const json = JSON.stringify(data);
    return Buffer.byteLength(json, 'utf-8');
  }
  
  /**
   * Format bytes to human-readable string
   */
  private static formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
  
  /**
   * Create request payload attachment
   */
  static attachRequest(
    itemUuid: string,
    launchUuid: string,
    method: string,
    url: string,
    payload: any,
    options?: AttachmentOptions
  ): SaveLogRequest {
    return this.createAttachment(
      itemUuid,
      launchUuid,
      {
        name: `Request: ${method} ${url}`,
        type: 'request',
        data: {
          method,
          url,
          payload,
          timestamp: new Date().toISOString()
        }
      },
      options
    );
  }
  
  /**
   * Create response data attachment
   */
  static attachResponse(
    itemUuid: string,
    launchUuid: string,
    status: number,
    data: any,
    options?: AttachmentOptions
  ): SaveLogRequest {
    return this.createAttachment(
      itemUuid,
      launchUuid,
      {
        name: `Response: ${status}`,
        type: 'response',
        data: {
          status,
          data,
          timestamp: new Date().toISOString()
        }
      },
      options
    );
  }
  
  /**
   * Create entity snapshot attachment
   */
  static attachEntity(
    itemUuid: string,
    launchUuid: string,
    entityType: string,
    entity: any,
    options?: AttachmentOptions
  ): SaveLogRequest {
    return this.createAttachment(
      itemUuid,
      launchUuid,
      {
        name: `Entity: ${entityType}`,
        type: 'entity',
        data: entity
      },
      options
    );
  }
  
  /**
   * Create test data attachment (generic)
   */
  static attachTestData(
    itemUuid: string,
    launchUuid: string,
    name: string,
    data: any,
    options?: AttachmentOptions
  ): SaveLogRequest {
    return this.createAttachment(
      itemUuid,
      launchUuid,
      {
        name: `Test Data: ${name}`,
        type: 'other',
        data
      },
      options
    );
  }
}
