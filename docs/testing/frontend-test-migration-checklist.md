# Frontend Test Migration Checklist

## Overview

This checklist guides the migration of existing frontend tests from the legacy structure (`components/__tests__/`) to the new standardized structure (`src/__tests__/{unit,integration}/`).

---

## Pre-Migration Assessment

### 1. Identify All Test Files

```powershell
# Find all existing test files
Get-ChildItem -Path "frontend/src" -Recurse -Include "*.test.tsx","*.test.ts","*.spec.tsx","*.spec.ts" | Select-Object FullName
```

### 2. Categorize Tests by Type

For each test file, determine:
- **Unit Test**: Tests a single component/function in isolation
- **Integration Test**: Tests multiple components or API interactions
- **E2E Test**: Tests full user workflows (rare in Jest, usually Playwright)

### 3. Document Current Coverage

```powershell
# Generate coverage report
cd frontend
npm run test:coverage
```

Save baseline coverage metrics before migration.

---

## Migration Steps

### Step 1: Create New Test File Structure

#### 1.1 Determine New Location

| Old Location | Test Type | New Location |
|-------------|-----------|-------------|
| `components/__tests__/RegressionStatusBadge.test.tsx` | Unit | `__tests__/unit/components/RegressionStatusBadge.test.tsx` |
| `components/risk/__tests__/PortfolioSummary.test.tsx` | Unit | `__tests__/unit/components/PortfolioSummary.test.tsx` |
| `components/__tests__/CreditEventWorkflow.test.tsx` | Integration | `__tests__/integration/pages/CreditEventWorkflow.test.tsx` |
| `utils/__tests__/dateUtils.test.ts` | Unit | `__tests__/unit/utils/dateUtils.test.ts` |

#### 1.2 Create Directory (if needed)

```powershell
# Example for component test
New-Item -ItemType Directory -Force -Path "frontend/src/__tests__/unit/components"
```

### Step 2: Copy and Update Test File

#### 2.1 Copy Test File

```powershell
# Example
Copy-Item `
  "frontend/src/components/__tests__/RegressionStatusBadge.test.tsx" `
  "frontend/src/__tests__/unit/components/RegressionStatusBadge.test.tsx"
```

#### 2.2 Update Import Paths

**Old import paths (from `components/__tests__/`):**
```typescript
import RegressionStatusBadge from '../RegressionStatusBadge';
import { formatDate } from '../../utils/dateUtils';
import { withStoryId } from '../../utils/testHelpers';
```

**New import paths (from `__tests__/unit/components/`):**
```typescript
import RegressionStatusBadge from '../../../components/risk/RegressionStatusBadge';
import { formatDate } from '../../../utils/dateUtils';
import { withStoryId } from '../../../utils/testHelpers';
```

**Using Path Aliases (recommended):**
```typescript
import RegressionStatusBadge from '@components/risk/RegressionStatusBadge';
import { formatDate } from '@utils/dateUtils';
import { withStoryId } from '@utils/testHelpers';
```

> **Note**: Path aliases (`@components`, `@utils`, `@tests`) are configured in `jest.config.js`.

#### 2.3 Update Test Helper Usage

**Ensure correct usage of `withStoryId` and `describeStory`:**

```typescript
import { describeStory, withStoryId } from '@utils/testHelpers';

// Suite wrapper with story ID
describeStory(
  { storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' },
  'RegressionStatusBadge Unit Tests',
  () => {
    // Individual test with story ID (replaces `it`)
    withStoryId(
      { storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' }
    )('should render PASS status with green styling', () => {
      // Test implementation
    });
  }
);
```

**Common Mistake:**
```typescript
// ❌ WRONG - withStoryId returns void when called
it(withStoryId({ storyId: 'UTS-2.2', ... })('test name', () => {}));

// ✅ CORRECT - withStoryId replaces `it`
withStoryId({ storyId: 'UTS-2.2', ... })('test name', () => {});
```

#### 2.4 Verify Test Structure

Ensure tests follow Arrange-Act-Assert pattern:

```typescript
withStoryId({ storyId: 'UTS-X.X', ... })('should do something', () => {
  // Arrange - Setup test data and environment
  render(<Component prop="value" />);
  
  // Act - Perform action
  const element = screen.getByRole('button');
  fireEvent.click(element);
  
  // Assert - Verify outcome
  expect(element).toHaveClass('active');
});
```

### Step 3: Run Migrated Tests

#### 3.1 Run Individual Test File

```powershell
# Run specific test file
npm test -- __tests__/unit/components/RegressionStatusBadge.test.tsx
```

#### 3.2 Run Test Type

```powershell
# Run all unit tests
npm run test:unit

# Run all integration tests
npm run test:integration
```

#### 3.3 Fix Failing Tests

Common issues:
- **Import path errors**: Check relative paths or path aliases
- **Missing test utilities**: Ensure setup files are imported
- **Async issues**: Add `await waitFor()` for async operations
- **Mock issues**: Update mocks to match new structure

### Step 4: Update Test Configuration (if needed)

#### 4.1 Check jest.config.js

Ensure `testMatch` patterns include new locations:

```javascript
testMatch: [
  '<rootDir>/src/__tests__/**/*.test.{ts,tsx}',
  '<rootDir>/src/__tests__/**/*.spec.{ts,tsx}'
],
```

#### 4.2 Verify Path Aliases

```javascript
moduleNameMapper: {
  '^@components/(.*)$': '<rootDir>/src/components/$1',
  '^@utils/(.*)$': '<rootDir>/src/utils/$1',
  '^@tests/(.*)$': '<rootDir>/src/__tests__/$1'
},
```

### Step 5: Delete Old Test File

**Only after verifying new test passes:**

```powershell
# Delete old test file
Remove-Item "frontend/src/components/__tests__/RegressionStatusBadge.test.tsx"
```

### Step 6: Update Documentation

#### 6.1 Update Test Coverage Report

```powershell
npm run test:coverage
```

Verify coverage metrics are maintained or improved.

#### 6.2 Update README Files

If test demonstrates specific patterns, consider adding to relevant README:
- `__tests__/unit/README.md`
- `__tests__/integration/README.md`
- `__tests__/setup/README.md`

---

## Migration Tracking

### Test Migration Status

| Test File | Type | Old Location | New Location | Status | Notes |
|-----------|------|--------------|--------------|--------|-------|
| RegressionStatusBadge.test.tsx | Unit | components/__tests__/ | __tests__/unit/components/ | ✅ Complete | Example test |
| ... | ... | ... | ... | ⏳ Pending | ... |

### Coverage Metrics

| Metric | Before Migration | After Migration | Change |
|--------|------------------|-----------------|--------|
| Overall Coverage | X% | Y% | +/- Z% |
| Components | X% | Y% | +/- Z% |
| Utils | X% | Y% | +/- Z% |

---

## Integration Test Specific Migration

### Identifying Integration Tests

Integration tests typically:
- Render multiple components together
- Mock API calls or services
- Test data flow between components
- Use `waitFor` for async operations

### Additional Steps for Integration Tests

#### 1. Move to `__tests__/integration/`

```powershell
New-Item -ItemType Directory -Force -Path "frontend/src/__tests__/integration/pages"
Copy-Item `
  "frontend/src/components/__tests__/CreditEventWorkflow.test.tsx" `
  "frontend/src/__tests__/integration/pages/CreditEventWorkflow.test.tsx"
```

#### 2. Update API Mocking

If using MSW (Mock Service Worker), ensure handlers are in `__tests__/setup/`:

```typescript
// __tests__/setup/mswHandlers.ts
import { rest } from 'msw';

export const handlers = [
  rest.get('/api/credit-events', (req, res, ctx) => {
    return res(ctx.json([{ id: 1, type: 'DEFAULT' }]));
  }),
];
```

```typescript
// Integration test
import { setupServer } from 'msw/node';
import { handlers } from '@tests/setup/mswHandlers';

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

#### 3. Verify Async Testing

Ensure proper use of `waitFor` and `async/await`:

```typescript
withStoryId({ storyId: 'UTS-X.X', testType: 'integration', ... })(
  'should load and display data',
  async () => {
    render(<Component />);
    
    await waitFor(() => {
      expect(screen.getByText(/loaded/i)).toBeInTheDocument();
    });
  }
);
```

---

## Common Migration Issues

### Issue 1: Import Path Errors

**Problem:**
```
Module not found: Can't resolve '../../../components/risk/RegressionStatusBadge'
```

**Solution:**
- Count directory levels correctly: `__tests__/unit/components/` needs `../../../` to reach `src/`
- Or use path aliases: `@components/risk/RegressionStatusBadge`

### Issue 2: Test Helper Function Signature

**Problem:**
```
Argument of type 'void' is not assignable to parameter of type 'string'
```

**Solution:**
- Use `withStoryId(options)('test name', testFn)` directly
- Do NOT wrap in `it()`: `it(withStoryId(...)(...))` is incorrect

### Issue 3: Missing Test Setup

**Problem:**
```
ReferenceError: expect is not defined
```

**Solution:**
- Ensure `jest.config.js` has `setupFilesAfterEnv: ['<rootDir>/src/__tests__/setup/setupTests.ts']`
- Verify `setupTests.ts` imports `@testing-library/jest-dom`

### Issue 4: Path Alias Not Resolved

**Problem:**
```
Cannot find module '@components/RegressionStatusBadge'
```

**Solution:**
- Verify `jest.config.js` has `moduleNameMapper` configured
- Check that paths match: `@components/(.*)$` → `<rootDir>/src/components/$1`

### Issue 5: Tests Run Twice

**Problem:**
Tests run for both old and new locations.

**Solution:**
- Add old location to `testPathIgnorePatterns` in `jest.config.js`:
  ```javascript
  testPathIgnorePatterns: [
    '/node_modules/',
    '/build/',
    '<rootDir>/src/components/__tests__/' // Ignore old location
  ],
  ```

---

## Validation Checklist

After migration, verify:

- [ ] All tests pass: `npm run test:all`
- [ ] Coverage maintained: `npm run test:coverage`
- [ ] No duplicate tests (old and new locations)
- [ ] Path aliases work correctly
- [ ] Story IDs are present and correct
- [ ] Test names follow convention: `should <expected behavior>`
- [ ] Arrange-Act-Assert pattern used
- [ ] Async tests use `waitFor` appropriately
- [ ] Old test files deleted
- [ ] Documentation updated

---

## Rollback Procedure

If migration causes issues:

### 1. Restore Old Test File

```powershell
git restore frontend/src/components/__tests__/RegressionStatusBadge.test.tsx
```

### 2. Delete New Test File

```powershell
Remove-Item "frontend/src/__tests__/unit/components/RegressionStatusBadge.test.tsx"
```

### 3. Remove from Ignore Patterns

If added to `testPathIgnorePatterns`, remove the entry.

### 4. Investigate Issue

- Check error messages carefully
- Verify import paths
- Ensure test helpers are used correctly
- Run tests in isolation: `npm test -- path/to/file.test.tsx`

---

## Post-Migration Tasks

### 1. Clean Up Old Directories

After all tests migrated:

```powershell
# Remove old __tests__ directories (if empty)
Remove-Item -Path "frontend/src/components/__tests__" -Recurse -Force
```

### 2. Update CI/CD Pipeline

Ensure CI runs correct test commands:

```yaml
# Example GitHub Actions
- name: Run Unit Tests
  run: npm run test:unit
  
- name: Run Integration Tests
  run: npm run test:integration

- name: Generate Coverage
  run: npm run test:coverage
```

### 3. Update Team Documentation

- Notify team of new test structure
- Update onboarding documentation
- Add to code review checklist

### 4. Generate Allure Report

```powershell
npm run test:allure
```

Verify story IDs appear in Allure report.

---

## Tips for Efficient Migration

### Batch Migration by Directory

Migrate all tests in a directory together:

```powershell
# Migrate all component tests
$tests = Get-ChildItem "frontend/src/components" -Recurse -Include "*.test.tsx"
foreach ($test in $tests) {
  # Copy, update, verify, delete
}
```

### Use Script for Import Path Updates

Create a PowerShell script to automate path updates:

```powershell
# update-imports.ps1
param($filePath)

$content = Get-Content $filePath -Raw
$content = $content -replace "from '\.\./", "from '../../../components/"
$content = $content -replace "from '\.\./\.\./utils/", "from '../../../utils/"
Set-Content $filePath $content
```

### Migrate High-Value Tests First

Prioritize tests covering:
- Critical user workflows
- Complex business logic
- Bug-prone areas

### Leverage Git for Safety

Commit after each successful test migration:

```powershell
git add frontend/src/__tests__/unit/components/RegressionStatusBadge.test.tsx
git commit -m "Migrate RegressionStatusBadge unit test to new structure"
```

---

## Questions & Support

### Where should I put shared test utilities?

`__tests__/setup/` directory. Examples:
- `customRender.ts`: Custom render function with providers
- `mockData.ts`: Shared test fixtures
- `mswHandlers.ts`: MSW mock API handlers
- `testUtils.ts`: Helper functions for tests

### Can I use both `it` and `withStoryId`?

**No.** Use `withStoryId` directly, not wrapped in `it`:

```typescript
// ✅ Correct
withStoryId({ storyId: 'UTS-2.2', ... })('should work', () => {});

// ❌ Wrong
it(withStoryId({ storyId: 'UTS-2.2', ... })('should work', () => {}));
```

### What if a test fits multiple categories?

Choose based on primary purpose:
- **Unit**: Single component in isolation
- **Integration**: Multiple components or API calls
- **E2E**: Full user workflow (usually Playwright, not Jest)

When in doubt, lean toward **integration** if test has multiple moving parts.

### Should I migrate all tests at once?

**No.** Migrate incrementally:
1. Start with simple unit tests
2. Move to complex unit tests
3. Tackle integration tests
4. Verify all old tests are migrated
5. Delete old directories

This allows catching issues early and maintaining CI stability.

---

## Example Migration: Complete Workflow

### Before

```
frontend/src/
  components/
    risk/
      RegressionStatusBadge.tsx
      __tests__/
        RegressionStatusBadge.test.tsx
```

**Test file** (`components/risk/__tests__/RegressionStatusBadge.test.tsx`):
```typescript
import React from 'react';
import { render, screen } from '@testing-library/react';
import RegressionStatusBadge from '../RegressionStatusBadge';

describe('RegressionStatusBadge', () => {
  it('should render PASS status', () => {
    render(<RegressionStatusBadge status="PASS" />);
    expect(screen.getByText(/PASS/i)).toBeInTheDocument();
  });
});
```

### After

```
frontend/src/
  components/
    risk/
      RegressionStatusBadge.tsx
  __tests__/
    unit/
      components/
        RegressionStatusBadge.test.tsx
```

**Test file** (`__tests__/unit/components/RegressionStatusBadge.test.tsx`):
```typescript
import React from 'react';
import { render, screen } from '@testing-library/react';
import { describeStory, withStoryId } from '@utils/testHelpers';
import RegressionStatusBadge from '@components/risk/RegressionStatusBadge';

describeStory(
  { storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' },
  'RegressionStatusBadge Unit Tests',
  () => {
    withStoryId(
      { storyId: 'UTS-2.2', testType: 'unit', service: 'frontend', microservice: 'risk-ui' }
    )('should render PASS status with green styling', () => {
      // Arrange
      render(<RegressionStatusBadge status="PASS" />);
      
      // Act
      const badge = screen.getByText(/Regression: PASS/i);
      
      // Assert
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveClass('bg-green-600');
    });
  }
);
```

**Key Changes:**
1. ✅ Moved from `components/risk/__tests__/` to `__tests__/unit/components/`
2. ✅ Updated imports to use path aliases (`@components`, `@utils`)
3. ✅ Added `describeStory` wrapper with story ID metadata
4. ✅ Replaced `it` with `withStoryId` for traceability
5. ✅ Enhanced test to verify styling classes
6. ✅ Added Arrange-Act-Assert comments

---

## Conclusion

This migration establishes a scalable, maintainable test architecture for the frontend. By following this checklist systematically, you ensure:

- **Consistency**: All tests follow same structure
- **Traceability**: Story IDs link tests to requirements
- **Maintainability**: Clear separation of unit vs integration tests
- **Discoverability**: Predictable test locations

**Happy Testing! 🧪**
