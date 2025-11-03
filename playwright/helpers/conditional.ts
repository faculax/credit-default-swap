import { Locator, TestInfo } from '@playwright/test';

/**
 * Skip the current test if the provided locator is absent (count === 0).
 * Returns true if skipped so callers can early-return.
 */
export async function skipIfMissing(locator: Locator, reason: string, testInfo: TestInfo): Promise<boolean> {
  if (await locator.count() === 0) {
    testInfo.skip(true, reason);
    return true;
  }
  return false;
}

/**
 * Guard a set of required locators; if any is missing the test is skipped.
 */
export async function requireAll(locators: Array<{ locator: Locator; name: string }>, context: string, testInfo: TestInfo): Promise<boolean> {
  for (const { locator, name } of locators) {
    if (await locator.count() === 0) {
      testInfo.skip(true, `${context}: missing required element '${name}'`);
      return true;
    }
  }
  return false;
}
