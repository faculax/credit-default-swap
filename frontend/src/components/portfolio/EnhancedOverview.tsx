import React, { useState, useEffect, useCallback } from 'react';
import { BondPortfolioConstituent, BasketPortfolioConstituent, CdsPortfolioConstituent } from '../../services/portfolioService';

interface EnhancedOverviewProps {
  portfolioId: number;
  cdsConstituents: CdsPortfolioConstituent[];
  bondConstituents: BondPortfolioConstituent[];
  basketConstituents: BasketPortfolioConstituent[];
  pricingData: any;
}

interface IssuerExposure {
  issuer: string;
  sector: string;
  bondNotional: number;
  cdsProtectionBought: number;
  cdsProtectionSold: number;
  netCreditExposure: number;
  hedgeRatio: number;
  hedgeStatus: 'OVER_HEDGED' | 'UNDER_HEDGED' | 'BALANCED' | 'UNHEDGED';
  recommendation: string;
}

interface SectorExposure {
  sector: string;
  notional: number;
  percentage: number;
  instrumentCount: number;
}

const EnhancedOverview: React.FC<EnhancedOverviewProps> = ({ 
  portfolioId, 
  cdsConstituents, 
  bondConstituents,
  basketConstituents,
  pricingData 
}) => {
  const [issuerExposures, setIssuerExposures] = useState<IssuerExposure[]>([]);
  const [sectorExposures, setSectorExposures] = useState<SectorExposure[]>([]);

  useEffect(() => {
    calculateExposures();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cdsConstituents, bondConstituents, basketConstituents]);

  const calculateExposures = useCallback(() => {
    // Map to aggregate by issuer
    const issuerMap = new Map<string, IssuerExposure>();

    // Process bonds
    bondConstituents.forEach(bc => {
      const issuer = bc.bond.issuer;
      if (!issuerMap.has(issuer)) {
        issuerMap.set(issuer, {
          issuer,
          sector: getSectorFromIssuer(issuer),
          bondNotional: 0,
          cdsProtectionBought: 0,
          cdsProtectionSold: 0,
          netCreditExposure: 0,
          hedgeRatio: 0,
          hedgeStatus: 'UNHEDGED',
          recommendation: ''
        });
      }
      const exposure = issuerMap.get(issuer)!;
      exposure.bondNotional += bc.bond.notional;
    });

    // Process CDS
    cdsConstituents.forEach(cc => {
      const issuer = cc.trade.referenceEntity;
      if (!issuerMap.has(issuer)) {
        issuerMap.set(issuer, {
          issuer,
          sector: getSectorFromIssuer(issuer),
          bondNotional: 0,
          cdsProtectionBought: 0,
          cdsProtectionSold: 0,
          netCreditExposure: 0,
          hedgeRatio: 0,
          hedgeStatus: 'UNHEDGED',
          recommendation: ''
        });
      }
      const exposure = issuerMap.get(issuer)!;
      
      // Assuming BUY = buying protection (short credit), SELL = selling protection (long credit)
      // This is a simplification - you may need to check actual buySell field
      exposure.cdsProtectionBought += cc.trade.notionalAmount;
    });

    // Calculate net exposures and recommendations
    const exposures: IssuerExposure[] = [];
    issuerMap.forEach(exp => {
      exp.netCreditExposure = exp.bondNotional - exp.cdsProtectionBought + exp.cdsProtectionSold;
      
      if (exp.bondNotional > 0) {
        exp.hedgeRatio = (exp.cdsProtectionBought / exp.bondNotional) * 100;
        
        if (exp.hedgeRatio > 110) {
          exp.hedgeStatus = 'OVER_HEDGED';
          exp.recommendation = `Consider reducing CDS protection by $${((exp.cdsProtectionBought - exp.bondNotional) / 1000000).toFixed(1)}M`;
        } else if (exp.hedgeRatio >= 90 && exp.hedgeRatio <= 110) {
          exp.hedgeStatus = 'BALANCED';
          exp.recommendation = 'Well hedged';
        } else if (exp.hedgeRatio > 0) {
          exp.hedgeStatus = 'UNDER_HEDGED';
          exp.recommendation = `Consider buying $${((exp.bondNotional - exp.cdsProtectionBought) / 1000000).toFixed(1)}M CDS protection`;
        } else {
          exp.hedgeStatus = 'UNHEDGED';
          exp.recommendation = `⚠️ Buy $${(exp.bondNotional / 1000000).toFixed(1)}M CDS protection to hedge bond exposure`;
        }
      } else if (exp.cdsProtectionBought > 0) {
        exp.hedgeStatus = 'UNHEDGED';
        exp.recommendation = `Consider buying $${(exp.cdsProtectionBought / 1000000).toFixed(1)}M in bonds to capture basis`;
      }
      
      exposures.push(exp);
    });

    setIssuerExposures(exposures.sort((a, b) => Math.abs(b.netCreditExposure) - Math.abs(a.netCreditExposure)));

    // Calculate sector exposures
    const sectorMap = new Map<string, { notional: number; count: number }>();
    
    bondConstituents.forEach(bc => {
      const sector = getSectorFromIssuer(bc.bond.issuer);
      if (!sectorMap.has(sector)) {
        sectorMap.set(sector, { notional: 0, count: 0 });
      }
      const sectorData = sectorMap.get(sector)!;
      sectorData.notional += bc.bond.notional;
      sectorData.count++;
    });

    cdsConstituents.forEach(cc => {
      const sector = getSectorFromIssuer(cc.trade.referenceEntity);
      if (!sectorMap.has(sector)) {
        sectorMap.set(sector, { notional: 0, count: 0 });
      }
      const sectorData = sectorMap.get(sector)!;
      sectorData.notional += cc.trade.notionalAmount;
      sectorData.count++;
    });

    // Add baskets as "BASKET" sector for now
    // TODO: In future, unwind basket constituents to their actual sectors
    basketConstituents.forEach(bc => {
      const sector = 'BASKET';
      if (!sectorMap.has(sector)) {
        sectorMap.set(sector, { notional: 0, count: 0 });
      }
      const sectorData = sectorMap.get(sector)!;
      // Use basket.notional (backend field name)
      const notional = bc.basket?.notional || bc.weightValue || 0;
      sectorData.notional += notional;
      sectorData.count++;
    });

    const totalNotional = Array.from(sectorMap.values()).reduce((sum, s) => sum + s.notional, 0);
    const sectors: SectorExposure[] = [];
    
    sectorMap.forEach((data, sector) => {
      sectors.push({
        sector,
        notional: data.notional,
        percentage: totalNotional > 0 ? (data.notional / totalNotional) * 100 : 0,
        instrumentCount: data.count
      });
    });

    setSectorExposures(sectors.sort((a, b) => b.notional - a.notional));
  }, [basketConstituents, bondConstituents, cdsConstituents]); // useCallback dependency array

  const getSectorFromIssuer = (issuer: string): string => {
    const sectorMap: Record<string, string> = {
      'AAPL': 'TECH', 'MSFT': 'TECH', 'AMZN': 'TECH', 'GOOGL': 'TECH',
      'TSLA': 'TECH', 'NFLX': 'TECH', 'META': 'TECH', 'NVDA': 'TECH', 'AMD': 'TECH',
      'JPM': 'FINANCIALS', 'BAC': 'FINANCIALS', 'WFC': 'FINANCIALS',
      'GS': 'FINANCIALS', 'MS': 'FINANCIALS', 'C': 'FINANCIALS'
    };
    return sectorMap[issuer] || 'OTHER';
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  };

  const getHedgeStatusColor = (status: string) => {
    switch (status) {
      case 'BALANCED': return 'text-fd-green';
      case 'UNDER_HEDGED': return 'text-yellow-400';
      case 'OVER_HEDGED': return 'text-blue-400';
      case 'UNHEDGED': return 'text-red-400';
      default: return 'text-fd-text-muted';
    }
  };

  const getHedgeStatusBadge = (status: string) => {
    switch (status) {
      case 'BALANCED': return 'bg-green-900/30 text-green-400 border-green-500';
      case 'UNDER_HEDGED': return 'bg-yellow-900/30 text-yellow-400 border-yellow-500';
      case 'OVER_HEDGED': return 'bg-blue-900/30 text-blue-400 border-blue-500';
      case 'UNHEDGED': return 'bg-red-900/30 text-red-400 border-red-500';
      default: return 'bg-fd-dark text-fd-text-muted border-fd-border';
    }
  };

  const getSectorColor = (index: number) => {
    const colors = [
      'bg-fd-green', 'bg-blue-500', 'bg-yellow-500', 'bg-purple-500',
      'bg-pink-500', 'bg-indigo-500', 'bg-teal-500', 'bg-orange-500'
    ];
    return colors[index % colors.length];
  };

  const totalCdsNotional = cdsConstituents.reduce((sum, c) => sum + c.trade.notionalAmount, 0);
  const totalBondNotional = bondConstituents.reduce((sum, c) => sum + c.bond.notional, 0);
  const totalBasketNotional = basketConstituents.reduce((sum, c) => {
    // Use basket.notional (backend field name)
    const notional = c.basket?.notional || c.weightValue || 0;
    return sum + notional;
  }, 0);
  const totalNotional = totalCdsNotional + totalBondNotional + totalBasketNotional;

  return (
    <div className="space-y-6">
      {/* Instrument Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Instrument Type Breakdown */}
        <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
          <h3 className="text-lg font-semibold text-fd-text mb-4">Instrument Distribution</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-4 h-4 bg-fd-green rounded"></div>
                <span className="text-fd-text">🛡️ CDS Trades</span>
              </div>
              <div className="text-right">
                <div className="text-fd-text font-medium">{formatCurrency(totalCdsNotional)}</div>
                <div className="text-sm text-fd-text-muted">
                  {totalNotional > 0 ? ((totalCdsNotional / totalNotional) * 100).toFixed(1) : 0}% • {cdsConstituents.length} positions
                </div>
              </div>
            </div>
            <div className="w-full bg-fd-dark rounded-full h-2">
              <div 
                className="bg-fd-green h-2 rounded-full transition-all"
                style={{ width: `${totalNotional > 0 ? (totalCdsNotional / totalNotional) * 100 : 0}%` }}
              ></div>
            </div>

            <div className="flex items-center justify-between mt-4">
              <div className="flex items-center space-x-3">
                <div className="w-4 h-4 bg-blue-500 rounded"></div>
                <span className="text-fd-text">📜 Corporate Bonds</span>
              </div>
              <div className="text-right">
                <div className="text-fd-text font-medium">{formatCurrency(totalBondNotional)}</div>
                <div className="text-sm text-fd-text-muted">
                  {totalNotional > 0 ? ((totalBondNotional / totalNotional) * 100).toFixed(1) : 0}% • {bondConstituents.length} positions
                </div>
              </div>
            </div>
            <div className="w-full bg-fd-dark rounded-full h-2">
              <div 
                className="bg-blue-500 h-2 rounded-full transition-all"
                style={{ width: `${totalNotional > 0 ? (totalBondNotional / totalNotional) * 100 : 0}%` }}
              ></div>
            </div>

            <div className="flex items-center justify-between mt-4">
              <div className="flex items-center space-x-3">
                <div className="w-4 h-4 bg-purple-500 rounded"></div>
                <span className="text-fd-text">🗂️ Credit Baskets</span>
              </div>
              <div className="text-right">
                <div className="text-fd-text font-medium">{formatCurrency(totalBasketNotional)}</div>
                <div className="text-sm text-fd-text-muted">
                  {totalNotional > 0 ? ((totalBasketNotional / totalNotional) * 100).toFixed(1) : 0}% • {basketConstituents.length} positions
                </div>
              </div>
            </div>
            <div className="w-full bg-fd-dark rounded-full h-2">
              <div 
                className="bg-purple-500 h-2 rounded-full transition-all"
                style={{ width: `${totalNotional > 0 ? (totalBasketNotional / totalNotional) * 100 : 0}%` }}
              ></div>
            </div>
          </div>
        </div>

        {/* Sector Exposure */}
        <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
          <h3 className="text-lg font-semibold text-fd-text mb-4">Sector Allocation</h3>
          <div className="space-y-3">
            {sectorExposures.slice(0, 5).map((sector, idx) => (
              <div key={sector.sector} className="space-y-1">
                <div className="flex items-center justify-between text-sm">
                  <div className="flex items-center space-x-2">
                    <div className={`w-3 h-3 rounded ${getSectorColor(idx)}`}></div>
                    <span className="text-fd-text">{sector.sector}</span>
                  </div>
                  <div className="text-fd-text-muted">
                    {sector.percentage.toFixed(1)}% • {sector.instrumentCount} instruments
                  </div>
                </div>
                <div className="w-full bg-fd-dark rounded-full h-1.5">
                  <div 
                    className={`${getSectorColor(idx)} h-1.5 rounded-full transition-all`}
                    style={{ width: `${sector.percentage}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Net Exposure by Issuer */}
      <div className="bg-fd-darker border border-fd-border rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-fd-text">Net Credit Exposure by Issuer</h3>
          <div className="text-sm text-fd-text-muted">
            Showing bond positions vs CDS hedges
          </div>
        </div>
        
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-fd-border">
            <thead>
              <tr className="text-left text-xs font-medium text-fd-text-muted uppercase tracking-wider">
                <th className="px-4 py-3">Issuer</th>
                <th className="px-4 py-3">Sector</th>
                <th className="px-4 py-3 text-right">Bond Exposure</th>
                <th className="px-4 py-3 text-right">CDS Protection</th>
                <th className="px-4 py-3 text-right">Net Exposure</th>
                <th className="px-4 py-3 text-center">Hedge Ratio</th>
                <th className="px-4 py-3 text-center">Status</th>
                <th className="px-4 py-3">Recommendation</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-fd-border">
              {issuerExposures.map(exp => (
                <tr key={exp.issuer} className="hover:bg-fd-dark transition-colors">
                  <td className="px-4 py-3 text-sm font-medium text-fd-text">{exp.issuer}</td>
                  <td className="px-4 py-3 text-sm text-fd-text-muted">{exp.sector}</td>
                  <td className="px-4 py-3 text-sm text-right text-fd-text">
                    {exp.bondNotional > 0 ? formatCurrency(exp.bondNotional) : '-'}
                  </td>
                  <td className="px-4 py-3 text-sm text-right text-fd-text">
                    {exp.cdsProtectionBought > 0 ? formatCurrency(exp.cdsProtectionBought) : '-'}
                  </td>
                  <td className={`px-4 py-3 text-sm text-right font-medium ${
                    exp.netCreditExposure > 0 ? 'text-red-400' : exp.netCreditExposure < 0 ? 'text-fd-green' : 'text-fd-text'
                  }`}>
                    {formatCurrency(Math.abs(exp.netCreditExposure))}
                    {exp.netCreditExposure > 0 ? ' Long' : exp.netCreditExposure < 0 ? ' Short' : ''}
                  </td>
                  <td className="px-4 py-3 text-sm text-center text-fd-text">
                    {exp.bondNotional > 0 ? `${exp.hedgeRatio.toFixed(0)}%` : 'N/A'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium border ${getHedgeStatusBadge(exp.hedgeStatus)}`}>
                      {exp.hedgeStatus.replace('_', ' ')}
                    </span>
                  </td>
                  <td className={`px-4 py-3 text-sm ${getHedgeStatusColor(exp.hedgeStatus)}`}>
                    {exp.recommendation}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {issuerExposures.length === 0 && (
          <div className="text-center py-8 text-fd-text-muted">
            No exposures to analyze. Add some CDS trades and bonds to see hedge recommendations.
          </div>
        )}
      </div>

      {/* Key Metrics Summary */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
          <div className="text-sm text-fd-text-muted mb-1">Total Instruments</div>
          <div className="text-2xl font-semibold text-fd-text">
            {cdsConstituents.length + bondConstituents.length + basketConstituents.length}
          </div>
          <div className="text-xs text-fd-text-muted mt-1">
            {cdsConstituents.length} CDS • {bondConstituents.length} Bonds • {basketConstituents.length} Baskets
          </div>
        </div>

        <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
          <div className="text-sm text-fd-text-muted mb-1">Unique Issuers</div>
          <div className="text-2xl font-semibold text-fd-text">
            {issuerExposures.length}
          </div>
          <div className="text-xs text-fd-text-muted mt-1">
            {sectorExposures.length} sectors
          </div>
        </div>

        <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
          <div className="text-sm text-fd-text-muted mb-1">Hedge Coverage</div>
          <div className="text-2xl font-semibold text-fd-text">
            {totalBondNotional > 0 ? ((totalCdsNotional / totalBondNotional) * 100).toFixed(0) : 0}%
          </div>
          <div className="text-xs text-fd-text-muted mt-1">
            CDS / Bond ratio
          </div>
        </div>

        <div className="bg-fd-darker border border-fd-border rounded-lg p-4">
          <div className="text-sm text-fd-text-muted mb-1">Unhedged Exposure</div>
          <div className="text-2xl font-semibold text-red-400">
            {issuerExposures.filter(e => e.hedgeStatus === 'UNHEDGED' || e.hedgeStatus === 'UNDER_HEDGED').length}
          </div>
          <div className="text-xs text-fd-text-muted mt-1">
            issuers need attention
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnhancedOverview;
