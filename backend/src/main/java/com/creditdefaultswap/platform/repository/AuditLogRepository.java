package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    /**
     * Find audit logs for a specific entity
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
        AuditLog.EntityType entityType, 
        String entityId
    );
    
    /**
     * Find audit logs by correlation ID
     */
    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(UUID correlationId);
    
    /**
     * Find audit logs by actor
     */
    List<AuditLog> findByActorOrderByTimestampDesc(String actor);
    
    /**
     * Find audit logs within date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(
        @Param("from") LocalDateTime from, 
        @Param("to") LocalDateTime to
    );
}