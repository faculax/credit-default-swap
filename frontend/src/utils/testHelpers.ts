type TestType = 'unit' | 'integration' | 'e2e';
type Service = 'frontend' | 'backend';

interface StoryIdOptions {
  storyId: string;
  testType?: TestType;
  service?: Service;
  microservice?: string;
}

type TestFn = jest.ProvidesCallback | jest.DoneCallback | (() => void | Promise<void>);

/**
 * Wraps a Jest test with Allure story traceability labels.
 *
 * Usage:
 * ```typescript
 * withStoryId({ storyId: 'UTS-305', testType: 'unit', microservice: 'frontend' })(
 *   'renders trade form with required fields',
 *   () => {
 *     // test implementation
 *   }
 * );
 * ```
 */
export function withStoryId(options: StoryIdOptions) {
  const { storyId, testType = 'unit', service = 'frontend', microservice = '' } = options;

  return (testName: string, testFn: TestFn, timeout?: number) => {
    // Add metadata to test name for extraction by reporter
    const microserviceTag = microservice ? ` [microservice:${microservice}]` : '';
    const enrichedTestName = `${testName} [story:${storyId}] [testType:${testType}] [service:${service}]${microserviceTag}`;
    
    // eslint-disable-next-line no-undef
    return test(enrichedTestName, testFn as jest.ProvidesCallback, timeout);
  };
}

/**
 * Applies story metadata to a Jest describe block.
 * Useful for grouping multiple tests under the same story.
 */
export function describeStory(options: StoryIdOptions, suiteName: string, suiteFn: () => void) {
  const { storyId, testType = 'unit', service = 'frontend', microservice = '' } = options;
  
  // Add metadata to suite name for extraction by reporter
  const microserviceTag = microservice ? ` [microservice:${microservice}]` : '';
  const enrichedSuiteName = `${suiteName} [story:${storyId}] [testType:${testType}] [service:${service}]${microserviceTag}`;
  
  // eslint-disable-next-line no-undef
  describe(enrichedSuiteName, suiteFn);
}
