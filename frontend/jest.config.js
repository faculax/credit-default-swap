// Jest configuration for CDS Platform Frontend
// This file extends create-react-app's default Jest configuration

module.exports = {
  // Test file patterns
  testMatch: [
    '<rootDir>/src/__tests__/**/*.test.{ts,tsx}',
    '<rootDir>/src/__tests__/**/*.spec.{ts,tsx}',
  ],
  
  // Ignore patterns
  testPathIgnorePatterns: [
    '/node_modules/',
    '/build/',
    '/coverage/',
  ],
  
  // Coverage collection
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/__tests__/**',
    '!src/index.tsx',
    '!src/setupTests.ts',
  ],
  
  // Coverage thresholds
  coverageThresholds: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70,
    },
  },
  
  // Module name mapper for path aliases
  moduleNameMapper: {
    '^@components/(.*)$': '<rootDir>/src/components/$1',
    '^@utils/(.*)$': '<rootDir>/src/utils/$1',
    '^@hooks/(.*)$': '<rootDir>/src/hooks/$1',
    '^@services/(.*)$': '<rootDir>/src/services/$1',
    '^@types/(.*)$': '<rootDir>/src/types/$1',
    '^@tests/(.*)$': '<rootDir>/src/__tests__/$1',
  },
  
  // Setup files
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
  
  // Test environment
  testEnvironment: 'jsdom',
  
  // Reporters
  reporters: [
    'default',
    [
      'jest-allure2-reporter',
      {
        resultsDir: 'allure-results',
        overwrite: false,
        
        // Extract metadata from test names using tag patterns
        testCase: {
          labels: {
            story: ({ testCase }) => {
              const match = testCase.fullName.match(/\[story:([\w-]+(?:\.[\w-]+)?)\]/);
              return match ? [match[1]] : [];
            },
            testType: ({ testCase }) => {
              const match = testCase.fullName.match(/\[testType:([\w-]+)\]/);
              return match ? [match[1]] : ['unit'];
            },
            service: ({ testCase }) => {
              const match = testCase.fullName.match(/\[service:([\w-]+)\]/);
              return match ? [match[1]] : ['frontend'];
            },
            microservice: ({ testCase }) => {
              const match = testCase.fullName.match(/\[microservice:([\w-]+)\]/);
              return match ? [match[1]] : [];
            },
            severity: ({ testCase }) => {
              const match = testCase.fullName.match(/\[severity:([\w-]+)\]/);
              return match ? [match[1]] : ['normal'];
            },
            epic: ({ testCase }) => {
              const match = testCase.fullName.match(/\[epic:([\w-]+)\]/);
              return match ? [match[1]] : [];
            },
            feature: ({ testCase }) => {
              const match = testCase.fullName.match(/\[feature:([\w-]+)\]/);
              return match ? [match[1]] : [];
            }
          }
        },
        
        // Add environment information
        environment: () => ({
          'Test Framework': 'Jest',
          'Test Type': 'Frontend Unit/Integration',
          'Node Version': process.version,
          'Platform': process.platform
        })
      }
    ]
  ]
};
