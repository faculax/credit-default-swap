module.exports = {
  reporters: [
    'default',
    [
      'jest-allure2-reporter',
      {
        resultsDir: 'allure-results',
        testMapper: (testResult) => {
          // Extract story labels from test metadata
          return testResult;
        }
      }
    ]
  ]
};
