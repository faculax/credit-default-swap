# Frontend Test Story Tagging

## Overview

Frontend tests use the `withStoryId` and `describeStory` helper functions to apply story traceability labels to Jest test cases. These helpers embed story metadata directly in test names, enabling traceability reporting and coverage analysis.

## Helper Functions

### `withStoryId(options)(testName, testFn, timeout?)`

Wraps a single test case with story metadata.

**Parameters:**
- `options.storyId` (required): Story identifier (e.g., "UTS-401")
- `options.testType` (optional): Test type - "unit", "integration", "e2e" (default: "unit")
- `options.service` (optional): Service name (default: "frontend")
- `options.microservice` (optional): Microservice name for granular tracking

**Example:**
```typescript
import { withStoryId } from '../../../utils/testHelpers';
import { render } from '@testing-library/react';
import MyComponent from '../MyComponent';

withStoryId({
  storyId: 'UTS-401',
  testType: 'unit',
  service: 'frontend',
  microservice: 'trade-ui'
})('renders trade form correctly', () => {
  const { getByText } = render(<MyComponent />);
  expect(getByText('Submit')).toBeInTheDocument();
});
```

### `describeStory(options, suiteName, suiteFn)`

Applies story metadata to an entire test suite.

**Example:**
```typescript
import { describeStory } from '../../../utils/testHelpers';

describeStory(
  {
    storyId: 'UTS-403',
    testType: 'integration',
    microservice: 'settlement-ui'
  },
  'Cash Settlement Flow',
  () => {
    test('calculates settlement amount', () => {
      // test implementation
    });

    test('displays settlement details', () => {
      // test implementation
    });
  }
);
```

## Test Name Format

Story metadata is embedded in test names using a tag format:

```
[story:UTS-401] [testType:unit] [service:frontend] [microservice:trade-ui]
```

This format enables:
- Visual identification in test output
- Extraction for traceability reports
- Filtering tests by story ID
- Coverage analysis across epics

## Running Tests

Standard Jest execution:
```bash
npm test
```

Run specific story tests:
```bash
npm test -- --testNamePattern="UTS-401"
```

## Test Output Example

```
PASS  src/components/risk/__tests__/RegressionStatusBadge.test.tsx
  √ renders regression status badge [story:UTS-501] [testType:unit] [service:frontend] [microservice:risk-ui] (18 ms)
```

## Integration with Traceability Matrix

The story tags embedded in test names can be extracted and integrated into the traceability matrix using a custom parser script. This aligns with the backend `@StoryId` annotation approach and enables unified coverage reporting.

### Future Enhancement: Allure Reporter Integration

To generate full Allure reports with proper label extraction, consider:

1. **Ejecting from react-scripts** to enable custom Jest reporters
2. **Using jest-allure2-reporter** with custom test name parsing
3. **Creating a post-processor** to extract story tags from Jest JSON output into Allure format

## Best Practices

1. **Use canonical story IDs**: Follow the pattern defined in `unified-testing-config/story-catalog.json`
2. **Tag all tests**: Every test should have a story ID for complete traceability
3. **Match test types**: Use the same testType values as backend tests (unit, integration, e2e)
4. **Specify microservices**: Use granular microservice labels for multi-service frontends
5. **Validate story IDs**: Run `node scripts/validate-story-ids.mjs` to verify all referenced stories exist

## Related Documentation

- [Backend Story ID Annotation](./story-id-annotation.md)
- [Story Catalog](../../unified-testing-config/story-catalog.json)
- [Label Schema](../../unified-testing-config/label-schema.json)
- [Traceability Matrix Export](./traceability-matrix.md)
