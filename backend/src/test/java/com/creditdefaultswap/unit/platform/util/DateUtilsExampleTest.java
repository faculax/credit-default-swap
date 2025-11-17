package com.creditdefaultswap.unit.platform.util;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import com.creditdefaultswap.unit.platform.testing.allure.EpicType;
import com.creditdefaultswap.unit.platform.testing.allure.FeatureType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example unit test demonstrating proper structure and naming conventions.
 * 
 * This is a pure unit test with no external dependencies or Spring context.
 * Unit tests should be fast (<100ms) and test individual methods in isolation.
 */
@Epic(EpicType.UNIT_TESTS)
class DateUtilsExampleTest {
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Date Utilities - Calculate Days Between")
    void shouldCalculateDaysBetweenTwoDates() {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 10);
        
        // Act
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        
        // Assert
        assertEquals(9, daysBetween, "Should calculate 9 days between Jan 1 and Jan 10");
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Date Utilities - Leap Year Validation")
    void shouldHandleLeapYearCorrectly() {
        // Arrange
        LocalDate leapYear = LocalDate.of(2024, 2, 29);
        LocalDate nonLeapYear = LocalDate.of(2023, 3, 1);
        
        // Act
        boolean is2024LeapYear = leapYear.isLeapYear();
        boolean is2023LeapYear = nonLeapYear.isLeapYear();
        
        // Assert
        assertTrue(is2024LeapYear, "2024 should be a leap year");
        assertFalse(is2023LeapYear, "2023 should not be a leap year");
    }
    
    @Test
    @Feature(FeatureType.BACKEND_SERVICE)
    @Story("Date Utilities - Past Date Validation")
    void shouldValidateDateIsInPast() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(1);
        
        // Act & Assert
        assertTrue(pastDate.isBefore(LocalDate.now()), "Date should be in the past");
        assertFalse(futureDate.isBefore(LocalDate.now()), "Date should not be in the past");
    }
}

