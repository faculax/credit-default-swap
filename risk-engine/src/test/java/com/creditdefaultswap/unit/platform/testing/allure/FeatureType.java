package com.creditdefaultswap.unit.platform.testing.allure;

/**
 * Constants for Allure @Feature annotation values.
 * Used at class or method level to categorize service/component in Behaviors view.
 */
public final class FeatureType {
    public static final String BACKEND_SERVICE = "Backend Service";
    public static final String FRONTEND_SERVICE = "Frontend Service";
    public static final String GATEWAY_SERVICE = "Gateway Service";
    public static final String RISK_ENGINE_SERVICE = "Risk Engine Service";
    
    private FeatureType() {
        // Prevent instantiation
    }
}
