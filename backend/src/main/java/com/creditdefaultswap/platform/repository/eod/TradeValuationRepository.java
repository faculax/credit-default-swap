package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.TradeValuation;
import com.creditdefaultswap.platform.model.eod.TradeValuation.ValuationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeValuationRepository extends JpaRepository<TradeValuation, Long> {
    
    Optional<TradeValuation> findByValuationDateAndTradeId(LocalDate valuationDate, Long tradeId);
    
    List<TradeValuation> findByValuationDate(LocalDate valuationDate);
    
    List<TradeValuation> findByValuationDateAndValuationStatus(LocalDate valuationDate, ValuationStatus status);
    
    List<TradeValuation> findByTradeIdOrderByValuationDateDesc(Long tradeId);
    
    @Query("SELECT v FROM TradeValuation v WHERE v.tradeId = :tradeId " +
           "AND v.valuationDate >= :startDate AND v.valuationDate <= :endDate " +
           "ORDER BY v.valuationDate ASC")
    List<TradeValuation> findByTradeIdAndDateRange(
        @Param("tradeId") Long tradeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    Optional<TradeValuation> findFirstByTradeIdOrderByValuationDateDesc(Long tradeId);
    
    long countByValuationDateAndValuationStatus(LocalDate valuationDate, ValuationStatus status);
}
