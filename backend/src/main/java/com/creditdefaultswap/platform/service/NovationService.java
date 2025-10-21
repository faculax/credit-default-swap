package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.AuditLog;
import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.CCPAccount;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CCPAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NovationService {
    
    private final CDSTradeRepository cdsTradeRepository;
    private final CCPAccountRepository ccpAccountRepository;
    private final AuditService auditService;
    private final MarginAccountService marginAccountService;
    
    @Autowired
    public NovationService(CDSTradeRepository cdsTradeRepository, 
                          CCPAccountRepository ccpAccountRepository,
                          AuditService auditService,
                          MarginAccountService marginAccountService) {
        this.cdsTradeRepository = cdsTradeRepository;
        this.ccpAccountRepository = ccpAccountRepository;
        this.auditService = auditService;
        this.marginAccountService = marginAccountService;
    }
    
    /**
     * Execute novation of a bilateral trade to CCP clearing
     */
    @Transactional
    public NovationResult novateToClearing(Long tradeId, String ccpName, String memberFirm, String actor) {
        // Validate trade exists and is eligible for novation
        CDSTrade originalTrade = cdsTradeRepository.findById(tradeId)
                .orElseThrow(() -> new NovationException("Trade not found: " + tradeId));
        
        validateTradeEligibility(originalTrade);
        
        // Find appropriate CCP account
        CCPAccount ccpAccount = findEligibleCcpAccount(ccpName, memberFirm, originalTrade);
        
        try {
            // Generate novation reference
            String novationReference = generateNovationReference();
            UUID correlationId = UUID.randomUUID();
            
            // Create audit entry for novation start
            auditService.logAudit(AuditLog.EntityType.TRADE, tradeId.toString(), 
                               AuditLog.AuditAction.TRANSITION, actor, 
                               "Novation started - terminating bilateral trade", correlationId);
            
            // Terminate original bilateral trade
            originalTrade.setTradeStatus(TradeStatus.TERMINATED);
            originalTrade.setNovationTimestamp(LocalDateTime.now());
            originalTrade.setNovationReference(novationReference);
            originalTrade.setUpdatedAt(LocalDateTime.now());
            cdsTradeRepository.save(originalTrade);
            
            // Create new CCP trade with identical economic terms
            CDSTrade ccpTrade = createCcpTrade(originalTrade, ccpAccount, novationReference);
            ccpTrade = cdsTradeRepository.save(ccpTrade);
            
            // Set up margin account automatically for the new CCP trade
            MarginAccountService.MarginAccountSetupResult marginSetupResult = 
                marginAccountService.setupMarginAccountForCcpTrade(ccpTrade, actor);
            
            if (!marginSetupResult.isSuccess()) {
                throw new NovationException("Margin account setup failed: " + marginSetupResult.getMessage());
            }
            
            // Create audit entries for both trades
            auditService.logAudit(AuditLog.EntityType.TRADE, originalTrade.getId().toString(), 
                               AuditLog.AuditAction.UPDATE, actor, 
                               "Trade terminated via novation to " + ccpName, correlationId);
            
            auditService.logAudit(AuditLog.EntityType.TRADE, ccpTrade.getId().toString(), 
                               AuditLog.AuditAction.CREATE, actor, 
                               "CCP trade created via novation from trade " + tradeId, correlationId);
            
            return new NovationResult(true, "Novation completed successfully", 
                                    originalTrade, ccpTrade, novationReference);
            
        } catch (Exception e) {
            throw new NovationException("Novation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate that a trade is eligible for novation
     */
    private void validateTradeEligibility(CDSTrade trade) {
        if (trade.getTradeStatus() != TradeStatus.ACTIVE && trade.getTradeStatus() != TradeStatus.PENDING) {
            throw new NovationException("Trade status must be ACTIVE or PENDING for novation. Current status: " + trade.getTradeStatus());
        }
        
        if (trade.getIsCleared()) {
            throw new NovationException("Trade is already cleared and cannot be novated");
        }
        
        if (trade.getOriginalTradeId() != null) {
            throw new NovationException("Trade is already a result of novation and cannot be novated again");
        }
    }
    
    /**
     * Find eligible CCP account for the novation
     */
    private CCPAccount findEligibleCcpAccount(String ccpName, String memberFirm, CDSTrade trade) {
        // Check if CCP account exists and is active
        if (!ccpAccountRepository.existsActiveCcpAccount(ccpName, memberFirm)) {
            throw new NovationException("No active CCP account found for " + ccpName + " and member " + memberFirm);
        }
        
        // Find accounts eligible for CDS products
        List<CCPAccount> eligibleAccounts = ccpAccountRepository.findByEligibleProductTypeAndActiveStatus("CDS");
        
        Optional<CCPAccount> matchingAccount = eligibleAccounts.stream()
                .filter(account -> account.getCcpName().equals(ccpName) && 
                                 account.getMemberFirm().equals(memberFirm))
                .findFirst();
        
        return matchingAccount.orElseThrow(() -> 
                new NovationException("No CCP account found eligible for CDS products"));
    }
    
    /**
     * Create CCP trade from original bilateral trade
     */
    private CDSTrade createCcpTrade(CDSTrade originalTrade, CCPAccount ccpAccount, String novationReference) {
        CDSTrade ccpTrade = new CDSTrade();
        
        // Copy all economic terms from original trade
        ccpTrade.setReferenceEntity(originalTrade.getReferenceEntity());
        ccpTrade.setNotionalAmount(originalTrade.getNotionalAmount());
        ccpTrade.setSpread(originalTrade.getSpread());
        ccpTrade.setMaturityDate(originalTrade.getMaturityDate());
        ccpTrade.setEffectiveDate(originalTrade.getEffectiveDate());
        ccpTrade.setTradeDate(originalTrade.getTradeDate());
        ccpTrade.setCurrency(originalTrade.getCurrency());
        ccpTrade.setPremiumFrequency(originalTrade.getPremiumFrequency());
        ccpTrade.setDayCountConvention(originalTrade.getDayCountConvention());
        ccpTrade.setBuySellProtection(originalTrade.getBuySellProtection());
        ccpTrade.setRestructuringClause(originalTrade.getRestructuringClause());
        ccpTrade.setPaymentCalendar(originalTrade.getPaymentCalendar());
        ccpTrade.setAccrualStartDate(originalTrade.getAccrualStartDate());
        ccpTrade.setRecoveryRate(originalTrade.getRecoveryRate());
        ccpTrade.setSettlementType(originalTrade.getSettlementType());
        
        // Set CCP-specific fields
        ccpTrade.setCounterparty(ccpAccount.getCcpName());
        ccpTrade.setCcpName(ccpAccount.getCcpName());
        ccpTrade.setCcpMemberId(ccpAccount.getMemberId());
        ccpTrade.setClearingAccount(ccpAccount.getAccountNumber());
        ccpTrade.setNettingSetId(generateNettingSetId(ccpAccount, originalTrade));
        ccpTrade.setOriginalTradeId(originalTrade.getId());
        ccpTrade.setNovationReference(novationReference);
        ccpTrade.setNovationTimestamp(LocalDateTime.now());
        ccpTrade.setIsCleared(true);
        ccpTrade.setTradeStatus(TradeStatus.ACTIVE);
        
        // Generate new trade identifiers
        ccpTrade.setUti(generateUti());
        ccpTrade.setUsi(generateUsi());
        
        return ccpTrade;
    }
    
    /**
     * Generate unique novation reference
     */
    private String generateNovationReference() {
        return "NOV-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generate netting set ID based on CCP account and trade characteristics
     */
    private String generateNettingSetId(CCPAccount ccpAccount, CDSTrade trade) {
        return ccpAccount.getCcpName() + "-" + ccpAccount.getAccountNumber() + "-" + trade.getCurrency();
    }
    
    /**
     * Generate Unique Transaction Identifier
     */
    private String generateUti() {
        return "UTI-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
    
    /**
     * Generate Unique Swap Identifier
     */
    private String generateUsi() {
        return "USI-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
    
    /**
     * Get novation history for a trade
     */
    public List<CDSTrade> getNovationHistory(Long tradeId) {
        return cdsTradeRepository.findByOriginalTradeIdOrderByCreatedAtDesc(tradeId);
    }
    
    /**
     * Check if trade can be novated
     */
    public boolean canNovate(Long tradeId) {
        try {
            CDSTrade trade = cdsTradeRepository.findById(tradeId).orElse(null);
            if (trade == null) return false;
            
            validateTradeEligibility(trade);
            return true;
        } catch (NovationException e) {
            return false;
        }
    }
    
    /**
     * Result object for novation operations
     */
    public static class NovationResult {
        private final boolean success;
        private final String message;
        private final CDSTrade originalTrade;
        private final CDSTrade ccpTrade;
        private final String novationReference;
        
        public NovationResult(boolean success, String message, CDSTrade originalTrade, 
                            CDSTrade ccpTrade, String novationReference) {
            this.success = success;
            this.message = message;
            this.originalTrade = originalTrade;
            this.ccpTrade = ccpTrade;
            this.novationReference = novationReference;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public CDSTrade getOriginalTrade() { return originalTrade; }
        public CDSTrade getCcpTrade() { return ccpTrade; }
        public String getNovationReference() { return novationReference; }
    }
    
    /**
     * Exception for novation-related errors
     */
    public static class NovationException extends RuntimeException {
        public NovationException(String message) {
            super(message);
        }
        
        public NovationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}