package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.BasketPortfolioConstituent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BasketPortfolioConstituentRepository extends JpaRepository<BasketPortfolioConstituent, Long> {
    
    List<BasketPortfolioConstituent> findByPortfolioId(Long portfolioId);
    
    List<BasketPortfolioConstituent> findByPortfolioIdAndActiveTrue(Long portfolioId);
    
    Optional<BasketPortfolioConstituent> findByPortfolioIdAndBasketId(Long portfolioId, Long basketId);
    
    @Query("SELECT bpc FROM BasketPortfolioConstituent bpc " +
           "JOIN FETCH bpc.basket b " +
           "WHERE bpc.portfolio.id = :portfolioId AND bpc.active = true")
    List<BasketPortfolioConstituent> findActiveWithBasketsByPortfolioId(@Param("portfolioId") Long portfolioId);
    
    boolean existsByPortfolioIdAndBasketId(Long portfolioId, Long basketId);
}
