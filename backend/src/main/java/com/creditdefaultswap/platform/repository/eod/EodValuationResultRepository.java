package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.EodValuationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EodValuationResultRepository extends JpaRepository<EodValuationResult, Long> {
    
    /**
     * Find valuation for a specific trade on a specific date
     */
    Optional<EodValuationResult> findByValuationDateAndTradeId(LocalDate valuationDate, Long tradeId);
    
    /**
     * Find all valuations for a specific date
     */
    List<EodValuationResult> findByValuationDate(LocalDate valuationDate);
    
    /**
     * Find all valuations for a specific trade (historical time series)
     */
    List<EodValuationResult> findByTradeIdOrderByValuationDateDesc(Long tradeId);
    
    /**
     * Find valuations for a specific job
     */
    List<EodValuationResult> findByJobId(String jobId);
    
    /**
     * Find valuations by status
     */
    List<EodValuationResult> findByStatus(EodValuationResult.ValuationStatus status);
    
    /**
     * Find valuations for a specific reference entity
     */
    List<EodValuationResult> findByReferenceEntityAndValuationDate(String referenceEntity, LocalDate valuationDate);
    
    /**
     * Find valuations in a date range
     */
    List<EodValuationResult> findByValuationDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find latest valuation for a trade
     */
    Optional<EodValuationResult> findFirstByTradeIdOrderByValuationDateDesc(Long tradeId);
    
    /**
     * Get summary statistics for a date
     */
    @Query("""
        SELECT 
            COUNT(e), 
            SUM(e.npv), 
            SUM(e.accruedInterest), 
            SUM(e.totalValue),
            SUM(e.notionalAmount)
        FROM EodValuationResult e 
        WHERE e.valuationDate = :valuationDate 
        AND e.status = 'VALID'
        """)
    Object[] getSummaryStatistics(LocalDate valuationDate);
    
    /**
     * Get total exposure by reference entity
     */
    @Query("""
        SELECT e.referenceEntity, SUM(e.totalValue), COUNT(e)
        FROM EodValuationResult e 
        WHERE e.valuationDate = :valuationDate 
        AND e.status = 'VALID'
        GROUP BY e.referenceEntity
        ORDER BY SUM(e.totalValue) DESC
        """)
    List<Object[]> getExposureByReferenceEntity(LocalDate valuationDate);
    
    /**
     * Delete valuations older than a specific date (for archiving)
     */
    void deleteByValuationDateBefore(LocalDate date);
}
