package com.creditdefaultswap.unit.platform.testing.allure;

/**
 * Enum for Allure @Epic annotation values.
 * Used at class level to categorize test type in Behaviors view.
 */
public enum EpicType {
    UNIT_TESTS("Unit Tests"),
    INTEGRATION_TESTS("Integration Tests"),
    E2E_TESTS("E2E Tests");
    
    private final String value;
    
    EpicType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
