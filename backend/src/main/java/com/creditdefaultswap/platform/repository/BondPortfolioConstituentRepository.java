package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.BondPortfolioConstituent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BondPortfolioConstituentRepository extends JpaRepository<BondPortfolioConstituent, Long> {
    
    List<BondPortfolioConstituent> findByPortfolioIdAndActiveTrue(Long portfolioId);
    
    @Query("SELECT c FROM BondPortfolioConstituent c WHERE c.portfolio.id = :portfolioId AND c.bond.id = :bondId")
    Optional<BondPortfolioConstituent> findByPortfolioIdAndBondId(
            @Param("portfolioId") Long portfolioId, 
            @Param("bondId") Long bondId
    );
    
    @Query("SELECT c FROM BondPortfolioConstituent c WHERE c.bond.id = :bondId AND c.active = true")
    List<BondPortfolioConstituent> findActiveByBondId(@Param("bondId") Long bondId);
    
    void deleteByPortfolioIdAndBondId(Long portfolioId, Long bondId);
}
