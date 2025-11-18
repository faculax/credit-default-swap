import { rest } from 'msw';

/**
 * MSW handlers for Market Data API endpoints
 */

export const marketDataHandlers = [
  // GET /api/market-data/curves - Get discount/credit curves
  rest.get('/api/market-data/curves', (req, res, ctx) => {
    const curveType = req.url.searchParams.get('type');
    const currency = req.url.searchParams.get('currency');
    
    let curves = [
      {
        curveId: 'USD-OIS-2025',
        curveName: 'USD OIS Discount Curve',
        type: 'DISCOUNT',
        currency: 'USD',
        asOfDate: '2025-01-15',
        interpolation: 'LOG_LINEAR',
        pointCount: 11
      },
      {
        curveId: 'EUR-ESTR-2025',
        curveName: 'EUR ESTR Discount Curve',
        type: 'DISCOUNT',
        currency: 'EUR',
        asOfDate: '2025-01-15',
        interpolation: 'LOG_LINEAR',
        pointCount: 11
      },
      {
        curveId: 'TEST-CORP-CDS',
        curveName: 'Test Corporation Credit Curve',
        type: 'CREDIT',
        currency: 'USD',
        asOfDate: '2025-01-15',
        interpolation: 'PIECEWISE_FLAT_HAZARD',
        pointCount: 8
      }
    ];
    
    // Filter by type
    if (curveType) {
      curves = curves.filter(c => c.type === curveType);
    }
    
    // Filter by currency
    if (currency) {
      curves = curves.filter(c => c.currency === currency);
    }
    
    return res(
      ctx.status(200),
      ctx.json({
        curves,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 50) + 10
        }
      })
    );
  }),

  // GET /api/market-data/spreads - Get CDS spreads
  rest.get('/api/market-data/spreads', (req, res, ctx) => {
    const ticker = req.url.searchParams.get('ticker');
    const sector = req.url.searchParams.get('sector');
    
    let spreads = [
      {
        ticker: 'TEST',
        entityName: 'Test Corporation',
        sector: 'FINANCIALS',
        rating: 'BBB',
        spreads: {
          '1Y': 120.5,
          '3Y': 135.0,
          '5Y': 150.0,
          '7Y': 162.5,
          '10Y': 175.0
        },
        asOfDate: '2025-01-15',
        marketDepth: 'LIQUID'
      },
      {
        ticker: 'SAMPLE',
        entityName: 'Sample Industries Inc',
        sector: 'INDUSTRIALS',
        rating: 'A',
        spreads: {
          '1Y': 85.0,
          '3Y': 95.0,
          '5Y': 105.0,
          '7Y': 115.0,
          '10Y': 125.0
        },
        asOfDate: '2025-01-15',
        marketDepth: 'LIQUID'
      },
      {
        ticker: 'EURO',
        entityName: 'European Test AG',
        sector: 'TECHNOLOGY',
        rating: 'BBB+',
        spreads: {
          '1Y': 140.0,
          '3Y': 155.0,
          '5Y': 175.0,
          '7Y': 190.0,
          '10Y': 210.0
        },
        asOfDate: '2025-01-15',
        marketDepth: 'MODERATE'
      }
    ];
    
    // Filter by ticker
    if (ticker) {
      spreads = spreads.filter(s => s.ticker === ticker);
    }
    
    // Filter by sector
    if (sector) {
      spreads = spreads.filter(s => s.sector === sector);
    }
    
    return res(
      ctx.status(200),
      ctx.json({
        spreads,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 80) + 20
        }
      })
    );
  })
];
