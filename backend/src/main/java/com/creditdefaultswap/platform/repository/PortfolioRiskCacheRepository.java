package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.PortfolioRiskCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PortfolioRiskCacheRepository extends JpaRepository<PortfolioRiskCache, Long> {
    
    @Query("SELECT p FROM PortfolioRiskCache p WHERE p.portfolio.id = ?1 AND p.valuationDate = ?2")
    Optional<PortfolioRiskCache> findByPortfolioIdAndValuationDate(Long portfolioId, LocalDate valuationDate);
    
    @Query("SELECT p FROM PortfolioRiskCache p WHERE p.portfolio.id = ?1 ORDER BY p.valuationDate DESC LIMIT 1")
    Optional<PortfolioRiskCache> findLatestByPortfolioId(Long portfolioId);
}
