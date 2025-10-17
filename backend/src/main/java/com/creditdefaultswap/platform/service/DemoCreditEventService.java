package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class DemoCreditEventService {
    
    private final CDSTradeRepository tradeRepository;
    private final CreditEventService creditEventService;
    private final Random random = new Random();
    
    // Common reference entities that might have credit events
    private static final String[] CREDIT_EVENT_ENTITIES = {
        "Lehman Brothers Holdings Inc.",
        "Washington Mutual Bank",
        "General Motors Corporation", 
        "Chrysler LLC",
        "CIT Group Inc.",
        "Tribune Company",
        "Six Flags Inc.",
        "Lyondell Chemical Company",
        "Extended Stay Inc.",
        "Nortel Networks Corporation"
    };
    
    // Recovery rates typically seen in credit events (as percentages)
    private static final double[] TYPICAL_RECOVERY_RATES = {
        0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50, 0.55, 0.60
    };
    
    @Autowired
    public DemoCreditEventService(CDSTradeRepository tradeRepository,
                                 CreditEventService creditEventService) {
        this.tradeRepository = tradeRepository;
        this.creditEventService = creditEventService;
    }
    
    /**
     * Generate realistic demo credit events for a CDS trade
     * Events are backdated to be between trade date and current date
     */
    @Transactional
    public List<CreditEvent> generateDemoCreditEvents(Long tradeId) {
        // Verify trade exists
        CDSTrade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new IllegalArgumentException("Trade not found with ID: " + tradeId));
        
        // Only generate events for ACTIVE trades to maintain realistic workflow
        if (trade.getTradeStatus() != TradeStatus.ACTIVE) {
            throw new IllegalStateException("Can only generate demo events for ACTIVE trades. Current status: " + trade.getTradeStatus());
        }
        
        List<CreditEvent> generatedEvents = new ArrayList<>();
        
        // Decide if this reference entity should have credit events (30% chance)
        if (random.nextDouble() < 0.3) {
            return generatedEvents; // No events for this entity
        }
        
        // Generate 1-3 credit events
        int numEvents = random.nextInt(3) + 1;
        
        LocalDate tradeDate = trade.getTradeDate();
        LocalDate currentDate = LocalDate.now();
        
        // Ensure we have enough time range for events and all dates are in the past
        if (tradeDate.isAfter(currentDate.minusDays(30))) {
            // Trade is recent, generate just one past event (between trade date and yesterday)
            LocalDate maxEventDate = currentDate.minusDays(1); // Ensure it's in the past
            if (tradeDate.isBefore(maxEventDate)) {
                LocalDate eventDate = generateRandomDateBetween(tradeDate.plusDays(1), maxEventDate);
                CreditEvent event = generateSingleCreditEvent(trade, eventDate);
                if (event != null) {
                    generatedEvents.add(event);
                }
            }
            // If trade is today or yesterday, skip generating events
        } else {
            // Generate multiple events spread over time (all in the past)
            LocalDate maxEventDate = currentDate.minusDays(1); // Ensure all events are in the past
            for (int i = 0; i < numEvents; i++) {
                LocalDate eventDate = generateRandomDateBetween(tradeDate.plusDays(1), maxEventDate);
                CreditEvent event = generateSingleCreditEvent(trade, eventDate);
                if (event != null) {
                    generatedEvents.add(event);
                }
            }
        }
        
        return generatedEvents;
    }
    
    /**
     * Generate a single realistic credit event
     */
    private CreditEvent generateSingleCreditEvent(CDSTrade trade, LocalDate eventDate) {
        try {
            // Validate date is not in the future before proceeding
            if (eventDate.isAfter(LocalDate.now())) {
                System.err.println("Skipping credit event generation for trade " + trade.getId() + 
                                 ": event date " + eventDate + " is in the future");
                return null;
            }
            
            // Choose event type with realistic probabilities
            CreditEventType eventType = chooseRandomEventType();
            
            // Notice date is typically 1-7 days after event date (but not in the future)
            LocalDate maxNoticeDate = LocalDate.now(); // Don't allow future notice dates
            LocalDate tentativeNoticeDate = eventDate.plusDays(random.nextInt(7) + 1);
            LocalDate noticeDate = tentativeNoticeDate.isAfter(maxNoticeDate) ? maxNoticeDate : tentativeNoticeDate;
            
            // Choose settlement method (70% cash, 30% physical for demo purposes)
            SettlementMethod settlementMethod = random.nextDouble() < 0.7 ? 
                SettlementMethod.CASH : SettlementMethod.PHYSICAL;
            
            // Generate realistic comments
            String comments = generateRealisticComments(eventType, trade.getReferenceEntity());
            
            // Create the credit event request
            var request = new com.creditdefaultswap.platform.dto.CreateCreditEventRequest();
            request.setEventType(eventType);
            request.setEventDate(eventDate);
            request.setNoticeDate(noticeDate);
            request.setSettlementMethod(settlementMethod);
            request.setComments(comments);
            
            // Use the existing service to create the event (ensures all validation and processing)
            var response = creditEventService.recordCreditEvent(trade.getId(), request);
            return response.getCreditEvent();
            
        } catch (Exception e) {
            // Log but don't fail the entire generation process
            // This prevents transaction rollback when individual event creation fails
            System.err.println("Failed to generate credit event for trade " + trade.getId() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Choose event type with realistic distribution
     */
    private CreditEventType chooseRandomEventType() {
        double rand = random.nextDouble();
        
        // Bankruptcy is most common in demo scenarios
        if (rand < 0.4) return CreditEventType.BANKRUPTCY;
        if (rand < 0.6) return CreditEventType.FAILURE_TO_PAY;
        if (rand < 0.8) return CreditEventType.RESTRUCTURING;
        if (rand < 0.9) return CreditEventType.OBLIGATION_DEFAULT;
        return CreditEventType.REPUDIATION_MORATORIUM;
    }
    
    /**
     * Generate realistic comments based on event type and entity
     */
    private String generateRealisticComments(CreditEventType eventType, String referenceEntity) {
        String[] bankruptcyComments = {
            "Chapter 11 bankruptcy filing confirmed",
            "Voluntary bankruptcy petition filed",
            "Court-supervised reorganization initiated",
            "Automatic stay in effect"
        };
        
        String[] failureToPayComments = {
            "Missed interest payment on senior debt",
            "Grace period expired without payment",
            "Default on bond coupon payment",
            "Payment failure on scheduled maturity"
        };
        
        String[] restructuringComments = {
            "Debt restructuring agreement reached",
            "Terms modified under workout agreement", 
            "Voluntary restructuring with creditors",
            "Amended payment schedule implemented"
        };
        
        String[] accelerationComments = {
            "Loan acceleration triggered by covenant breach",
            "Cross-default clause activated",
            "Acceleration notice issued by trustee",
            "Early termination of credit facility"
        };
        
        String[] repudiationComments = {
            "Government intervention in debt obligations",
            "Sovereign debt repudiation declared",
            "Moratorium on external debt payments",
            "Regulatory suspension of payments"
        };
        
        String[] payoutComments = {
            "Automatic payout triggered by credit event",
            "CDS protection payment processed",
            "Settlement payout completed",
            "Protection buyer compensated"
        };
        
        String[] comments = switch (eventType) {
            case BANKRUPTCY -> bankruptcyComments;
            case FAILURE_TO_PAY -> failureToPayComments;
            case RESTRUCTURING -> restructuringComments;
            case OBLIGATION_DEFAULT -> accelerationComments;
            case REPUDIATION_MORATORIUM -> repudiationComments;
            case PAYOUT -> payoutComments;
        };
        
        return comments[random.nextInt(comments.length)] + " - " + referenceEntity;
    }
    
    /**
     * Generate a random date between start and end (inclusive)
     */
    private LocalDate generateRandomDateBetween(LocalDate start, LocalDate end) {
        long daysBetween = start.until(end).getDays();
        if (daysBetween <= 0) {
            return start;
        }
        long randomDays = random.nextLong(daysBetween + 1);
        return start.plusDays(randomDays);
    }
    
    /**
     * Get a realistic recovery rate for demo purposes
     */
    private double getRealisticRecoveryRate() {
        return TYPICAL_RECOVERY_RATES[random.nextInt(TYPICAL_RECOVERY_RATES.length)];
    }
    
    /**
     * Check if a reference entity is likely to have credit events (for realism)
     */
    private boolean isLikelyToHaveCreditEvents(String referenceEntity) {
        // Check if entity name contains any stress indicators
        String entityLower = referenceEntity.toLowerCase();
        return entityLower.contains("bank") || 
               entityLower.contains("financial") ||
               entityLower.contains("mortgage") ||
               entityLower.contains("automotive") ||
               entityLower.contains("airline") ||
               entityLower.contains("retail") ||
               entityLower.contains("energy") ||
               entityLower.contains("telecom");
    }
}