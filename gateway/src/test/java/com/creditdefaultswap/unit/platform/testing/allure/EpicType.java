package com.creditdefaultswap.unit.platform.testing.allure;

/**
 * Constants for Allure @Epic annotation values.
 * Used at class level to categorize test type in Behaviors view.
 */
public final class EpicType {
    public static final String UNIT_TESTS = "Unit Tests";
    public static final String INTEGRATION_TESTS = "Integration Tests";
    public static final String E2E_TESTS = "E2E Tests";
    
    private EpicType() {
        // Prevent instantiation
    }
}
