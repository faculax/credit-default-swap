package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.TradeAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TradeAmendmentRepository extends JpaRepository<TradeAmendment, Long> {
    
    List<TradeAmendment> findByTradeIdOrderByNewVersionDesc(Long tradeId);
    
    List<TradeAmendment> findByTradeIdAndNewVersionOrderByCreatedAt(Long tradeId, Integer version);
    
    List<TradeAmendment> findByAmendmentDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<TradeAmendment> findByAmendedBy(String amendedBy);
    
    List<TradeAmendment> findByFieldName(String fieldName);
}