/// <reference types="node" />
import { defineConfig, devices } from '@playwright/test';

// Environment variables (override in CI):
// BASE_URL - points to running frontend
// AUTH_USER / AUTH_PASS - credentials for login helper
// SEED - deterministic seed for data generation

const baseURL = process.env.BASE_URL || 'http://localhost:3000';

export default defineConfig({
  testDir: './tests',
  globalSetup: require.resolve('./global-setup'),
  timeout: 60_000,
  expect: { timeout: 5_000 },
  retries: process.env.CI ? 2 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['list']] : [ ['list'], ['html', { open: 'never' }] ],
  use: {
    baseURL,
    trace: 'on', // always capture for richer debugging (can toggle to 'retain-on-failure' later)
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } }
  ],
  // Tag filtering example: run: npx playwright test --grep "@smoke"
});
