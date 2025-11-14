package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.RiskLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskLimitRepository extends JpaRepository<RiskLimit, Long> {
    
    List<RiskLimit> findByIsActiveTrue();
    
    List<RiskLimit> findByPortfolioIdAndIsActiveTrue(Long portfolioId);
    
    List<RiskLimit> findByFirmWideAndIsActiveTrue(Boolean firmWide);
    
    List<RiskLimit> findBySectorAndIsActiveTrue(String sector);
}
