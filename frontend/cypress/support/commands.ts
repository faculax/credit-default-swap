// ***********************************************
// Custom Cypress commands for the CDS Platform
// ***********************************************

/// <reference types="cypress" />

/**
 * Helper to tag E2E tests with story metadata
 * Usage: cy.tagTest({ storyId: 'UTS-2.2', severity: 'critical' })
 */
Cypress.Commands.add('tagTest', (options: {
  storyId?: string;
  testType?: string;
  service?: string;
  microservice?: string;
  severity?: string;
  epic?: string;
  feature?: string;
}) => {
  if (options.storyId) {
    cy.allure().label('story', options.storyId);
  }
  if (options.testType) {
    cy.allure().label('testType', options.testType);
  }
  if (options.service) {
    cy.allure().label('service', options.service);
  }
  if (options.microservice) {
    cy.allure().label('microservice', options.microservice);
  }
  if (options.severity) {
    cy.allure().severity(options.severity as any);
  }
  if (options.epic) {
    cy.allure().epic(options.epic);
  }
  if (options.feature) {
    cy.allure().feature(options.feature);
  }
});

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Tag test with story metadata for Allure reporting
       * @param options - Story metadata
       * @example cy.tagTest({ storyId: 'UTS-2.2', severity: 'critical' })
       */
      tagTest(options: {
        storyId?: string;
        testType?: string;
        service?: string;
        microservice?: string;
        severity?: string;
        epic?: string;
        feature?: string;
      }): Chainable<void>;
    }
  }
}

export {};
