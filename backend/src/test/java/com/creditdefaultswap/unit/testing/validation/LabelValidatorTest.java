package com.creditdefaultswap.unit.testing.validation;

import com.creditdefaultswap.testing.validation.LabelValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelValidatorTest {

    private LabelValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LabelValidator();
    }

    @Test
    void shouldLoadSchemaSuccessfully() {
        assertNotNull(validator.getValidTestTypes());
        assertNotNull(validator.getValidServices());
        assertNotNull(validator.getValidMicroservices());
    }

    @Test
    void shouldContainExpectedTestTypes() {
        assertTrue(validator.getValidTestTypes().contains("unit"));
        assertTrue(validator.getValidTestTypes().contains("integration"));
        assertTrue(validator.getValidTestTypes().contains("contract"));
        assertTrue(validator.getValidTestTypes().contains("e2e"));
    }

    @Test
    void shouldContainExpectedServices() {
        assertTrue(validator.getValidServices().contains("backend"));
        assertTrue(validator.getValidServices().contains("frontend"));
    }

    @Test
    void shouldContainExpectedMicroservices() {
        assertTrue(validator.getValidMicroservices().contains("cds-platform"));
        assertTrue(validator.getValidMicroservices().contains("gateway"));
        assertTrue(validator.getValidMicroservices().contains("risk-engine"));
        assertTrue(validator.getValidMicroservices().contains("risk-ui"));
    }

    @Test
    void shouldValidateValidTestType() {
        assertDoesNotThrow(() -> validator.validateTestType("unit"));
        assertDoesNotThrow(() -> validator.validateTestType("integration"));
    }

    @Test
    void shouldRejectInvalidTestType() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateTestType("invalid")
        );
        assertTrue(exception.getMessage().contains("Invalid testType"));
    }

    @Test
    void shouldRejectNullTestType() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateTestType(null)
        );
        assertTrue(exception.getMessage().contains("cannot be null"));
    }

    @Test
    void shouldValidateValidService() {
        assertDoesNotThrow(() -> validator.validateService("backend"));
        assertDoesNotThrow(() -> validator.validateService("frontend"));
    }

    @Test
    void shouldRejectInvalidService() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateService("invalid-service")
        );
        assertTrue(exception.getMessage().contains("Invalid service"));
    }

    @Test
    void shouldValidateValidMicroservice() {
        assertDoesNotThrow(() -> validator.validateMicroservice("cds-platform"));
        assertDoesNotThrow(() -> validator.validateMicroservice("risk-ui"));
    }

    @Test
    void shouldRejectInvalidMicroservice() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateMicroservice("unknown-service")
        );
        assertTrue(exception.getMessage().contains("Invalid microservice"));
    }

    @Test
    void shouldAllowNullMicroservice() {
        // Microservice is optional
        assertDoesNotThrow(() -> validator.validateMicroservice(null));
        assertDoesNotThrow(() -> validator.validateMicroservice(""));
    }

    @Test
    void shouldValidateAllLabelsAtOnce() {
        assertDoesNotThrow(() -> 
            validator.validateLabels("unit", "backend", "cds-platform")
        );
        
        assertDoesNotThrow(() -> 
            validator.validateLabels("integration", "frontend", null)
        );
    }

    @Test
    void shouldRejectInvalidLabelsInBatch() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateLabels("invalid", "backend", "cds-platform")
        );
        assertTrue(exception.getMessage().contains("Invalid testType"));
    }

    @Test
    void shouldCheckValidityWithoutThrowing() {
        assertTrue(validator.isValidTestType("unit"));
        assertFalse(validator.isValidTestType("invalid"));
        assertFalse(validator.isValidTestType(null));

        assertTrue(validator.isValidService("backend"));
        assertFalse(validator.isValidService("invalid"));

        assertTrue(validator.isValidMicroservice("cds-platform"));
        assertTrue(validator.isValidMicroservice(null));
        assertTrue(validator.isValidMicroservice(""));
        assertFalse(validator.isValidMicroservice("invalid"));
    }
}
