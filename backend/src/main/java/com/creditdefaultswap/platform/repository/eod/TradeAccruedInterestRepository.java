package com.creditdefaultswap.platform.repository.eod;

import com.creditdefaultswap.platform.model.eod.TradeAccruedInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeAccruedInterestRepository extends JpaRepository<TradeAccruedInterest, Long> {
    
    Optional<TradeAccruedInterest> findByCalculationDateAndTradeId(LocalDate calculationDate, Long tradeId);
    
    List<TradeAccruedInterest> findByCalculationDate(LocalDate calculationDate);
    
    List<TradeAccruedInterest> findByTradeIdOrderByCalculationDateDesc(Long tradeId);
    
    Optional<TradeAccruedInterest> findFirstByTradeIdOrderByCalculationDateDesc(Long tradeId);
}
