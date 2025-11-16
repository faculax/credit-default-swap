import { defineConfig } from 'cypress';
import allureWriter from '@shelex/cypress-allure-plugin/writer';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts',
    
    setupNodeEvents(on, config) {
      // Allure reporter plugin
      allureWriter(on, config);
      
      return config;
    },
    
    // Video and screenshot settings
    video: true,
    screenshotOnRunFailure: true,
    videosFolder: 'cypress/videos',
    screenshotsFolder: 'cypress/screenshots',
    
    // Viewport settings
    viewportWidth: 1280,
    viewportHeight: 720,
    
    // Test execution settings
    defaultCommandTimeout: 10000,
    requestTimeout: 10000,
    responseTimeout: 10000,
    
    // Retry settings
    retries: {
      runMode: 2,
      openMode: 0
    }
  },
  
  env: {
    allure: true,
    allureResultsPath: 'allure-results',
    allureAttachRequests: true
  }
});
