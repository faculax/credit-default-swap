package com.creditdefaultswap.platform.service.bond;

import com.creditdefaultswap.platform.model.Bond;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Bond pricing service for deterministic and survival-based valuation
 * Epic 14: Credit Bonds Enablement (Stories 14.5, 14.6, 14.7, 14.8)
 */
@Service
public class BondPricingService {
    
    @Autowired
    private CashflowScheduleService cashflowScheduleService;
    
    /**
     * Calculate deterministic present value using risk-free discount curve
     * For MVP, uses simplified constant discount rate
     */
    public double calculatePresentValue(Bond bond, LocalDate valuationDate, double discountRate) {
        List<CashflowScheduleService.Cashflow> cashflows = cashflowScheduleService.generateSchedule(bond);
        
        double pv = 0.0;
        
        for (CashflowScheduleService.Cashflow cf : cashflows) {
            if (cf.getPaymentDate().isAfter(valuationDate)) {
                double yearsToPayment = DayCountUtil.dayCountFraction(valuationDate, cf.getPaymentDate(), bond.getDayCount());
                double discountFactor = Math.exp(-discountRate * yearsToPayment);
                pv += cf.getTotalAmount() * discountFactor;
            }
        }
        
        return pv;
    }
    
    /**
     * Calculate accrued interest
     */
    public double calculateAccruedInterest(Bond bond, LocalDate valuationDate) {
        LocalDate lastCouponDate = cashflowScheduleService.getLastCouponDate(bond, valuationDate);
        LocalDate nextCouponDate = cashflowScheduleService.getNextCouponDate(bond, valuationDate);
        
        double annualCoupon = bond.getNotional().doubleValue() * bond.getCouponRate().doubleValue();
        double couponPerPayment = annualCoupon / bond.getCouponFrequency().getPaymentsPerYear();
        
        return DayCountUtil.calculateAccruedInterest(
            lastCouponDate,
            valuationDate,
            nextCouponDate,
            couponPerPayment,
            bond.getDayCount()
        );
    }
    
    /**
     * Calculate clean price (PV without accrued interest)
     */
    public double calculateCleanPrice(Bond bond, LocalDate valuationDate, double discountRate) {
        double pv = calculatePresentValue(bond, valuationDate, discountRate);
        double accruedInterest = calculateAccruedInterest(bond, valuationDate);
        
        // Clean price = PV - Accrued
        // Expressed as percentage of face value
        return ((pv - accruedInterest) / bond.getFaceValue().doubleValue()) * 100.0;
    }
    
    /**
     * Calculate dirty price (PV with accrued interest)
     */
    public double calculateDirtyPrice(Bond bond, LocalDate valuationDate, double discountRate) {
        double pv = calculatePresentValue(bond, valuationDate, discountRate);
        
        // Dirty price = PV
        // Expressed as percentage of face value
        return (pv / bond.getFaceValue().doubleValue()) * 100.0;
    }
    
    /**
     * Calculate yield to maturity using secant method
     * Story 14.6: Yield solver
     */
    public double calculateYieldToMaturity(Bond bond, LocalDate valuationDate, double observedPrice, boolean isClean) {
        double targetPrice = observedPrice;
        if (isClean) {
            // Convert clean price to dirty price for calculation
            double accrued = calculateAccruedInterest(bond, valuationDate);
            targetPrice = observedPrice + (accrued / bond.getFaceValue().doubleValue() * 100.0);
        }
        
        // Initial guesses for yield (in percentage terms)
        double y1 = 0.01; // 1%
        double y2 = 0.10; // 10%
        
        double tolerance = 0.000001; // 0.0001%
        int maxIterations = 100;
        
        for (int i = 0; i < maxIterations; i++) {
            double pv1 = calculateDirtyPrice(bond, valuationDate, y1);
            double pv2 = calculateDirtyPrice(bond, valuationDate, y2);
            
            if (Math.abs(pv2 - targetPrice) < tolerance) {
                return y2;
            }
            
            // Secant method update
            double y3 = y2 - (pv2 - targetPrice) * (y2 - y1) / (pv2 - pv1);
            
            // Bound the yield between reasonable limits
            y3 = Math.max(-0.5, Math.min(2.0, y3)); // Between -50% and 200%
            
            y1 = y2;
            y2 = y3;
        }
        
        return y2; // Return best estimate
    }
    
    /**
     * Calculate Z-spread (spread over risk-free curve)
     * Story 14.6: Z-spread solver
     */
    public double calculateZSpread(Bond bond, LocalDate valuationDate, double observedPrice, 
                                   double baseDiscountRate, boolean isClean) {
        double targetPrice = observedPrice;
        if (isClean) {
            double accrued = calculateAccruedInterest(bond, valuationDate);
            targetPrice = observedPrice + (accrued / bond.getFaceValue().doubleValue() * 100.0);
        }
        
        // Solve for spread z such that PV with (baseRate + z) = observedPrice
        double z1 = 0.0;
        double z2 = 0.05; // 500 bps initial guess
        
        double tolerance = 0.000001;
        int maxIterations = 100;
        
        for (int i = 0; i < maxIterations; i++) {
            double pv1 = calculateDirtyPrice(bond, valuationDate, baseDiscountRate + z1);
            double pv2 = calculateDirtyPrice(bond, valuationDate, baseDiscountRate + z2);
            
            if (Math.abs(pv2 - targetPrice) < tolerance) {
                return z2;
            }
            
            double z3 = z2 - (pv2 - targetPrice) * (z2 - z1) / (pv2 - pv1);
            z3 = Math.max(-0.5, Math.min(2.0, z3));
            
            z1 = z2;
            z2 = z3;
        }
        
        return z2;
    }
    
    /**
     * Calculate risky PV with hazard curve (survival-based pricing)
     * Story 14.7: Survival-based hazard pricing
     */
    public double calculateRiskyPV(Bond bond, LocalDate valuationDate, double discountRate, double hazardRate) {
        List<CashflowScheduleService.Cashflow> cashflows = cashflowScheduleService.generateSchedule(bond);
        
        double pv = 0.0;
        
        for (CashflowScheduleService.Cashflow cf : cashflows) {
            if (cf.getPaymentDate().isAfter(valuationDate)) {
                double yearsToPayment = DayCountUtil.dayCountFraction(valuationDate, cf.getPaymentDate(), bond.getDayCount());
                double discountFactor = Math.exp(-discountRate * yearsToPayment);
                double survivalProbability = Math.exp(-hazardRate * yearsToPayment);
                
                // Expected cashflow = nominal * survival probability + recovery * (1 - survival) for principal
                // Using default recovery rate of 0.40 for bonds
                double recoveryRate = 0.40;
                double expectedCashflow;
                if (cf.isFinal()) {
                    double recoveryValue = bond.getNotional().doubleValue() * recoveryRate;
                    expectedCashflow = cf.getCouponAmount() * survivalProbability + 
                                     cf.getPrincipal() * survivalProbability +
                                     recoveryValue * (1 - survivalProbability);
                } else {
                    expectedCashflow = cf.getCouponAmount() * survivalProbability;
                }
                
                pv += expectedCashflow * discountFactor;
            }
        }
        
        return pv;
    }
    
    /**
     * Calculate IR DV01 (interest rate sensitivity)
     * Story 14.8: Bond sensitivities
     */
    public double calculateIRDV01(Bond bond, LocalDate valuationDate, double discountRate) {
        double basePV = calculatePresentValue(bond, valuationDate, discountRate);
        double bumpedPV = calculatePresentValue(bond, valuationDate, discountRate + 0.0001); // 1 bp bump
        
        return basePV - bumpedPV; // Positive means PV decreases when rates increase
    }
    
    /**
     * Calculate Spread DV01 (credit spread sensitivity)
     * Story 14.8: Bond sensitivities
     */
    public double calculateSpreadDV01(Bond bond, LocalDate valuationDate, double discountRate, double hazardRate) {
        double basePV = calculateRiskyPV(bond, valuationDate, discountRate, hazardRate);
        double bumpedPV = calculateRiskyPV(bond, valuationDate, discountRate, hazardRate + 0.0001); // 1 bp bump
        
        return basePV - bumpedPV;
    }
    
    /**
     * Calculate Jump-to-Default (JTD) exposure
     * Story 14.8: Bond sensitivities
     */
    public double calculateJTD(Bond bond, LocalDate valuationDate, double markToMarket) {
        // JTD = Notional * LGD - Current MTM Gain
        // For long position: JTD = Notional * (1 - RecoveryRate)
        // Using default recovery rate of 0.40 for bonds
        double recoveryRate = 0.40;
        double lgd = 1.0 - recoveryRate;
        double exposureAtDefault = bond.getNotional().doubleValue() * lgd;
        
        // If MTM is positive (in the money), reduce JTD
        if (markToMarket > 0) {
            return exposureAtDefault - markToMarket;
        }
        
        return exposureAtDefault;
    }
    
    /**
     * Calculate modified duration
     */
    public double calculateModifiedDuration(Bond bond, LocalDate valuationDate, double yield) {
        double basePV = calculatePresentValue(bond, valuationDate, yield);
        double bumpedPV = calculatePresentValue(bond, valuationDate, yield + 0.0001);
        
        double dPV = basePV - bumpedPV;
        double dY = 0.0001;
        
        return (dPV / basePV) / dY;
    }
}
