package com.creditdefaultswap.platform.model;

/**
 * Price quotation convention for bonds
 */
public enum PriceConvention {
    CLEAN,    // Price excluding accrued interest
    DIRTY     // Price including accrued interest
}
