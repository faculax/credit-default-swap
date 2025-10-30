import { defineConfig, devices } from '@playwright/test';

// Environment variables (override in CI):
// BASE_URL - points to running frontend
// AUTH_USER / AUTH_PASS - credentials for login helper
// SEED - deterministic seed for data generation

const baseURL = process.env.BASE_URL || 'http://localhost:3000';

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 5_000 },
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['list']] : 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ],
  // Tag filtering example: run: npx playwright test --grep "@smoke"
});
