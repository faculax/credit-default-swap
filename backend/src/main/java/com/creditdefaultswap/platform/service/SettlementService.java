package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.SettlementView;
import com.creditdefaultswap.platform.model.CashSettlement;
import com.creditdefaultswap.platform.model.PhysicalSettlementInstruction;
import com.creditdefaultswap.platform.repository.CashSettlementRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementService {
    
    private final CashSettlementRepository cashSettlementRepository;
    private final PhysicalSettlementRepository physicalSettlementRepository;
    
    @Autowired
    public SettlementService(CashSettlementRepository cashSettlementRepository,
                            PhysicalSettlementRepository physicalSettlementRepository) {
        this.cashSettlementRepository = cashSettlementRepository;
        this.physicalSettlementRepository = physicalSettlementRepository;
    }
    
    /**
     * Get settlement details for a credit event (either cash or physical)
     */
    public Optional<SettlementView> getSettlement(UUID creditEventId) {
        // Check for cash settlement first
        Optional<CashSettlement> cashSettlement = cashSettlementRepository.findByCreditEventId(creditEventId);
        if (cashSettlement.isPresent()) {
            CashSettlement cash = cashSettlement.get();
            return Optional.of(SettlementView.fromCashSettlement(
                Long.valueOf(cash.getTradeId()), 
                cash.getCreditEventId(),
                cash.getNotional(),
                cash.getRecoveryRate(),
                cash.getPayoutAmount(),
                cash.getCalculatedAt()
            ));
        }
        
        // Check for physical settlement
        Optional<PhysicalSettlementInstruction> physicalSettlement = 
            physicalSettlementRepository.findByCreditEventId(creditEventId);
        if (physicalSettlement.isPresent()) {
            PhysicalSettlementInstruction physical = physicalSettlement.get();
            return Optional.of(SettlementView.fromPhysicalSettlement(
                Long.valueOf(physical.getTradeId()),
                physical.getCreditEventId(),
                physical.getReferenceObligationIsin(),
                physical.getProposedDeliveryDate(),
                physical.getNotes(),
                physical.getStatus().toString(),
                physical.getCreatedAt()
            ));
        }
        
        // No settlement found
        return Optional.empty();
    }
    
    /**
     * Get cash settlement details specifically
     */
    public Optional<CashSettlement> getCashSettlement(UUID creditEventId) {
        return cashSettlementRepository.findByCreditEventId(creditEventId);
    }
    
    /**
     * Get physical settlement details specifically
     */
    public Optional<PhysicalSettlementInstruction> getPhysicalSettlement(UUID creditEventId) {
        return physicalSettlementRepository.findByCreditEventId(creditEventId);
    }
}