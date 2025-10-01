package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.CashSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashSettlementRepository extends JpaRepository<CashSettlement, UUID> {
    
    /**
     * Find cash settlement by credit event ID (unique constraint)
     */
    Optional<CashSettlement> findByCreditEventId(UUID creditEventId);
    
    /**
     * Find all cash settlements for a trade
     */
    List<CashSettlement> findByTradeIdOrderByCalculatedAtDesc(Long tradeId);
    
    /**
     * Check if cash settlement exists for credit event
     */
    boolean existsByCreditEventId(UUID creditEventId);
}