package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.RiskConcentration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskConcentrationRepository extends JpaRepository<RiskConcentration, Long> {
    
    List<RiskConcentration> findByCalculationDateAndConcentrationType(
        LocalDate calculationDate, String concentrationType);
    
    List<RiskConcentration> findByCalculationDateOrderByRanking(LocalDate calculationDate);
    
    List<RiskConcentration> findByCalculationDate(LocalDate calculationDate);
    
    List<RiskConcentration> findByCalculationDateOrderByGrossNotionalDesc(LocalDate calculationDate);
    
    void deleteByCalculationDate(LocalDate calculationDate);
}
