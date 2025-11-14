package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.PortfolioRiskMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRiskMetricsRepository extends JpaRepository<PortfolioRiskMetrics, Long> {
    
    Optional<PortfolioRiskMetrics> findByCalculationDateAndPortfolioId(LocalDate calculationDate, Long portfolioId);
    
    List<PortfolioRiskMetrics> findByCalculationDate(LocalDate calculationDate);
    
    List<PortfolioRiskMetrics> findByPortfolioIdOrderByCalculationDateDesc(Long portfolioId);
    
    List<PortfolioRiskMetrics> findByPortfolioIdAndCalculationDateBetweenOrderByCalculationDate(
        Long portfolioId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(p.cs01) FROM PortfolioRiskMetrics p WHERE p.calculationDate = :date")
    BigDecimal getTotalCs01ForDate(@Param("date") LocalDate date);
    
    @Query("SELECT SUM(p.jtd) FROM PortfolioRiskMetrics p WHERE p.calculationDate = :date")
    BigDecimal getTotalJtdForDate(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM PortfolioRiskMetrics p WHERE p.calculationDate = :date ORDER BY ABS(p.cs01) DESC")
    List<PortfolioRiskMetrics> findTopByCs01ForDate(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM PortfolioRiskMetrics p WHERE p.calculationDate = :date ORDER BY ABS(p.jtd) DESC")
    List<PortfolioRiskMetrics> findTopByJtdForDate(@Param("date") LocalDate date);
}
