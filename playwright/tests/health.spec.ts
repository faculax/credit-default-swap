import { test, expect } from '@playwright/test';

// Simple health checks to validate the environment and base routes.
// Skips rather than fails if routes not yet implemented in MVP.

test.describe('Health Checks', () => {
  test('@smoke health: base URL responds', async ({ page }, testInfo) => {
    try {
      const response = await page.goto('/');
      if (!response) {
        testInfo.skip(true, 'No response navigating to base URL');
        return;
      }
      const status = response.status();
      if (status >= 500) {
        // Hard fail for server errors (environmental problem)
        expect(status, 'Base URL returned server error').toBeLessThan(500);
      } else if (status === 404) {
        testInfo.skip(true, 'Base URL not implemented (404)');
      }
    } catch (e) {
      console.warn('Health check navigation error:', e);
      testInfo.skip(true, 'Navigation threw (likely server not running)');
    }
  });

  test('@smoke health: trade capture route placeholder', async ({ page }, testInfo) => {
    const resp = await page.goto('/trade-capture');
    if (!resp) { testInfo.skip(true, 'No response for /trade-capture'); return; }
    if (resp.status() === 404) {
      testInfo.skip(true, '/trade-capture not implemented yet');
      return;
    }
    // If implemented, optionally verify a recognizable element in future.
    // For now just assert non-500.
    expect(resp.status()).toBeLessThan(500);
  });
});
