import { test } from '@playwright/test';

/**
 * Invalid test - missing assertions
 */
test.describe('Invalid test example', () => {
  test('should do something but has no assertions', async ({ page }) => {
    await page.goto('http://localhost:3000');
    await page.click('#button');
    // TODO: Add assertions here
  });

  test('missing closing brace', async ({ page }) => {
    await page.goto('/test')
    console.log('Debug statement that should be removed');
  }
});
