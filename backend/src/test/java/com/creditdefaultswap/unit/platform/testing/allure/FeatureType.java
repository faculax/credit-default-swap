package com.creditdefaultswap.unit.platform.testing.allure;

/**
 * Enum for Allure @Feature annotation values.
 * Used at class or method level to categorize service/component in Behaviors view.
 */
public enum FeatureType {
    BACKEND_SERVICE("Backend Service"),
    FRONTEND_SERVICE("Frontend Service"),
    GATEWAY_SERVICE("Gateway Service"),
    RISK_ENGINE_SERVICE("Risk Engine Service");
    
    private final String value;
    
    FeatureType(String value) {
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
