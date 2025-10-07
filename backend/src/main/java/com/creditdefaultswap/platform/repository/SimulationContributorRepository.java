package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.SimulationContributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationContributorRepository extends JpaRepository<SimulationContributor, Long> {
    
    List<SimulationContributor> findByRunIdOrderByMarginalElPctDesc(String runId);
    
    void deleteByRunId(String runId);
}
