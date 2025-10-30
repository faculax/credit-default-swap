import { Page } from '@playwright/test';

export interface Credentials {
  username: string;
  password: string;
}

export async function login(page: Page, creds?: Partial<Credentials>) {
  const username = creds?.username || process.env.AUTH_USER || 'demo';
  const password = creds?.password || process.env.AUTH_PASS || 'demo';
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await Promise.all([
    page.waitForURL(/dashboard|trade-capture/),
    page.getByRole('button', { name: /sign in/i }).click()
  ]);
}
