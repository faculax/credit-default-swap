package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.SimulationHorizonMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationHorizonMetricsRepository extends JpaRepository<SimulationHorizonMetrics, Long> {
    
    List<SimulationHorizonMetrics> findByRunIdOrderByTenor(String runId);
    
    void deleteByRunId(String runId);
}
