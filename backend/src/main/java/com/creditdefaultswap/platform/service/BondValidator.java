package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.Bond;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for Bond business rules beyond Jakarta validation annotations
 * Epic 14: Credit Bonds Enablement (Story 14.3)
 */
@Component
public class BondValidator {
    
    /**
     * Validate bond business rules
     * @param bond The bond to validate
     * @return List of validation error messages (empty if valid)
     */
    public List<String> validate(Bond bond) {
        List<String> errors = new ArrayList<>();
        
        // Date ordering
        if (bond.getIssueDate() != null && bond.getMaturityDate() != null) {
            if (!bond.getIssueDate().isBefore(bond.getMaturityDate())) {
                errors.add("Issue date must be before maturity date");
            }
            
            // Check maturity is not in the past
            if (bond.getMaturityDate().isBefore(LocalDate.now())) {
                errors.add("Maturity date cannot be in the past");
            }
        }
        
        // Notional validation
        if (bond.getNotional() != null && bond.getNotional().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Notional must be positive");
        }
        
        // Coupon rate bounds
        if (bond.getCouponRate() != null) {
            if (bond.getCouponRate().compareTo(BigDecimal.ZERO) < 0) {
                errors.add("Coupon rate cannot be negative");
            }
            if (bond.getCouponRate().compareTo(new BigDecimal("1.0")) > 0) {
                errors.add("Coupon rate exceeds 100% - please verify");
            }
        }
        
        // Recovery rate bounds (already in entity but double-check)
        if (bond.getRecoveryRate() != null) {
            if (bond.getRecoveryRate().compareTo(BigDecimal.ZERO) < 0 || 
                bond.getRecoveryRate().compareTo(BigDecimal.ONE) > 0) {
                errors.add("Recovery rate must be between 0 and 1");
            }
        }
        
        // Credit curve ID format suggestion
        if (bond.getCreditCurveId() != null && !bond.getCreditCurveId().isEmpty()) {
            String suggested = bond.getIssuer() + "_" + bond.getSeniority() + "_" + bond.getCurrency();
            if (!bond.getCreditCurveId().equals(suggested)) {
                // This is just a warning, not an error
                // errors.add("Credit curve ID format suggestion: " + suggested);
            }
        }
        
        // Issuer validation (basic non-empty check - could be extended to reference data)
        if (bond.getIssuer() == null || bond.getIssuer().trim().isEmpty()) {
            errors.add("Issuer is required");
        }
        
        // Currency validation (basic format)
        if (bond.getCurrency() != null && bond.getCurrency().length() != 3) {
            errors.add("Currency must be 3 characters (ISO code)");
        }
        
        return errors;
    }
    
    /**
     * Check if bond is valid
     * @param bond The bond to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(Bond bond) {
        return validate(bond).isEmpty();
    }
}
