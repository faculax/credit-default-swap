import React, { useState } from 'react';
import { MarketDataSnapshot, DiscountCurveData, DefaultCurveData } from '../../services/risk/riskTypes';

interface Props {
  marketDataSnapshot?: MarketDataSnapshot | null;
}

const MarketDataPanel: React.FC<Props> = ({ marketDataSnapshot }) => {
  const [activeView, setActiveView] = useState<'structured' | 'raw'>('structured');
  const [activeRawFile, setActiveRawFile] = useState<'market' | 'todaysmarket' | 'curveconfig'>('market');

  console.log('🔍 MarketDataPanel rendered with:', { 
    marketDataSnapshot: !!marketDataSnapshot, 
    activeView,
    activeRawFile 
  });

  const handleViewChange = (view: 'structured' | 'raw') => {
    console.log('🔄 View change requested:', { from: activeView, to: view });
    setActiveView(view);
    console.log('✅ View state updated to:', view);
  };

  // For demonstration purposes, use mock data if no snapshot available
  const mockSnapshot: MarketDataSnapshot = {
    valuationDate: '2025-10-15',
    baseCurrency: 'USD',
    discountCurves: [
      {
        currency: 'USD',
        curveId: 'USD6M',
        quotes: [
          { tenor: '0D/1D', quoteName: 'MM/USD/0D/1D', value: 0.050000, type: 'DEPOSIT' },
          { tenor: '1D/1W', quoteName: 'MM/USD/1D/1W', value: 0.050100, type: 'DEPOSIT' },
          { tenor: '1W/1M', quoteName: 'MM/USD/1W/1M', value: 0.050200, type: 'DEPOSIT' },
          { tenor: '1M/3M', quoteName: 'IR_SWAP/USD/1M/3M', value: 0.050500, type: 'SWAP' },
          { tenor: '3M/6M', quoteName: 'IR_SWAP/USD/3M/6M', value: 0.051000, type: 'SWAP' },
        ]
      }
    ],
    defaultCurves: [
      {
        referenceEntity: 'TECH_CORP',
        currency: 'USD',
        curveId: 'TECH_CORP_SR_USD',
        recoveryRate: 0.40,
        spreadQuotes: [
          { tenor: '6M', quoteName: 'CDS/TECH_CORP/USD/6M', value: 0.0150, type: 'CDS_SPREAD' },
          { tenor: '1Y', quoteName: 'CDS/TECH_CORP/USD/1Y', value: 0.0155, type: 'CDS_SPREAD' },
          { tenor: '2Y', quoteName: 'CDS/TECH_CORP/USD/2Y', value: 0.0160, type: 'CDS_SPREAD' },
          { tenor: '3Y', quoteName: 'CDS/TECH_CORP/USD/3Y', value: 0.0165, type: 'CDS_SPREAD' },
          { tenor: '5Y', quoteName: 'CDS/TECH_CORP/USD/5Y', value: 0.0170, type: 'CDS_SPREAD' },
        ]
      }
    ],
    fxRates: {
      'FX/EUR/USD': 1.10,
      'FX/GBP/USD': 1.25
    },
    marketDataFileContent: `# Market Data File for CDS Calculation
# Valuation Date: 2025-10-15

# Yield Curves
MM/USD/0D/1D 0.050000
MM/USD/1D/1W 0.050100
MM/USD/1W/1M 0.050200
IR_SWAP/USD/1M/3M 0.050500
IR_SWAP/USD/3M/6M 0.051000

# FX Rates
FX/EUR/USD 1.10
FX/GBP/USD 1.25

# CDS Curves
CDS/TECH_CORP/USD/6M 0.0150
CDS/TECH_CORP/USD/1Y 0.0155
CDS/TECH_CORP/USD/2Y 0.0160
CDS/TECH_CORP/USD/3Y 0.0165
CDS/TECH_CORP/USD/5Y 0.0170
RECOVERY_RATE/TECH_CORP/USD 0.40`,
    todaysMarketFileContent: `<?xml version="1.0"?>
<TodaysMarket>
  <Configuration id="default">
    <DiscountingCurvesId>default</DiscountingCurvesId>
    <IndexForwardingCurvesId>default</IndexForwardingCurvesId>
  </Configuration>
  <DiscountingCurves id="default">
    <DiscountingCurve currency="USD">Yield/USD/USD6M</DiscountingCurve>
  </DiscountingCurves>
  <DefaultCurves id="default">
    <DefaultCurve name="TECH_CORP">Default/USD/TECH_CORP_SR_USD</DefaultCurve>
  </DefaultCurves>
</TodaysMarket>`,
    curveConfigFileContent: `<?xml version="1.0"?>
<CurveConfiguration>
  <YieldCurves>
    <YieldCurve>
      <CurveId>USD6M</CurveId>
      <CurveDescription>USD 6M yield curve</CurveDescription>
      <Currency>USD</Currency>
    </YieldCurve>
  </YieldCurves>
  <DefaultCurves>
    <DefaultCurve>
      <CurveId>TECH_CORP_SR_USD</CurveId>
      <CurveDescription>TECH_CORP SR CDS USD</CurveDescription>
      <Currency>USD</Currency>
      <Type>SpreadCDS</Type>
    </DefaultCurve>
  </DefaultCurves>
</CurveConfiguration>`
  };

  const snapshot = marketDataSnapshot || mockSnapshot;

  console.log('📊 Using snapshot data:', { 
    isFromProps: !!marketDataSnapshot,
    isMockData: !marketDataSnapshot,
    hasDiscountCurves: !!snapshot?.discountCurves?.length,
    hasDefaultCurves: !!snapshot?.defaultCurves?.length,
    hasFxRates: !!snapshot?.fxRates && Object.keys(snapshot.fxRates).length > 0,
    discountCurvesCount: snapshot?.discountCurves?.length || 0,
    defaultCurvesCount: snapshot?.defaultCurves?.length || 0,
    fxRatesCount: snapshot?.fxRates ? Object.keys(snapshot.fxRates).length : 0
  });

  if (!snapshot) {
    return (
      <div className="bg-fd-darker p-6 rounded-md border border-dashed border-fd-border text-center">
        <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
        </svg>
        <p className="text-fd-text-muted">
          No market data snapshot available for this calculation.
        </p>
        <p className="text-fd-text-muted text-sm mt-2">
          Market data snapshots are captured with each risk calculation to provide full transparency.
        </p>
      </div>
    );
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatBasisPoints = (value?: number) => {
    if (value === undefined || value === null) return '-';
    return (value * 10000).toFixed(2) + ' bps';
  };

  const formatPercent = (value?: number) => {
    if (value === undefined || value === null) return '-';
    return (value * 100).toFixed(2) + '%';
  };

  const formatRate = (value?: number) => {
    if (value === undefined || value === null) return '-';
    return value.toFixed(6);
  };

  return (
    <div className="space-y-6">
      {/* Header with view toggle */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-fd-text flex items-center gap-2">
            <svg className="w-5 h-5 text-fd-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"></path>
            </svg>
            Market Data Used for Calculation
          </h3>
          <p className="text-sm text-fd-text-muted mt-1">
            Valuation Date: {formatDate(snapshot.valuationDate)} • Base Currency: {snapshot.baseCurrency || 'USD'}
          </p>
        </div>
        
        <div className="flex gap-2">
          <button
            onClick={() => {
              console.log('🟢 Structured View button clicked');
              handleViewChange('structured');
            }}
            className={`px-3 py-1.5 text-sm font-medium rounded transition-colors ${
              activeView === 'structured'
                ? 'bg-fd-green text-fd-dark'
                : 'bg-fd-dark text-fd-text hover:bg-fd-border'
            }`}
          >
            Structured View
          </button>
          <button
            onClick={() => {
              console.log('📄 Raw Files button clicked');
              handleViewChange('raw');
            }}
            className={`px-3 py-1.5 text-sm font-medium rounded transition-colors ${
              activeView === 'raw'
                ? 'bg-fd-green text-fd-dark'
                : 'bg-fd-dark text-fd-text hover:bg-fd-border'
            }`}
          >
            Raw Files
          </button>
        </div>
      </div>

      {activeView === 'structured' ? (
        <>
          {console.log('🏗️ Rendering structured view...')}
          {/* Discount Curves */}
          {snapshot.discountCurves && snapshot.discountCurves.length > 0 && (
            <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
              <h4 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
                </svg>
                Discount Curves ({snapshot.discountCurves.length})
              </h4>
              
              <div className="space-y-4">
                {snapshot.discountCurves.map((curve, idx) => (
                  <div key={idx} className="bg-fd-dark rounded p-3">
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium text-fd-text">
                        {curve.currency} - {curve.curveId}
                      </span>
                      <span className="text-xs text-fd-text-muted">
                        {curve.quotes?.length || 0} quotes
                      </span>
                    </div>
                    
                    {curve.quotes && curve.quotes.length > 0 && (
                      <div className="mt-2">
                        <div className="grid grid-cols-4 gap-2 text-xs font-medium text-fd-text-muted border-b border-fd-border pb-1 mb-1">
                          <span>Tenor</span>
                          <span>Type</span>
                          <span>Quote Name</span>
                          <span className="text-right">Rate</span>
                        </div>
                        {curve.quotes.map((quote, qIdx) => (
                          <div key={qIdx} className="grid grid-cols-4 gap-2 text-xs py-1 hover:bg-fd-darker">
                            <span className="font-mono text-fd-text">{quote.tenor}</span>
                            <span className="text-fd-text-muted">{quote.type}</span>
                            <span className="font-mono text-fd-text-muted text-xs">{quote.quoteName}</span>
                            <span className="font-mono text-fd-text text-right">{formatRate(quote.value)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Default/Credit Curves */}
          {snapshot.defaultCurves && snapshot.defaultCurves.length > 0 && (
            <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
              <h4 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-1.964-1.333-2.732 0L3.268 16c-.77 1.333.192 3 1.732 3z"></path>
                </svg>
                Credit/Default Curves ({snapshot.defaultCurves.length})
              </h4>
              
              <div className="space-y-4">
                {snapshot.defaultCurves.map((curve, idx) => (
                  <div key={idx} className="bg-fd-dark rounded p-3">
                    <div className="flex items-center justify-between mb-2">
                      <div>
                        <span className="font-medium text-fd-text">
                          {curve.referenceEntity}
                        </span>
                        <span className="text-fd-text-muted ml-2 text-sm">
                          ({curve.currency} - {curve.curveId})
                        </span>
                      </div>
                      <div className="flex items-center gap-4">
                        {curve.recoveryRate !== undefined && curve.recoveryRate !== null && (
                          <div className="text-right">
                            <div className="text-xs text-fd-text-muted">Recovery Rate</div>
                            <div className="font-mono text-fd-green font-semibold">
                              {formatPercent(curve.recoveryRate)}
                            </div>
                          </div>
                        )}
                        <span className="text-xs text-fd-text-muted">
                          {curve.spreadQuotes?.length || 0} spreads
                        </span>
                      </div>
                    </div>
                    
                    {curve.spreadQuotes && curve.spreadQuotes.length > 0 && (
                      <div className="mt-2">
                        <div className="grid grid-cols-3 gap-2 text-xs font-medium text-fd-text-muted border-b border-fd-border pb-1 mb-1">
                          <span>Tenor</span>
                          <span>Quote Name</span>
                          <span className="text-right">Spread</span>
                        </div>
                        {curve.spreadQuotes.map((quote, qIdx) => (
                          <div key={qIdx} className="grid grid-cols-3 gap-2 text-xs py-1 hover:bg-fd-darker">
                            <span className="font-mono text-fd-text">{quote.tenor}</span>
                            <span className="font-mono text-fd-text-muted text-xs">{quote.quoteName}</span>
                            <span className="font-mono text-fd-text text-right">{formatBasisPoints(quote.value)}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* FX Rates */}
          {snapshot.fxRates && Object.keys(snapshot.fxRates).length > 0 && (
            <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
              <h4 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                FX Rates ({Object.keys(snapshot.fxRates).length})
              </h4>
              
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                {Object.entries(snapshot.fxRates).map(([pair, rate]) => (
                  <div key={pair} className="bg-fd-dark rounded p-2">
                    <div className="text-xs text-fd-text-muted">{pair}</div>
                    <div className="font-mono text-fd-text font-medium">{rate.toFixed(6)}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      ) : (
        <>
          {console.log('📄 Rendering raw files view...')}
          {/* Raw Files View */}
          <div className="bg-fd-darker rounded-md border border-fd-border overflow-hidden">
            {/* File tabs */}
            <div className="flex border-b border-fd-border bg-fd-dark">
              {[
                { key: 'market', label: 'market.txt', icon: '📊' },
                { key: 'todaysmarket', label: 'todaysmarket.xml', icon: '🗓️' },
                { key: 'curveconfig', label: 'curveconfig.xml', icon: '📈' }
              ].map(tab => (
                <button
                  key={tab.key}
                  onClick={() => setActiveRawFile(tab.key as any)}
                  className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                    activeRawFile === tab.key
                      ? 'border-fd-green text-fd-text bg-fd-darker'
                      : 'border-transparent text-fd-text-muted hover:text-fd-text hover:bg-fd-darker'
                  }`}
                >
                  <span className="mr-2">{tab.icon}</span>
                  {tab.label}
                </button>
              ))}
            </div>
            
            {/* File content */}
            <div className="p-4 bg-fd-darker">
              <pre className="text-xs font-mono text-fd-text bg-fd-dark p-4 rounded overflow-x-auto max-h-96 overflow-y-auto border border-fd-border">
                {activeRawFile === 'market' && (snapshot.marketDataFileContent || 'No market data file content available')}
                {activeRawFile === 'todaysmarket' && (snapshot.todaysMarketFileContent || 'No todaysmarket.xml content available')}
                {activeRawFile === 'curveconfig' && (snapshot.curveConfigFileContent || 'No curveconfig.xml content available')}
              </pre>
            </div>
          </div>
          
          <div className="text-xs text-fd-text-muted bg-fd-dark rounded p-3 border border-fd-border">
            <p>
              💡 <strong>Tip:</strong> These are the exact files that were sent to ORE for this calculation.
              You can use these to reproduce the calculation or audit the pricing.
            </p>
          </div>
        </>
      )}
    </div>
  );
};

export default MarketDataPanel;
