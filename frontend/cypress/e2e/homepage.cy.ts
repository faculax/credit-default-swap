/**
 * Sample E2E Test - Homepage Navigation
 * Story: UTS-4.2 - Configure Allure Adapter for E2E Runner
 * 
 * This test demonstrates:
 * - Basic Cypress E2E test structure
 * - Story metadata tagging in test title
 * - Allure screenshot and video attachments
 */

describe('Homepage Navigation [story:UTS-4.2] [testType:e2e] [service:frontend]', () => {
  beforeEach(() => {
    // Visit the homepage before each test
    cy.visit('/');
  });

  it('should load the homepage successfully [story:UTS-4.2] [severity:critical]', () => {
    // Verify page title or main heading exists
    cy.get('body').should('be.visible');
    
    // Take a screenshot for Allure report
    cy.screenshot('homepage-loaded');
    
    // Add step to Allure report
    cy.allure().step('Homepage loaded successfully');
  });

  it('should display the main navigation menu [story:UTS-4.2] [severity:normal]', () => {
    // Check if main navigation elements exist
    cy.get('nav').should('exist');
    
    // Example: check for specific menu items if they exist
    // cy.contains('Trades').should('be.visible');
    // cy.contains('Risk').should('be.visible');
    
    cy.allure().step('Navigation menu is visible');
  });

  it('should handle responsive design [story:UTS-4.2] [severity:minor]', () => {
    // Test mobile viewport
    cy.viewport('iphone-x');
    cy.get('body').should('be.visible');
    cy.screenshot('mobile-view');
    
    // Test tablet viewport
    cy.viewport('ipad-2');
    cy.get('body').should('be.visible');
    cy.screenshot('tablet-view');
    
    // Test desktop viewport
    cy.viewport(1920, 1080);
    cy.get('body').should('be.visible');
    cy.screenshot('desktop-view');
    
    cy.allure().step('Responsive design verified across viewports');
  });
});

describe('Error Handling [story:UTS-4.2] [testType:e2e] [service:frontend] [feature:error-handling]', () => {
  it('should handle 404 pages gracefully [story:UTS-4.2] [severity:normal]', () => {
    // Visit a non-existent route
    cy.visit('/non-existent-page', { failOnStatusCode: false });
    
    // Verify error handling (adjust based on your app's 404 handling)
    cy.get('body').should('be.visible');
    cy.screenshot('404-page');
    
    cy.allure().step('404 page rendered without crash');
  });
});
