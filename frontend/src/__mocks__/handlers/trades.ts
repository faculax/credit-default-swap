import { rest } from 'msw';
import cdsTradeList from '../api/trades/cds-trade-list.json';

/**
 * MSW handlers for CDS Trade API endpoints
 * Used in Jest tests with React Testing Library
 */

export const tradesHandlers = [
  // GET /api/trades - List all trades with pagination
  rest.get('/api/trades', (req, res, ctx) => {
    const page = req.url.searchParams.get('page') || '1';
    const pageSize = req.url.searchParams.get('pageSize') || '20';
    const status = req.url.searchParams.get('status');
    
    let trades = cdsTradeList.data.trades;
    
    // Filter by status if provided
    if (status) {
      trades = trades.filter(trade => trade.tradeStatus === status);
    }
    
    return res(
      ctx.status(200),
      ctx.json({
        trades,
        pagination: {
          page: parseInt(page),
          pageSize: parseInt(pageSize),
          totalPages: 1,
          totalItems: trades.length
        },
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 100) + 20
        }
      })
    );
  }),

  // GET /api/trades/:tradeId - Get trade details
  rest.get('/api/trades/:tradeId', (req, res, ctx) => {
    const { tradeId } = req.params;
    
    const trade = cdsTradeList.data.trades.find(t => t.tradeId === tradeId);
    
    if (!trade) {
      return res(
        ctx.status(404),
        ctx.json({
          error: 'Trade not found',
          message: `Trade with ID ${tradeId} does not exist`,
          timestamp: new Date().toISOString()
        })
      );
    }
    
    return res(
      ctx.status(200),
      ctx.json({
        trade,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 50) + 10
        }
      })
    );
  }),

  // POST /api/trades - Create new trade
  rest.post('/api/trades', async (req, res, ctx) => {
    const tradeData = await req.json();
    
    // Validation example
    if (!tradeData.notionalAmount || !tradeData.referenceEntity) {
      return res(
        ctx.status(400),
        ctx.json({
          error: 'Validation failed',
          message: 'Missing required fields: notionalAmount, referenceEntity',
          timestamp: new Date().toISOString()
        })
      );
    }
    
    const newTrade = {
      tradeId: `TEST-CDS-${Date.now()}`,
      ...tradeData,
      tradeStatus: 'PENDING',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    
    return res(
      ctx.status(201),
      ctx.json({
        trade: newTrade,
        message: 'Trade created successfully',
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 200) + 50
        }
      })
    );
  }),

  // PUT /api/trades/:tradeId - Update trade
  rest.put('/api/trades/:tradeId', async (req, res, ctx) => {
    const { tradeId } = req.params;
    const updates = await req.json();
    
    const trade = cdsTradeList.data.trades.find(t => t.tradeId === tradeId);
    
    if (!trade) {
      return res(
        ctx.status(404),
        ctx.json({
          error: 'Trade not found',
          message: `Trade with ID ${tradeId} does not exist`,
          timestamp: new Date().toISOString()
        })
      );
    }
    
    const updatedTrade = {
      ...trade,
      ...updates,
      updatedAt: new Date().toISOString()
    };
    
    return res(
      ctx.status(200),
      ctx.json({
        trade: updatedTrade,
        message: 'Trade updated successfully',
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 150) + 30
        }
      })
    );
  }),

  // DELETE /api/trades/:tradeId - Delete/cancel trade
  rest.delete('/api/trades/:tradeId', (req, res, ctx) => {
    const { tradeId } = req.params;
    
    const trade = cdsTradeList.data.trades.find(t => t.tradeId === tradeId);
    
    if (!trade) {
      return res(
        ctx.status(404),
        ctx.json({
          error: 'Trade not found',
          message: `Trade with ID ${tradeId} does not exist`,
          timestamp: new Date().toISOString()
        })
      );
    }
    
    return res(
      ctx.status(200),
      ctx.json({
        message: 'Trade cancelled successfully',
        tradeId,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 100) + 20
        }
      })
    );
  })
];
