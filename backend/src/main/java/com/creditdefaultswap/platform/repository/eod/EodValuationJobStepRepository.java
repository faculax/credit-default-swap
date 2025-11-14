package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.EodValuationJobStep;
import com.creditdefaultswap.platform.model.eod.EodValuationJobStep.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EodValuationJobStepRepository extends JpaRepository<EodValuationJobStep, Long> {
    
    List<EodValuationJobStep> findByJobIdOrderByStepNumberAsc(Long jobId);
    
    Optional<EodValuationJobStep> findByJobIdAndStepNumber(Long jobId, Integer stepNumber);
    
    List<EodValuationJobStep> findByStatus(StepStatus status);
    
    long countByJobIdAndStatus(Long jobId, StepStatus status);
}
