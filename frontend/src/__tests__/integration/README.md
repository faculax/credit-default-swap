# Integration Tests

This directory contains **integration tests** that test interactions between multiple React components, hooks, and services.

## Characteristics

- **Component Integration** — Tests multiple components working together
- **API Mocking** — Uses MSW (Mock Service Worker) to mock HTTP requests
- **Context & Routing** — Tests React Context, routing, and state management
- **Slower Execution** — Tests may take 100ms-1s due to rendering complexity
- **Realistic Scenarios** — Tests simulate real user workflows

## Example

```typescript
// src/__tests__/integration/features/TradeFlow.integration.test.tsx
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describeStory, withStoryId } from '../../../utils/testHelpers';
import { TradeCapturePage } from '../../../pages/TradeCapturePage';
import { setupServer } from 'msw/node';
import { rest } from 'msw';

// Mock API server
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
    await user.type(screen.getByLabelText(/counterparty/i), 'Test Corp');
    await user.click(screen.getByRole('button', { name: /submit/i }));
    
    // Assert
    await waitFor(() => {
      expect(screen.getByText(/success/i)).toBeInTheDocument();
    });
  });

  it(withStoryId('should handle validation errors', 'UTS-2.2', 'integration'), async () => {
    const user = userEvent.setup();
    render(<TradeCapturePage />);
    
    // Submit without filling required fields
    await user.click(screen.getByRole('button', { name: /submit/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/required/i)).toBeInTheDocument();
    });
  });
});
```

## Structure

Organize by features or user workflows:

```
integration/
├── features/
│   ├── TradeFlow.integration.test.tsx
│   ├── SettlementWorkflow.integration.test.tsx
│   └── CreditEventProcessing.integration.test.tsx
└── pages/
    ├── TradeDashboard.integration.test.tsx
    └── RiskAnalytics.integration.test.tsx
```

## Running Integration Tests

```bash
# Run only integration tests
npm run test:integration

# Run all tests (unit + integration)
npm run test:all

# Run specific integration test
npm run test:integration -- TradeFlow.integration.test.tsx
```

## API Mocking with MSW

Integration tests should use **Mock Service Worker (MSW)** for realistic API mocking:

```typescript
import { setupServer } from 'msw/node';
import { rest } from 'msw';

const server = setupServer(
  rest.get('/api/trades', (req, res, ctx) => {
    return res(ctx.json([
      { id: 1, notional: 1000000 },
      { id: 2, notional: 2000000 },
    ]));
  }),
  
  rest.post('/api/trades', (req, res, ctx) => {
    return res(ctx.status(201), ctx.json({ id: 3 }));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

## Best Practices

- **Test User Workflows**: Focus on complete user journeys, not isolated components
- **Use MSW for APIs**: More realistic than `jest.mock()` for HTTP requests
- **Wait for Async Updates**: Always use `waitFor()` for async state changes
- **Test Error States**: Include tests for loading, error, and edge cases
- **Avoid Over-Mocking**: Mock only external dependencies (APIs), not internal components

---

**See Also**: [Frontend Folder Conventions](../../../../../docs/testing/frontend-folder-conventions.md)
