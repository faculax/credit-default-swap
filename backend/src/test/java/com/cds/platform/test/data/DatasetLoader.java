package com.cds.platform.test.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for loading test datasets from the test data registry
 * 
 * Usage:
 * <pre>
 * CDSTrade trade = DatasetLoader.load("cds-trades/single-name-basic.json", CDSTrade.class);
 * </pre>
 */
public class DatasetLoader {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    private static final String DATASETS_PATH = "datasets/";
    
    /**
     * Load a dataset from the registry and deserialize to target type
     * 
     * @param datasetPath Path to dataset relative to datasets/ (e.g. "cds-trades/single-name-basic.json")
     * @param targetType Target class to deserialize to
     * @return Deserialized object
     * @throws IOException if dataset cannot be loaded or parsed
     */
    public static <T> T load(String datasetPath, Class<T> targetType) throws IOException {
        Resource resource = new ClassPathResource(DATASETS_PATH + datasetPath);
        
        try (InputStream inputStream = resource.getInputStream()) {
            // Parse the wrapper
            JsonNode wrapper = objectMapper.readTree(inputStream);
            
            // Validate version (optional - could log warning if outdated)
            String version = wrapper.get("version").asText();
            
            // Extract the actual data
            JsonNode data = wrapper.get("data");
            
            // Deserialize to target type
            return objectMapper.treeToValue(data, targetType);
        }
    }
    
    /**
     * Load raw JSON node for custom processing
     * 
     * @param datasetPath Path to dataset
     * @return JsonNode of the data field
     * @throws IOException if dataset cannot be loaded
     */
    public static JsonNode loadRaw(String datasetPath) throws IOException {
        Resource resource = new ClassPathResource(DATASETS_PATH + datasetPath);
        
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode wrapper = objectMapper.readTree(inputStream);
            return wrapper.get("data");
        }
    }
    
    /**
     * Load dataset metadata (version, checksum, description)
     * 
     * @param datasetPath Path to dataset
     * @return DatasetMetadata object
     * @throws IOException if dataset cannot be loaded
     */
    public static DatasetMetadata loadMetadata(String datasetPath) throws IOException {
        Resource resource = new ClassPathResource(DATASETS_PATH + datasetPath);
        
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode wrapper = objectMapper.readTree(inputStream);
            
            return new DatasetMetadata(
                wrapper.get("version").asText(),
                wrapper.has("checksum") ? wrapper.get("checksum").asText() : null,
                wrapper.get("lastUpdated").asText(),
                wrapper.get("description").asText()
            );
        }
    }
    
    /**
     * Validate dataset checksum (optional verification)
     * 
     * @param datasetPath Path to dataset
     * @return true if checksum matches, false otherwise
     * @throws IOException if dataset cannot be loaded
     */
    public static boolean validateChecksum(String datasetPath) throws IOException {
        Resource resource = new ClassPathResource(DATASETS_PATH + datasetPath);
        
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] dataBytes = inputStream.readAllBytes();
            
            // Calculate actual checksum
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataBytes);
            String actualChecksum = "sha256:" + Base64.getEncoder().encodeToString(hash);
            
            // Parse expected checksum
            JsonNode wrapper = objectMapper.readTree(dataBytes);
            String expectedChecksum = wrapper.get("checksum").asText();
            
            return actualChecksum.equals(expectedChecksum);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Get the ObjectMapper instance for custom operations
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * Dataset metadata holder
     */
    public static class DatasetMetadata {
        private final String version;
        private final String checksum;
        private final String lastUpdated;
        private final String description;
        
        public DatasetMetadata(String version, String checksum, String lastUpdated, String description) {
            this.version = version;
            this.checksum = checksum;
            this.lastUpdated = lastUpdated;
            this.description = description;
        }
        
        public String getVersion() { return version; }
        public String getChecksum() { return checksum; }
        public String getLastUpdated() { return lastUpdated; }
        public String getDescription() { return description; }
        
        @Override
        public String toString() {
            return String.format("DatasetMetadata{version='%s', description='%s', lastUpdated='%s'}", 
                version, description, lastUpdated);
        }
    }
}
