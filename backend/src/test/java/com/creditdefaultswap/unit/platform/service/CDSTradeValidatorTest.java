package com.creditdefaultswap.unit.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.service.CDSTradeValidator;
import com.creditdefaultswap.platform.service.CDSTradeValidator.ValidationResult;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CDSTradeValidator
 * Story 3.2: Validation & Business Rules
 */
@Epic(EpicType.UNIT_TESTS)
@ExtendWith(MockitoExtension.class)
class CDSTradeValidatorTest {
    
    private CDSTradeValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new CDSTradeValidator();
    }
    
    // Helper method to create a valid trade
    private CDSTrade createValidTrade() {
        CDSTrade trade = new CDSTrade();
        trade.setReferenceEntity("Test Corp");
        trade.setNotionalAmount(new BigDecimal("1000000.00"));
        trade.setSpread(new BigDecimal("250.0000"));
        trade.setTradeDate(LocalDate.now().minusDays(1));
        trade.setEffectiveDate(LocalDate.now());
        trade.setMaturityDate(LocalDate.now().plusYears(5));
        trade.setAccrualStartDate(LocalDate.now().minusDays(1));
        trade.setCurrency("USD");
        trade.setPremiumFrequency("QUARTERLY");
        trade.setDayCountConvention("ACT_360");
        trade.setPaymentCalendar("NYC");
        trade.setBuySellProtection(CDSTrade.ProtectionDirection.BUY);
        return trade;
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Valid Trade Passes")
    void testValidTrade_ShouldPass() {
        // Arrange
        CDSTrade trade = createValidTrade();
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertTrue(result.isValid());
        assertTrue(result.getFieldErrors().isEmpty());
        assertTrue(result.getGlobalErrors().isEmpty());
        assertTrue(validator.isValid(trade));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Maturity Equals Effective Rejected")
    void testMaturityEqualsEffective_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        LocalDate sameDate = LocalDate.now();
        trade.setEffectiveDate(sameDate);
        trade.setMaturityDate(sameDate);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("maturityDate"));
        assertEquals("Maturity date must be strictly after effective date", 
                    result.getFieldErrors().get("maturityDate"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Effective Before Trade Rejected")
    void testEffectiveBeforeTrade_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setTradeDate(LocalDate.now());
        trade.setEffectiveDate(LocalDate.now().minusDays(1));
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("effectiveDate"));
        assertEquals("Effective date must be on or after trade date", 
                    result.getFieldErrors().get("effectiveDate"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Zero Notional Rejected")
    void testZeroNotional_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setNotionalAmount(BigDecimal.ZERO);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("notionalAmount"));
        assertEquals("Notional amount must be greater than zero", 
                    result.getFieldErrors().get("notionalAmount"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Negative Notional Rejected")
    void testNegativeNotional_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setNotionalAmount(new BigDecimal("-1000000.00"));
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("notionalAmount"));
        assertEquals("Notional amount must be greater than zero", 
                    result.getFieldErrors().get("notionalAmount"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Negative Spread Rejected")
    void testNegativeSpread_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setSpread(new BigDecimal("-50.0000"));
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("spread"));
        assertEquals("Spread cannot be negative", 
                    result.getFieldErrors().get("spread"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Future Trade Date Rejected")
    void testFutureTradeDate_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setTradeDate(LocalDate.now().plusDays(1));
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("tradeDate"));
        assertEquals("Trade date cannot be in the future", 
                    result.getFieldErrors().get("tradeDate"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Today Trade Date Accepted")
    void testTodayTradeDate_ShouldAccept() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setTradeDate(LocalDate.now());
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertTrue(result.isValid());
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Accrual After Effective Rejected")
    void testAccrualAfterEffective_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setEffectiveDate(LocalDate.now());
        trade.setAccrualStartDate(LocalDate.now().plusDays(1));
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("accrualStartDate"));
        assertEquals("Accrual start date must be on or before effective date", 
                    result.getFieldErrors().get("accrualStartDate"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Invalid Premium Frequency Rejected")
    void testInvalidPremiumFrequency_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setPremiumFrequency("INVALID");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("premiumFrequency"));
        assertTrue(result.getFieldErrors().get("premiumFrequency").contains("Invalid premium frequency"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Invalid Day Count Convention Rejected")
    void testInvalidDayCountConvention_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setDayCountConvention("INVALID_DCF");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("dayCountConvention"));
        assertTrue(result.getFieldErrors().get("dayCountConvention").contains("Invalid day count convention"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Invalid Payment Calendar Rejected")
    void testInvalidPaymentCalendar_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setPaymentCalendar("INVALID");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("paymentCalendar"));
        assertTrue(result.getFieldErrors().get("paymentCalendar").contains("Invalid payment calendar"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Invalid Restructuring Clause Rejected")
    void testInvalidRestructuringClause_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setRestructuringClause("INVALID_RC");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("restructuringClause"));
        assertTrue(result.getFieldErrors().get("restructuringClause").contains("Invalid restructuring clause"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Valid Restructuring Clauses Accepted")
    void testValidRestructuringClauses_ShouldAccept() {
        // Test all valid restructuring clause values
        String[] validClauses = {"NO_R", "MR", "MMR", "XR", "CR"};
        
        for (String clause : validClauses) {
            // Arrange
            CDSTrade trade = createValidTrade();
            trade.setRestructuringClause(clause);
            
            // Act
            ValidationResult result = validator.validate(trade);
            
            // Assert
            assertTrue(result.isValid(), "Restructuring clause " + clause + " should be valid");
        }
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Empty Restructuring Clause Accepted")
    void testEmptyRestructuringClause_ShouldAccept() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setRestructuringClause("");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertTrue(result.isValid());
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Null Restructuring Clause Accepted")
    void testNullRestructuringClause_ShouldAccept() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setRestructuringClause(null);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertTrue(result.isValid());
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Null Buy Sell Protection Rejected")
    void testNullBuySellProtection_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setBuySellProtection(null);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("buySellProtection"));
        assertEquals("Buy/Sell protection indicator is required", 
                    result.getFieldErrors().get("buySellProtection"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Invalid Currency Format Rejected")
    void testInvalidCurrencyFormat_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setCurrency("US"); // Too short
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("currency"));
        assertEquals("Currency must be 3 characters (ISO code)", 
                    result.getFieldErrors().get("currency"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Empty Reference Entity Rejected")
    void testEmptyReferenceEntity_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setReferenceEntity("");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("referenceEntity"));
        assertEquals("Reference entity is required", 
                    result.getFieldErrors().get("referenceEntity"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Multiple Errors Aggregated")
    void testMultipleErrors_ShouldReturnAll() {
        // Arrange
        CDSTrade trade = new CDSTrade();
        trade.setNotionalAmount(BigDecimal.ZERO);
        trade.setSpread(new BigDecimal("-10"));
        trade.setTradeDate(LocalDate.now().plusDays(1));
        trade.setEffectiveDate(LocalDate.now());
        trade.setMaturityDate(LocalDate.now()); // Same as effective
        trade.setBuySellProtection(null);
        trade.setReferenceEntity("");
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().size() >= 6, 
                   "Should have at least 6 field errors, got: " + result.getFieldErrors().size());
        assertTrue(result.getFieldErrors().containsKey("notionalAmount"));
        assertTrue(result.getFieldErrors().containsKey("spread"));
        assertTrue(result.getFieldErrors().containsKey("tradeDate"));
        assertTrue(result.getFieldErrors().containsKey("maturityDate"));
        assertTrue(result.getFieldErrors().containsKey("buySellProtection"));
        assertTrue(result.getFieldErrors().containsKey("referenceEntity"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Null Trade Global Error")
    void testNullTrade_ShouldReturnGlobalError() {
        // Act
        ValidationResult result = validator.validate(null);
        
        // Assert
        assertFalse(result.isValid());
        assertEquals(1, result.getGlobalErrors().size());
        assertEquals("Trade object cannot be null", result.getGlobalErrors().get(0));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Valid All Premium Frequencies")
    void testAllValidPremiumFrequencies_ShouldPass() {
        String[] validFrequencies = {"QUARTERLY", "SEMI_ANNUAL", "ANNUAL", "MONTHLY"};
        
        for (String frequency : validFrequencies) {
            // Arrange
            CDSTrade trade = createValidTrade();
            trade.setPremiumFrequency(frequency);
            
            // Act
            ValidationResult result = validator.validate(trade);
            
            // Assert
            assertTrue(result.isValid(), 
                      "Premium frequency " + frequency + " should be valid");
        }
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Valid All Day Count Conventions")
    void testAllValidDayCountConventions_ShouldPass() {
        String[] validConventions = {"ACT_360", "ACT_365", "30_360", "ACT_ACT"};
        
        for (String convention : validConventions) {
            // Arrange
            CDSTrade trade = createValidTrade();
            trade.setDayCountConvention(convention);
            
            // Act
            ValidationResult result = validator.validate(trade);
            
            // Assert
            assertTrue(result.isValid(), 
                      "Day count convention " + convention + " should be valid");
        }
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Valid All Payment Calendars")
    void testAllValidPaymentCalendars_ShouldPass() {
        String[] validCalendars = {"NYC", "LON", "TKY", "TARGET"};
        
        for (String calendar : validCalendars) {
            // Arrange
            CDSTrade trade = createValidTrade();
            trade.setPaymentCalendar(calendar);
            
            // Act
            ValidationResult result = validator.validate(trade);
            
            // Assert
            assertTrue(result.isValid(), 
                      "Payment calendar " + calendar + " should be valid");
        }
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Zero Spread Accepted")
    void testZeroSpread_ShouldAccept() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setSpread(BigDecimal.ZERO);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertTrue(result.isValid(), "Zero spread should be valid (spread >= 0)");
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Null Notional Rejected")
    void testNullNotional_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setNotionalAmount(null);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("notionalAmount"));
        assertEquals("Notional amount is required", 
                    result.getFieldErrors().get("notionalAmount"));
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("CDS Trade Validation - Null Spread Rejected")
    void testNullSpread_ShouldReject() {
        // Arrange
        CDSTrade trade = createValidTrade();
        trade.setSpread(null);
        
        // Act
        ValidationResult result = validator.validate(trade);
        
        // Assert
        assertFalse(result.isValid());
        assertTrue(result.getFieldErrors().containsKey("spread"));
        assertEquals("Spread is required", 
                    result.getFieldErrors().get("spread"));
    }
}
