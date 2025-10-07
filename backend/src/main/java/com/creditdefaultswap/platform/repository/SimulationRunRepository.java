package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.SimulationRun;
import com.creditdefaultswap.platform.model.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {
    
    Optional<SimulationRun> findByRunId(String runId);
    
    List<SimulationRun> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
    
    List<SimulationRun> findByStatus(SimulationStatus status);
}
