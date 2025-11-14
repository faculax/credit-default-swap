package com.creditdefaultswap.platform.controller.accounting;

import com.creditdefaultswap.platform.model.accounting.AccountingEvent;
import com.creditdefaultswap.platform.service.accounting.AccountingEventService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST API for accounting system integration
 * Provides endpoints for retrieving valuation-based accounting events
 * and marking them as posted to General Ledger
 */
@RestController
@RequestMapping("/api/accounting")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class AccountingIntegrationController {
    
    private final AccountingEventService accountingEventService;
    
    /**
     * Generate accounting events from daily P&L
     * Typically called automatically after EOD valuation completes
     */
    @PostMapping("/events/generate")
    public ResponseEntity<?> generateAccountingEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String jobId) {
        try {
            log.info("Generating accounting events for date: {}", date);
            
            List<AccountingEvent> events = accountingEventService.generateAccountingEvents(date, jobId);
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Generated " + events.size() + " accounting events",
                "date", date,
                "eventCount", events.size(),
                "events", events
            ));
        } catch (Exception e) {
            log.error("Error generating accounting events: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to generate accounting events: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Get all accounting events for a specific date
     */
    @GetMapping("/events/{date}")
    public ResponseEntity<List<AccountingEvent>> getEventsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AccountingEvent> events = accountingEventService.getEventsByDate(date);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get pending (unposted) accounting events for a date
     * Accounting system polls this endpoint to retrieve events for GL posting
     */
    @GetMapping("/events/{date}/pending")
    public ResponseEntity<List<AccountingEvent>> getPendingEvents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AccountingEvent> events = accountingEventService.getPendingEvents(date);
        log.info("Retrieved {} pending accounting events for date: {}", events.size(), date);
        return ResponseEntity.ok(events);
    }
    
    /**
     * Get accounting events by status
     */
    @GetMapping("/events")
    public ResponseEntity<List<AccountingEvent>> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AccountingEvent.EventStatus status) {
        // Implementation would query by date range and status
        return ResponseEntity.ok(accountingEventService.getEventsByDate(startDate));
    }
    
    /**
     * Mark a single event as posted to GL
     * Called by accounting system after successful GL posting
     */
    @PostMapping("/events/{eventId}/mark-posted")
    public ResponseEntity<?> markEventAsPosted(
            @PathVariable Long eventId,
            @RequestBody PostingConfirmationDto confirmation) {
        try {
            accountingEventService.markEventAsPosted(
                eventId, 
                confirmation.getGlBatchId(), 
                confirmation.getPostedBy()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Event marked as posted",
                "eventId", eventId,
                "glBatchId", confirmation.getGlBatchId()
            ));
        } catch (Exception e) {
            log.error("Error marking event as posted: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to mark event as posted: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Mark multiple events as posted (bulk operation)
     * Useful when accounting system posts events in batches
     */
    @PostMapping("/events/mark-posted-batch")
    public ResponseEntity<?> markEventsAsPosted(
            @RequestBody BulkPostingConfirmationDto confirmation) {
        try {
            accountingEventService.markEventsAsPosted(
                confirmation.getEventIds(),
                confirmation.getGlBatchId(),
                confirmation.getPostedBy()
            );
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Events marked as posted",
                "eventCount", confirmation.getEventIds().size(),
                "glBatchId", confirmation.getGlBatchId()
            ));
        } catch (Exception e) {
            log.error("Error marking events as posted: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to mark events as posted: " + e.getMessage()
                ));
        }
    }
    
    /**
     * Get accounting summary for a date
     * Shows total debits, credits, and posting status
     */
    @GetMapping("/summary/{date}")
    public ResponseEntity<Map<String, Object>> getAccountingSummary(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> summary = accountingEventService.getAccountingSummary(date);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Health check endpoint for accounting integration
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Accounting Integration",
            "timestamp", java.time.LocalDateTime.now()
        ));
    }
    
    // DTOs
    
    @Data
    public static class PostingConfirmationDto {
        private String glBatchId;
        private String postedBy;
    }
    
    @Data
    public static class BulkPostingConfirmationDto {
        private List<Long> eventIds;
        private String glBatchId;
        private String postedBy;
    }
}
