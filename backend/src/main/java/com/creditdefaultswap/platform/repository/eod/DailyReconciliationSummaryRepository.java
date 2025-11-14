package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.DailyReconciliationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReconciliationSummaryRepository extends JpaRepository<DailyReconciliationSummary, Long> {
    
    Optional<DailyReconciliationSummary> findByReconciliationDate(LocalDate reconciliationDate);
    
    List<DailyReconciliationSummary> findByReconciliationStatusOrderByReconciliationDateDesc(String reconciliationStatus);
    
    List<DailyReconciliationSummary> findTop20ByOrderByReconciliationDateDesc();
}
