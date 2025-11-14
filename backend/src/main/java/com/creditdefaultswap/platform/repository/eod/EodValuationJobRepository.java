package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.EodValuationJob;
import com.creditdefaultswap.platform.model.eod.EodValuationJob.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EodValuationJobRepository extends JpaRepository<EodValuationJob, Long> {
    
    Optional<EodValuationJob> findByJobId(String jobId);
    
    Optional<EodValuationJob> findByValuationDate(LocalDate valuationDate);
    
    List<EodValuationJob> findByStatus(JobStatus status);
    
    List<EodValuationJob> findByValuationDateBetween(LocalDate startDate, LocalDate endDate);
    
    Optional<EodValuationJob> findFirstByStatusOrderByValuationDateDesc(JobStatus status);
    
    boolean existsByValuationDate(LocalDate valuationDate);
    
    List<EodValuationJob> findAllByOrderByValuationDateDesc();
}
