package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.RiskLimitBreach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskLimitBreachRepository extends JpaRepository<RiskLimitBreach, Long> {
    
    List<RiskLimitBreach> findByIsResolvedFalseOrderByBreachDateDesc();
    
    List<RiskLimitBreach> findByBreachDateAndIsResolvedFalse(LocalDate breachDate);
    
    List<RiskLimitBreach> findByRiskLimitIdAndIsResolvedFalse(Long riskLimitId);
}
