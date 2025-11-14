package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.EodPortfolioValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EodPortfolioValuationRepository extends JpaRepository<EodPortfolioValuation, Long> {
    
    /**
     * Find portfolio valuation for a specific portfolio on a specific date
     */
    Optional<EodPortfolioValuation> findByValuationDateAndPortfolioIdAndBook(
        LocalDate valuationDate, String portfolioId, String book);
    
    /**
     * Find all portfolio valuations for a specific date
     */
    List<EodPortfolioValuation> findByValuationDate(LocalDate valuationDate);
    
    /**
     * Find valuations for a specific portfolio (time series)
     */
    List<EodPortfolioValuation> findByPortfolioIdAndBookOrderByValuationDateDesc(
        String portfolioId, String book);
    
    /**
     * Find valuations by desk
     */
    List<EodPortfolioValuation> findByDeskAndValuationDate(String desk, LocalDate valuationDate);
    
    /**
     * Find valuations by business unit
     */
    List<EodPortfolioValuation> findByBusinessUnitAndValuationDate(String businessUnit, LocalDate valuationDate);
    
    /**
     * Find valuations for a specific job
     */
    List<EodPortfolioValuation> findByJobId(String jobId);
    
    /**
     * Find valuations in a date range
     */
    List<EodPortfolioValuation> findByValuationDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get latest valuation for a portfolio
     */
    Optional<EodPortfolioValuation> findFirstByPortfolioIdAndBookOrderByValuationDateDesc(
        String portfolioId, String book);
    
    /**
     * Get firm-wide summary for a date
     */
    @Query("""
        SELECT 
            COUNT(p), 
            SUM(p.totalNpv), 
            SUM(p.totalAccrued), 
            SUM(p.totalValue),
            SUM(p.numTrades)
        FROM EodPortfolioValuation p 
        WHERE p.valuationDate = :valuationDate
        """)
    Object[] getFirmWideSummary(LocalDate valuationDate);
    
    /**
     * Get P&L by desk
     */
    @Query("""
        SELECT p.desk, SUM(p.dailyPnl), SUM(p.totalValue)
        FROM EodPortfolioValuation p 
        WHERE p.valuationDate = :valuationDate
        GROUP BY p.desk
        ORDER BY SUM(p.dailyPnl) DESC
        """)
    List<Object[]> getPnlByDesk(LocalDate valuationDate);
}
