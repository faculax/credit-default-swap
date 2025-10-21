package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.MarginPositionRepository;
import com.creditdefaultswap.platform.repository.MarginStatementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing margin account setup and initial position creation for CCP trades
 */
@Service
public class MarginAccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarginAccountService.class);
    
    private final MarginPositionRepository marginPositionRepository;
    private final MarginStatementRepository marginStatementRepository;
    private final AuditService auditService;
    
    @Autowired
    public MarginAccountService(MarginPositionRepository marginPositionRepository,
                               MarginStatementRepository marginStatementRepository,
                               AuditService auditService) {
        this.marginPositionRepository = marginPositionRepository;
        this.marginStatementRepository = marginStatementRepository;
        this.auditService = auditService;
    }
    
    /**
     * Automatically set up margin account for a newly created CCP trade
     * Creates initial margin positions for proper risk management
     * 
     * DISABLED: Auto-creation on trade save is disabled to prevent duplicates.
     * Use the "Generate Statements" button in the UI to create margin statements.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public MarginAccountSetupResult setupMarginAccountForCcpTrade(CDSTrade ccpTrade, String actor) {
        logger.info("Margin account auto-setup is DISABLED. Use manual statement generation instead for trade: {}", ccpTrade.getId());
        
        // Return empty result - no auto-creation
        return new MarginAccountSetupResult(
                true,
                "Auto-setup disabled - use manual generation",
                null,
                null,
                null
        );
    }
    
    /* DISABLED - Original auto-creation logic
    private MarginAccountSetupResult setupMarginAccountForCcpTrade_DISABLED(CDSTrade ccpTrade, String actor) {
        try {
            logger.info("Setting up margin account for CCP trade: {}", ccpTrade.getId());
            
            // Create or find margin statement for this CCP account
            MarginStatement marginStatement = createOrGetMarginStatement(ccpTrade);
            
            // Calculate initial margin estimate based on trade characteristics
            BigDecimal initialMarginEstimate = calculateInitialMarginEstimate(ccpTrade);
            
            // Create initial margin position
            MarginPosition initialMarginPosition = createInitialMarginPosition(
                marginStatement, ccpTrade, initialMarginEstimate);
            
            // Create zero variation margin position (starting point)
            MarginPosition variationMarginPosition = createVariationMarginPosition(
                marginStatement, ccpTrade);
            
            // Save positions
            marginPositionRepository.save(initialMarginPosition);
            marginPositionRepository.save(variationMarginPosition);
            
            // Log audit trail
            auditService.logAudit(
                AuditLog.EntityType.TRADE, 
                ccpTrade.getId().toString(),
                AuditLog.AuditAction.CREATE,
                actor,
                String.format("Margin account setup completed for CCP trade. IM: %s %s, Account: %s",
                    initialMarginEstimate, ccpTrade.getCurrency(), ccpTrade.getClearingAccount())
            );
            
            logger.info("Margin account setup completed for trade {} with IM estimate: {} {}",
                ccpTrade.getId(), initialMarginEstimate, ccpTrade.getCurrency());
            
            return new MarginAccountSetupResult(true, 
                "Margin account setup successful", 
                marginStatement, 
                initialMarginPosition, 
                variationMarginPosition);
                
        } catch (Exception e) {
            logger.error("Failed to setup margin account for trade {}: {}", ccpTrade.getId(), e.getMessage(), e);
            
            auditService.logAudit(
                AuditLog.EntityType.TRADE,
                ccpTrade.getId().toString(), 
                AuditLog.AuditAction.UPDATE,
                actor,
                "Margin account setup failed: " + e.getMessage()
            );
            
            return new MarginAccountSetupResult(false, 
                "Margin account setup failed: " + e.getMessage(), 
                null, null, null);
        }
    }
    
    /**
     * Create or get existing margin statement for this CCP account and date
     */
    private MarginStatement createOrGetMarginStatement(CDSTrade ccpTrade) {
        LocalDate statementDate = LocalDate.now();
        
        // Try to find existing statement for this account and date
        var existingStatements = marginStatementRepository
            .findByCcpNameAndMemberFirmAndAccountNumberAndStatementDate(
                ccpTrade.getCcpName(),
                ccpTrade.getCcpMemberId(), // Using member ID as member firm for now
                ccpTrade.getClearingAccount(),
                statementDate
            );
        
        if (!existingStatements.isEmpty()) {
            logger.debug("Using existing margin statement: {}", existingStatements.get(0).getStatementId());
            return existingStatements.get(0);
        }
        
        // Create new margin statement
        MarginStatement statement = new MarginStatement();
        statement.setStatementId(generateStatementId(ccpTrade));
        statement.setCcpName(ccpTrade.getCcpName());
        statement.setMemberFirm(ccpTrade.getCcpMemberId()); // Using member ID as firm identifier
        statement.setAccountNumber(ccpTrade.getClearingAccount());
        statement.setStatementDate(statementDate);
        
        // Debug logging for currency
        String currency = ccpTrade.getCurrency();
        logger.info("Creating margin statement for trade ID: {}, currency: {}", ccpTrade.getId(), currency);
        if (currency == null || currency.trim().isEmpty()) {
            currency = "USD"; // Default fallback
            logger.warn("CCP Trade currency is null/empty for trade ID: {}, using default: USD", ccpTrade.getId());
        }
        statement.setCurrency(currency);
        
        // Set statement format for auto-generated statements
        statement.setStatementFormat(MarginStatement.StatementFormat.PROPRIETARY);
        
        statement.setFileName("AUTO_GENERATED_" + ccpTrade.getId());
        statement.setStatus(MarginStatement.StatementStatus.PROCESSED);
        statement.setCreatedAt(LocalDateTime.now());
        statement.setProcessedAt(LocalDateTime.now());
        
        statement = marginStatementRepository.save(statement);
        logger.info("Created new margin statement: {} for account: {}", 
            statement.getStatementId(), ccpTrade.getClearingAccount());
        
        return statement;
    }
    
    /**
     * Calculate initial margin estimate for CDS trade
     * This is a simplified estimation - in reality would use SIMM or other models
     */
    private BigDecimal calculateInitialMarginEstimate(CDSTrade ccpTrade) {
        // Simplified IM calculation: percentage of notional based on spread and maturity
        BigDecimal notional = ccpTrade.getNotionalAmount();
        BigDecimal spread = ccpTrade.getSpread().divide(BigDecimal.valueOf(10000.0), 6, java.math.RoundingMode.HALF_UP); // Convert bps to decimal
        
        // Basic formula: IM = Notional * max(1%, spread * maturity_factor)
        BigDecimal baseRate = new BigDecimal("0.01"); // 1% minimum
        BigDecimal spreadBasedRate = spread.multiply(new BigDecimal("0.5")); // 50% of spread
        BigDecimal imRate = baseRate.max(spreadBasedRate);
        
        // Cap at 10% of notional for safety
        BigDecimal maxRate = new BigDecimal("0.10");
        imRate = imRate.min(maxRate);
        
        BigDecimal initialMargin = notional.multiply(imRate).abs(); // Always positive
        
        logger.debug("Calculated IM for trade {}: {} {} (rate: {}%)", 
            ccpTrade.getId(), initialMargin, ccpTrade.getCurrency(), 
            imRate.multiply(new BigDecimal("100")));
        
        return initialMargin;
    }
    
    /**
     * Create initial margin position
     */
    private MarginPosition createInitialMarginPosition(MarginStatement statement, 
                                                      CDSTrade ccpTrade, 
                                                      BigDecimal amount) {
        MarginPosition position = new MarginPosition();
        position.setStatement(statement);
        position.setPositionType(MarginPosition.PositionType.INITIAL_MARGIN);
        position.setAmount(amount);
        position.setCurrency(ccpTrade.getCurrency());
        position.setEffectiveDate(LocalDate.now());
        position.setAccountNumber(ccpTrade.getClearingAccount());
        position.setPortfolioCode(generatePortfolioCode(ccpTrade));
        position.setProductClass("CDS");
        position.setNettingSetId(ccpTrade.getNettingSetId());
        position.setCreatedAt(LocalDateTime.now());
        
        return position;
    }
    
    /**
     * Create variation margin position (starts at zero)
     */
    private MarginPosition createVariationMarginPosition(MarginStatement statement, CDSTrade ccpTrade) {
        MarginPosition position = new MarginPosition();
        position.setStatement(statement);
        position.setPositionType(MarginPosition.PositionType.VARIATION_MARGIN);
        position.setAmount(BigDecimal.ZERO); // Start with zero VM
        position.setCurrency(ccpTrade.getCurrency());
        position.setEffectiveDate(LocalDate.now());
        position.setAccountNumber(ccpTrade.getClearingAccount());
        position.setPortfolioCode(generatePortfolioCode(ccpTrade));
        position.setProductClass("CDS");
        position.setNettingSetId(ccpTrade.getNettingSetId());
        position.setCreatedAt(LocalDateTime.now());
        
        return position;
    }
    
    /**
     * Generate statement ID for auto-created margin statement
     */
    private String generateStatementId(CDSTrade ccpTrade) {
        return String.format("AUTO-%s-%s-%s", 
            ccpTrade.getCcpName(),
            LocalDate.now().toString().replace("-", ""),
            UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
    
    /**
     * Generate portfolio code based on trade characteristics
     */
    private String generatePortfolioCode(CDSTrade ccpTrade) {
        return String.format("CDS_%s_%s", 
            ccpTrade.getCcpName(),
            ccpTrade.getReferenceEntity().replaceAll("[^A-Z0-9]", "").substring(0, 
                Math.min(10, ccpTrade.getReferenceEntity().length()))
        );
    }
    
    /**
     * Result object for margin account setup operations
     */
    public static class MarginAccountSetupResult {
        private final boolean success;
        private final String message;
        private final MarginStatement marginStatement;
        private final MarginPosition initialMarginPosition;
        private final MarginPosition variationMarginPosition;
        
        public MarginAccountSetupResult(boolean success, String message, 
                                       MarginStatement marginStatement,
                                       MarginPosition initialMarginPosition,
                                       MarginPosition variationMarginPosition) {
            this.success = success;
            this.message = message;
            this.marginStatement = marginStatement;
            this.initialMarginPosition = initialMarginPosition;
            this.variationMarginPosition = variationMarginPosition;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public MarginStatement getMarginStatement() { return marginStatement; }
        public MarginPosition getInitialMarginPosition() { return initialMarginPosition; }
        public MarginPosition getVariationMarginPosition() { return variationMarginPosition; }
    }
    
    /**
     * Exception for margin account setup errors
     */
    public static class MarginAccountSetupException extends RuntimeException {
        public MarginAccountSetupException(String message) {
            super(message);
        }
        
        public MarginAccountSetupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}