package com.creditdefaultswap.platform.repository;

import com.creditdefaultswap.platform.model.PhysicalSettlementInstruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhysicalSettlementRepository extends JpaRepository<PhysicalSettlementInstruction, UUID> {
    
    /**
     * Find physical settlement instruction by credit event ID (unique constraint)
     */
    Optional<PhysicalSettlementInstruction> findByCreditEventId(UUID creditEventId);
    
    /**
     * Find all physical settlement instructions for a trade
     */
    List<PhysicalSettlementInstruction> findByTradeIdOrderByCreatedAtDesc(Long tradeId);
    
    /**
     * Check if physical settlement instruction exists for credit event
     */
    boolean existsByCreditEventId(UUID creditEventId);
    
    /**
     * Find instructions by status
     */
    List<PhysicalSettlementInstruction> findByStatus(PhysicalSettlementInstruction.InstructionStatus status);
}