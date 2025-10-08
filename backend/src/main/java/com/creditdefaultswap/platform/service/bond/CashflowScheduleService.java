package com.creditdefaultswap.platform.service.bond;

import com.creditdefaultswap.platform.model.Bond;
import com.creditdefaultswap.platform.model.CouponFrequency;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating bond cashflow schedules
 * Epic 14: Credit Bonds Enablement (Story 14.4)
 */
@Service
public class CashflowScheduleService {
    
    /**
     * Represents a single cashflow in the bond schedule
     */
    public static class Cashflow {
        private LocalDate paymentDate;
        private double couponAmount;
        private double principal;
        private boolean isFinal;
        
        public Cashflow(LocalDate paymentDate, double couponAmount, double principal, boolean isFinal) {
            this.paymentDate = paymentDate;
            this.couponAmount = couponAmount;
            this.principal = principal;
            this.isFinal = isFinal;
        }
        
        public LocalDate getPaymentDate() {
            return paymentDate;
        }
        
        public double getCouponAmount() {
            return couponAmount;
        }
        
        public double getPrincipal() {
            return principal;
        }
        
        public boolean isFinal() {
            return isFinal;
        }
        
        public double getTotalAmount() {
            return couponAmount + principal;
        }
    }
    
    /**
     * Generate cashflow schedule for a bond
     * @param bond The bond
     * @return List of cashflows
     */
    public List<Cashflow> generateSchedule(Bond bond) {
        List<Cashflow> cashflows = new ArrayList<>();
        
        LocalDate currentDate = bond.getIssueDate();
        LocalDate maturityDate = bond.getMaturityDate();
        int paymentsPerYear = bond.getCouponFrequency().getPaymentsPerYear();
        int monthsBetweenPayments = 12 / paymentsPerYear;
        
        // Calculate coupon amount per payment
        double annualCoupon = bond.getNotional().doubleValue() * bond.getCouponRate().doubleValue();
        double couponPerPayment = annualCoupon / paymentsPerYear;
        
        // Generate coupon dates
        LocalDate nextPaymentDate = currentDate.plusMonths(monthsBetweenPayments);
        
        while (!nextPaymentDate.isAfter(maturityDate)) {
            boolean isFinal = nextPaymentDate.equals(maturityDate) || 
                            nextPaymentDate.plusMonths(monthsBetweenPayments).isAfter(maturityDate);
            
            double principal = isFinal ? bond.getNotional().doubleValue() : 0.0;
            
            cashflows.add(new Cashflow(nextPaymentDate, couponPerPayment, principal, isFinal));
            
            if (isFinal) {
                break;
            }
            
            nextPaymentDate = nextPaymentDate.plusMonths(monthsBetweenPayments);
        }
        
        // Ensure maturity date is included if not already
        if (cashflows.isEmpty() || !cashflows.get(cashflows.size() - 1).getPaymentDate().equals(maturityDate)) {
            cashflows.add(new Cashflow(maturityDate, couponPerPayment, bond.getNotional().doubleValue(), true));
        }
        
        return cashflows;
    }
    
    /**
     * Find the last coupon date before a given date
     * @param bond The bond
     * @param asOfDate The reference date
     * @return Last coupon date before asOfDate
     */
    public LocalDate getLastCouponDate(Bond bond, LocalDate asOfDate) {
        List<Cashflow> schedule = generateSchedule(bond);
        
        LocalDate lastCouponDate = bond.getIssueDate();
        
        for (Cashflow cf : schedule) {
            if (cf.getPaymentDate().isBefore(asOfDate) || cf.getPaymentDate().equals(asOfDate)) {
                lastCouponDate = cf.getPaymentDate();
            } else {
                break;
            }
        }
        
        return lastCouponDate;
    }
    
    /**
     * Find the next coupon date after a given date
     * @param bond The bond
     * @param asOfDate The reference date
     * @return Next coupon date after asOfDate
     */
    public LocalDate getNextCouponDate(Bond bond, LocalDate asOfDate) {
        List<Cashflow> schedule = generateSchedule(bond);
        
        for (Cashflow cf : schedule) {
            if (cf.getPaymentDate().isAfter(asOfDate)) {
                return cf.getPaymentDate();
            }
        }
        
        return bond.getMaturityDate();
    }
}
