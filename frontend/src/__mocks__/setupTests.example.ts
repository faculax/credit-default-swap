/**
 * MSW Test Setup Example
 * 
 * This file shows how to set up MSW in your test files
 * Copy this pattern to your test setup (e.g., setupTests.ts)
 */

import { setupServer } from 'msw/node';
import { handlers } from './__mocks__/handlers';

// Create MSW server with all handlers
export const server = setupServer(...handlers);

// Establish API mocking before all tests
beforeAll(() => {
  server.listen({
    onUnhandledRequest: 'warn' // Warn about requests not handled by MSW
  });
});

// Reset handlers after each test to avoid test interference
afterEach(() => {
  server.resetHandlers();
});

// Clean up after all tests are done
afterAll(() => {
  server.close();
});

/**
 * Example: Component Test with MSW
 * 
 * ```typescript
 * import { render, screen, waitFor } from '@testing-library/react';
 * import userEvent from '@testing-library/user-event';
 * import { server } from './setupTests';
 * import { rest } from 'msw';
 * import CDSTradeList from './CDSTradeList';
 * 
 * describe('CDSTradeList', () => {
 *   it('loads and displays trade list', async () => {
 *     render(<CDSTradeList />);
 *     
 *     // Wait for trades to load (mocked by MSW)
 *     await waitFor(() => {
 *       expect(screen.getByText('TEST-CDS-001')).toBeInTheDocument();
 *     });
 *     
 *     expect(screen.getByText('Test Corporation')).toBeInTheDocument();
 *   });
 *   
 *   it('handles error state', async () => {
 *     // Override handler for this test
 *     server.use(
 *       rest.get('/api/trades', (req, res, ctx) => {
 *         return res(
 *           ctx.status(500),
 *           ctx.json({ error: 'Internal server error' })
 *         );
 *       })
 *     );
 *     
 *     render(<CDSTradeList />);
 *     
 *     await waitFor(() => {
 *       expect(screen.getByText(/error/i)).toBeInTheDocument();
 *     });
 *   });
 *   
 *   it('filters trades by status', async () => {
 *     const user = userEvent.setup();
 *     render(<CDSTradeList />);
 *     
 *     // Wait for initial load
 *     await waitFor(() => {
 *       expect(screen.getByText('TEST-CDS-001')).toBeInTheDocument();
 *     });
 *     
 *     // Filter by status
 *     const filterSelect = screen.getByLabelText('Status');
 *     await user.selectOptions(filterSelect, 'ACTIVE');
 *     
 *     // MSW will handle the filtered request
 *     await waitFor(() => {
 *       expect(screen.getAllByTestId('trade-row')).toHaveLength(2);
 *     });
 *   });
 * });
 * ```
 */

/**
 * Example: Custom Hook Test with MSW
 * 
 * ```typescript
 * import { renderHook, waitFor } from '@testing-library/react';
 * import { useTradeList } from './useTradeList';
 * 
 * describe('useTradeList', () => {
 *   it('fetches trades successfully', async () => {
 *     const { result } = renderHook(() => useTradeList());
 *     
 *     expect(result.current.loading).toBe(true);
 *     
 *     await waitFor(() => {
 *       expect(result.current.loading).toBe(false);
 *     });
 *     
 *     expect(result.current.trades).toHaveLength(3);
 *     expect(result.current.error).toBeNull();
 *   });
 * });
 * ```
 */

/**
 * Example: Form Submission Test with MSW
 * 
 * ```typescript
 * import { render, screen, waitFor } from '@testing-library/react';
 * import userEvent from '@testing-library/user-event';
 * import CDSTradeForm from './CDSTradeForm';
 * 
 * describe('CDSTradeForm', () => {
 *   it('submits trade successfully', async () => {
 *     const user = userEvent.setup();
 *     const onSuccess = jest.fn();
 *     
 *     render(<CDSTradeForm onSuccess={onSuccess} />);
 *     
 *     // Fill form
 *     await user.type(screen.getByLabelText('Notional Amount'), '10000000');
 *     await user.type(screen.getByLabelText('Spread (bps)'), '150');
 *     await user.selectOptions(screen.getByLabelText('Buy/Sell'), 'BUY');
 *     
 *     // Submit (MSW will handle POST /api/trades)
 *     await user.click(screen.getByText('Submit Trade'));
 *     
 *     await waitFor(() => {
 *       expect(onSuccess).toHaveBeenCalled();
 *     });
 *     
 *     expect(screen.getByText(/success/i)).toBeInTheDocument();
 *   });
 * });
 * ```
 */
