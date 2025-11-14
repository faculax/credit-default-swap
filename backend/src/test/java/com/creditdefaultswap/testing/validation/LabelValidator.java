package com.creditdefaultswap.testing.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Validates test labels against the unified label schema.
 * 
 * This utility ensures that all test labels (testType, service, microservice)
 * conform to the centralized schema defined in unified-testing-config/label-schema.json.
 */
public class LabelValidator {

    private final Set<String> validTestTypes;
    private final Set<String> validServices;
    private final Set<String> validMicroservices;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new LabelValidator by loading the label schema from classpath.
     * 
     * @throws IllegalStateException if schema cannot be loaded or parsed
     */
    public LabelValidator() {
        this.objectMapper = new ObjectMapper();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("label-schema.json")) {
            if (is == null) {
                throw new IllegalStateException(
                    "label-schema.json not found in classpath. " +
                    "Ensure unified-testing-config/label-schema.json is included in test resources."
                );
            }
            
            JsonNode schema = objectMapper.readTree(is);
            
            this.validTestTypes = extractIds(schema, "testTypes", "name");
            this.validServices = extractIds(schema, "services", "id");
            this.validMicroservices = extractIds(schema, "microservices", "id");
            
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load label schema", e);
        }
    }

    /**
     * Extracts IDs from a JSON array in the schema.
     */
    private Set<String> extractIds(JsonNode schema, String arrayName, String idField) {
        JsonNode array = schema.get(arrayName);
        if (array == null || !array.isArray()) {
            throw new IllegalStateException("Missing or invalid '" + arrayName + "' in schema");
        }
        
        Set<String> ids = new HashSet<>();
        for (JsonNode item : array) {
            JsonNode idNode = item.get(idField);
            if (idNode != null) {
                ids.add(idNode.asText());
            }
        }
        return ids;
    }

    /**
     * Validates a testType label value.
     * 
     * @param testType the test type to validate
     * @throws IllegalArgumentException if testType is invalid
     */
    public void validateTestType(String testType) {
        if (testType == null || testType.trim().isEmpty()) {
            throw new IllegalArgumentException("testType cannot be null or empty");
        }
        
        if (!validTestTypes.contains(testType)) {
            throw new IllegalArgumentException(
                String.format("Invalid testType '%s'. Valid values: %s",
                    testType, String.join(", ", validTestTypes))
            );
        }
    }

    /**
     * Validates a service label value.
     * 
     * @param service the service to validate
     * @throws IllegalArgumentException if service is invalid
     */
    public void validateService(String service) {
        if (service == null || service.trim().isEmpty()) {
            throw new IllegalArgumentException("service cannot be null or empty");
        }
        
        if (!validServices.contains(service)) {
            throw new IllegalArgumentException(
                String.format("Invalid service '%s'. Valid values: %s",
                    service, String.join(", ", validServices))
            );
        }
    }

    /**
     * Validates a microservice label value.
     * 
     * @param microservice the microservice to validate
     * @throws IllegalArgumentException if microservice is invalid
     */
    public void validateMicroservice(String microservice) {
        if (microservice == null || microservice.trim().isEmpty()) {
            return; // microservice is optional
        }
        
        if (!validMicroservices.contains(microservice)) {
            throw new IllegalArgumentException(
                String.format("Invalid microservice '%s'. Valid values: %s",
                    microservice, String.join(", ", validMicroservices))
            );
        }
    }

    /**
     * Validates all labels together.
     * 
     * @param testType the test type
     * @param service the service
     * @param microservice the microservice (optional)
     * @throws IllegalArgumentException if any label is invalid
     */
    public void validateLabels(String testType, String service, String microservice) {
        validateTestType(testType);
        validateService(service);
        if (microservice != null && !microservice.trim().isEmpty()) {
            validateMicroservice(microservice);
        }
    }

    /**
     * Gets all valid test type values.
     */
    public Set<String> getValidTestTypes() {
        return Collections.unmodifiableSet(validTestTypes);
    }

    /**
     * Gets all valid service values.
     */
    public Set<String> getValidServices() {
        return Collections.unmodifiableSet(validServices);
    }

    /**
     * Gets all valid microservice values.
     */
    public Set<String> getValidMicroservices() {
        return Collections.unmodifiableSet(validMicroservices);
    }

    /**
     * Checks if a testType is valid without throwing an exception.
     */
    public boolean isValidTestType(String testType) {
        return testType != null && validTestTypes.contains(testType);
    }

    /**
     * Checks if a service is valid without throwing an exception.
     */
    public boolean isValidService(String service) {
        return service != null && validServices.contains(service);
    }

    /**
     * Checks if a microservice is valid without throwing an exception.
     */
    public boolean isValidMicroservice(String microservice) {
        return microservice == null || microservice.trim().isEmpty() || validMicroservices.contains(microservice);
    }
}
