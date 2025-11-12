package com.creditdefaultswap.platform.lineage;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * JPA EntityListener that automatically tracks database operations.
 * Attach this to entities using @EntityListeners(LineageEntityListener.class)
 * 
 * Epic 10: Data Lineage - Automated Operation Tracking
 */
public class LineageEntityListener {
    
    private static final Logger logger = LoggerFactory.getLogger(LineageEntityListener.class);
    
    /**
     * Called after an entity is loaded from database (SELECT)
     */
    @PostLoad
    public void onEntityLoad(Object entity) {
        // Track in legacy tracker
        if (DatabaseOperationTracker.isTrackingEnabled()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                DatabaseOperationTracker.recordRead(tableName, entityId);
            } catch (Exception e) {
                logger.warn("Failed to track entity load (legacy): {}", e.getMessage());
            }
        }
        
        // Track in correlation context
        if (RequestCorrelationContext.isInitialized()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                RequestCorrelationContext.recordEntityOperation(tableName, "READ", entityId);
            } catch (Exception e) {
                logger.warn("Failed to track entity load (correlation): {}", e.getMessage());
            }
        }
    }
    
    /**
     * Called before an entity is persisted to database (INSERT)
     */
    @PrePersist
    public void onPrePersist(Object entity) {
        // Track in legacy tracker
        if (DatabaseOperationTracker.isTrackingEnabled()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                DatabaseOperationTracker.recordWrite(tableName, entityId, 
                    DatabaseOperationTracker.OperationType.INSERT);
            } catch (Exception e) {
                logger.warn("Failed to track entity pre-persist (legacy): {}", e.getMessage());
            }
        }
        
        // Track in correlation context
        if (RequestCorrelationContext.isInitialized()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                RequestCorrelationContext.recordEntityOperation(tableName, "INSERT", entityId);
            } catch (Exception e) {
                logger.warn("Failed to track entity pre-persist (correlation): {}", e.getMessage());
            }
        }
    }
    
    /**
     * Called after an entity is persisted to database (INSERT)
     */
    @PostPersist
    public void onPostPersist(Object entity) {
        // Track in correlation context with generated ID
        if (RequestCorrelationContext.isInitialized()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                RequestCorrelationContext.recordEntityOperation(tableName, "INSERT", entityId);
            } catch (Exception e) {
                logger.warn("Failed to track entity post-persist (correlation): {}", e.getMessage());
            }
        }
    }
    
    /**
     * Called before an entity is updated in database (UPDATE)
     */
    @PreUpdate
    public void onPreUpdate(Object entity) {
        // Track in legacy tracker
        if (DatabaseOperationTracker.isTrackingEnabled()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                DatabaseOperationTracker.recordWrite(tableName, entityId, 
                    DatabaseOperationTracker.OperationType.UPDATE);
            } catch (Exception e) {
                logger.warn("Failed to track entity pre-update (legacy): {}", e.getMessage());
            }
        }
        
        // Track in correlation context
        if (RequestCorrelationContext.isInitialized()) {
            try {
                String tableName = DatabaseOperationTracker.extractTableName(entity.getClass());
                Object entityId = extractEntityId(entity);
                RequestCorrelationContext.recordEntityOperation(tableName, "UPDATE", entityId);
            } catch (Exception e) {
                logger.warn("Failed to track entity pre-update (correlation): {}", e.getMessage());
            }
        }
    }
    
    /**
     * Called after an entity is updated in database (UPDATE)
     */
    @PostUpdate
    public void onPostUpdate(Object entity) {
        // No additional tracking needed - PreUpdate handles it
    }
    
    /**
     * Extract entity ID using reflection (looks for @Id annotation or getId() method)
     */
    private Object extractEntityId(Object entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            // Try to find @Id annotated field
            Class<?> entityClass = entity.getClass();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
            
            // Fallback: try getId() method
            try {
                java.lang.reflect.Method getIdMethod = entityClass.getMethod("getId");
                return getIdMethod.invoke(entity);
            } catch (NoSuchMethodException e) {
                // No getId() method found
            }
            
        } catch (Exception e) {
            logger.debug("Could not extract entity ID: {}", e.getMessage());
        }
        
        return null;
    }
}
