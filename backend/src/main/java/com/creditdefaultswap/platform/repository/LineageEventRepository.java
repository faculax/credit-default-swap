package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.LineageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LineageEventRepository extends JpaRepository<LineageEvent, UUID> {

    List<LineageEvent> findByDatasetOrderByCreatedAtDesc(String dataset);
    
    List<LineageEvent> findByDatasetOrderByCreatedAtAsc(String dataset);
    
    List<LineageEvent> findByDatasetAndCreatedAtAfterOrderByCreatedAtAsc(String dataset, OffsetDateTime createdAt);

    List<LineageEvent> findByRunIdOrderByCreatedAtDesc(String runId);
    
    @Query("SELECT e FROM LineageEvent e WHERE " +
           "CAST(e.outputs AS string) LIKE CONCAT('%', :correlationId, '%') " +
           "OR CAST(e.inputs AS string) LIKE CONCAT('%', :correlationId, '%')")
    List<LineageEvent> findByCorrelationId(@Param("correlationId") String correlationId);
    
    /**
     * Find events that touched a specific table (either as primary dataset or in tracked tables)
     * Using CAST instead of :: to avoid Hibernate parameter parsing issues
     */
    @Query(value = "SELECT * FROM lineage_events WHERE " +
           "dataset = :tableName " +
           "OR outputs->'_tracked_tables_written' @> to_jsonb(CAST(:tableName AS text)) " +
           "OR outputs->'_tracked_tables_read' @> to_jsonb(CAST(:tableName AS text)) " +
           "ORDER BY created_at ASC",
           nativeQuery = true)
    List<LineageEvent> findByAnyTableTouched(@Param("tableName") String tableName);
}
