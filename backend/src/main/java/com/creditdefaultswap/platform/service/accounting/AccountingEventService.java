package com.creditdefaultswap.platform.service.accounting;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.accounting.AccountingEvent;
import com.creditdefaultswap.platform.model.eod.DailyPnlResult;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.accounting.AccountingEventRepository;
import com.creditdefaultswap.platform.repository.eod.DailyPnlResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating accounting events from EOD valuations
 * Transforms P&L results into journal entries for posting to General Ledger
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountingEventService {
    
    private final AccountingEventRepository eventRepository;
    private final DailyPnlResultRepository pnlRepository;
    private final CDSTradeRepository tradeRepository;
    
    // Account code mappings (configurable via properties in production)
    private static final Map<String, String> ACCOUNT_CODES = new HashMap<>();
    
    static {
        // Asset accounts
        ACCOUNT_CODES.put("CDS_ASSET_LONG", "1234.10");      // CDS asset - protection bought
        ACCOUNT_CODES.put("CDS_ASSET_SHORT", "1234.20");     // CDS asset - protection sold
        ACCOUNT_CODES.put("ACCRUED_INTEREST_RECEIVABLE", "1240.10");
        ACCOUNT_CODES.put("ACCRUED_INTEREST_PAYABLE", "2140.10");
        
        // P&L accounts
        ACCOUNT_CODES.put("MTM_PNL_UNREALIZED", "5100.10");  // Unrealized MTM gain/loss
        ACCOUNT_CODES.put("INTEREST_INCOME", "4500.10");     // Interest income
        ACCOUNT_CODES.put("INTEREST_EXPENSE", "6500.10");    // Interest expense
        
        // Credit event accounts
        ACCOUNT_CODES.put("CREDIT_EVENT_LOSS", "6100.10");
        ACCOUNT_CODES.put("CREDIT_EVENT_GAIN", "4100.10");
    }
    
    /**
     * Generate accounting events from daily P&L results
     */
    @Transactional
    public List<AccountingEvent> generateAccountingEvents(LocalDate eventDate, String jobId) {
        log.info("Generating accounting events for date: {}", eventDate);
        
        // Check if events already generated for this date
        if (eventRepository.existsByEventDateAndTradeIdAndEventType(eventDate, null, AccountingEvent.EventType.MTM_VALUATION)) {
            log.warn("Accounting events already exist for date: {}", eventDate);
            return eventRepository.findByEventDate(eventDate);
        }
        
        List<DailyPnlResult> pnlResults = pnlRepository.findByPnlDate(eventDate);
        List<AccountingEvent> events = new ArrayList<>();
        
        for (DailyPnlResult pnl : pnlResults) {
            try {
                // Generate MTM valuation entries
                events.addAll(generateMtmEntries(pnl, jobId));
                
                // Generate accrued interest entries
                events.addAll(generateAccruedEntries(pnl, jobId));
                
            } catch (Exception e) {
                log.error("Failed to generate accounting events for trade {}: {}", 
                    pnl.getTradeId(), e.getMessage(), e);
            }
        }
        
        // Save all events
        List<AccountingEvent> savedEvents = eventRepository.saveAll(events);
        
        log.info("Generated {} accounting events for date: {}", savedEvents.size(), eventDate);
        return savedEvents;
    }
    
    /**
     * Generate mark-to-market valuation entries
     * 
     * For protection buyers (LONG):
     *   If NPV increases (asset becomes more valuable):
     *     DR: CDS Asset         $npv_change
     *     CR: MTM P&L Unrealized $npv_change
     *   
     *   If NPV decreases:
     *     DR: MTM P&L Unrealized $npv_change
     *     CR: CDS Asset         $npv_change
     */
    private List<AccountingEvent> generateMtmEntries(DailyPnlResult pnl, String jobId) {
        List<AccountingEvent> events = new ArrayList<>();
        
        // Skip if no previous valuation (new trade - would be booked separately)
        if (pnl.getPreviousNpv() == null) {
            return events;
        }
        
        BigDecimal npvChange = pnl.getCurrentNpv().subtract(pnl.getPreviousNpv());
        
        // Skip if no material change
        if (npvChange.abs().compareTo(new BigDecimal("1.00")) < 0) {
            return events;
        }
        
        CDSTrade trade = tradeRepository.findById(pnl.getTradeId()).orElse(null);
        if (trade == null) {
            log.warn("Trade not found for P&L result: {}", pnl.getTradeId());
            return events;
        }
        
        boolean isLong = trade.getBuySellProtection() == CDSTrade.ProtectionDirection.BUY;
        String assetAccount = isLong ? "CDS_ASSET_LONG" : "CDS_ASSET_SHORT";
        
        // Asset leg (adjust CDS asset value)
        AccountingEvent assetEvent = AccountingEvent.builder()
            .eventDate(pnl.getPnlDate())
            .eventType(AccountingEvent.EventType.MTM_VALUATION)
            .tradeId(pnl.getTradeId())
            .referenceEntityName(trade.getReferenceEntity())
            .accountCode(ACCOUNT_CODES.get(assetAccount))
            .accountName(assetAccount.replace("_", " "))
            .currency(pnl.getCurrency())
            .currentNpv(pnl.getCurrentNpv())
            .previousNpv(pnl.getPreviousNpv())
            .npvChange(npvChange)
            .valuationJobId(jobId)
            .description(String.format("MTM revaluation for %s - Trade %d", 
                trade.getReferenceEntity(), pnl.getTradeId()))
            .build();
        
        // P&L leg (recognize unrealized gain/loss)
        AccountingEvent pnlEvent = AccountingEvent.builder()
            .eventDate(pnl.getPnlDate())
            .eventType(AccountingEvent.EventType.MTM_PNL_UNREALIZED)
            .tradeId(pnl.getTradeId())
            .referenceEntityName(trade.getReferenceEntity())
            .accountCode(ACCOUNT_CODES.get("MTM_PNL_UNREALIZED"))
            .accountName("MTM P&L Unrealized")
            .currency(pnl.getCurrency())
            .currentNpv(pnl.getCurrentNpv())
            .previousNpv(pnl.getPreviousNpv())
            .npvChange(npvChange)
            .valuationJobId(jobId)
            .description(String.format("MTM P&L for %s - Trade %d", 
                trade.getReferenceEntity(), pnl.getTradeId()))
            .build();
        
        // Determine debit/credit based on NPV change direction
        if (npvChange.compareTo(BigDecimal.ZERO) > 0) {
            // NPV increased - asset more valuable
            assetEvent.setDebitAmount(npvChange.abs());
            assetEvent.setCreditAmount(BigDecimal.ZERO);
            
            pnlEvent.setDebitAmount(BigDecimal.ZERO);
            pnlEvent.setCreditAmount(npvChange.abs());
        } else {
            // NPV decreased - asset less valuable
            assetEvent.setDebitAmount(BigDecimal.ZERO);
            assetEvent.setCreditAmount(npvChange.abs());
            
            pnlEvent.setDebitAmount(npvChange.abs());
            pnlEvent.setCreditAmount(BigDecimal.ZERO);
        }
        
        events.add(assetEvent);
        events.add(pnlEvent);
        
        return events;
    }
    
    /**
     * Generate accrued interest entries
     * 
     * For protection sellers (receiving premium):
     *   DR: Accrued Interest Receivable  $accrued_change
     *   CR: Interest Income              $accrued_change
     * 
     * For protection buyers (paying premium):
     *   DR: Interest Expense             $accrued_change
     *   CR: Accrued Interest Payable     $accrued_change
     */
    private List<AccountingEvent> generateAccruedEntries(DailyPnlResult pnl, String jobId) {
        List<AccountingEvent> events = new ArrayList<>();
        
        BigDecimal accruedChange = BigDecimal.ZERO;
        if (pnl.getPreviousAccrued() != null) {
            accruedChange = pnl.getCurrentAccrued().subtract(pnl.getPreviousAccrued());
        } else {
            accruedChange = pnl.getCurrentAccrued();
        }
        
        // Skip if no material change
        if (accruedChange.abs().compareTo(new BigDecimal("1.00")) < 0) {
            return events;
        }
        
        CDSTrade trade = tradeRepository.findById(pnl.getTradeId()).orElse(null);
        if (trade == null) {
            return events;
        }
        
        boolean isReceivingPremium = trade.getBuySellProtection() == CDSTrade.ProtectionDirection.SELL;
        
        AccountingEvent accruedEvent;
        AccountingEvent incomeExpenseEvent;
        
        if (isReceivingPremium) {
            // Protection seller - receiving premium (income)
            accruedEvent = AccountingEvent.builder()
                .eventDate(pnl.getPnlDate())
                .eventType(AccountingEvent.EventType.ACCRUED_INTEREST)
                .tradeId(pnl.getTradeId())
                .referenceEntityName(trade.getReferenceEntity())
                .accountCode(ACCOUNT_CODES.get("ACCRUED_INTEREST_RECEIVABLE"))
                .accountName("Accrued Interest Receivable")
                .debitAmount(accruedChange.abs())
                .creditAmount(BigDecimal.ZERO)
                .currency(pnl.getCurrency())
                .accruedChange(accruedChange)
                .valuationJobId(jobId)
                .description(String.format("Accrued premium receivable for %s - Trade %d", 
                    trade.getReferenceEntity(), pnl.getTradeId()))
                .build();
            
            incomeExpenseEvent = AccountingEvent.builder()
                .eventDate(pnl.getPnlDate())
                .eventType(AccountingEvent.EventType.ACCRUED_INTEREST)
                .tradeId(pnl.getTradeId())
                .referenceEntityName(trade.getReferenceEntity())
                .accountCode(ACCOUNT_CODES.get("INTEREST_INCOME"))
                .accountName("Interest Income")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(accruedChange.abs())
                .currency(pnl.getCurrency())
                .accruedChange(accruedChange)
                .valuationJobId(jobId)
                .description(String.format("Premium income for %s - Trade %d", 
                    trade.getReferenceEntity(), pnl.getTradeId()))
                .build();
        } else {
            // Protection buyer - paying premium (expense)
            incomeExpenseEvent = AccountingEvent.builder()
                .eventDate(pnl.getPnlDate())
                .eventType(AccountingEvent.EventType.ACCRUED_INTEREST)
                .tradeId(pnl.getTradeId())
                .referenceEntityName(trade.getReferenceEntity())
                .accountCode(ACCOUNT_CODES.get("INTEREST_EXPENSE"))
                .accountName("Interest Expense")
                .debitAmount(accruedChange.abs())
                .creditAmount(BigDecimal.ZERO)
                .currency(pnl.getCurrency())
                .accruedChange(accruedChange)
                .valuationJobId(jobId)
                .description(String.format("Premium expense for %s - Trade %d", 
                    trade.getReferenceEntity(), pnl.getTradeId()))
                .build();
            
            accruedEvent = AccountingEvent.builder()
                .eventDate(pnl.getPnlDate())
                .eventType(AccountingEvent.EventType.ACCRUED_INTEREST)
                .tradeId(pnl.getTradeId())
                .referenceEntityName(trade.getReferenceEntity())
                .accountCode(ACCOUNT_CODES.get("ACCRUED_INTEREST_PAYABLE"))
                .accountName("Accrued Interest Payable")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(accruedChange.abs())
                .currency(pnl.getCurrency())
                .accruedChange(accruedChange)
                .valuationJobId(jobId)
                .description(String.format("Accrued premium payable for %s - Trade %d", 
                    trade.getReferenceEntity(), pnl.getTradeId()))
                .build();
        }
        
        events.add(accruedEvent);
        events.add(incomeExpenseEvent);
        
        return events;
    }
    
    /**
     * Get pending accounting events for a date
     */
    public List<AccountingEvent> getPendingEvents(LocalDate date) {
        return eventRepository.findByEventDateAndStatus(date, AccountingEvent.EventStatus.PENDING);
    }
    
    /**
     * Get all events for a date
     */
    public List<AccountingEvent> getEventsByDate(LocalDate date) {
        return eventRepository.findByEventDate(date);
    }
    
    /**
     * Mark event as posted to GL
     */
    @Transactional
    public void markEventAsPosted(Long eventId, String glBatchId, String postedBy) {
        AccountingEvent event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        
        event.setStatus(AccountingEvent.EventStatus.POSTED);
        event.setPostedToGl(true);
        event.setPostedAt(LocalDateTime.now());
        event.setGlBatchId(glBatchId);
        event.setPostedBy(postedBy);
        
        eventRepository.save(event);
        
        log.info("Marked event {} as posted to GL batch {}", eventId, glBatchId);
    }
    
    /**
     * Mark multiple events as posted
     */
    @Transactional
    public void markEventsAsPosted(List<Long> eventIds, String glBatchId, String postedBy) {
        for (Long eventId : eventIds) {
            markEventAsPosted(eventId, glBatchId, postedBy);
        }
        log.info("Marked {} events as posted to GL batch {}", eventIds.size(), glBatchId);
    }
    
    /**
     * Get accounting summary for a date
     */
    public Map<String, Object> getAccountingSummary(LocalDate date) {
        List<AccountingEvent> events = eventRepository.findByEventDate(date);
        
        long pendingCount = events.stream()
            .filter(e -> e.getStatus() == AccountingEvent.EventStatus.PENDING)
            .count();
        
        long postedCount = events.stream()
            .filter(e -> e.getStatus() == AccountingEvent.EventStatus.POSTED)
            .count();
        
        BigDecimal totalDebits = events.stream()
            .filter(e -> e.getDebitAmount() != null)
            .map(AccountingEvent::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = events.stream()
            .filter(e -> e.getCreditAmount() != null)
            .map(AccountingEvent::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", date);
        summary.put("totalEvents", events.size());
        summary.put("pendingEvents", pendingCount);
        summary.put("postedEvents", postedCount);
        summary.put("totalDebits", totalDebits);
        summary.put("totalCredits", totalCredits);
        summary.put("balanced", totalDebits.compareTo(totalCredits) == 0);
        
        return summary;
    }
}
