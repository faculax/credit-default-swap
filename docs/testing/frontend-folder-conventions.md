# Frontend Test Folder Conventions

**Story**: `UTS-2.2` — Frontend Test Folder and Naming Conventions  
**Status**: ✅ Implemented  
**Last Updated**: 2024

---

## 📋 Overview

This document defines the **standard test folder structure** for all React/TypeScript frontend code in the CDS Platform. Organizing tests by type (unit, integration, e2e) ensures:

- Clear separation of fast unit tests vs. slower integration tests
- Ability to run different test suites independently via npm scripts
- Consistent structure across all frontend microservices
- Better IDE navigation and test discovery

---

## 🏗️ Folder Structure

All frontend tests are organized under `src/__tests__/` with the following hierarchy:

```
frontend/
└── src/
    ├── __tests__/
    │   ├── unit/              ← Pure unit tests (isolated components/functions)
    │   ├── integration/       ← Integration tests (component interactions)
    │   └── setup/             ← Test setup files and utilities
    ├── components/
    ├── services/
    ├── hooks/
    └── utils/
```

For end-to-end tests (optional):
```
frontend/
├── e2e/                       ← E2E tests (Cypress/Playwright)
│   ├── specs/
│   ├── fixtures/
│   └── support/
└── src/
```

### Test Type Definitions

| Test Type | Directory | Purpose | Characteristics |
|-----------|-----------|---------|-----------------|
| **Unit** | `src/__tests__/unit/` | Test individual components/functions in isolation | • No network calls<br>• Mocks for dependencies<br>• Fast (<100ms per test)<br>• Uses `@testing-library/react` |
| **Integration** | `src/__tests__/integration/` | Test component interactions and data flow | • May use MSW for API mocking<br>• Tests multiple components<br>• Slower (100ms-1s per test)<br>• Tests hooks, context, routing |
| **E2E** | `e2e/` | Full user journey tests in real browser | • Real browser (Cypress/Playwright)<br>• Tests entire app<br>• Slowest (seconds per test)<br>• Optional for most projects |

---

## 📦 File Naming Conventions

Tests should follow these naming patterns:

### Unit Tests
```
src/__tests__/unit/
├── components/
│   ├── RegressionStatusBadge.test.tsx      ← Component unit test
│   └── TradeCard.test.tsx
├── utils/
│   ├── dateUtils.test.ts                   ← Utility function test
│   └── formatters.test.ts
└── hooks/
    └── useTradeData.test.ts                ← Custom hook test
```

**Naming Rules:**
- File: `{ComponentName}.test.tsx` or `{fileName}.test.ts`
- Test suite: `describe('{ComponentName}')`
- Tests use story IDs: `withStoryId('UTS-X.Y', testType: 'unit')`

### Integration Tests
```
src/__tests__/integration/
├── features/
│   ├── TradeFlow.integration.test.tsx      ← Feature flow test
│   └── SettlementWorkflow.integration.test.tsx
└── pages/
    └── TradeDashboard.integration.test.tsx ← Page-level test
```

**Naming Rules:**
- File: `{FeatureName}.integration.test.tsx`
- Test suite: `describe('{Feature} Integration')`
- Tests use story IDs: `withStoryId('UTS-X.Y', testType: 'integration')`

### E2E Tests (Optional)
```
e2e/
└── specs/
    ├── trade-lifecycle.e2e.ts              ← E2E user journey
    └── credit-event-processing.e2e.ts
```

**Naming Rules:**
- File: `{feature-name}.e2e.ts`
- Test suite: `describe('{Feature} E2E')`

---

## 🚀 NPM Scripts

Tests are run via **npm scripts** with Jest configured for different test types:

### Run All Unit Tests (Default)
```bash
npm test
# or
npm run test:unit
```
Runs only `src/__tests__/unit/**/*.test.{ts,tsx}`

### Run Integration Tests
```bash
npm run test:integration
```
Runs `src/__tests__/integration/**/*.integration.test.{ts,tsx}`

### Run All Tests (Unit + Integration)
```bash
npm run test:all
```

### Run Tests in Watch Mode
```bash
npm run test:watch
```

### Generate Coverage Report
```bash
npm run test:coverage
```

### Run E2E Tests (if configured)
```bash
npm run test:e2e
```

---

## ✅ Test Structure Requirements

### Unit Tests
```typescript
// src/__tests__/unit/components/RegressionStatusBadge.test.tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import { describeStory, withStoryId } from '../../../utils/testHelpers';
import { RegressionStatusBadge } from '../../../components/risk/RegressionStatusBadge';

describeStory('RegressionStatusBadge', 'UTS-2.2', () => {
  it(withStoryId('should render passed status correctly', 'UTS-2.2', 'unit'), () => {
    // Arrange
    render(<RegressionStatusBadge status="passed" />);
    
    // Act
    const badge = screen.getByText(/passed/i);
    
    // Assert
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-green-100');
  });
});
```

### Integration Tests
```typescript
// src/__tests__/integration/features/TradeFlow.integration.test.tsx
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describeStory, withStoryId } from '../../../utils/testHelpers';
import { TradeCapturePage } from '../../../pages/TradeCapturePage';
import { setupServer } from 'msw/node';
import { rest } from 'msw';

const server = setupServer(
  rest.post('/api/trades', (req, res, ctx) => {
    return res(ctx.json({ id: 1, status: 'success' }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describeStory('Trade Flow Integration', 'UTS-2.2', () => {
  it(withStoryId('should submit trade successfully', 'UTS-2.2', 'integration'), async () => {
    // Arrange
    const user = userEvent.setup();
    render(<TradeCapturePage />);
    
    // Act
    await user.type(screen.getByLabelText(/notional/i), '1000000');
    await user.click(screen.getByRole('button', { name: /submit/i }));
    
    // Assert
    await waitFor(() => {
      expect(screen.getByText(/success/i)).toBeInTheDocument();
    });
  });
});
```

---

## 🔄 Migration Checklist

When migrating existing tests to this structure:

1. **Identify test type** — Is it a unit test (isolated) or integration test (multiple components)?
2. **Move file** — Relocate from `src/components/__tests__/` to `src/__tests__/unit/components/` or `integration/features/`
3. **Update imports** — Adjust relative import paths (e.g., `../../../components/...`)
4. **Update test name** — Add `.integration` suffix if it's an integration test
5. **Update story tags** — Use `withStoryId()` with correct `testType` parameter
6. **Run tests** — Verify with `npm test` (unit) or `npm run test:integration`

**See Also:** [Frontend Test Migration Checklist](./frontend-test-migration-checklist.md) for detailed step-by-step instructions.

---

## 🎯 Quick Reference

| Scenario | Test Type | Location | NPM Command |
|----------|-----------|----------|-------------|
| Testing `RegressionStatusBadge` component rendering | Unit | `__tests__/unit/components/RegressionStatusBadge.test.tsx` | `npm test` |
| Testing `useTradeData` hook with mocks | Unit | `__tests__/unit/hooks/useTradeData.test.ts` | `npm test` |
| Testing trade submission flow with API | Integration | `__tests__/integration/features/TradeFlow.integration.test.tsx` | `npm run test:integration` |
| Testing full user journey in browser | E2E | `e2e/specs/trade-lifecycle.e2e.ts` | `npm run test:e2e` |

---

## 🛠️ Jest Configuration

The Jest configuration is updated in `package.json` or `jest.config.js`:

```javascript
// jest.config.js
module.exports = {
  testMatch: [
    '<rootDir>/src/__tests__/**/*.test.{ts,tsx}',  // All tests
  ],
  testPathIgnorePatterns: [
    '/node_modules/',
    '/build/',
  ],
  collectCoverageFrom: [
    'src/**/*.{ts,tsx}',
    '!src/**/*.d.ts',
    '!src/__tests__/**',
  ],
};
```

**Profile-specific patterns** are handled via npm scripts using `--testPathPattern`:
- Unit: `--testPathPattern="__tests__/unit"`
- Integration: `--testPathPattern="__tests__/integration"`

---

## 📚 TypeScript Path Aliases

Update `tsconfig.json` to support cleaner imports:

```json
{
  "compilerOptions": {
    "baseUrl": "src",
    "paths": {
      "@components/*": ["components/*"],
      "@utils/*": ["utils/*"],
      "@hooks/*": ["hooks/*"],
      "@services/*": ["services/*"],
      "@tests/*": ["__tests__/*"]
    }
  }
}
```

This allows imports like:
```typescript
import { RegressionStatusBadge } from '@components/risk/RegressionStatusBadge';
import { withStoryId } from '@utils/testHelpers';
```

---

## 🛡️ Enforcement

- **CI Pipeline** — Runs unit tests on every commit, integration tests on PR merge
- **Label Validation** — Frontend validate-labels.mjs enforces correct `testType` labels
- **ESLint Rules** — (Future) Custom rule to prevent tests outside `__tests__/` folders

---

## 📚 Related Documentation

- [Unified Label Conventions](./unified-label-conventions.md) — Label schema and validation rules
- [Frontend Test Migration Checklist](./frontend-test-migration-checklist.md) — Step-by-step migration guide
- [Backend Folder Conventions](./backend-folder-conventions.md) — Backend test structure (Java)
- [Story Traceability Matrix](./story-traceability-matrix.md) — Linking tests to user stories

---

## ❓ FAQ

**Q: Can I have both unit and integration tests for the same component?**  
A: Yes! `__tests__/unit/components/FooComponent.test.tsx` (fast isolated tests) and `__tests__/integration/features/FooFlow.integration.test.tsx` (slower integrated tests) are both valid.

**Q: Where do I put test utilities and helpers?**  
A: Put shared test utilities in `src/__tests__/setup/` or `src/utils/testHelpers.ts` for test-specific helpers.

**Q: What if a test uses mocks but also renders multiple components?**  
A: If it tests interaction between multiple components or involves routing/context, it's an integration test.

**Q: Should E2E tests be in the same repo?**  
A: It depends on your architecture. For small projects, keep them in `e2e/`. For large projects, consider a separate `e2e-tests` repository.

**Q: How do I run a single test file?**  
A: Use `npm test -- RegressionStatusBadge.test.tsx` to run a specific file.

**Q: Can I use the old `__tests__` folders inside component directories?**  
A: No, all tests should be migrated to the centralized `src/__tests__/` structure for consistency.

---

**Story Completion**: This document fulfills UTS-2.2 requirements for defining frontend test folder and naming conventions.
