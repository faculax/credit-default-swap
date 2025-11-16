// CRACO (Create React App Configuration Override)
// Allows customizing CRA configuration without ejecting

module.exports = {
  jest: {
    configure: (jestConfig) => {
      // Add Allure reporter
      jestConfig.reporters = [
        'default',
        [
          'jest-allure2-reporter',
          {
            resultsDir: 'allure-results',
            overwrite: false,
            attachments: {
              subDir: 'attachments'
            },
            environmentInfo: {
              'Test Framework': 'Jest',
              'Test Type': 'Frontend Unit/Integration',
              'Node Version': process.version,
              'Platform': process.platform
            },
            testCase: {
              name: ({ testCase }) => testCase.fullName,
              labels: ({ testCase }) => {
                const labels = [];
                const fullName = testCase.fullName || '';
                
                // Extract story ID
                const storyMatch = fullName.match(/\[story:([\w.-]+)\]/);
                if (storyMatch) {
                  labels.push({ name: 'story', value: storyMatch[1] });
                }
                
                // Extract test type  
                const testTypeMatch = fullName.match(/\[testType:(\w+)\]/);
                if (testTypeMatch) {
                  labels.push({ name: 'testType', value: testTypeMatch[1] });
                } else {
                  labels.push({ name: 'testType', value: 'unit' });
                }
                
                // Extract service
                const serviceMatch = fullName.match(/\[service:(\w+)\]/);
                if (serviceMatch) {
                  labels.push({ name: 'service', value: serviceMatch[1] });
                } else {
                  labels.push({ name: 'service', value: 'frontend' });
                }
                
                // Extract microservice
                const microserviceMatch = fullName.match(/\[microservice:(\w+)\]/);
                if (microserviceMatch) {
                  labels.push({ name: 'microservice', value: microserviceMatch[1] });
                }
                
                // Extract severity
                const severityMatch = fullName.match(/\[severity:(\w+)\]/);
                if (severityMatch) {
                  labels.push({ name: 'severity', value: severityMatch[1] });
                } else {
                  labels.push({ name: 'severity', value: 'normal' });
                }
                
                // Extract epic
                const epicMatch = fullName.match(/\[epic:([\w-]+)\]/);
                if (epicMatch) {
                  labels.push({ name: 'epic', value: epicMatch[1] });
                }
                
                // Extract feature
                const featureMatch = fullName.match(/\[feature:([\w-]+)\]/);
                if (featureMatch) {
                  labels.push({ name: 'feature', value: featureMatch[1] });
                }
                
                return labels;
              }
            }
          }
        ]
      ];
      
      return jestConfig;
    }
  }
};
