package com.creditdefaultswap.platform.model;

public enum WeightType {
    NOTIONAL,  // Weight represents absolute notional amount
    PERCENT    // Weight represents percentage of total (sum ≈ 1.0)
}
