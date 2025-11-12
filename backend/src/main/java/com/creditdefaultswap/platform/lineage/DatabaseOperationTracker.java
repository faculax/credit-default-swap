package com.creditdefaultswap.platform.lineage;

import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe tracker for database operations during request execution.
 * Automatically captures actual table reads and writes at runtime.
 * 
 * Epic 10: Data Lineage - Automated Operation Tracking
 */
public class DatabaseOperationTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseOperationTracker.class);
    
    // Thread-local storage for per-request tracking
    private static final ThreadLocal<Set<TableOperation>> OPERATIONS = 
        ThreadLocal.withInitial(LinkedHashSet::new);
    
    // Track if tracking is enabled for current thread
    private static final ThreadLocal<Boolean> TRACKING_ENABLED = 
        ThreadLocal.withInitial(() -> false);
    
    // Store correlation metadata for enrichment
    private static final ThreadLocal<Map<String, Object>> CORRELATION_METADATA = 
        ThreadLocal.withInitial(LinkedHashMap::new);
    
    /**
     * Enable tracking for current thread (called at start of AOP interceptor)
     */
    public static void enableTracking() {
        TRACKING_ENABLED.set(true);
        OPERATIONS.get().clear();
        // DO NOT clear CORRELATION_METADATA - it's set by EnhancedLineageAspect before tracking is enabled
        // CORRELATION_METADATA.get().clear();
        logger.debug("Database operation tracking enabled for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Disable tracking for current thread (called at end of AOP interceptor)
     */
    public static void disableTracking() {
        TRACKING_ENABLED.set(false);
        logger.debug("Database operation tracking disabled for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Check if tracking is enabled for current thread
     */
    public static boolean isTrackingEnabled() {
        return TRACKING_ENABLED.get();
    }
    
    /**
     * Record a database read operation
     */
    public static void recordRead(String tableName, Object entityId) {
        if (!isTrackingEnabled()) {
            return;
        }
        
        TableOperation operation = new TableOperation(
            tableName, 
            OperationType.READ, 
            entityId != null ? entityId.toString() : null
        );
        
        OPERATIONS.get().add(operation);
        logger.trace("Recorded READ: table={}, id={}", tableName, entityId);
    }
    
    /**
     * Record a database write operation (insert or update)
     */
    public static void recordWrite(String tableName, Object entityId, OperationType writeType) {
        if (!isTrackingEnabled()) {
            return;
        }
        
        TableOperation operation = new TableOperation(tableName, writeType, 
            entityId != null ? entityId.toString() : null);
        
        OPERATIONS.get().add(operation);
        logger.trace("Recorded {}: table={}, id={}", writeType, tableName, entityId);
    }
    
    /**
     * Get all tracked operations for current thread
     */
    public static Set<TableOperation> getTrackedOperations() {
        return new LinkedHashSet<>(OPERATIONS.get());
    }
    
    /**
     * Get only read operations
     */
    public static Set<TableOperation> getReadOperations() {
        Set<TableOperation> reads = new LinkedHashSet<>();
        for (TableOperation op : OPERATIONS.get()) {
            if (op.getType() == OperationType.READ) {
                reads.add(op);
            }
        }
        return reads;
    }
    
    /**
     * Get only write operations (inserts and updates)
     */
    public static Set<TableOperation> getWriteOperations() {
        Set<TableOperation> writes = new LinkedHashSet<>();
        for (TableOperation op : OPERATIONS.get()) {
            if (op.getType() == OperationType.INSERT || op.getType() == OperationType.UPDATE) {
                writes.add(op);
            }
        }
        return writes;
    }
    
    /**
     * Get operations grouped by table name
     */
    public static Map<String, List<TableOperation>> getOperationsByTable() {
        Map<String, List<TableOperation>> grouped = new LinkedHashMap<>();
        for (TableOperation op : OPERATIONS.get()) {
            grouped.computeIfAbsent(op.getTableName(), k -> new ArrayList<>()).add(op);
        }
        return grouped;
    }
    
    /**
     * Clear tracked operations (called at end of request)
     */
    public static void clear() {
        OPERATIONS.remove();
        TRACKING_ENABLED.remove();
        CORRELATION_METADATA.remove();
        logger.debug("Cleared database operation tracking for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Set correlation metadata for this request
     */
    public static void setCorrelationMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            CORRELATION_METADATA.get().putAll(metadata);
            logger.debug("Set correlation metadata: {} fields", metadata.size());
        }
    }
    
    /**
     * Get correlation metadata for this request
     */
    public static Map<String, Object> getCorrelationMetadata() {
        return new LinkedHashMap<>(CORRELATION_METADATA.get());
    }
    
    /**
     * Extract table name from entity class
     */
    public static String extractTableName(Class<?> entityClass) {
        if (entityClass == null) {
            return "unknown";
        }
        
        // Check for @Table annotation
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        
        // Fallback: convert class name to snake_case table name
        String className = entityClass.getSimpleName();
        String tableName = toSnakeCase(className);
        
        // Handle common pluralization (simple rules)
        if (!tableName.endsWith("s")) {
            tableName += "s";
        }
        
        return tableName;
    }
    
    /**
     * Convert CamelCase to snake_case
     */
    private static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Data class representing a database operation
     */
    public static class TableOperation {
        private final String tableName;
        private final OperationType type;
        private final String entityId;
        private final long timestamp;
        
        public TableOperation(String tableName, OperationType type, String entityId) {
            this.tableName = tableName;
            this.type = type;
            this.entityId = entityId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public OperationType getType() {
            return type;
        }
        
        public String getEntityId() {
            return entityId;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableOperation that = (TableOperation) o;
            return Objects.equals(tableName, that.tableName) && 
                   type == that.type && 
                   Objects.equals(entityId, that.entityId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(tableName, type, entityId);
        }
        
        @Override
        public String toString() {
            return String.format("%s(%s, id=%s)", type, tableName, entityId);
        }
    }
    
    /**
     * Enum for database operation types
     */
    public enum OperationType {
        READ,       // SELECT
        INSERT,     // INSERT
        UPDATE      // UPDATE
    }
}
