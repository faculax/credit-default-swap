package com.creditdefaultswap.platform.model;

/**
 * Day count convention for bond accrual calculations
 */
public enum DayCount {
    ACT_ACT,      // Actual/Actual - exact days over exact year
    THIRTY_360    // 30/360 - assumes 30 days per month, 360 days per year
}
