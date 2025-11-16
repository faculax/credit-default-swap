# Frontend Allure Setup Guide

**Epic 04: Frontend Allure Integration**  
**Story 4.1: Configure Allure Adapter for Jest Unit Tests**  
**Version:** 1.0  
**Last Updated:** November 14, 2025

Complete guide for integrating Allure reporting with Jest tests in the React frontend.

---

## Table of Contents

1. [Overview](#overview)
2. [Dependencies](#dependencies)
3. [Configuration](#configuration)
4. [Usage](#usage)
5. [Running Tests](#running-tests)
6. [Generating Reports](#generating-reports)
7. [Troubleshooting](#troubleshooting)

---

## Overview

The frontend uses **jest-allure2-reporter** to integrate Jest tests with Allure reporting. Tests automatically generate Allure-compatible result files that can be merged with backend reports for unified quality dashboards.

**Key Features:**
- ✅ Automatic story ID extraction from test metadata
- ✅ Test type classification (unit, integration, e2e)
- ✅ Severity and priority labeling
- ✅ Service and microservice tagging
- ✅ Environment information capture

---

## Dependencies

### Installed Packages

```json
{
  "devDependencies": {
    "jest-allure2-reporter": "^2.2.8",
    "allure-js-commons": "^3.4.2",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/react": "^16.3.0",
    "@types/jest": "^30.0.0"
  }
}
```

### Installation

If packages are missing:

```bash
npm install --save-dev jest-allure2-reporter allure-js-commons
```

---

## Configuration

### Jest Configuration (`jest.config.js`)

The Jest configuration includes Allure reporter with metadata extraction:

```javascript
reporters: [
  'default',
  [
    'jest-allure2-reporter',
    {
      resultsDir: 'allure-results',
      overwrite: false,
      
      // Extract metadata from test names using tag patterns
      testCase: {
        labels: {
          story: ({ testCase }) => {
            const match = testCase.fullName.match(/\[story:([\w-]+(?:\.[\w-]+)?)\]/);
            return match ? [match[1]] : [];
          },
          testType: ({ testCase }) => {
            const match = testCase.fullName.match(/\[testType:([\w-]+)\]/);
            return match ? [match[1]] : ['unit'];
          },
          service: ({ testCase }) => {
            const match = testCase.fullName.match(/\[service:([\w-]+)\]/);
            return match ? [match[1]] : ['frontend'];
          }
          // ... more extractors
        }
      },
      
      // Add environment information
      environment: () => ({
        'Test Framework': 'Jest',
        'Test Type': 'Frontend Unit/Integration',
        'Node Version': process.version,
        'Platform': process.platform
      })
    }
  ]
]
```

**Key Settings:**
- `resultsDir`: Output directory for Allure JSON files (standardized as `allure-results`)
- `overwrite: false`: Append results instead of overwriting (important for multiple test runs)
- `testCase.labels`: Extract metadata from test names using regex patterns
- `environment`: Add build environment info to reports

---

## Usage

### Test Helper Functions

Use the helper functions from `src/utils/testHelpers.ts` to add Allure-compatible metadata:

#### 1. `withStoryId()` - Individual Test Labeling

```typescript
import { withStoryId } from '../utils/testHelpers';

withStoryId({ 
  storyId: 'UTS-2.2', 
  testType: 'unit', 
  service: 'frontend',
  microservice: 'risk-ui',
  severity: 'normal'
})('should render component correctly', () => {
  // Test implementation
  render(<MyComponent />);
  expect(screen.getByText('Hello')).toBeInTheDocument();
});
```

#### 2. `describeStory()` - Suite-Level Labeling

```typescript
import { describeStory, withStoryId } from '../utils/testHelpers';

describeStory(
  { 
    storyId: 'UTS-2.2', 
    testType: 'unit', 
    service: 'frontend',
    microservice: 'risk-ui'
  },
  'RegressionStatusBadge Unit Tests',
  () => {
    withStoryId({ 
      storyId: 'UTS-2.2', 
      testType: 'unit', 
      service: 'frontend',
      microservice: 'risk-ui'
    })('should render PASS status with green styling', () => {
      render(<RegressionStatusBadge status="PASS" />);
      const badge = screen.getByText(/Regression: PASS/i);
      expect(badge).toHaveClass('bg-green-600');
    });
    
    // More tests...
  }
);
```

### Metadata Options

```typescript
interface StoryIdOptions {
  storyId: string;           // Required - Story ID (e.g., 'UTS-2.2')
  testType?: TestType;       // Optional - 'unit' | 'integration' | 'contract' | 'e2e' | 'performance' | 'security'
  service?: Service;         // Optional - 'frontend' | 'backend' | 'gateway' | 'risk-engine'
  microservice?: string;     // Optional - Sub-component identifier
  severity?: Severity;       // Optional - 'trivial' | 'minor' | 'normal' | 'critical' | 'blocker'
  epic?: string;             // Optional - Epic label for grouping
  feature?: string;          // Optional - Feature label for categorization
}
```

---

## Running Tests

### NPM Scripts

```bash
# Run unit tests (default - generates Allure results)
npm test

# Run unit tests explicitly
npm run test:unit

# Run integration tests
npm run test:integration

# Run all tests
npm run test:all

# Run tests in watch mode (interactive)
npm run test:watch

# Run tests with coverage
npm run test:coverage
```

**Note:** All test commands automatically generate Allure results in `allure-results/` directory.

### Test Output

When tests run, you'll see:

```
PASS src/__tests__/unit/components/RegressionStatusBadge.test.tsx
  RegressionStatusBadge Unit Tests [story:UTS-2.2] [testType:unit] [service:frontend] [microservice:risk-ui]
    ✓ should render PASS status with green styling [story:UTS-2.2] [testType:unit] [service:frontend] [microservice:risk-ui] (45 ms)
    ✓ should render FAIL status with red styling [story:UTS-2.2] [testType:unit] [service:frontend] [microservice:risk-ui] (12 ms)

Test Suites: 1 passed, 1 total
Tests:       2 passed, 2 total

Allure results written to: allure-results/
```

### Allure Results Location

```
frontend/
├── allure-results/           # Generated by Jest
│   ├── {uuid}-result.json    # Test result (one per test)
│   ├── {uuid}-container.json # Test container (one per suite)
│   └── environment.json      # Environment metadata
└── allure-report/            # Generated HTML (after running allure:generate)
```

---

## Generating Reports

### Local Report Generation

#### Option 1: Quick Serve (Recommended)

```bash
# Run tests and open report in browser
npm run allure:report
```

This command:
1. Runs all tests (`npm run test:all`)
2. Generates HTML report from results
3. Opens report in default browser

#### Option 2: Manual Steps

```bash
# Step 1: Run tests
npm test

# Step 2: Generate HTML report
npm run allure:generate

# Step 3: Open report
npm run allure:serve
```

### Report Commands

```bash
# Clean previous results and reports
npm run allure:clean

# Generate HTML report from existing results
npm run allure:generate

# Generate and serve report (opens browser)
npm run allure:serve

# Full workflow: test → generate → serve
npm run allure:report
```

### View Report

After running `allure:serve`, the report opens at:
```
http://localhost:PORT/
```

Navigate through:
- **Overview**: Pass rate, duration, trends
- **Suites**: Tests organized by file/suite
- **Stories**: Tests filtered by story ID
- **Behaviors**: Tests grouped by epic/feature
- **Timeline**: Visual execution timeline

---

## Troubleshooting

### Issue 1: No Allure Results Generated

**Symptoms:**
```
Error: No Allure results found in allure-results/
```

**Causes:**
- Jest reporter not configured properly
- Tests didn't run
- Wrong output directory

**Solutions:**

**Check Jest config:**
```javascript
// jest.config.js - Verify reporters section exists
reporters: [
  'default',
  ['jest-allure2-reporter', { resultsDir: 'allure-results' }]
]
```

**Verify tests ran:**
```bash
# Run tests explicitly
npm run test:unit -- --watchAll=false

# Check for results
ls -la allure-results/
```

**Check results directory:**
```bash
# Should contain *-result.json files
find allure-results -name '*-result.json' | wc -l
```

### Issue 2: Labels Not Appearing in Report

**Symptoms:**
- Tests run, but story IDs don't show in Allure report
- Filters by story/testType return no results

**Causes:**
- Test names don't include metadata tags
- Regex patterns in jest.config.js don't match
- Test helpers not used

**Solutions:**

**Verify test uses helpers:**
```typescript
// ✅ Correct
withStoryId({ storyId: 'UTS-2.2', testType: 'unit' })('test name', () => {});

// ❌ Wrong
test('test name', () => {});  // No metadata
```

**Check test output:**
```
Test name should include tags:
✓ should render correctly [story:UTS-2.2] [testType:unit] [service:frontend]
```

**Inspect Allure JSON:**
```bash
# Check if labels exist in raw results
cat allure-results/*-result.json | jq '.labels'
```

### Issue 3: Allure CLI Not Found

**Symptoms:**
```
'allure' is not recognized as an internal or external command
```

**Cause:**
- Allure CLI not installed globally

**Solution:**

**Install Allure CLI:**

**macOS:**
```bash
brew install allure
```

**Windows:**
```powershell
scoop install allure
```

**Linux:**
```bash
# Download from GitHub releases
wget https://github.com/allure-framework/allure2/releases/download/2.25.0/allure-2.25.0.tgz
tar -zxf allure-2.25.0.tgz
sudo mv allure-2.25.0 /opt/
sudo ln -s /opt/allure-2.25.0/bin/allure /usr/local/bin/allure
```

**Verify installation:**
```bash
allure --version
# Should output: 2.25.0
```

### Issue 4: Tests Pass Locally But Fail in CI

**Symptoms:**
- Tests pass with `npm test`
- CI job fails with Allure errors

**Causes:**
- CI uses different Node version
- Missing dependencies in CI
- Environment variables not set

**Solutions:**

**Match Node versions:**
```yaml
# .github/workflows/frontend-tests.yml
- uses: actions/setup-node@v4
  with:
    node-version: '18'  # Match local version
```

**Install dependencies:**
```yaml
- run: npm ci  # Not npm install
```

**Check CI logs:**
```
Look for:
- "jest-allure2-reporter not found"
- "Cannot find module"
- Environment variable issues
```

### Issue 5: Coverage Reports Break With Allure

**Symptoms:**
```
Error: Coverage data not collected
```

**Cause:**
- Reporter conflicts with coverage collector

**Solution:**

**Use separate commands:**
```bash
# Coverage without Allure
npm run test:coverage

# Allure without coverage
npm test
```

**Or configure both:**
```javascript
// jest.config.js
reporters: [
  'default',
  ['jest-allure2-reporter', { ... }],
  ['jest-junit', { outputDirectory: 'coverage' }]
]
```

### Issue 6: Watch Mode Generates Duplicate Results

**Symptoms:**
- allure-results/ grows indefinitely in watch mode
- Duplicate test results in report

**Cause:**
- `overwrite: false` accumulates results

**Solution:**

**Clean before watch:**
```bash
npm run allure:clean
npm run test:watch
```

**Or use overwrite in dev:**
```javascript
// jest.config.js
['jest-allure2-reporter', {
  resultsDir: 'allure-results',
  overwrite: process.env.CI !== 'true'  // Overwrite locally, append in CI
}]
```

---

## CI Integration

### GitHub Actions Workflow

The frontend tests integrate with GitHub Actions:

```yaml
- name: Run Frontend Tests
  run: npm test

- name: Verify Allure Results
  run: |
    if [ ! -d "allure-results" ]; then
      echo "::error::No Allure results directory found"
      exit 1
    fi
    RESULT_COUNT=$(find allure-results -name '*-result.json' | wc -l)
    if [ "$RESULT_COUNT" -eq 0 ]; then
      echo "::error::No Allure results generated"
      exit 1
    fi
    echo "::notice::Found $RESULT_COUNT test results"

- name: Upload Allure Results
  uses: actions/upload-artifact@v4
  with:
    name: allure-results-frontend-${{ github.run_number }}
    path: frontend/allure-results/
    retention-days: 30
```

---

## Best Practices

### 1. Always Use Test Helpers

```typescript
// ✅ Good - Traceable
withStoryId({ storyId: 'UTS-2.2', testType: 'unit' })('test', () => {});

// ❌ Bad - No traceability
test('test', () => {});
```

### 2. Consistent Story IDs

```typescript
// ✅ Good - Valid format
storyId: 'UTS-2.2'
storyId: 'PROJ-123'
storyId: 'EPIC-5'

// ❌ Bad - Invalid format
storyId: 'story-2.2'
storyId: 'UTS2.2'
storyId: '123'
```

### 3. Clean Before Major Test Runs

```bash
# Before generating final reports
npm run allure:clean
npm run test:all
npm run allure:generate
```

### 4. Group Related Tests

```typescript
describeStory({ storyId: 'UTS-2.2', testType: 'unit' }, 'Component', () => {
  // All tests inherit story metadata
  withStoryId({ storyId: 'UTS-2.2', testType: 'unit' })('test 1', () => {});
  withStoryId({ storyId: 'UTS-2.2', testType: 'unit' })('test 2', () => {});
});
```

### 5. Use Appropriate Severity

```typescript
// Critical path
withStoryId({ storyId: 'UTS-2.2', severity: 'blocker' })('login must work', () => {});

// Nice-to-have
withStoryId({ storyId: 'UTS-2.2', severity: 'trivial' })('button color', () => {});
```

---

## Next Steps

- **Story 4.2**: ✅ Configure Allure for E2E tests (Cypress with @shelex/cypress-allure-plugin)
- **Story 4.3**: Enhance label decorators with more metadata
- **Story 4.4**: Harmonize npm scripts across services
- **Story 4.5**: Validate in CI with GitHub Actions

---

## E2E Testing with Cypress (Story 4.2)

### Cypress Setup

The project uses **Cypress** for end-to-end testing with **@shelex/cypress-allure-plugin** for Allure integration.

**Configuration**: `cypress.config.ts`

```typescript
export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:3000',
    setupNodeEvents(on, config) {
      allureWriter(on, config);
      return config;
    }
  },
  env: {
    allure: true,
    allureResultsPath: 'allure-results'
  }
});
```

### Running E2E Tests

```bash
# Run E2E tests headlessly (CI mode)
npm run test:e2e

# Open Cypress Test Runner (interactive)
npm run test:e2e:open

# Run E2E tests and generate Allure report
npm run allure:report:e2e
```

### E2E Test Structure

E2E tests use story metadata in test titles:

```typescript
describe('Homepage Navigation [story:UTS-4.2] [testType:e2e] [service:frontend]', () => {
  it('should load successfully [story:UTS-4.2] [severity:critical]', () => {
    cy.visit('/');
    cy.get('body').should('be.visible');
    cy.screenshot('homepage-loaded');
    cy.allure().step('Homepage loaded', () => {
      // Step logic
    });
  });
});
```

### E2E Test Metadata

Cypress extracts metadata from test titles automatically via `cypress/support/e2e.ts`:

- `[story:UTS-X.X]` → Story ID label
- `[testType:e2e]` → Test type (defaults to 'e2e')
- `[service:frontend]` → Service (defaults to 'frontend')
- `[severity:critical]` → Severity level
- `[epic:name]` → Epic grouping
- `[feature:name]` → Feature grouping

### Attachments

Cypress automatically attaches:
- **Screenshots**: Taken on test failure or via `cy.screenshot()`
- **Videos**: Full test execution videos (in CI mode)
- **Network Requests**: HTTP requests/responses (when `allureAttachRequests: true`)

Files are stored in:
- `cypress/screenshots/` (screenshots)
- `cypress/videos/` (videos)
- `allure-results/` (Allure JSON with attachment references)

### Custom Commands

Use the `cy.tagTest()` custom command for programmatic tagging:

```typescript
cy.tagTest({
  storyId: 'UTS-2.2',
  severity: 'blocker',
  epic: 'trade-capture'
});
```

---

## Next Steps

## Related Documentation

- [Backend Allure Setup](../../backend/docs/testing/backend-allure-setup.md)
- [Test Architecture Standards](../../unified-testing-stories/epic_02_test_architecture_standardization/README.md)
- [Story Traceability Guide](../../unified-testing-stories/epic_01_story_traceability_backbone/README.md)

---

## References

- [jest-allure2-reporter Documentation](https://github.com/wix-incubator/jest-allure2-reporter)
- [Allure Framework Docs](https://docs.qameta.io/allure/)
- [Jest Configuration](https://jestjs.io/docs/configuration)
- [Testing Library Best Practices](https://testing-library.com/docs/queries/about)
