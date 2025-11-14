import { describe, it, expect, beforeEach } from '@jest/globals';
import { loadLabelSchema, validateAllureLabels, validateJestLabels } from '../validate-labels.mjs';

describe('validate-labels', () => {
  let schema;

  beforeEach(() => {
    schema = loadLabelSchema();
  });

  describe('loadLabelSchema', () => {
    it('should load schema with expected structure', () => {
      expect(schema).toBeDefined();
      expect(Array.isArray(schema.testTypes)).toBe(true);
      expect(Array.isArray(schema.services)).toBe(true);
      expect(Array.isArray(schema.microservices)).toBe(true);
    });

    it('should contain expected test types', () => {
      const testTypeNames = schema.testTypes.map(t => t.name);
      expect(testTypeNames).toContain('unit');
      expect(testTypeNames).toContain('integration');
      expect(testTypeNames).toContain('contract');
      expect(testTypeNames).toContain('e2e');
    });

    it('should contain expected services', () => {
      const serviceIds = schema.services.map(s => s.id);
      expect(serviceIds).toContain('backend');
      expect(serviceIds).toContain('frontend');
    });

    it('should contain expected microservices', () => {
      const microserviceIds = schema.microservices.map(m => m.id);
      expect(microserviceIds).toContain('cds-platform');
      expect(microserviceIds).toContain('risk-ui');
    });
  });

  describe('validateAllureLabels', () => {
    it('should pass validation for valid labels', () => {
      const allureResults = [
        {
          name: 'test case 1',
          labels: [
            { name: 'story', value: 'UTS-401' },
            { name: 'testType', value: 'unit' },
            { name: 'service', value: 'backend' },
            { name: 'microservice', value: 'cds-platform' }
          ]
        }
      ];

      const result = validateAllureLabels(allureResults, schema);
      
      expect(result.errors).toHaveLength(0);
      expect(result.stats.totalTests).toBe(1);
      expect(result.stats.invalidTestTypes).toBe(0);
    });

    it('should detect invalid testType', () => {
      const allureResults = [
        {
          name: 'test case 1',
          labels: [
            { name: 'story', value: 'UTS-401' },
            { name: 'testType', value: 'invalid-type' },
            { name: 'service', value: 'backend' }
          ]
        }
      ];

      const result = validateAllureLabels(allureResults, schema);
      
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.stats.invalidTestTypes).toBe(1);
      expect(result.errors[0].label).toBe('testType');
    });

    it('should detect invalid service', () => {
      const allureResults = [
        {
          name: 'test case 1',
          labels: [
            { name: 'story', value: 'UTS-401' },
            { name: 'testType', value: 'unit' },
            { name: 'service', value: 'invalid-service' }
          ]
        }
      ];

      const result = validateAllureLabels(allureResults, schema);
      
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.stats.invalidServices).toBe(1);
    });

    it('should detect invalid microservice', () => {
      const allureResults = [
        {
          name: 'test case 1',
          labels: [
            { name: 'story', value: 'UTS-401' },
            { name: 'testType', value: 'unit' },
            { name: 'service', value: 'backend' },
            { name: 'microservice', value: 'unknown-service' }
          ]
        }
      ];

      const result = validateAllureLabels(allureResults, schema);
      
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.stats.invalidMicroservices).toBe(1);
    });

    it('should warn about missing story IDs', () => {
      const allureResults = [
        {
          name: 'test case 1',
          labels: [
            { name: 'testType', value: 'unit' },
            { name: 'service', value: 'backend' }
          ]
        }
      ];

      const result = validateAllureLabels(allureResults, schema);
      
      expect(result.warnings.length).toBeGreaterThan(0);
      expect(result.stats.missingStoryIds).toBe(1);
    });
  });

  describe('validateJestLabels', () => {
    it('should pass validation for valid labels in test names', () => {
      const jestOutput = JSON.stringify({
        testResults: [
          {
            assertionResults: [
              {
                fullName: 'renders component [story:UTS-501] [testType:unit] [service:frontend] [microservice:risk-ui]',
                title: 'renders component'
              }
            ]
          }
        ]
      });

      const result = validateJestLabels(jestOutput, schema);
      
      expect(result.errors).toHaveLength(0);
      expect(result.stats.totalTests).toBe(1);
    });

    it('should detect invalid testType in Jest test names', () => {
      const jestOutput = JSON.stringify({
        testResults: [
          {
            assertionResults: [
              {
                fullName: 'test [story:UTS-501] [testType:invalid] [service:frontend]',
                title: 'test'
              }
            ]
          }
        ]
      });

      const result = validateJestLabels(jestOutput, schema);
      
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.stats.invalidTestTypes).toBe(1);
    });

    it('should detect invalid service in Jest test names', () => {
      const jestOutput = JSON.stringify({
        testResults: [
          {
            assertionResults: [
              {
                fullName: 'test [story:UTS-501] [testType:unit] [service:invalid]',
                title: 'test'
              }
            ]
          }
        ]
      });

      const result = validateJestLabels(jestOutput, schema);
      
      expect(result.errors.length).toBeGreaterThan(0);
      expect(result.stats.invalidServices).toBe(1);
    });

    it('should warn about missing story tags', () => {
      const jestOutput = JSON.stringify({
        testResults: [
          {
            assertionResults: [
              {
                fullName: 'test without story tag',
                title: 'test without story tag'
              }
            ]
          }
        ]
      });

      const result = validateJestLabels(jestOutput, schema);
      
      expect(result.warnings.length).toBeGreaterThan(0);
      expect(result.stats.missingStoryIds).toBe(1);
    });
  });
});
