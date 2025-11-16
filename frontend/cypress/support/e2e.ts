// ***********************************************************
// This support file is loaded before all tests
// ***********************************************************

import '@shelex/cypress-allure-plugin';
import './commands';

// Hide fetch/XHR requests from command log for cleaner output
const app = window.top;
if (app && !app.document.head.querySelector('[data-hide-command-log-request]')) {
  const style = app.document.createElement('style');
  style.innerHTML = '.command-name-request, .command-name-xhr { display: none }';
  style.setAttribute('data-hide-command-log-request', '');
  app.document.head.appendChild(style);
}

// Global before hook for Allure metadata
beforeEach(function () {
  // Extract story metadata from test title
  const testTitle = this.currentTest?.title || '';
  
  // Extract story ID
  const storyMatch = testTitle.match(/\[story:([\w.-]+)\]/);
  if (storyMatch) {
    cy.allure().label('story', storyMatch[1]);
  }
  
  // Extract test type (default to e2e)
  const testTypeMatch = testTitle.match(/\[testType:(\w+)\]/);
  cy.allure().label('testType', testTypeMatch ? testTypeMatch[1] : 'e2e');
  
  // Extract service (default to frontend)
  const serviceMatch = testTitle.match(/\[service:(\w+)\]/);
  cy.allure().label('service', serviceMatch ? serviceMatch[1] : 'frontend');
  
  // Extract microservice
  const microserviceMatch = testTitle.match(/\[microservice:([\w-]+)\]/);
  if (microserviceMatch) {
    cy.allure().label('microservice', microserviceMatch[1]);
  }
  
  // Extract severity (default to normal)
  const severityMatch = testTitle.match(/\[severity:(\w+)\]/);
  cy.allure().severity(severityMatch ? severityMatch[1] : 'normal');
  
  // Extract epic
  const epicMatch = testTitle.match(/\[epic:([\w-]+)\]/);
  if (epicMatch) {
    cy.allure().epic(epicMatch[1]);
  }
  
  // Extract feature
  const featureMatch = testTitle.match(/\[feature:([\w-]+)\]/);
  if (featureMatch) {
    cy.allure().feature(featureMatch[1]);
  }
  
  // Add environment info
  cy.allure()
    .parameter('Browser', Cypress.browser.name)
    .parameter('Browser Version', Cypress.browser.version)
    .parameter('Platform', Cypress.platform)
    .parameter('Viewport', `${Cypress.config('viewportWidth')}x${Cypress.config('viewportHeight')}`);
});
