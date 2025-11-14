# Test Setup

This directory contains shared test configuration, utilities, and setup files for both unit and integration tests.

## Contents

- **Test utilities**: Shared helper functions for tests
- **Mock data**: Reusable mock objects and fixtures
- **Custom matchers**: Jest custom matchers for domain-specific assertions
- **MSW handlers**: Shared MSW request handlers for integration tests

## Example Files

```
setup/
├── testUtils.ts               ← Custom render functions, providers
├── mockData.ts                ← Shared mock data (trades, events, etc.)
├── mswHandlers.ts             ← MSW request handlers
└── customMatchers.ts          ← Jest custom matchers
```

## Test Utilities Example

```typescript
// setup/testUtils.tsx
import React from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';

interface WrapperProps {
  children: React.ReactNode;
}

const AllTheProviders: React.FC<WrapperProps> = ({ children }) => {
  return (
    <BrowserRouter>
      {children}
    </BrowserRouter>
  );
};

export const renderWithProviders = (
  ui: React.ReactElement,
  options?: Omit<RenderOptions, 'wrapper'>
) => render(ui, { wrapper: AllTheProviders, ...options });

export * from '@testing-library/react';
```

## Mock Data Example

```typescript
// setup/mockData.ts
import { CDSTrade, CreditEvent } from '../../types';

export const mockTrades: CDSTrade[] = [
  {
    id: 1,
    referenceEntity: 'ACME Corp',
    notionalAmount: 1000000,
    spread: 150,
    tradeDate: '2024-01-01',
    maturityDate: '2029-01-01',
    status: 'ACTIVE',
  },
  {
    id: 2,
    referenceEntity: 'XYZ Inc',
    notionalAmount: 2000000,
    spread: 200,
    tradeDate: '2024-01-15',
    maturityDate: '2029-01-15',
    status: 'ACTIVE',
  },
];

export const mockCreditEvent: CreditEvent = {
  id: 1,
  tradeId: 1,
  eventType: 'BANKRUPTCY',
  eventDate: '2024-06-01',
  noticeDate: '2024-06-02',
  settlementMethod: 'CASH',
};
```

## MSW Handlers Example

```typescript
// setup/mswHandlers.ts
import { rest } from 'msw';
import { mockTrades, mockCreditEvent } from './mockData';

export const handlers = [
  rest.get('/api/trades', (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTrades));
  }),

  rest.get('/api/trades/:id', (req, res, ctx) => {
    const { id } = req.params;
    const trade = mockTrades.find(t => t.id === Number(id));
    
    if (!trade) {
      return res(ctx.status(404), ctx.json({ error: 'Trade not found' }));
    }
    
    return res(ctx.status(200), ctx.json(trade));
  }),

  rest.post('/api/trades', (req, res, ctx) => {
    return res(
      ctx.status(201),
      ctx.json({ ...req.body, id: mockTrades.length + 1 })
    );
  }),
];
```

## Usage in Tests

```typescript
// In integration test
import { renderWithProviders } from '../setup/testUtils';
import { mockTrades } from '../setup/mockData';
import { handlers } from '../setup/mswHandlers';
import { setupServer } from 'msw/node';

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

it('should render trade list', async () => {
  renderWithProviders(<TradeList />);
  
  await waitFor(() => {
    expect(screen.getByText('ACME Corp')).toBeInTheDocument();
  });
});
```

---

**See Also**: [Frontend Folder Conventions](../../../../../docs/testing/frontend-folder-conventions.md)
