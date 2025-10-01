package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CreditEventRepository;
import com.creditdefaultswap.platform.repository.PhysicalSettlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    public CreditEvent recordCreditEvent(Long tradeId, CreateCreditEventRequest request) {
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
            return existingEvent.get();
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
        
        return creditEvent;
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