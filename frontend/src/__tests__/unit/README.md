# Unit Tests

This directory contains **pure unit tests** that test individual React components, hooks, and utility functions in isolation.

## Characteristics

- **Isolated Testing** — Tests use mocked dependencies, no real API calls
- **Fast Execution** — Each test should run in under 100ms
- **Component Testing** — Uses `@testing-library/react` for component tests
- **High Coverage** — Unit tests should cover the majority of component logic

## Example

```typescript
// src/__tests__/unit/components/RegressionStatusBadge.test.tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import { describeStory, withStoryId } from '../../../utils/testHelpers';
import { RegressionStatusBadge } from '../../../components/risk/RegressionStatusBadge';

describeStory('RegressionStatusBadge', 'UTS-2.2', () => {
  it(withStoryId('should render passed status', 'UTS-2.2', 'unit'), () => {
    // Arrange
    render(<RegressionStatusBadge status="passed" />);
    
    // Act
    const badge = screen.getByText(/passed/i);
    
    // Assert
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-green-100');
  });

  it(withStoryId('should render failed status', 'UTS-2.2', 'unit'), () => {
    render(<RegressionStatusBadge status="failed" />);
    
    const badge = screen.getByText(/failed/i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveClass('bg-red-100');
  });
});
```

## Structure

Mirror the production source structure:

```
unit/
├── components/
│   ├── RegressionStatusBadge.test.tsx
│   └── TradeCard.test.tsx
├── hooks/
│   └── useTradeData.test.ts
└── utils/
    ├── dateUtils.test.ts
    └── formatters.test.ts
```

## Running Unit Tests

```bash
# Run all unit tests (default)
npm test

# Explicitly specify unit tests
npm run test:unit

# Run specific test file
npm test -- RegressionStatusBadge.test.tsx

# Run in watch mode
npm run test:watch
```

## Best Practices

- **Arrange-Act-Assert**: Structure tests with clear AAA pattern
- **Descriptive Names**: Test names should describe the expected behavior
- **Mock External Dependencies**: Use `jest.mock()` for modules, MSW for APIs
- **Test User Interactions**: Use `@testing-library/user-event` for realistic interactions
- **Avoid Implementation Details**: Test behavior, not implementation

---

**See Also**: [Frontend Folder Conventions](../../../../../docs/testing/frontend-folder-conventions.md)
