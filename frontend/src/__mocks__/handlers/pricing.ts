import { rest } from 'msw';

/**
 * MSW handlers for CDS Pricing API endpoints
 */

export const pricingHandlers = [
  // POST /api/pricing/calculate - Calculate CDS pricing
  rest.post('/api/pricing/calculate', async (req, res, ctx) => {
    const pricingRequest = await req.json();
    
    // Validation
    if (!pricingRequest.tradeId && !pricingRequest.trade) {
      return res(
        ctx.status(400),
        ctx.json({
          error: 'Validation failed',
          message: 'Either tradeId or trade data is required',
          timestamp: new Date().toISOString()
        })
      );
    }
    
    // Mock pricing calculation result
    const result = {
      tradeId: pricingRequest.tradeId || 'TEMP-001',
      valuationDate: new Date().toISOString().split('T')[0],
      pricing: {
        presentValue: -125000.50,
        upfrontAmount: 350000.00,
        runningSpread: 150.0,
        duration: 4.35,
        dv01: 4350.25,
        creditDv01: 4350.25
      },
      riskMetrics: {
        deltaCS01: 43.50,
        gamma: 0.25,
        jumpToDefault: -9650000.00,
        recoveryRate01: 42000.00
      },
      marketData: {
        discountCurve: 'USD-OIS-2025',
        creditCurve: 'TEST-CORP-CDS',
        recoveryRate: 0.40,
        hazardRate: 0.0235
      },
      calculationMetadata: {
        engine: 'ORE',
        modelVersion: '1.8.14',
        calculationTime: Math.floor(Math.random() * 500) + 100
      }
    };
    
    return res(
      ctx.status(200),
      ctx.json({
        result,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: result.calculationMetadata.calculationTime
        }
      })
    );
  }),

  // POST /api/pricing/sensitivity - Calculate sensitivity analysis
  rest.post('/api/pricing/sensitivity', async (req, res, ctx) => {
    const sensitivityRequest = await req.json();
    
    if (!sensitivityRequest.tradeId) {
      return res(
        ctx.status(400),
        ctx.json({
          error: 'Validation failed',
          message: 'tradeId is required',
          timestamp: new Date().toISOString()
        })
      );
    }
    
    // Mock sensitivity results
    const result = {
      tradeId: sensitivityRequest.tradeId,
      valuationDate: new Date().toISOString().split('T')[0],
      spreadSensitivity: {
        baseSpread: 150.0,
        scenarios: [
          { spread: 100.0, pv: -75000.00, change: 50000.00 },
          { spread: 125.0, pv: -100000.00, change: 25000.00 },
          { spread: 150.0, pv: -125000.00, change: 0.00 },
          { spread: 175.0, pv: -150000.00, change: -25000.00 },
          { spread: 200.0, pv: -175000.00, change: -50000.00 }
        ]
      },
      recoveryRateSensitivity: {
        baseRecoveryRate: 0.40,
        scenarios: [
          { recoveryRate: 0.30, pv: -145000.00, change: -20000.00 },
          { recoveryRate: 0.35, pv: -135000.00, change: -10000.00 },
          { recoveryRate: 0.40, pv: -125000.00, change: 0.00 },
          { recoveryRate: 0.45, pv: -115000.00, change: 10000.00 },
          { recoveryRate: 0.50, pv: -105000.00, change: 20000.00 }
        ]
      },
      interestRateSensitivity: {
        parallelShift: [
          { shift: -0.01, pv: -123500.00, change: 1500.00 },
          { shift: -0.005, pv: -124250.00, change: 750.00 },
          { shift: 0.00, pv: -125000.00, change: 0.00 },
          { shift: 0.005, pv: -125750.00, change: -750.00 },
          { shift: 0.01, pv: -126500.00, change: -1500.00 }
        ]
      }
    };
    
    return res(
      ctx.status(200),
      ctx.json({
        result,
        metadata: {
          timestamp: new Date().toISOString(),
          requestId: `req-${Date.now()}`,
          executionTime: Math.floor(Math.random() * 1000) + 500
        }
      })
    );
  })
];
