package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.ValuationToleranceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValuationToleranceRuleRepository extends JpaRepository<ValuationToleranceRule, Long> {
    
    List<ValuationToleranceRule> findByIsActiveTrue();
    
    List<ValuationToleranceRule> findByRuleTypeAndIsActiveTrue(String ruleType);
    
    List<ValuationToleranceRule> findByPortfolioIdAndIsActiveTrue(Long portfolioId);
}
