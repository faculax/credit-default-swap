package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.DailyPnlResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPnlResultRepository extends JpaRepository<DailyPnlResult, Long> {
    
    /**
     * Find P&L for a specific trade on a specific date
     */
    Optional<DailyPnlResult> findByPnlDateAndTradeId(LocalDate pnlDate, Long tradeId);
    
    /**
     * Find all P&L results for a specific date
     */
    List<DailyPnlResult> findByPnlDate(LocalDate pnlDate);
    
    /**
     * Find P&L history for a trade
     */
    List<DailyPnlResult> findByTradeIdOrderByPnlDateDesc(Long tradeId);
    
    /**
     * Find P&L results for a date range
     */
    List<DailyPnlResult> findByPnlDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find large P&L movers for a date
     */
    List<DailyPnlResult> findByPnlDateAndLargePnlFlagTrue(LocalDate pnlDate);
    
    /**
     * Find trades with unexplained P&L
     */
    List<DailyPnlResult> findByPnlDateAndUnexplainedPnlFlagTrue(LocalDate pnlDate);
    
    /**
     * Find trades with credit events
     */
    List<DailyPnlResult> findByPnlDateAndCreditEventFlagTrue(LocalDate pnlDate);
    
    /**
     * Find P&L for a specific reference entity
     */
    List<DailyPnlResult> findByPnlDateAndReferenceEntity(LocalDate pnlDate, String referenceEntity);
    
    /**
     * Find latest P&L for a trade
     */
    Optional<DailyPnlResult> findFirstByTradeIdOrderByPnlDateDesc(Long tradeId);
    
    /**
     * Get total P&L for a date
     */
    @Query("""
        SELECT SUM(p.totalPnl)
        FROM DailyPnlResult p 
        WHERE p.pnlDate = :pnlDate
        """)
    BigDecimal getTotalPnlForDate(LocalDate pnlDate);
    
    /**
     * Get P&L attribution summary for a date
     */
    @Query("""
        SELECT 
            SUM(p.totalPnl),
            SUM(p.marketPnl),
            SUM(p.thetaPnl),
            SUM(p.accruedPnl),
            SUM(p.creditEventPnl),
            SUM(p.tradePnl),
            SUM(p.unexplainedPnl)
        FROM DailyPnlResult p 
        WHERE p.pnlDate = :pnlDate
        """)
    Object[] getPnlAttributionSummary(LocalDate pnlDate);
    
    /**
     * Get top winners for a date
     */
    @Query("""
        SELECT p
        FROM DailyPnlResult p 
        WHERE p.pnlDate = :pnlDate
        ORDER BY p.totalPnl DESC
        LIMIT :limit
        """)
    List<DailyPnlResult> getTopWinners(LocalDate pnlDate, int limit);
    
    /**
     * Get top losers for a date
     */
    @Query("""
        SELECT p
        FROM DailyPnlResult p 
        WHERE p.pnlDate = :pnlDate
        ORDER BY p.totalPnl ASC
        LIMIT :limit
        """)
    List<DailyPnlResult> getTopLosers(LocalDate pnlDate, int limit);
    
    /**
     * Get P&L by reference entity
     */
    @Query("""
        SELECT p.referenceEntity, SUM(p.totalPnl), COUNT(p)
        FROM DailyPnlResult p 
        WHERE p.pnlDate = :pnlDate
        GROUP BY p.referenceEntity
        ORDER BY SUM(p.totalPnl) DESC
        """)
    List<Object[]> getPnlByReferenceEntity(LocalDate pnlDate);
    
    /**
     * Count P&L records for a date
     */
    long countByPnlDate(LocalDate pnlDate);
}
