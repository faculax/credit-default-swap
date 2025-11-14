package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.FirmRiskSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FirmRiskSummaryRepository extends JpaRepository<FirmRiskSummary, Long> {
    
    Optional<FirmRiskSummary> findByCalculationDate(LocalDate calculationDate);
    
    List<FirmRiskSummary> findByCalculationDateBetweenOrderByCalculationDate(
        LocalDate startDate, LocalDate endDate);
    
    List<FirmRiskSummary> findTop20ByOrderByCalculationDateDesc();
    
    @Query("SELECT DISTINCT f.calculationDate FROM FirmRiskSummary f ORDER BY f.calculationDate DESC")
    List<LocalDate> findDistinctCalculationDates();
}
