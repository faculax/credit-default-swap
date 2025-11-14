package com.creditdefaultswap.platform.testing;

/**
 * Test Type Label Schema Integration
 * 
 * <p>This package contains constants and utilities for consistent test labeling
 * across all backend services. These values are derived from the shared
 * test-type-schema.json at the repository root.</p>
 * 
 * @see <a href="../../../../../../../schema/test-type-schema.json">test-type-schema.json</a>
 */
public final class TestLabels {
    
    private TestLabels() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Valid test type identifiers as defined in schema testTypes[].id
     */
    public static final class TestType {
        public static final String UNIT = "unit";
        public static final String INTEGRATION = "integration";
        public static final String CONTRACT = "contract";
        public static final String E2E = "e2e";
        public static final String PERFORMANCE = "performance";
        public static final String SECURITY = "security";
        
        private TestType() {
            throw new UnsupportedOperationException("Utility class");
        }
        
        /**
         * Validates if a test type is recognized by the schema.
         * 
         * @param testType test type string to validate
         * @return true if valid, false otherwise
         */
        public static boolean isValid(String testType) {
            return testType != null && (
                testType.equals(UNIT) ||
                testType.equals(INTEGRATION) ||
                testType.equals(CONTRACT) ||
                testType.equals(E2E) ||
                testType.equals(PERFORMANCE) ||
                testType.equals(SECURITY)
            );
        }
    }
    
    /**
     * Valid service identifiers as defined in schema microservices[].id
     */
    public static final class Service {
        public static final String FRONTEND = "frontend";
        public static final String BACKEND = "backend";
        public static final String GATEWAY = "gateway";
        public static final String RISK_ENGINE = "risk-engine";
        
        private Service() {
            throw new UnsupportedOperationException("Utility class");
        }
        
        /**
         * Validates if a service is recognized by the schema.
         * 
         * @param service service string to validate
         * @return true if valid, false otherwise
         */
        public static boolean isValid(String service) {
            return service != null && (
                service.equals(FRONTEND) ||
                service.equals(BACKEND) ||
                service.equals(GATEWAY) ||
                service.equals(RISK_ENGINE)
            );
        }
    }
    
    /**
     * Valid severity levels for test prioritization as defined in schema.
     */
    public static final class Severity {
        public static final String TRIVIAL = "trivial";
        public static final String MINOR = "minor";
        public static final String NORMAL = "normal";
        public static final String CRITICAL = "critical";
        public static final String BLOCKER = "blocker";
        
        private Severity() {
            throw new UnsupportedOperationException("Utility class");
        }
        
        /**
         * Validates if a severity is recognized by the schema.
         * 
         * @param severity severity string to validate
         * @return true if valid, false otherwise
         */
        public static boolean isValid(String severity) {
            return severity != null && (
                severity.equals(TRIVIAL) ||
                severity.equals(MINOR) ||
                severity.equals(NORMAL) ||
                severity.equals(CRITICAL) ||
                severity.equals(BLOCKER)
            );
        }
    }
    
    /**
     * Story ID pattern as defined in schema validationRules.storyIdPattern
     * 
     * <p>Format: PREFIX-NUMBER or PREFIX-NUMBER.NUMBER</p>
     * <p>Examples: UTS-2.3, PROJ-123, EPIC-5</p>
     */
    public static final String STORY_ID_PATTERN = "^(UTS|PROJ|EPIC)-\\d+(\\.\\d+)?$";
    
    /**
     * Validates story ID format against schema pattern.
     * 
     * @param storyId story ID to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidStoryId(String storyId) {
        return storyId != null && storyId.matches(STORY_ID_PATTERN);
    }
    
    /**
     * Required labels that must be present on every test per schema.
     */
    public static final class RequiredLabels {
        public static final String STORY_ID = "storyId";
        public static final String TEST_TYPE = "testType";
        public static final String SERVICE = "service";
        
        private RequiredLabels() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
    
    /**
     * Optional labels that can enhance test reporting.
     */
    public static final class OptionalLabels {
        public static final String MICROSERVICE = "microservice";
        public static final String SEVERITY = "severity";
        public static final String EPIC = "epic";
        public static final String FEATURE = "feature";
        public static final String OWNER = "owner";
        
        private OptionalLabels() {
            throw new UnsupportedOperationException("Utility class");
        }
    }
}
