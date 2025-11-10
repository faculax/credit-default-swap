package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.CreateCreditEventRequest;
import com.creditdefaultswap.platform.dto.CreditEventResponse;
import com.creditdefaultswap.platform.dto.SettlementView;
import com.creditdefaultswap.platform.model.CashSettlement;
import com.creditdefaultswap.platform.model.CreditEvent;
import com.creditdefaultswap.platform.model.PhysicalSettlementInstruction;
import com.creditdefaultswap.platform.service.CreditEventService;
import com.creditdefaultswap.platform.service.SettlementService;
import com.creditdefaultswap.platform.service.DemoCreditEventService;
import com.creditdefaultswap.platform.service.LineageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cds-trades")
public class CreditEventController {
    
    private final CreditEventService creditEventService;
    private final SettlementService settlementService;
    private final DemoCreditEventService demoCreditEventService;
    private final LineageService lineageService;
    
    @Autowired
    public CreditEventController(CreditEventService creditEventService,
                                SettlementService settlementService,
                                DemoCreditEventService demoCreditEventService,
                                LineageService lineageService) {
        this.creditEventService = creditEventService;
        this.settlementService = settlementService;
        this.demoCreditEventService = demoCreditEventService;
        this.lineageService = lineageService;
    }
    
    /**
     * Record a credit event for a trade
     * Story 4.1 & 4.2
     * 
     * Returns the created credit event along with list of all affected trade IDs
     * (includes propagated trades for BANKRUPTCY and RESTRUCTURING events)
     */
    @PostMapping("/{tradeId}/credit-events")
    public ResponseEntity<CreditEventResponse> recordCreditEvent(
            @PathVariable Long tradeId,
            @Valid @RequestBody CreateCreditEventRequest request) {
        
        CreditEventResponse response = creditEventService.recordCreditEvent(tradeId, request);
        
        // Track lineage
        lineageService.trackCreditEvent(tradeId, request.getEventType().toString(), 
            response.getCreditEvent().getId(), "system");
        
        // Return 201 for new creation, 200 for existing (idempotent)
        HttpStatus status = response.getCreditEvent().getCreatedAt().equals(response.getCreditEvent().getUpdatedAt()) ? 
            HttpStatus.CREATED : HttpStatus.OK;
            
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * Get all credit events for a trade
     */
    @GetMapping("/{tradeId}/credit-events")
    public ResponseEntity<List<CreditEvent>> getCreditEventsForTrade(@PathVariable Long tradeId) {
        List<CreditEvent> creditEvents = creditEventService.getCreditEventsForTrade(tradeId);
        return ResponseEntity.ok(creditEvents);
    }
    
    /**
     * Get cash settlement for a credit event
     * Story 4.3
     */
    @GetMapping("/{tradeId}/credit-events/{eventId}/cash-settlement")
    public ResponseEntity<CashSettlement> getCashSettlement(
            @PathVariable Long tradeId,
            @PathVariable UUID eventId) {
        
        Optional<CashSettlement> settlement = settlementService.getCashSettlement(eventId);
        
        return settlement.map(s -> ResponseEntity.ok(s))
                        .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get physical settlement instruction for a credit event
     * Story 4.4
     */
    @GetMapping("/{tradeId}/credit-events/{eventId}/physical-instruction")
    public ResponseEntity<PhysicalSettlementInstruction> getPhysicalSettlement(
            @PathVariable Long tradeId,
            @PathVariable UUID eventId) {
        
        Optional<PhysicalSettlementInstruction> instruction = settlementService.getPhysicalSettlement(eventId);
        
        return instruction.map(i -> ResponseEntity.ok(i))
                         .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get unified settlement view (cash or physical)
     * Story 4.5
     */
    @GetMapping("/{tradeId}/credit-events/{eventId}/settlement")
    public ResponseEntity<SettlementView> getSettlement(
            @PathVariable Long tradeId,
            @PathVariable UUID eventId) {
        
        Optional<SettlementView> settlement = settlementService.getSettlement(eventId);
        
        return settlement.map(s -> ResponseEntity.ok(s))
                        .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Generate demo credit events for a trade (for testing/demo purposes)
     */
    @PostMapping("/{tradeId}/demo-credit-events")
    public ResponseEntity<List<CreditEvent>> generateDemoCreditEvents(@PathVariable Long tradeId) {
        try {
            List<CreditEvent> generatedEvents = demoCreditEventService.generateDemoCreditEvents(tradeId);
            return ResponseEntity.ok(generatedEvents);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}