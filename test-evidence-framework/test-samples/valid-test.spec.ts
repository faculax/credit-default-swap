import { test, expect } from '@playwright/test';
import { allure } from 'allure-playwright';

/**
 * User creates CDS trade and views portfolio
 * Story ID: story-03-001
 * 
 * @epic CDS Trade Capture
 * @feature User Journeys
 * @story story-03-001
 */
test.describe('User creates CDS trade', () => {
  test.beforeEach(async ({ page }) => {
    await allure.epic('CDS Trade Capture');
    await allure.feature('User Journeys');
    await allure.story('story-03-001: User creates CDS trade');
    await allure.severity('critical');
  });

  test('should create trade successfully', async ({ page }) => {
    // Navigate to application
    await page.goto('/');
    
    // Wait for main content
    await page.waitForSelector('[data-testid="main-content"]');
    
    // Click create trade button
    await page.click('[data-testid="create-trade-btn"]');
    
    // Fill trade form
    await page.fill('[data-testid="notional-input"]', '1000000');
    await page.fill('[data-testid="reference-entity"]', 'ACME Corp');
    
    // Submit
    await page.click('[data-testid="submit-btn"]');
    
    // Verify success message
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="trade-id"]')).toContainText('TRADE-');
  });

  test('should handle validation errors', async ({ page }) => {
    await page.goto('/');
    await page.click('[data-testid="create-trade-btn"]');
    
    // Submit without filling required fields
    await page.click('[data-testid="submit-btn"]');
    
    // Verify error messages
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
    await expect(page.locator('[data-testid="error-message"]')).toContainText('required');
  });
});
