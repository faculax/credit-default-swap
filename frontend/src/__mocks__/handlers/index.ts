/**
 * MSW (Mock Service Worker) Handlers Index
 * 
 * Exports all API handlers for use in tests
 * 
 * Usage in test setup:
 * 
 * ```typescript
 * import { setupServer } from 'msw/node';
 * import { handlers } from './__mocks__/handlers';
 * 
 * const server = setupServer(...handlers);
 * 
 * beforeAll(() => server.listen());
 * afterEach(() => server.resetHandlers());
 * afterAll(() => server.close());
 * ```
 */

import { tradesHandlers } from './trades';
import { pricingHandlers } from './pricing';
import { marketDataHandlers } from './market-data';

// Export all handlers
export const handlers = [
  ...tradesHandlers,
  ...pricingHandlers,
  ...marketDataHandlers
];

// Export individual handler groups for selective use
export { tradesHandlers, pricingHandlers, marketDataHandlers };
