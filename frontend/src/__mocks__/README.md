# Frontend Mocks Registry

Centralized repository of API mocks and test fixtures for frontend React components.

## Structure

```
__mocks__/
├── README.md                    # This file
├── registry.json                # Mock catalog
├── api/                         # API response mocks
│   ├── trades/
│   │   ├── cds-trade-list.json
│   │   ├── cds-trade-detail.json
│   │   └── create-trade-response.json
│   ├── pricing/
│   │   ├── pricing-result.json
│   │   └── sensitivity-result.json
│   ├── market-data/
│   │   ├── curves.json
│   │   └── spreads.json
│   └── reference-data/
│       ├── issuers.json
│       ├── currencies.json
│       └── conventions.json
├── fixtures/                    # Test fixtures
│   ├── form-data/
│   │   ├── cds-form-valid.json
│   │   └── cds-form-invalid.json
│   └── user/
│       └── authenticated-user.json
└── handlers/                    # MSW request handlers
    ├── trades.ts
    ├── pricing.ts
    └── market-data.ts
```

## Mock Service Worker (MSW) Integration

We use MSW for API mocking in Jest tests:

```typescript
import { rest } from 'msw';
import { setupServer } from 'msw/node';
import cdsTradeList from './__mocks__/api/trades/cds-trade-list.json';

// Setup MSW server
const server = setupServer(
  rest.get('/api/trades', (req, res, ctx) => {
    return res(ctx.json(cdsTradeList));
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

## Mock Data Guidelines

### 1. Realistic API Responses

Mocks should mirror actual API responses:

```json
{
  "data": { ... },
  "metadata": {
    "timestamp": "2025-01-15T10:30:00Z",
    "requestId": "req-123"
  },
  "errors": null
}
```

### 2. Edge Cases

Include mocks for:
- Empty lists: `cds-trade-list-empty.json`
- Error responses: `create-trade-error-400.json`
- Loading states: `pricing-pending.json`
- Partial data: `trade-detail-minimal.json`

### 3. Naming Conventions

- Use kebab-case: `cds-trade-list.json`
- Include HTTP status: `create-trade-error-404.json`
- Be descriptive: `authenticated-user-trader.json`

## Usage in Tests

### Component Tests (React Testing Library)

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { rest } from 'msw';
import { server } from '../mocks/server';
import CDSTradeList from './CDSTradeList';
import cdsTradeListMock from '../__mocks__/api/trades/cds-trade-list.json';

test('displays CDS trade list', async () => {
  server.use(
    rest.get('/api/trades', (req, res, ctx) => {
      return res(ctx.json(cdsTradeListMock));
    })
  );
  
  render(<CDSTradeList />);
  
  await waitFor(() => {
    expect(screen.getByText('TEST-CDS-001')).toBeInTheDocument();
  });
});
```

### Direct Mock Import

```typescript
import { renderHook } from '@testing-library/react-hooks';
import { useCDSTrade } from './useCDSTrade';
import tradeDetailMock from '../__mocks__/api/trades/cds-trade-detail.json';

test('useCDSTrade hook returns trade data', () => {
  // Mock fetch
  global.fetch = jest.fn(() =>
    Promise.resolve({
      json: () => Promise.resolve(tradeDetailMock),
    })
  ) as jest.Mock;
  
  const { result } = renderHook(() => useCDSTrade('TEST-CDS-001'));
  
  expect(result.current.trade).toEqual(tradeDetailMock.data);
});
```

## MSW Handlers

Create reusable handlers in `handlers/`:

```typescript
// handlers/trades.ts
import { rest } from 'msw';
import cdsTradeList from '../api/trades/cds-trade-list.json';
import cdsTradeDetail from '../api/trades/cds-trade-detail.json';

export const tradeHandlers = [
  rest.get('/api/trades', (req, res, ctx) => {
    return res(ctx.json(cdsTradeList));
  }),
  
  rest.get('/api/trades/:tradeId', (req, res, ctx) => {
    const { tradeId } = req.params;
    return res(ctx.json(cdsTradeDetail));
  }),
  
  rest.post('/api/trades', async (req, res, ctx) => {
    const trade = await req.json();
    return res(
      ctx.status(201),
      ctx.json({ data: { ...trade, tradeId: 'TEST-NEW-001' } })
    );
  }),
];
```

## Registry Catalog

See `registry.json` for complete catalog with:
- Mock ID (path)
- Version
- Checksum
- API endpoint
- Related stories
- Dependencies

## Versioning

Mocks follow same versioning as backend datasets:

```json
{
  "version": "1.0.0",
  "checksum": "sha256:abc123...",
  "lastUpdated": "2025-11-18T00:00:00Z",
  "endpoint": "GET /api/trades",
  "data": { ... }
}
```

## Integration with Test Evidence Framework

The test-evidence-framework uses this registry to:
- Generate React component tests with mocks
- Track mock usage per story
- Validate API contract consistency
- Report mock coverage

## Best Practices

1. **Keep mocks in sync with backend**
   - Update mocks when API changes
   - Run contract tests to validate

2. **Use MSW for API mocking**
   - Intercepts requests at network level
   - Works with any HTTP library
   - Simulates real network behavior

3. **Organize by feature**
   - Group related mocks together
   - Mirror backend API structure
   - Use consistent naming

4. **Document edge cases**
   - Include error scenarios
   - Document expected behavior
   - Add comments in handlers

## Related

- [Backend Dataset Registry](../../backend/src/test/resources/datasets/README.md)
- [Test Evidence Framework](../../test-evidence-framework/README.md)
- [MSW Documentation](https://mswjs.io/)
- [Story 20.7: Test Data Registry](../../test-evidence-framework/epic_20_test_evidence_framework/story_20_7_test_data_and_mock_registry.md)
