package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CashSettlement;
import com.creditdefaultswap.platform.repository.CashSettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class CashSettlementService {
    
    private final CashSettlementRepository cashSettlementRepository;
    private final AuditService auditService;
    
    @Value("${cds.default-recovery-rate:0.40}")
    private String defaultRecoveryRateStr;
    
    private BigDecimal getDefaultRecoveryRate() {
        return new BigDecimal(defaultRecoveryRateStr);
    }
    
    @Autowired
    public CashSettlementService(CashSettlementRepository cashSettlementRepository,
                                AuditService auditService) {
        this.cashSettlementRepository = cashSettlementRepository;
        this.auditService = auditService;
    }
    
    /**
     * Calculate cash settlement for a credit event
     * Formula: Payout = Notional * (1 - RecoveryRate)
     */
    @Transactional
    public CashSettlement calculateCashSettlement(UUID creditEventId, CDSTrade trade) {
        return calculateCashSettlement(creditEventId, trade, null);
    }
    
    /**
     * Calculate cash settlement with optional recovery rate override
     */
    @Transactional
    public CashSettlement calculateCashSettlement(UUID creditEventId, CDSTrade trade, BigDecimal recoveryRateOverride) {
        // Check if calculation already exists (idempotency)
        Optional<CashSettlement> existing = cashSettlementRepository.findByCreditEventId(creditEventId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Determine recovery rate
        BigDecimal recoveryRate = determineRecoveryRate(recoveryRateOverride);
        
        // Calculate payout: Notional * (1 - RecoveryRate)
        BigDecimal notional = trade.getNotionalAmount();
        BigDecimal lossRate = BigDecimal.ONE.subtract(recoveryRate);
        BigDecimal payoutAmount = notional.multiply(lossRate)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Create settlement record
        CashSettlement settlement = new CashSettlement();
        settlement.setCreditEventId(creditEventId);
        settlement.setTradeId(trade.getId());
        settlement.setNotional(notional);
        settlement.setRecoveryRate(recoveryRate);
        settlement.setPayoutAmount(payoutAmount);
        
        settlement = cashSettlementRepository.save(settlement);
        
        // Log audit trail
        String auditSummary = String.format("Notional: %s, Recovery: %s%%, Payout: %s", 
            notional, recoveryRate.multiply(BigDecimal.valueOf(100)), payoutAmount);
        auditService.logCashSettlementCalculation(settlement.getId(), "SYSTEM", auditSummary);
        
        return settlement;
    }
    
    /**
     * Get cash settlement by credit event ID
     */
    public Optional<CashSettlement> getCashSettlement(UUID creditEventId) {
        return cashSettlementRepository.findByCreditEventId(creditEventId);
    }
    
    /**
     * Determine the recovery rate to use
     * Priority: 1) Override provided, 2) Trade default (future), 3) System default
     */
    private BigDecimal determineRecoveryRate(BigDecimal override) {
        if (override != null) {
            validateRecoveryRate(override);
            return override;
        }
        
        // TODO: Future enhancement - check trade for default recovery rate
        
        // Use system default
        return getDefaultRecoveryRate();
    }
    
    /**
     * Validate recovery rate is in valid range [0, 1]
     */
    private void validateRecoveryRate(BigDecimal recoveryRate) {
        if (recoveryRate.compareTo(BigDecimal.ZERO) < 0 || recoveryRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Recovery rate must be between 0 and 1, got: " + recoveryRate);
        }
    }
}