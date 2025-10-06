import React, { useEffect, useState } from 'react';
import { fetchRiskMeasures } from '../../services/risk/riskService';
import { RiskMeasures } from '../../services/risk/riskTypes';
import CashflowScheduleTable from './CashflowScheduleTable';
import { lifecycleService } from '../../services/lifecycleService';
import { CouponPeriod } from '../../types/lifecycle';
import { CDSTrade } from '../../data/referenceData';

interface Props { 
  tradeId: number;
  trade?: CDSTrade; // Optional trade object for richer display
}

const RiskMeasuresPanel: React.FC<Props> = ({ tradeId, trade }) => {
  const [data, setData] = useState<RiskMeasures | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [couponPeriods, setCouponPeriods] = useState<CouponPeriod[]>([]);
  const [payingPeriodId, setPayingPeriodId] = useState<number | null>(null);
  const [recalculating, setRecalculating] = useState(false);
  const [generatingSchedule, setGeneratingSchedule] = useState(false);
  const [valuationDate, setValuationDate] = useState<string | undefined>(undefined); // undefined means "today"
  const [refreshKey, setRefreshKey] = useState(0); // Used to force refresh when clicking same button

  const loadRiskMeasures = async (customValuationDate?: string, forceRefresh: boolean = false) => {
    if(!tradeId) return;
    setLoading(true);
    try {
      // When forceRefresh is true, use customValuationDate directly (even if undefined)
      // Otherwise fall back to state's valuationDate
      const dateToUse = forceRefresh ? customValuationDate : (customValuationDate || valuationDate);
      console.log(`🔍 DEBUG loadRiskMeasures called with:`, {
        customValuationDate,
        valuationDate,
        forceRefresh,
        dateToUse,
        tradeId,
        timestamp: new Date().toISOString()
      });
      console.log(`Loading risk measures for trade ${tradeId} with valuation date: ${dateToUse || 'today'}...`);
      const measures = await fetchRiskMeasures(tradeId, dateToUse);
      console.log('Risk measures loaded:', { 
        npv: measures.npv, 
        currency: measures.currency, 
        timestamp: measures.valuationTimestamp,
        loadedAt: new Date().toISOString() 
      });
      setData(measures);
      setError(null);
    } catch (e: any) {
      console.error('Failed to load risk measures:', e);
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const loadCouponPeriods = async () => {
    if(!tradeId) return;
    try {
      console.log('Loading coupon periods for trade:', tradeId);
      const periods = await lifecycleService.getCouponSchedule(tradeId);
      console.log('Loaded coupon periods:', periods.length, periods);
      setCouponPeriods(periods);
    } catch (e: any) {
      console.error('Failed to load coupon periods:', e);
      setCouponPeriods([]); // Ensure it's set to empty array on error
    }
  };

  const handlePayCoupon = async (periodId: number, payOnTime: boolean = false) => {
    setPayingPeriodId(periodId);
    try {
      await lifecycleService.payCoupon(tradeId, periodId, payOnTime);
      console.log('Coupon paid successfully, reloading data...');
      
      // Reload coupon periods to reflect payment
      await loadCouponPeriods();
      
      // Trigger risk recalculation - CRITICAL: Clear old data first to force re-render
      setRecalculating(true);
      setData(null); // Force clear to ensure React detects the change
      
      // Wait a bit to ensure backend has processed the payment
      await new Promise(resolve => setTimeout(resolve, 500));
      
      await loadRiskMeasures();
      console.log('Risk measures reloaded after coupon payment');
      setRecalculating(false);
    } catch (e: any) {
      alert('Failed to pay coupon: ' + e.message);
      setRecalculating(false);
    } finally {
      setPayingPeriodId(null);
    }
  };

  const handleUnpayCoupon = async (periodId: number) => {
    setPayingPeriodId(periodId);
    try {
      await lifecycleService.unpayCoupon(tradeId, periodId);
      console.log('Coupon payment cancelled successfully, reloading data...');
      
      // Reload coupon periods to reflect cancellation
      await loadCouponPeriods();
      
      // Trigger risk recalculation
      setRecalculating(true);
      setData(null);
      
      await new Promise(resolve => setTimeout(resolve, 500));
      
      await loadRiskMeasures();
      console.log('Risk measures reloaded after coupon cancellation');
      setRecalculating(false);
    } catch (e: any) {
      alert('Failed to cancel coupon payment: ' + e.message);
      setRecalculating(false);
    } finally {
      setPayingPeriodId(null);
    }
  };

  const handleGenerateSchedule = async () => {
    setGeneratingSchedule(true);
    try {
      await lifecycleService.generateCouponSchedule(tradeId);
      // Reload the schedule
      await loadCouponPeriods();
    } catch (e: any) {
      alert('Failed to generate coupon schedule: ' + e.message);
    } finally {
      setGeneratingSchedule(false);
    }
  };

  // Helper function to calculate business days offset (simplified - assumes no holidays)
  const getBusinessDaysFromToday = (days: number): string => {
    const today = new Date();
    const target = new Date(today);
    
    let addedDays = 0;
    let daysToAdd = days;
    
    while (daysToAdd > 0) {
      target.setDate(target.getDate() + 1);
      const dayOfWeek = target.getDay();
      // Skip weekends (0 = Sunday, 6 = Saturday)
      if (dayOfWeek !== 0 && dayOfWeek !== 6) {
        addedDays++;
        daysToAdd--;
      }
    }
    
    return target.toISOString().split('T')[0];
  };

  const handleQuickValuationDate = async (option: 'today' | 't+1' | 't+7' | 't+45') => {
    let newDate: string | undefined = undefined;
    
    switch(option) {
      case 'today':
        newDate = undefined; // undefined means use backend's "today"
        break;
      case 't+1':
        newDate = getBusinessDaysFromToday(1);
        break;
      case 't+7':
        newDate = getBusinessDaysFromToday(7);
        break;
      case 't+45':
        newDate = getBusinessDaysFromToday(45);
        break;
    }
    
    console.log(`🔍 DEBUG handleQuickValuationDate:`, { option, newDate, currentValuationDate: valuationDate });
    
    // Update state first
    setValuationDate(newDate);
    
    // Force a fresh fetch by clearing data first
    setData(null);
    setLoading(true);
    setError(null);
    
    // Small delay to ensure state updates propagate
    await new Promise(resolve => setTimeout(resolve, 10));
    
    // Now fetch with the new date, passing forceRefresh=true to use newDate directly
    await loadRiskMeasures(newDate, true);
  };

  useEffect(() => {
    loadRiskMeasures();
    loadCouponPeriods();
  }, [tradeId]);

  if(!tradeId) return <div className="text-fd-text">No trade selected</div>;
  if(loading) return (
    <div className="flex items-center gap-2 text-fd-text">
      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-fd-green"></div>
      <span>Calculating risk measures with ORE...</span>
    </div>
  );
  if(error) return <div className="text-red-400" role="alert">Failed: {error}</div>;
  if(!data) return null;

  const formatValue = (value: number | null | undefined, decimals: number = 2): string => {
    if (value === null || value === undefined) return '-';
    return typeof value === 'number' ? value.toLocaleString(undefined, {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }) : String(value);
  };

  const formatCurrency = (value: number | null | undefined, currency: string = 'USD'): string => {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const formatBasisPoints = (value: number | null | undefined): string => {
    if (value === null || value === undefined) return '-';
    return (value * 10000).toFixed(2) + ' bps';
  };

  // Find the next unpaid coupon (earliest payment date among unpaid)
  const getNextUnpaidCoupon = (): CouponPeriod | null => {
    const unpaidCoupons = couponPeriods
      .filter(p => !p.paid)
      .sort((a, b) => new Date(a.paymentDate).getTime() - new Date(b.paymentDate).getTime());
    return unpaidCoupons.length > 0 ? unpaidCoupons[0] : null;
  };

  // Find the most recently paid coupon (latest payment date among paid coupons)
  const getMostRecentlyPaidCoupon = (): CouponPeriod | null => {
    const paidCoupons = couponPeriods
      .filter(p => p.paid)
      .sort((a, b) => new Date(b.paymentDate).getTime() - new Date(a.paymentDate).getTime());
    return paidCoupons.length > 0 ? paidCoupons[0] : null;
  };

  const nextUnpaidCoupon = getNextUnpaidCoupon();
  const mostRecentlyPaidCoupon = getMostRecentlyPaidCoupon();
  
  const canPayCoupon = (period: CouponPeriod): boolean => {
    if (period.paid) return false;
    if (!nextUnpaidCoupon) return false;
    return period.id === nextUnpaidCoupon.id;
  };

  const canUnpayCoupon = (period: CouponPeriod): boolean => {
    if (!period.paid) return false;
    if (!mostRecentlyPaidCoupon) return false;
    return period.id === mostRecentlyPaidCoupon.id;
  };

  // Calculate coupon statistics
  const totalCoupons = couponPeriods.length;
  const paidCoupons = couponPeriods.filter(p => p.paid).length;
  const unpaidCoupons = totalCoupons - paidCoupons;
  const totalPaidAmount = couponPeriods
    .filter(p => p.paid && p.couponAmount)
    .reduce((sum, p) => sum + (p.couponAmount || 0), 0);

  // Derive frequency and maturity from coupon periods
  const getScheduleDescription = () => {
    if (couponPeriods.length === 0) return '';
    
    // Get maturity from last period
    const lastPeriod = couponPeriods[couponPeriods.length - 1];
    const maturityDate = new Date(lastPeriod.periodEndDate);
    const maturityYear = maturityDate.getFullYear();
    const maturityMonth = maturityDate.toLocaleDateString('en-US', { month: 'short' });
    
    // Use trade's premium frequency if available
    let frequency = 'Periodic';
    if (trade?.premiumFrequency) {
      // Map the database values to display names
      const frequencyMap: Record<string, string> = {
        'MONTHLY': 'Monthly',
        'QUARTERLY': 'Quarterly',
        'SEMI_ANNUAL': 'Semi-annual',
        'ANNUAL': 'Annual'
      };
      frequency = frequencyMap[trade.premiumFrequency] || trade.premiumFrequency;
    } else if (couponPeriods.length > 1) {
      // Fall back to calculating from payment date intervals if trade data not available
      const firstPayment = new Date(couponPeriods[0].paymentDate);
      const secondPayment = new Date(couponPeriods[1].paymentDate);
      const daysDiff = Math.round((secondPayment.getTime() - firstPayment.getTime()) / (1000 * 60 * 60 * 24));
      
      // More lenient ranges for CDS IMM schedules
      // Quarterly: ~91 days (3 months)
      // Semi-annual: ~182 days (6 months)
      // Monthly: ~30 days
      // Annual: ~365 days
      if (daysDiff >= 25 && daysDiff <= 35) frequency = 'Monthly';
      else if (daysDiff >= 80 && daysDiff <= 100) frequency = 'Quarterly';
      else if (daysDiff >= 170 && daysDiff <= 195) frequency = 'Semi-annual';
      else if (daysDiff >= 350 && daysDiff <= 375) frequency = 'Annual';
    }
    
    return `${frequency} until ${maturityMonth} ${maturityYear}`;
  };

  const hasCashflows = data.cashflows && data.cashflows.length > 0;
  const hasCDSMetrics = data.fairSpreadClean !== null || data.protectionLegNPV !== null;

  return (
    <div className="space-y-6">
      {/* Core ORE Valuation */}
      <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
        <div className="flex justify-between items-start mb-3">
          <h3 className="text-fd-green font-semibold flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
            </svg>
            ORE Valuation
          </h3>
          
          {/* Valuation Date Quick Selector */}
          <div className="flex items-center gap-2">
            <span className="text-xs text-fd-text-muted">Valuation Date:</span>
            <div className="flex gap-1">
              <button
                onClick={() => handleQuickValuationDate('today')}
                disabled={loading || recalculating}
                className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                  valuationDate === undefined || valuationDate === ''
                    ? 'bg-fd-green text-fd-dark' 
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                Today
              </button>
              <button
                onClick={() => handleQuickValuationDate('t+1')}
                disabled={loading || recalculating}
                className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                  valuationDate === getBusinessDaysFromToday(1) 
                    ? 'bg-fd-green text-fd-dark' 
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                T+1
              </button>
              <button
                onClick={() => handleQuickValuationDate('t+7')}
                disabled={loading || recalculating}
                className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                  valuationDate === getBusinessDaysFromToday(7) 
                    ? 'bg-fd-green text-fd-dark' 
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                T+7
              </button>
              <button
                onClick={() => handleQuickValuationDate('t+45')}
                disabled={loading || recalculating}
                className={`px-2 py-1 text-xs font-medium rounded transition-colors ${
                  valuationDate === getBusinessDaysFromToday(45) 
                    ? 'bg-fd-green text-fd-dark' 
                    : 'bg-fd-dark text-fd-text hover:bg-fd-border'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                T+45
              </button>
            </div>
          </div>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Net Present Value</div>
            <div className="text-2xl font-bold text-fd-green font-mono">
              {formatCurrency(data.npv, data.currency)}
            </div>
          </div>
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Currency</div>
            <div className="text-2xl font-bold text-fd-text font-mono">{data.currency || 'USD'}</div>
          </div>
          <div className="bg-fd-dark rounded p-3">
            <div className="text-xs text-fd-text-muted mb-1">Valuation Time</div>
            <div className="text-sm text-fd-text">
              {new Date(data.valuationTimestamp).toLocaleString()}
            </div>
          </div>
        </div>
      </div>

      {/* CDS-Specific Metrics (if available) */}
      {hasCDSMetrics && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"></path>
            </svg>
            CDS Valuation Breakdown
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3 text-sm">
            <div>
              <span className="text-fd-text-muted">Fair Spread (Clean):</span>
              <div className="font-mono text-fd-text font-semibold">{formatBasisPoints(data.fairSpreadClean)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Fair Spread (Dirty):</span>
              <div className="font-mono text-fd-text font-semibold">{formatBasisPoints(data.fairSpreadDirty)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Protection Leg NPV:</span>
              <div className="font-mono text-fd-text font-semibold">{formatCurrency(data.protectionLegNPV, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Premium Leg NPV:</span>
              <div className="font-mono text-fd-text font-semibold">{formatCurrency(data.premiumLegNPVClean, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Accrued Premium:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.accruedPremium, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Upfront Premium:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.upfrontPremium, data.currency)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Coupon Leg BPS:</span>
              <div className="font-mono text-fd-text">{formatValue(data.couponLegBPS, 2)}</div>
            </div>
            <div>
              <span className="text-fd-text-muted">Current Notional:</span>
              <div className="font-mono text-fd-text">{formatCurrency(data.currentNotional, data.currency)}</div>
            </div>
            {paidCoupons > 0 && (
              <div>
                <span className="text-fd-text-muted">Total Paid Coupons:</span>
                <div className="font-mono text-fd-green font-semibold">{formatCurrency(totalPaidAmount, data.currency)}</div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Credit Risk Profile (if available) */}
      {(data.defaultProbabilities && data.defaultProbabilities.length > 0) && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-1.964-1.333-2.732 0L3.268 16c-.77 1.333.192 3 1.732 3z"></path>
            </svg>
            Credit Risk Profile
          </h3>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <h4 className="text-sm font-medium text-fd-text mb-2">Default Probabilities by Period</h4>
              <div className="space-y-1 text-sm">
                {data.defaultProbabilities.slice(0, 6).map((prob, idx) => (
                  <div key={idx} className="flex justify-between">
                    <span className="text-fd-text-muted">Period {idx + 1}:</span>
                    <span className="font-mono text-fd-text">{(prob * 100).toFixed(2)}%</span>
                  </div>
                ))}
              </div>
            </div>
            
            {data.expectedLosses && data.expectedLosses.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-fd-text mb-2">Expected Losses by Period</h4>
                <div className="space-y-1 text-sm">
                  {data.expectedLosses.slice(0, 6).map((loss, idx) => (
                    <div key={idx} className="flex justify-between">
                      <span className="text-fd-text-muted">Period {idx + 1}:</span>
                      <span className="font-mono text-fd-text">{formatCurrency(loss, data.currency)}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Coupon Payment Schedule */}
      {couponPeriods.length > 0 && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
            Coupon Payment Schedule
            <span className="ml-auto text-sm font-normal text-fd-text-muted">
              {paidCoupons} of {totalCoupons} paid
            </span>
            {recalculating && (
              <span className="ml-2 text-sm font-normal text-fd-text-muted flex items-center gap-1">
                <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-fd-green"></div>
                Recalculating risk...
              </span>
            )}
          </h3>
          
          {/* Schedule description */}
          <div className="mb-3 text-sm text-fd-text-muted italic">
            {getScheduleDescription()}
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-fd-text">
              <thead>
                <tr className="text-left border-b-2 border-fd-border bg-fd-dark">
                  <th className="py-2 px-3 font-medium">Payment Date</th>
                  <th className="py-2 px-3 font-medium">Period</th>
                  <th className="py-2 px-3 font-medium text-right">Days</th>
                  <th className="py-2 px-3 font-medium text-right">Notional</th>
                  <th className="py-2 px-3 font-medium text-right">Coupon Amount</th>
                  <th className="py-2 px-3 font-medium">Status</th>
                  <th className="py-2 px-3 font-medium text-center">Action</th>
                </tr>
              </thead>
              <tbody>
                {couponPeriods.map((period) => (
                  <tr 
                    key={period.id} 
                    className={`border-b border-fd-border hover:bg-fd-dark transition-colors ${period.paid ? 'opacity-60' : ''}`}
                  >
                    <td className="py-2 px-3 font-mono">{new Date(period.paymentDate).toLocaleDateString()}</td>
                    <td className="py-2 px-3 text-fd-text-muted">
                      {new Date(period.periodStartDate).toLocaleDateString()} → {new Date(period.periodEndDate).toLocaleDateString()}
                    </td>
                    <td className="py-2 px-3 font-mono text-right">{period.accrualDays}</td>
                    <td className="py-2 px-3 font-mono text-right">{formatCurrency(period.notionalAmount, data?.currency)}</td>
                    <td className="py-2 px-3 font-mono text-right">
                      {period.couponAmount ? formatCurrency(period.couponAmount, data?.currency) : '-'}
                    </td>
                    <td className="py-2 px-3">
                      {period.paid ? (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-500/20 text-green-400">
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                          </svg>
                          Paid
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-yellow-500/20 text-yellow-400">
                          <svg className="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                          </svg>
                          Unpaid
                        </span>
                      )}
                    </td>
                    <td className="py-2 px-3 text-center">
                      {/* Show Cancel button for the most recently paid coupon */}
                      {period.paid && canUnpayCoupon(period) && (
                        <div className="flex flex-col gap-1 items-center">
                          <button
                            onClick={() => handleUnpayCoupon(period.id)}
                            disabled={payingPeriodId === period.id || recalculating}
                            title="Cancel this payment (for demo)"
                            className="px-2 py-1 text-xs bg-red-600 text-white rounded font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors w-full"
                          >
                            {payingPeriodId === period.id ? (
                              <span className="flex items-center gap-1 justify-center">
                                <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white"></div>
                              </span>
                            ) : (
                              '↩ Cancel Payment'
                            )}
                          </button>
                          <span className="text-xs text-fd-text-muted">
                            {new Date(period.paidAt!).toLocaleString()}
                          </span>
                        </div>
                      )}
                      
                      {/* Show payment timestamp for other paid coupons */}
                      {period.paid && !canUnpayCoupon(period) && period.paidAt && (
                        <span className="text-xs text-fd-text-muted">
                          {new Date(period.paidAt).toLocaleString()}
                        </span>
                      )}
                      
                      {/* Show payment buttons for unpaid coupons */}
                      {!period.paid && (
                        <div className="flex gap-1 justify-center">
                          <button
                            onClick={() => handlePayCoupon(period.id, true)}
                            disabled={!canPayCoupon(period) || payingPeriodId === period.id || recalculating}
                            title={!canPayCoupon(period) ? 'You must pay earlier coupons first' : 'Pay on the scheduled payment date'}
                            className="px-2 py-1 text-xs bg-fd-green text-fd-dark rounded font-medium hover:bg-fd-green-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                          >
                            {payingPeriodId === period.id ? (
                              <span className="flex items-center gap-1">
                                <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-fd-dark"></div>
                              </span>
                            ) : canPayCoupon(period) ? (
                              'Pay On Time'
                            ) : (
                              '🔒 Locked'
                            )}
                          </button>
                          <button
                            onClick={() => handlePayCoupon(period.id, false)}
                            disabled={!canPayCoupon(period) || payingPeriodId === period.id || recalculating}
                            title={!canPayCoupon(period) ? 'You must pay earlier coupons first' : 'Pay now (backdated)'}
                            className="px-2 py-1 text-xs bg-blue-600 text-white rounded font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                          >
                            {canPayCoupon(period) ? 'Pay Now' : '🔒'}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          <div className="mt-3 text-xs text-fd-text-muted">
            <p>
              💡 <strong>Tip:</strong> Coupons must be paid sequentially by payment date. 
              Only the next unpaid coupon can be paid — earlier coupons must be settled before later ones.
              <br />
              • <strong>Pay On Time:</strong> Marks the coupon as paid on its scheduled payment date (no accrued premium).
              <br />
              • <strong>Pay Now:</strong> Marks the coupon as paid today (shows accrued premium if applicable).
              <br />
              • <strong>Cancel:</strong> Reverts the most recently paid coupon (for demo purposes only).
              <br />
              Paying or canceling a coupon will trigger a risk recalculation.
            </p>
          </div>
        </div>
      )}

      {/* No coupon schedule message */}
      {couponPeriods.length === 0 && data && !loading && (
        <div className="bg-fd-darker p-4 rounded-md border border-dashed border-fd-border text-center">
          <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
          </svg>
          <p className="text-fd-text-muted mb-4">
            No coupon schedule generated for this trade yet.
          </p>
          <button
            onClick={handleGenerateSchedule}
            disabled={generatingSchedule}
            className="px-4 py-2 bg-fd-green text-fd-dark rounded font-medium hover:bg-fd-green-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {generatingSchedule ? (
              <span className="flex items-center gap-2">
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-fd-dark"></div>
                Generating Schedule...
              </span>
            ) : (
              'Generate Coupon Schedule'
            )}
          </button>
        </div>
      )}

      {/* Cashflow Schedule - Hidden: Merged with Coupon Payment Schedule above */}
      {/* hasCashflows && (
        <div className="bg-fd-darker p-4 rounded-md border border-fd-border">
          <h3 className="text-fd-green font-semibold mb-3 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            Cashflow Schedule
            <span className="ml-auto text-sm font-normal text-fd-text-muted">
              {data.cashflows!.length} payment{data.cashflows!.length !== 1 ? 's' : ''}
            </span>
          </h3>
          
          <CashflowScheduleTable cashflows={data.cashflows!} />
        </div>
      ) */}

      {/* No data message */}
      {!hasCashflows && !hasCDSMetrics && (
        <div className="bg-fd-darker p-6 rounded-md border border-dashed border-fd-border text-center">
          <svg className="w-12 h-12 text-fd-text-muted mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <p className="text-fd-text-muted">
            Additional CDS metrics and cashflow schedule will appear here once available.
          </p>
          <p className="text-fd-text-muted text-sm mt-2">
            Currently showing NPV calculated by ORE.
          </p>
        </div>
      )}
    </div>
  );
};

export default RiskMeasuresPanel;
