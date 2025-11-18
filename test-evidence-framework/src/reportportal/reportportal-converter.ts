/**
 * ReportPortal Result Converters
 * 
 * Converts validation results, Allure results, and test reports into
 * ReportPortal format for upload. Supports multiple test frameworks and types.
 * 
 * Story 20.8: ReportPortal Integration
 */

import type {
  ValidationReport,
  ValidationResult,
  ValidationIssue,
} from '../models/validation-model.js';
import type {
  ReportPortalTestItem,
  ReportPortalLog,
  ReportPortalAttribute,
  ReportPortalStatus,
  ReportPortalLogLevel,
  ReportPortalParameter,
} from '../models/reportportal-model.js';

/**
 * Allure status type
 */
type AllureStatus = 'passed' | 'failed' | 'broken' | 'skipped';

/**
 * Allure result structure (simplified)
 */
interface AllureResult {
  uuid: string;
  name: string;
  fullName?: string;
  description?: string;
  status: AllureStatus;
  statusDetails?: {
    message?: string;
    trace?: string;
  };
  start: number;
  stop: number;
  labels?: Array<{ name: string; value: string }>;
  parameters?: Array<{ name: string; value: string }>;
  steps?: AllureStep[];
  attachments?: Array<{ name: string; source: string; type: string }>;
}

/**
 * Allure step structure
 */
interface AllureStep {
  name: string;
  status: AllureStatus;
  statusDetails?: {
    message?: string;
    trace?: string;
  };
  start: number;
  stop: number;
  steps?: AllureStep[];
  attachments?: Array<{ name: string; source: string; type: string }>;
}

/**
 * ReportPortal result converter
 */
export class ReportPortalConverter {
  /**
   * Convert validation result to ReportPortal test item
   */
  convertValidationResult(
    result: ValidationResult,
    launchUuid: string,
    parentUuid?: string
  ): ReportPortalTestItem {
    const startTime = Date.now();
    const endTime = startTime + 100; // Validation is instant

    const status = this.mapValidationStatus(result);
    const description = this.buildValidationDescription(result);
    const attributes = this.buildValidationAttributes(result);

    return {
      name: this.getTestFileName(result.filePath),
      description,
      type: 'test',
      startTime,
      endTime,
      status,
      launchUuid,
      parentId: parentUuid,
      attributes,
      codeRef: result.filePath,
      hasStats: true,
    };
  }

  /**
   * Convert validation report to ReportPortal test suite
   */
  convertValidationReport(
    report: ValidationReport,
    launchUuid: string
  ): ReportPortalTestItem {
    const startTime = typeof report.timestamp === 'number' ? report.timestamp : Date.now();
    const endTime = startTime + 1000;

    const status: ReportPortalStatus =
      report.summary.invalidTests === 0 ? 'passed' : 'failed';

    const description = this.buildReportDescription(report);
    const attributes = this.buildReportAttributes(report);

    return {
      name: 'Validation Suite',
      description,
      type: 'suite',
      startTime,
      endTime,
      status,
      launchUuid,
      attributes,
      hasStats: true,
    };
  }

  /**
   * Convert validation issues to ReportPortal logs
   */
  convertValidationIssues(
    issues: ValidationIssue[],
    itemUuid: string,
    launchUuid: string
  ): ReportPortalLog[] {
    return issues.map((issue, index) => {
      const level = this.mapSeverityToLogLevel(issue.severity);
      const message = this.formatIssueMessage(issue);

      return {
        itemUuid,
        time: Date.now() + index, // Slightly offset to preserve order
        message,
        level,
      };
    });
  }

  /**
   * Convert Allure result to ReportPortal test item
   */
  convertAllureResult(
    result: AllureResult,
    launchUuid: string,
    parentUuid?: string
  ): ReportPortalTestItem {
    const status = this.mapAllureStatus(result.status);
    const description = result.description || '';
    const attributes = this.buildAllureAttributes(result);
    const parameters = this.buildAllureParameters(result);

    return {
      name: result.name,
      description,
      type: 'test',
      startTime: result.start,
      endTime: result.stop,
      status,
      launchUuid,
      parentId: parentUuid,
      attributes,
      codeRef: result.fullName,
      parameters,
      hasStats: true,
    };
  }

  /**
   * Convert Allure step to ReportPortal test item
   */
  convertAllureStep(
    step: AllureStep,
    launchUuid: string,
    parentUuid: string,
    index: number
  ): ReportPortalTestItem {
    const status = this.mapAllureStatus(step.status);

    return {
      name: step.name,
      type: 'step',
      startTime: step.start,
      endTime: step.stop,
      status,
      launchUuid,
      parentId: parentUuid,
      uniqueId: `${parentUuid}-step-${index}`,
      hasStats: false,
    };
  }

  /**
   * Convert Allure status details to ReportPortal log
   */
  convertAllureStatusDetails(
    result: AllureResult | AllureStep,
    itemUuid: string
  ): ReportPortalLog | null {
    if (!result.statusDetails?.message && !result.statusDetails?.trace) {
      return null;
    }

    const level: ReportPortalLogLevel =
      result.status === 'failed' || result.status === 'broken' ? 'error' : 'info';

    let message = '';
    if (result.statusDetails.message) {
      message += result.statusDetails.message;
    }
    if (result.statusDetails.trace) {
      if (message) message += '\n\n';
      message += `Stack Trace:\n${result.statusDetails.trace}`;
    }

    return {
      itemUuid,
      time: result.stop || result.start,
      message,
      level,
    };
  }

  /**
   * Build validation status from result
   */
  private mapValidationStatus(result: ValidationResult): ReportPortalStatus {
    if (result.valid) {
      return 'passed';
    }

    const hasErrors = result.issues.some((issue) => issue.severity === 'error');
    return hasErrors ? 'failed' : 'skipped';
  }

  /**
   * Build validation description
   */
  private buildValidationDescription(result: ValidationResult): string {
    const lines: string[] = [`**File:** ${result.filePath}`];

    if (result.framework) {
      lines.push(`**Framework:** ${result.framework}`);
    }

    if (result.metrics) {
      const metrics = result.metrics;
      lines.push(
        `**Lines of Code:** ${metrics.linesOfCode}`,
        `**Test Cases:** ${metrics.testCaseCount || 0}`,
        `**Assertions:** ${metrics.assertionCount || 0}`
      );
    }

    if (result.issues.length > 0) {
      const errors = result.issues.filter((i) => i.severity === 'error').length;
      const warnings = result.issues.filter((i) => i.severity === 'warning').length;
      lines.push(
        `\n**Issues Found:** ${result.issues.length}`,
        `- Errors: ${errors}`,
        `- Warnings: ${warnings}`
      );
    }

    return lines.join('\n');
  }

  /**
   * Build validation attributes
   */
  // eslint-disable-next-line sonarjs/cognitive-complexity
  private buildValidationAttributes(result: ValidationResult): ReportPortalAttribute[] {
    const attributes: ReportPortalAttribute[] = [];

    if (result.framework) {
      attributes.push({ key: 'framework', value: result.framework });
    }

    if (result.testType) {
      attributes.push({ key: 'test-type', value: result.testType });
    }

    if (result.valid) {
      attributes.push({ value: 'validated' });
    }

    return attributes;
  }

  /**
   * Build report description
   */
  private buildReportDescription(report: ValidationReport): string {
    const lines: string[] = [
      `**Total Tests:** ${report.summary.totalTests}`,
      `**Valid Tests:** ${report.summary.validTests}`,
      `**Invalid Tests:** ${report.summary.invalidTests}`,
      `**Total Issues:** ${report.summary.totalIssues}`,
    ];

    if (report.quality) {
      lines.push(
        `\n**Quality Assessment:**`,
        `- Score: ${report.quality.score}/100`,
        `- Grade: ${report.quality.grade}`
      );
    }

    return lines.join('\n');
  }

  /**
   * Build report attributes
   */
  private buildReportAttributes(report: ValidationReport): ReportPortalAttribute[] {
    const attributes: ReportPortalAttribute[] = [
      { key: 'type', value: 'validation-report' },
      { key: 'total-tests', value: report.summary.totalTests.toString() },
      { key: 'valid-tests', value: report.summary.validTests.toString() },
      { key: 'invalid-tests', value: report.summary.invalidTests.toString() },
    ];

    if (report.quality) {
      attributes.push({ key: 'quality-grade', value: report.quality.grade });
    }

    return attributes;
  }

  /**
   * Map validation severity to log level
   */
  private mapSeverityToLogLevel(
    severity: 'error' | 'warning' | 'info' | 'suggestion'
  ): ReportPortalLogLevel {
    switch (severity) {
      case 'error':
        return 'error';
      case 'warning':
        return 'warn';
      case 'info':
        return 'info';
      case 'suggestion':
        return 'debug';
    }
  }

  /**
   * Format validation issue as message
   */
  private formatIssueMessage(issue: ValidationIssue): string {
    const parts: string[] = [`[${issue.severity.toUpperCase()}] ${issue.message}`];

    if (issue.ruleId) {
      parts.push(`Rule: ${issue.ruleId}`);
    }

    if (issue.line !== undefined) {
      const location = issue.column ? `Line: ${issue.line}, Column: ${issue.column}` : `Line: ${issue.line}`;
      parts.push(location);
    }

    if (issue.snippet) {
      parts.push(`\nCode:\n${issue.snippet}`);
    }

    if (issue.suggestion) {
      parts.push(`\nSuggestion: ${issue.suggestion}`);
    }

    return parts.join('\n');
  }

  /**
   * Map Allure status to ReportPortal status
   */
  private mapAllureStatus(
    status: 'passed' | 'failed' | 'broken' | 'skipped'
  ): ReportPortalStatus {
    switch (status) {
      case 'passed':
        return 'passed';
      case 'failed':
      case 'broken':
        return 'failed';
      case 'skipped':
        return 'skipped';
    }
  }

  /**
   * Build Allure attributes
   */
  private buildAllureAttributes(result: AllureResult): ReportPortalAttribute[] {
    if (!result.labels) {
      return [];
    }

    return result.labels
      .filter((label) =>
        ['feature', 'story', 'epic', 'severity', 'tag'].includes(label.name)
      )
      .map((label) => ({
        key: label.name,
        value: label.value,
      }));
  }

  /**
   * Build Allure parameters
   */
  private buildAllureParameters(result: AllureResult): ReportPortalParameter[] {
    if (!result.parameters) {
      return [];
    }

    return result.parameters.map((param) => ({
      key: param.name,
      value: param.value,
    }));
  }

  /**
   * Get test file name from path
   */
  private getTestFileName(filePath: string): string {
    return filePath.split(/[\\/]/).pop() || filePath;
  }
}
