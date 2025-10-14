package com.creditdefaultswap.platform.model;

/**
 * Basket credit derivative types
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
public enum BasketType {
    /**
     * First-to-Default: Protection triggers at first default among constituents
     */
    FIRST_TO_DEFAULT,
    
    /**
     * N-th-to-Default: Protection triggers at N-th default (parameterized)
     */
    NTH_TO_DEFAULT,
    
    /**
     * Tranchette: Loss slice with attachment/detachment points
     */
    TRANCHETTE
}
