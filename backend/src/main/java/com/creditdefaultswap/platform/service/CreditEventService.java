package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.dto.CreditEventResponse;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CreditEventService {
    
    private final CreditEventRepository creditEventRepository;
    private final CDSTradeRepository tradeRepository;
    private final PhysicalSettlementRepository physicalSettlementRepository;
    private final AuditService auditService;
    private final CashSettlementService cashSettlementService;
    
    @Autowired
    public CreditEventService(CreditEventRepository creditEventRepository,
                             CDSTradeRepository tradeRepository,
                             PhysicalSettlementRepository physicalSettlementRepository,
                             AuditService auditService,
                             CashSettlementService cashSettlementService) {
        this.creditEventRepository = creditEventRepository;
        this.tradeRepository = tradeRepository;
        this.physicalSettlementRepository = physicalSettlementRepository;
        this.auditService = auditService;
        this.cashSettlementService = cashSettlementService;
    }
    
    /**
     * Record a credit event for a trade with full validation and settlement processing
     */
    @Transactional
    public CreditEventResponse recordCreditEvent(Long tradeId, CreateCreditEventRequest request) {
        // List to track all affected trade IDs
        List<Long> affectedTradeIds = new ArrayList<>();
        affectedTradeIds.add(tradeId);
        
        // Validation
        validateRequest(request);
        
        // Check trade exists and is in ACTIVE state
        CDSTrade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found with ID: " + tradeId));
        
        if (trade.getTradeStatus() != TradeStatus.ACTIVE) {
            throw new IllegalStateException("Trade must be ACTIVE to record credit event. Current status: " + trade.getTradeStatus());
        }
        
        // Check for existing credit event (idempotency)
        Optional<CreditEvent> existingEvent = creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            tradeId, request.getEventType(), request.getEventDate());
        
        if (existingEvent.isPresent()) {
            // Return existing event (idempotent behavior)
            return new CreditEventResponse(existingEvent.get(), affectedTradeIds);
        }
        
        // Create new credit event
        CreditEvent creditEvent = new CreditEvent();
        creditEvent.setTradeId(tradeId);
        creditEvent.setEventType(request.getEventType());
        creditEvent.setEventDate(request.getEventDate());
        creditEvent.setNoticeDate(request.getNoticeDate());
        creditEvent.setSettlementMethod(request.getSettlementMethod());
        creditEvent.setComments(request.getComments());
        
        // Save credit event
        creditEvent = creditEventRepository.save(creditEvent);
        
        // Update trade status
        TradeStatus oldStatus = trade.getTradeStatus();
        trade.setTradeStatus(TradeStatus.CREDIT_EVENT_RECORDED);
        tradeRepository.save(trade);
        
        // Log audit trail
        auditService.logCreditEventCreation(creditEvent.getId(), "SYSTEM", trade.getReferenceEntity());
        auditService.logTradeStatusTransition(tradeId, "SYSTEM", oldStatus.name(), TradeStatus.CREDIT_EVENT_RECORDED.name());
        
        // Process settlement based on method
        if (request.getSettlementMethod() == SettlementMethod.CASH) {
            // Trigger cash settlement calculation
            cashSettlementService.calculateCashSettlement(creditEvent.getId(), trade);
        } else if (request.getSettlementMethod() == SettlementMethod.PHYSICAL) {
            // Create physical settlement scaffold
            createPhysicalSettlementScaffold(creditEvent, trade);
        }
        
        // Check if this is a terminal event that triggers automatic payout
        if (request.getEventType() == CreditEventType.BANKRUPTCY || 
            request.getEventType() == CreditEventType.RESTRUCTURING) {
            // Automatically create PAYOUT event using the trade's settlement type
            createPayoutEvent(tradeId, trade, request.getEventDate(), trade.getSettlementType());
            
            // Propagate credit event to all other ACTIVE CDS for the same reference entity
            List<Long> propagatedTradeIds = propagateCreditEventToReferenceEntity(trade.getReferenceEntity(), tradeId, request);
            affectedTradeIds.addAll(propagatedTradeIds);
        }
        
        return new CreditEventResponse(creditEvent, affectedTradeIds);
    }
    
    /**
     * Create automatic PAYOUT event for terminal credit events
     */
    private void createPayoutEvent(Long tradeId, CDSTrade trade, LocalDate triggerEventDate, SettlementMethod settlementMethod) {
        // Check if PAYOUT event already exists for this date
        Optional<CreditEvent> existingPayout = creditEventRepository.findByTradeIdAndEventTypeAndEventDate(
            tradeId, CreditEventType.PAYOUT, triggerEventDate);
        
        if (existingPayout.isPresent()) {
            return; // Already exists, skip creation
        }
        
        // Create PAYOUT event
        CreditEvent payoutEvent = new CreditEvent();
        payoutEvent.setTradeId(tradeId);
        payoutEvent.setEventType(CreditEventType.PAYOUT);
        payoutEvent.setEventDate(triggerEventDate);
        payoutEvent.setNoticeDate(triggerEventDate);
        payoutEvent.setSettlementMethod(settlementMethod);
        payoutEvent.setComments("Automatic payout triggered by credit event - CDS protection paid out");
        
        creditEventRepository.save(payoutEvent);
        
        // Update trade status to SETTLED
        TradeStatus newStatus = settlementMethod == SettlementMethod.CASH 
            ? TradeStatus.SETTLED_CASH 
            : TradeStatus.SETTLED_PHYSICAL;
        trade.setTradeStatus(newStatus);
        tradeRepository.save(trade);
        
        // Log audit trail
        auditService.logCreditEventCreation(payoutEvent.getId(), "SYSTEM", 
            "Automatic payout for " + trade.getReferenceEntity());
        auditService.logTradeStatusTransition(tradeId, "SYSTEM", 
            TradeStatus.CREDIT_EVENT_RECORDED.name(), newStatus.name());
    }
    
    /**
     * Propagate credit event to all other ACTIVE CDS for the same reference entity
     * This ensures that when BANKRUPTCY or RESTRUCTURING is recorded for one CDS,
     * all other active CDS for that reference entity are automatically triggered
     * 
     * @return List of trade IDs that were affected by the propagation
     */
    private List<Long> propagateCreditEventToReferenceEntity(String referenceEntity, Long originTradeId, CreateCreditEventRequest request) {
        List<Long> affectedTradeIds = new ArrayList<>();
        
        // Find all other ACTIVE trades for the same reference entity
        List<CDSTrade> affectedTrades = tradeRepository.findByReferenceEntityOrderByCreatedAtDesc(referenceEntity)
            .stream()
            .filter(t -> t.getTradeStatus() == TradeStatus.ACTIVE && !t.getId().equals(originTradeId))
            .toList();
        
        if (affectedTrades.isEmpty()) {
            return affectedTradeIds; // No other active trades to propagate to
        }
        
        // For each affected trade, create the same credit event and payout
        for (CDSTrade affectedTrade : affectedTrades) {
            try {
                // Create credit event for affected trade
                CreditEvent affectedEvent = new CreditEvent();
                affectedEvent.setTradeId(affectedTrade.getId());
                affectedEvent.setEventType(request.getEventType());
                affectedEvent.setEventDate(request.getEventDate());
                affectedEvent.setNoticeDate(request.getNoticeDate());
                affectedEvent.setSettlementMethod(request.getSettlementMethod());
                affectedEvent.setComments("Propagated credit event for reference entity: " + referenceEntity + 
                    " (originated from Trade ID: " + originTradeId + ")");
                
                // Save credit event
                affectedEvent = creditEventRepository.save(affectedEvent);
                
                // Update trade status
                TradeStatus oldStatus = affectedTrade.getTradeStatus();
                affectedTrade.setTradeStatus(TradeStatus.CREDIT_EVENT_RECORDED);
                tradeRepository.save(affectedTrade);
                
                // Log audit trail
                auditService.logCreditEventCreation(affectedEvent.getId(), "SYSTEM", 
                    "Propagated to " + affectedTrade.getReferenceEntity());
                auditService.logTradeStatusTransition(affectedTrade.getId(), "SYSTEM", 
                    oldStatus.name(), TradeStatus.CREDIT_EVENT_RECORDED.name());
                
                // Process settlement based on method
                if (request.getSettlementMethod() == SettlementMethod.CASH) {
                    cashSettlementService.calculateCashSettlement(affectedEvent.getId(), affectedTrade);
                } else if (request.getSettlementMethod() == SettlementMethod.PHYSICAL) {
                    createPhysicalSettlementScaffold(affectedEvent, affectedTrade);
                }
                
                // Create PAYOUT event for affected trade using its settlement type
                createPayoutEvent(affectedTrade.getId(), affectedTrade, request.getEventDate(), affectedTrade.getSettlementType());
                
                // Add to affected list
                affectedTradeIds.add(affectedTrade.getId());
                
            } catch (Exception e) {
                // Log error but continue processing other trades
                auditService.logCreditEventCreation(null, "SYSTEM", 
                    "Failed to propagate credit event to Trade ID: " + affectedTrade.getId() + " - " + e.getMessage());
            }
        }
        
        return affectedTradeIds;
    }
    
    /**
     * Get all credit events for a trade
     */
    public List<CreditEvent> getCreditEventsForTrade(Long tradeId) {
        // Verify trade exists
        if (!tradeRepository.existsById(tradeId)) {
            throw new IllegalArgumentException("Trade not found with ID: " + tradeId);
        }
        
        return creditEventRepository.findByTradeIdOrderByEventDateDesc(tradeId);
    }
    
    /**
     * Create physical settlement instruction scaffold
     */
    private void createPhysicalSettlementScaffold(CreditEvent creditEvent, CDSTrade trade) {
        // Check if scaffold already exists
        if (physicalSettlementRepository.existsByCreditEventId(creditEvent.getId())) {
            return; // Already exists, skip creation
        }
        
        PhysicalSettlementInstruction instruction = new PhysicalSettlementInstruction();
        instruction.setCreditEventId(creditEvent.getId());
        instruction.setTradeId(trade.getId());
        instruction.setStatus(PhysicalSettlementInstruction.InstructionStatus.DRAFT);
        
        instruction = physicalSettlementRepository.save(instruction);
        
        // Log audit trail
        auditService.logPhysicalSettlementCreation(instruction.getId(), "SYSTEM");
    }
    
    /**
     * Validate credit event request
     */
    private void validateRequest(CreateCreditEventRequest request) {
        if (request.getEventDate() == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        
        if (request.getNoticeDate() == null) {
            throw new IllegalArgumentException("Notice date is required");
        }
        
        if (request.getEventDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }
        
        if (request.getNoticeDate().isBefore(request.getEventDate())) {
            throw new IllegalArgumentException("Notice date must be on or after event date");
        }
        
        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (request.getSettlementMethod() == null) {
            throw new IllegalArgumentException("Settlement method is required");
        }
    }
}