package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CdsPortfolioConstituent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CdsPortfolioConstituentRepository extends JpaRepository<CdsPortfolioConstituent, Long> {
    
    @Query("SELECT c FROM CdsPortfolioConstituent c WHERE c.portfolio.id = ?1 AND c.active = true")
    List<CdsPortfolioConstituent> findActiveByPortfolioId(Long portfolioId);
    
    @Query("SELECT c FROM CdsPortfolioConstituent c WHERE c.portfolio.id = ?1 AND c.trade.id = ?2")
    Optional<CdsPortfolioConstituent> findByPortfolioIdAndTradeId(Long portfolioId, Long tradeId);
    
    @Query("SELECT COUNT(c) FROM CdsPortfolioConstituent c WHERE c.portfolio.id = ?1 AND c.active = true")
    long countActiveByPortfolioId(Long portfolioId);
}
