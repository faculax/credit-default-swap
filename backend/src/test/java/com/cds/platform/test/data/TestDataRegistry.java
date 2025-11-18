package com.cds.platform.test.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Singleton registry for test datasets
 * Provides fast lookup and querying of available datasets
 * 
 * Usage:
 * <pre>
 * TestDataRegistry registry = TestDataRegistry.getInstance();
 * DatasetEntry entry = registry.getDataset("cds-trades/single-name-basic");
 * List<DatasetEntry> cdsDatasets = registry.getDatasetsByType("CDSTrade");
 * </pre>
 */
public class TestDataRegistry {
    
    private static volatile TestDataRegistry instance;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Map<String, DatasetEntry> datasetsByPath = new ConcurrentHashMap<>();
    private final Map<String, List<DatasetEntry>> datasetsByType = new ConcurrentHashMap<>();
    private final Map<String, List<DatasetEntry>> datasetsByCategory = new ConcurrentHashMap<>();
    private final Map<String, List<DatasetEntry>> datasetsByTag = new ConcurrentHashMap<>();
    
    private TestDataRegistry() {
        loadRegistry();
    }
    
    /**
     * Get singleton instance
     */
    public static TestDataRegistry getInstance() {
        if (instance == null) {
            synchronized (TestDataRegistry.class) {
                if (instance == null) {
                    instance = new TestDataRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Load registry from datasets/registry.json
     */
    private void loadRegistry() {
        try {
            ClassPathResource resource = new ClassPathResource("datasets/registry.json");
            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(inputStream);
                JsonNode datasets = root.get("datasets");
                
                if (datasets.isArray()) {
                    for (JsonNode node : datasets) {
                        DatasetEntry entry = parseEntry(node);
                        datasetsByPath.put(entry.getPath(), entry);
                        
                        // Index by type
                        datasetsByType.computeIfAbsent(entry.getType(), k -> new ArrayList<>()).add(entry);
                        
                        // Index by category (first part of path)
                        String category = entry.getPath().split("/")[0];
                        datasetsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);
                        
                        // Index by tags
                        for (String tag : entry.getTags()) {
                            datasetsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(entry);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data registry", e);
        }
    }
    
    private DatasetEntry parseEntry(JsonNode node) {
        return new DatasetEntry(
            node.get("id").asText(),
            node.get("path").asText(),
            node.get("version").asText(),
            node.has("checksum") ? node.get("checksum").asText() : null,
            node.get("type").asText(),
            node.get("description").asText(),
            parseTags(node.get("tags")),
            parseUsedBy(node.get("usedBy")),
            parseDependencies(node.has("dependencies") ? node.get("dependencies") : null),
            node.has("validFrom") ? node.get("validFrom").asText() : null,
            node.has("validTo") ? node.get("validTo").asText() : null
        );
    }
    
    private List<String> parseTags(JsonNode tagsNode) {
        List<String> tags = new ArrayList<>();
        if (tagsNode != null && tagsNode.isArray()) {
            tagsNode.forEach(tag -> tags.add(tag.asText()));
        }
        return tags;
    }
    
    private List<String> parseUsedBy(JsonNode usedByNode) {
        List<String> usedBy = new ArrayList<>();
        if (usedByNode != null && usedByNode.isArray()) {
            usedByNode.forEach(test -> usedBy.add(test.asText()));
        }
        return usedBy;
    }
    
    private List<String> parseDependencies(JsonNode depsNode) {
        List<String> dependencies = new ArrayList<>();
        if (depsNode != null && depsNode.isArray()) {
            depsNode.forEach(dep -> dependencies.add(dep.asText()));
        }
        return dependencies;
    }
    
    /**
     * Get dataset by path
     * @param path Dataset path (e.g. "cds-trades/single-name-basic")
     */
    public DatasetEntry getDataset(String path) {
        return datasetsByPath.get(path);
    }
    
    /**
     * Get all datasets of a specific type
     * @param type Dataset type (e.g. "CDSTrade", "DiscountCurve")
     */
    public List<DatasetEntry> getDatasetsByType(String type) {
        return datasetsByType.getOrDefault(type, Collections.emptyList());
    }
    
    /**
     * Get all datasets in a category
     * @param category Category name (e.g. "cds-trades", "market-data")
     */
    public List<DatasetEntry> getDatasetsByCategory(String category) {
        return datasetsByCategory.getOrDefault(category, Collections.emptyList());
    }
    
    /**
     * Get all datasets with a specific tag
     * @param tag Tag name (e.g. "single-name", "integration-test")
     */
    public List<DatasetEntry> getDatasetsByTag(String tag) {
        return datasetsByTag.getOrDefault(tag, Collections.emptyList());
    }
    
    /**
     * Get all available datasets
     */
    public Collection<DatasetEntry> getAllDatasets() {
        return datasetsByPath.values();
    }
    
    /**
     * Get all available types
     */
    public Set<String> getAvailableTypes() {
        return datasetsByType.keySet();
    }
    
    /**
     * Get all available categories
     */
    public Set<String> getAvailableCategories() {
        return datasetsByCategory.keySet();
    }
    
    /**
     * Get all available tags
     */
    public Set<String> getAvailableTags() {
        return datasetsByTag.keySet();
    }
    
    /**
     * Find datasets used by a specific test
     * @param testName Test class or method name
     */
    public List<DatasetEntry> getDatasetsUsedByTest(String testName) {
        return datasetsByPath.values().stream()
            .filter(entry -> entry.getUsedBy().contains(testName))
            .collect(Collectors.toList());
    }
    
    /**
     * Dataset registry entry
     */
    public static class DatasetEntry {
        private final String id;
        private final String path;
        private final String version;
        private final String checksum;
        private final String type;
        private final String description;
        private final List<String> tags;
        private final List<String> usedBy;
        private final List<String> dependencies;
        private final String validFrom;
        private final String validTo;
        
        public DatasetEntry(String id, String path, String version, String checksum, 
                          String type, String description, List<String> tags,
                          List<String> usedBy, List<String> dependencies,
                          String validFrom, String validTo) {
            this.id = id;
            this.path = path;
            this.version = version;
            this.checksum = checksum;
            this.type = type;
            this.description = description;
            this.tags = Collections.unmodifiableList(tags);
            this.usedBy = Collections.unmodifiableList(usedBy);
            this.dependencies = Collections.unmodifiableList(dependencies);
            this.validFrom = validFrom;
            this.validTo = validTo;
        }
        
        public String getId() { return id; }
        public String getPath() { return path; }
        public String getVersion() { return version; }
        public String getChecksum() { return checksum; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public List<String> getTags() { return tags; }
        public List<String> getUsedBy() { return usedBy; }
        public List<String> getDependencies() { return dependencies; }
        public String getValidFrom() { return validFrom; }
        public String getValidTo() { return validTo; }
        
        public boolean hasTag(String tag) {
            return tags.contains(tag);
        }
        
        public boolean hasDependencies() {
            return !dependencies.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("DatasetEntry{id='%s', path='%s', type='%s', version='%s'}", 
                id, path, type, version);
        }
    }
}
