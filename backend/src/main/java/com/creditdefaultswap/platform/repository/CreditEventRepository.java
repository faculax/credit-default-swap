package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CreditEvent;
import com.creditdefaultswap.platform.model.CreditEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditEventRepository extends JpaRepository<CreditEvent, UUID> {
    
    /**
     * Find existing credit event with same trade, event type, and date for idempotency
     */
    Optional<CreditEvent> findByTradeIdAndEventTypeAndEventDate(
        Long tradeId, 
        CreditEventType eventType, 
        LocalDate eventDate
    );
    
    /**
     * Find all credit events for a specific trade
     */
    List<CreditEvent> findByTradeIdOrderByEventDateDesc(Long tradeId);
    
    /**
     * Check if a trade has any terminal credit events
     */
    @Query("SELECT COUNT(ce) > 0 FROM CreditEvent ce WHERE ce.tradeId = :tradeId")
    boolean hasAnyTerminalEvent(@Param("tradeId") Long tradeId);
    
    /**
     * Find credit events by trade and settlement method
     */
    List<CreditEvent> findByTradeIdAndSettlementMethod(Long tradeId, com.creditdefaultswap.platform.model.SettlementMethod settlementMethod);
}