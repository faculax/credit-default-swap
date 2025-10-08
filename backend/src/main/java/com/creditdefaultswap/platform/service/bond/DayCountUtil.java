package com.creditdefaultswap.platform.service.bond;

import com.creditdefaultswap.platform.model.DayCount;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Day count convention utilities for bond accrual calculations
 * Epic 14: Credit Bonds Enablement (Story 14.4)
 */
public class DayCountUtil {
    
    /**
     * Calculate day count fraction between two dates
     * @param startDate Start date
     * @param endDate End date
     * @param convention Day count convention
     * @return Day count fraction
     */
    public static double dayCountFraction(LocalDate startDate, LocalDate endDate, DayCount convention) {
        switch (convention) {
            case ACT_ACT:
                return actualActual(startDate, endDate);
            case THIRTY_360:
                return thirty360(startDate, endDate);
            default:
                throw new IllegalArgumentException("Unsupported day count convention: " + convention);
        }
    }
    
    /**
     * Actual/Actual day count
     * Uses exact number of days and exact days in year
     */
    private static double actualActual(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        
        // Determine if leap year affects the period
        int year = startDate.getYear();
        boolean isLeapYear = startDate.isLeapYear();
        int daysInYear = isLeapYear ? 366 : 365;
        
        // For simplicity, use the year of the start date
        // More sophisticated implementations might split across year boundaries
        return (double) days / daysInYear;
    }
    
    /**
     * 30/360 day count (Bond Basis)
     * Assumes 30 days in each month and 360 days per year
     */
    private static double thirty360(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        int d1 = startDate.getDayOfMonth();
        int m1 = startDate.getMonthValue();
        int y1 = startDate.getYear();
        
        int d2 = endDate.getDayOfMonth();
        int m2 = endDate.getMonthValue();
        int y2 = endDate.getYear();
        
        // Adjust day counts according to 30/360 convention
        if (d1 == 31) {
            d1 = 30;
        }
        if (d2 == 31 && d1 >= 30) {
            d2 = 30;
        }
        
        int days = (y2 - y1) * 360 + (m2 - m1) * 30 + (d2 - d1);
        return (double) days / 360.0;
    }
    
    /**
     * Calculate accrued interest for a bond
     * @param lastCouponDate Last coupon payment date
     * @param settlementDate Settlement date
     * @param nextCouponDate Next coupon payment date
     * @param couponAmount Full coupon amount
     * @param dayCount Day count convention
     * @return Accrued interest amount
     */
    public static double calculateAccruedInterest(
            LocalDate lastCouponDate,
            LocalDate settlementDate,
            LocalDate nextCouponDate,
            double couponAmount,
            DayCount dayCount) {
        
        double periodFraction = dayCountFraction(lastCouponDate, settlementDate, dayCount);
        double fullPeriodFraction = dayCountFraction(lastCouponDate, nextCouponDate, dayCount);
        
        if (fullPeriodFraction == 0) {
            return 0;
        }
        
        return couponAmount * (periodFraction / fullPeriodFraction);
    }
}
