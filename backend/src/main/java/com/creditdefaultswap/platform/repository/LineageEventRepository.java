package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.LineageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LineageEventRepository extends JpaRepository<LineageEvent, UUID> {

    List<LineageEvent> findByDatasetOrderByCreatedAtDesc(String dataset);

    List<LineageEvent> findByRunIdOrderByCreatedAtDesc(String runId);
}
