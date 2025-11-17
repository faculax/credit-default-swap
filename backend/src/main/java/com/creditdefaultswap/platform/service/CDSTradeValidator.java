package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Validator for CDS Trade business rules beyond Jakarta validation annotations.
 * Story 3.2: Validation & Business Rules
 * 
 * Enforces CDS-specific validation rules and provides structured error messages
 * to ensure only coherent, bookable trades are persisted.
 */
@Component
public class CDSTradeValidator {
    
    // Allowed values for enum-like string fields
    private static final Set<String> ALLOWED_PREMIUM_FREQUENCIES = Set.of(
        "QUARTERLY", "SEMI_ANNUAL", "ANNUAL", "MONTHLY"
    );
    
    private static final Set<String> ALLOWED_DAY_COUNT_CONVENTIONS = Set.of(
        "ACT_360", "ACT_365", "30_360", "ACT_ACT"
    );
    
    private static final Set<String> ALLOWED_PAYMENT_CALENDARS = Set.of(
        "NYC", "LON", "TKY", "TARGET"
    );
    
    private static final Set<String> ALLOWED_RESTRUCTURING_CLAUSES = Set.of(
        "NO_R", "MR", "MMR", "XR", "CR"
    );
    
    /**
     * Validate CDS trade business rules
     * @param trade The CDS trade to validate
     * @return ValidationResult containing field errors and global errors
     */
    public ValidationResult validate(CDSTrade trade) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        List<String> globalErrors = new ArrayList<>();
        
        if (trade == null) {
            globalErrors.add("Trade object cannot be null");
            return new ValidationResult(fieldErrors, globalErrors);
        }
        
        // Notional amount validation
        validateNotionalAmount(trade, fieldErrors);
        
        // Spread validation
        validateSpread(trade, fieldErrors);
        
        // Date validations
        validateDates(trade, fieldErrors);
        
        // Enum validations
        validateEnums(trade, fieldErrors);
        
        // String field validations
        validateStringFields(trade, fieldErrors);
        
        return new ValidationResult(fieldErrors, globalErrors);
    }
    
    private void validateNotionalAmount(CDSTrade trade, Map<String, String> fieldErrors) {
        if (trade.getNotionalAmount() == null) {
            fieldErrors.put("notionalAmount", "Notional amount is required");
        } else if (trade.getNotionalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            fieldErrors.put("notionalAmount", "Notional amount must be greater than zero");
        }
    }
    
    private void validateSpread(CDSTrade trade, Map<String, String> fieldErrors) {
        if (trade.getSpread() == null) {
            fieldErrors.put("spread", "Spread is required");
        } else if (trade.getSpread().compareTo(BigDecimal.ZERO) < 0) {
            fieldErrors.put("spread", "Spread cannot be negative");
        }
    }
    
    private void validateDates(CDSTrade trade, Map<String, String> fieldErrors) {
        LocalDate tradeDate = trade.getTradeDate();
        LocalDate effectiveDate = trade.getEffectiveDate();
        LocalDate maturityDate = trade.getMaturityDate();
        LocalDate accrualStartDate = trade.getAccrualStartDate();
        
        // Trade date validation
        if (tradeDate != null) {
            if (tradeDate.isAfter(LocalDate.now())) {
                fieldErrors.put("tradeDate", "Trade date cannot be in the future");
            }
        }
        
        // Maturity vs Effective validation
        if (maturityDate != null && effectiveDate != null) {
            if (!maturityDate.isAfter(effectiveDate)) {
                fieldErrors.put("maturityDate", "Maturity date must be strictly after effective date");
            }
        }
        
        // Effective vs Trade validation
        if (effectiveDate != null && tradeDate != null) {
            if (effectiveDate.isBefore(tradeDate)) {
                fieldErrors.put("effectiveDate", "Effective date must be on or after trade date");
            }
        }
        
        // Accrual start validation
        if (accrualStartDate != null && effectiveDate != null) {
            if (accrualStartDate.isAfter(effectiveDate)) {
                fieldErrors.put("accrualStartDate", "Accrual start date must be on or before effective date");
            }
        }
    }
    
    private void validateEnums(CDSTrade trade, Map<String, String> fieldErrors) {
        // BuySellProtection validation
        if (trade.getBuySellProtection() == null) {
            fieldErrors.put("buySellProtection", "Buy/Sell protection indicator is required");
        }
        // No additional validation needed - enum type ensures valid values (BUY or SELL)
        
        // Premium frequency validation
        if (trade.getPremiumFrequency() != null) {
            if (!ALLOWED_PREMIUM_FREQUENCIES.contains(trade.getPremiumFrequency())) {
                fieldErrors.put("premiumFrequency", 
                    "Invalid premium frequency. Allowed values: " + ALLOWED_PREMIUM_FREQUENCIES);
            }
        }
        
        // Day count convention validation
        if (trade.getDayCountConvention() != null) {
            if (!ALLOWED_DAY_COUNT_CONVENTIONS.contains(trade.getDayCountConvention())) {
                fieldErrors.put("dayCountConvention", 
                    "Invalid day count convention. Allowed values: " + ALLOWED_DAY_COUNT_CONVENTIONS);
            }
        }
        
        // Payment calendar validation
        if (trade.getPaymentCalendar() != null) {
            if (!ALLOWED_PAYMENT_CALENDARS.contains(trade.getPaymentCalendar())) {
                fieldErrors.put("paymentCalendar", 
                    "Invalid payment calendar. Allowed values: " + ALLOWED_PAYMENT_CALENDARS);
            }
        }
        
        // Restructuring clause validation (optional field)
        if (trade.getRestructuringClause() != null && !trade.getRestructuringClause().trim().isEmpty()) {
            if (!ALLOWED_RESTRUCTURING_CLAUSES.contains(trade.getRestructuringClause())) {
                fieldErrors.put("restructuringClause", 
                    "Invalid restructuring clause. Allowed values: " + ALLOWED_RESTRUCTURING_CLAUSES);
            }
        }
    }
    
    private void validateStringFields(CDSTrade trade, Map<String, String> fieldErrors) {
        // Reference entity validation
        if (trade.getReferenceEntity() == null || trade.getReferenceEntity().trim().isEmpty()) {
            fieldErrors.put("referenceEntity", "Reference entity is required");
        }
        
        // Currency validation (basic ISO format)
        if (trade.getCurrency() != null && trade.getCurrency().length() != 3) {
            fieldErrors.put("currency", "Currency must be 3 characters (ISO code)");
        }
    }
    
    /**
     * Check if trade is valid
     * @param trade The trade to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(CDSTrade trade) {
        ValidationResult result = validate(trade);
        return result.isValid();
    }
    
    /**
     * Result object containing validation errors
     */
    public static class ValidationResult {
        private final Map<String, String> fieldErrors;
        private final List<String> globalErrors;
        
        public ValidationResult(Map<String, String> fieldErrors, List<String> globalErrors) {
            this.fieldErrors = fieldErrors;
            this.globalErrors = globalErrors;
        }
        
        public Map<String, String> getFieldErrors() {
            return fieldErrors;
        }
        
        public List<String> getGlobalErrors() {
            return globalErrors;
        }
        
        public boolean isValid() {
            return fieldErrors.isEmpty() && globalErrors.isEmpty();
        }
        
        public boolean hasErrors() {
            return !isValid();
        }
    }
}
