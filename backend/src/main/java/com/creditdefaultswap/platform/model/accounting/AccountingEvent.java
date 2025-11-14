package com.creditdefaultswap.platform.model.accounting;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Accounting event generated from EOD valuations
 * Represents a journal entry to be posted to the General Ledger
 */
@Entity
@Table(name = "accounting_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    
    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;
    
    @Column(name = "trade_id")
    private Long tradeId;
    
    @Column(name = "reference_entity_name")
    private String referenceEntityName;
    
    // Journal Entry Details
    @Column(name = "account_code", nullable = false)
    private String accountCode;
    
    @Column(name = "account_name", nullable = false)
    private String accountName;
    
    @Column(name = "debit_amount", precision = 20, scale = 4)
    private BigDecimal debitAmount;
    
    @Column(name = "credit_amount", precision = 20, scale = 4)
    private BigDecimal creditAmount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    // Valuation context
    @Column(name = "current_npv", precision = 20, scale = 4)
    private BigDecimal currentNpv;
    
    @Column(name = "previous_npv", precision = 20, scale = 4)
    private BigDecimal previousNpv;
    
    @Column(name = "npv_change", precision = 20, scale = 4)
    private BigDecimal npvChange;
    
    @Column(name = "accrued_change", precision = 20, scale = 4)
    private BigDecimal accruedChange;
    
    // Status tracking
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;
    
    @Column(name = "posted_to_gl")
    @Builder.Default
    private Boolean postedToGl = false;
    
    @Column(name = "posted_at")
    private LocalDateTime postedAt;
    
    @Column(name = "gl_batch_id")
    private String glBatchId;
    
    // Source tracking
    @Column(name = "valuation_job_id")
    private String valuationJobId;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    // Audit
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "posted_by")
    private String postedBy;
    
    public enum EventType {
        MTM_VALUATION,          // Mark-to-market revaluation
        MTM_PNL_UNREALIZED,     // Unrealized P&L recognition
        ACCRUED_INTEREST,       // Accrued interest recognition
        NEW_TRADE_BOOKING,      // Initial trade booking
        TRADE_MATURITY,         // Trade maturity/termination
        CREDIT_EVENT,           // Credit event settlement
        POSITION_CLOSE          // Position closure
    }
    
    public enum EventStatus {
        PENDING,                // Generated, awaiting GL posting
        POSTED,                 // Successfully posted to GL
        FAILED,                 // Failed to post
        CANCELLED              // Event cancelled/reversed
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
