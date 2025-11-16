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
        
        // Extract labels from test names
        testCase: ({ testCase }) => {
          const fullName = testCase.fullName || '';
          
          // Extract feature (maps to Allure Feature in Behaviors)
          const featureMatch = fullName.match(/\[feature:([\w\s-]+)\]/);
          if (featureMatch) {
            testCase.addLabel('feature', featureMatch[1]);
          } else {
            testCase.addLabel('feature', 'Frontend Service');
          }
          
          // Extract epic (maps to Allure Story in Behaviors)
          const epicMatch = fullName.match(/\[epic:([\w\s-]+)\]/);
          if (epicMatch) {
            testCase.addLabel('story', epicMatch[1]);
          }
          
          // Extract story ID
          const storyMatch = fullName.match(/\[story:([\w.-]+)\]/);
          if (storyMatch) {
            testCase.addLabel('story', storyMatch[1]);
          }
          
          // Extract other metadata
          const serviceMatch = fullName.match(/\[service:([\w-]+)\]/);
          if (serviceMatch) {
            testCase.addLabel('service', serviceMatch[1]);
          }
          
          const testTypeMatch = fullName.match(/\[testType:([\w-]+)\]/);
          if (testTypeMatch) {
            testCase.addLabel('testType', testTypeMatch[1]);
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
