# Story 16.8: Valuation Reconciliation & Exceptions

## Story
**As a** operations manager  
**I want** automated reconciliation of valuation results  
**So that** I can identify and resolve discrepancies quickly

## Acceptance Criteria
- [ ] Compare current valuations against expected ranges
- [ ] Detect anomalies and outliers
- [ ] Flag large daily P&L moves (> threshold)
- [ ] Compare valuations with external sources (if available)
- [ ] Generate exception reports
- [ ] Support manual review and sign-off workflow
- [ ] Track exception resolution
- [ ] Provide revaluation capability
- [ ] Maintain audit trail of all changes
- [ ] Alert on critical exceptions

## Technical Details

### Database Schema
```sql
-- Valuation tolerance rules
CREATE TABLE valuation_tolerance_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- NPV_CHANGE, SPREAD_CHANGE, PNL_THRESHOLD
    
    -- Tolerance thresholds
    absolute_threshold DECIMAL(20, 4), -- Absolute value threshold
    percentage_threshold DECIMAL(5, 2), -- Percentage change threshold
    
    -- Applicability
    applies_to VARCHAR(50), -- ALL, PORTFOLIO, TRADE_TYPE
    portfolio_id BIGINT REFERENCES cds_portfolios(id),
    trade_type VARCHAR(50),
    
    severity VARCHAR(20) NOT NULL, -- INFO, WARNING, ERROR, CRITICAL
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Valuation exceptions detected
CREATE TABLE valuation_exceptions (
    id BIGSERIAL PRIMARY KEY,
    exception_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id) ON DELETE CASCADE,
    
    exception_type VARCHAR(50) NOT NULL,
    -- Types: LARGE_NPV_CHANGE, LARGE_PNL, MISSING_VALUATION, 
    --        STALE_MARKET_DATA, NEGATIVE_ACCRUED, CALCULATION_ERROR
    
    -- Exception details
    current_value DECIMAL(20, 4),
    previous_value DECIMAL(20, 4),
    value_change DECIMAL(20, 4),
    percentage_change DECIMAL(5, 2),
    
    threshold_value DECIMAL(20, 4),
    rule_id BIGINT REFERENCES valuation_tolerance_rules(id),
    
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    -- Status: OPEN, UNDER_REVIEW, APPROVED, REJECTED, REVALUED
    
    -- Resolution tracking
    assigned_to VARCHAR(100),
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    
    -- Links
    valuation_result_id BIGINT REFERENCES eod_valuation_results(id),
    revaluation_result_id BIGINT REFERENCES eod_valuation_results(id),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- External valuation comparison (if available)
CREATE TABLE external_valuations (
    id BIGSERIAL PRIMARY KEY,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id),
    
    external_source VARCHAR(50) NOT NULL, -- e.g., BLOOMBERG, MARKIT, COUNTERPARTY
    external_npv DECIMAL(20, 4),
    external_spread DECIMAL(10, 6),
    
    our_npv DECIMAL(20, 4),
    our_spread DECIMAL(10, 6),
    
    npv_difference DECIMAL(20, 4),
    spread_difference DECIMAL(10, 6),
    
    within_tolerance BOOLEAN,
    tolerance_threshold DECIMAL(20, 4),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(valuation_date, trade_id, external_source)
);

-- Revaluation requests and results
CREATE TABLE revaluation_requests (
    id BIGSERIAL PRIMARY KEY,
    request_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    valuation_date DATE NOT NULL,
    trade_id BIGINT NOT NULL REFERENCES cds_trades(id),
    
    request_reason VARCHAR(50) NOT NULL, -- EXCEPTION, USER_REQUEST, CORRECTION
    requested_by VARCHAR(100) NOT NULL,
    
    original_valuation_id BIGINT REFERENCES eod_valuation_results(id),
    new_valuation_id BIGINT REFERENCES eod_valuation_results(id),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Status: PENDING, IN_PROGRESS, COMPLETED, FAILED
    
    original_npv DECIMAL(20, 4),
    new_npv DECIMAL(20, 4),
    npv_difference DECIMAL(20, 4),
    
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    notes TEXT,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- Exception resolution workflow
CREATE TABLE exception_workflow_steps (
    id BIGSERIAL PRIMARY KEY,
    exception_id BIGINT NOT NULL REFERENCES valuation_exceptions(id) ON DELETE CASCADE,
    
    step_number INTEGER NOT NULL,
    step_action VARCHAR(50) NOT NULL, 
    -- Actions: ASSIGNED, INVESTIGATED, MARKET_DATA_CHECKED, 
    --          REVALUED, APPROVED, REJECTED
    
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    notes TEXT,
    attachments JSONB -- Store file references, screenshots, etc.
);

-- Daily reconciliation summary
CREATE TABLE daily_reconciliation_summary (
    id BIGSERIAL PRIMARY KEY,
    reconciliation_date DATE NOT NULL UNIQUE,
    
    total_valuations INTEGER NOT NULL,
    total_exceptions INTEGER NOT NULL,
    
    -- Exception breakdown by severity
    info_count INTEGER DEFAULT 0,
    warning_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    critical_count INTEGER DEFAULT 0,
    
    -- Exception breakdown by type
    large_npv_change_count INTEGER DEFAULT 0,
    large_pnl_count INTEGER DEFAULT 0,
    missing_valuation_count INTEGER DEFAULT 0,
    calculation_error_count INTEGER DEFAULT 0,
    
    -- Status breakdown
    open_exceptions INTEGER DEFAULT 0,
    under_review_exceptions INTEGER DEFAULT 0,
    resolved_exceptions INTEGER DEFAULT 0,
    
    reconciliation_status VARCHAR(20) NOT NULL,
    -- Status: IN_PROGRESS, PENDING_REVIEW, APPROVED, ISSUES
    
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    job_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_exceptions_date ON valuation_exceptions(exception_date);
CREATE INDEX idx_exceptions_trade ON valuation_exceptions(trade_id);
CREATE INDEX idx_exceptions_status ON valuation_exceptions(status);
CREATE INDEX idx_exceptions_severity ON valuation_exceptions(severity);
CREATE INDEX idx_exceptions_open ON valuation_exceptions(status, severity) 
    WHERE status = 'OPEN';

CREATE INDEX idx_external_valuations_date ON external_valuations(valuation_date);
CREATE INDEX idx_revaluation_requests_status ON revaluation_requests(status);
CREATE INDEX idx_workflow_steps_exception ON exception_workflow_steps(exception_id);
```

### Java Models
```java
@Entity
@Table(name = "valuation_exceptions")
public class ValuationException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false)
    private CDSTrade trade;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false)
    private ExceptionType exceptionType;
    
    @Column(name = "current_value", precision = 20, scale = 4)
    private BigDecimal currentValue;
    
    @Column(name = "previous_value", precision = 20, scale = 4)
    private BigDecimal previousValue;
    
    @Column(name = "value_change", precision = 20, scale = 4)
    private BigDecimal valueChange;
    
    @Column(name = "percentage_change", precision = 5, scale = 2)
    private BigDecimal percentageChange;
    
    @Column(name = "threshold_value", precision = 20, scale = 4)
    private BigDecimal thresholdValue;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private ValuationToleranceRule rule;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private ExceptionSeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExceptionStatus status = ExceptionStatus.OPEN;
    
    @Column(name = "assigned_to")
    private String assignedTo;
    
    @Column(name = "reviewed_by")
    private String reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
}

public enum ExceptionType {
    LARGE_NPV_CHANGE,
    LARGE_PNL,
    MISSING_VALUATION,
    STALE_MARKET_DATA,
    NEGATIVE_ACCRUED,
    CALCULATION_ERROR,
    EXTERNAL_VALUATION_MISMATCH
}

public enum ExceptionStatus {
    OPEN,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    REVALUED
}

public enum ExceptionSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
```

### Service Implementation
```java
@Service
public class ValuationReconciliationService {
    
    @Autowired
    private EodValuationResultRepository valuationRepo;
    
    @Autowired
    private ValuationExceptionRepository exceptionRepo;
    
    @Autowired
    private ValuationToleranceRuleRepository toleranceRuleRepo;
    
    @Transactional
    public void reconcileValuations(LocalDate date) {
        List<EodValuationResult> valuations = valuationRepo
            .findByValuationDate(date);
        
        List<ValuationToleranceRule> rules = toleranceRuleRepo
            .findByIsActiveTrue();
        
        List<ValuationException> exceptions = new ArrayList<>();
        
        for (EodValuationResult valuation : valuations) {
            // Check each rule
            for (ValuationToleranceRule rule : rules) {
                ValuationException exception = checkRule(
                    valuation, rule, date
                );
                if (exception != null) {
                    exceptions.add(exception);
                }
            }
            
            // Additional checks
            checkForAnomalies(valuation, date).ifPresent(exceptions::add);
        }
        
        // Save all exceptions
        exceptionRepo.saveAll(exceptions);
        
        // Create summary
        createReconciliationSummary(date, valuations.size(), exceptions);
        
        // Send alerts for critical exceptions
        sendCriticalAlerts(exceptions);
        
        log.info("Reconciliation complete for {}: {} exceptions found", 
                date, exceptions.size());
    }
    
    private ValuationException checkRule(
        EodValuationResult valuation,
        ValuationToleranceRule rule,
        LocalDate date
    ) {
        if (rule.getRuleType().equals("NPV_CHANGE")) {
            return checkNpvChange(valuation, rule, date);
        } else if (rule.getRuleType().equals("PNL_THRESHOLD")) {
            return checkPnlThreshold(valuation, rule, date);
        }
        return null;
    }
    
    private ValuationException checkNpvChange(
        EodValuationResult current,
        ValuationToleranceRule rule,
        LocalDate date
    ) {
        // Get previous day valuation
        LocalDate previousDate = getPreviousBusinessDay(date);
        EodValuationResult previous = valuationRepo
            .findByValuationDateAndTradeId(previousDate, current.getTradeId());
        
        if (previous == null) {
            return null; // New trade, no comparison
        }
        
        BigDecimal npvChange = current.getNpv().subtract(previous.getNpv());
        BigDecimal percentageChange = npvChange
            .divide(previous.getNpv().abs(), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        
        // Check thresholds
        boolean breached = false;
        if (rule.getAbsoluteThreshold() != null && 
            npvChange.abs().compareTo(rule.getAbsoluteThreshold()) > 0) {
            breached = true;
        }
        if (rule.getPercentageThreshold() != null &&
            percentageChange.abs().compareTo(rule.getPercentageThreshold()) > 0) {
            breached = true;
        }
        
        if (breached) {
            ValuationException exception = new ValuationException();
            exception.setExceptionDate(date);
            exception.setTrade(current.getTrade());
            exception.setExceptionType(ExceptionType.LARGE_NPV_CHANGE);
            exception.setCurrentValue(current.getNpv());
            exception.setPreviousValue(previous.getNpv());
            exception.setValueChange(npvChange);
            exception.setPercentageChange(percentageChange);
            exception.setRule(rule);
            exception.setSeverity(rule.getSeverity());
            exception.setValuationResultId(current.getId());
            
            return exception;
        }
        
        return null;
    }
    
    private Optional<ValuationException> checkForAnomalies(
        EodValuationResult valuation,
        LocalDate date
    ) {
        // Check for negative accrued interest (shouldn't happen)
        if (valuation.getAccruedInterest().compareTo(BigDecimal.ZERO) < 0) {
            ValuationException exception = new ValuationException();
            exception.setExceptionDate(date);
            exception.setTrade(valuation.getTrade());
            exception.setExceptionType(ExceptionType.NEGATIVE_ACCRUED);
            exception.setCurrentValue(valuation.getAccruedInterest());
            exception.setSeverity(ExceptionSeverity.ERROR);
            return Optional.of(exception);
        }
        
        return Optional.empty();
    }
    
    @Transactional
    public void requestRevaluation(
        Long tradeId,
        LocalDate valuationDate,
        String reason,
        String requestedBy
    ) {
        RevaluationRequest request = new RevaluationRequest();
        request.setTradeId(tradeId);
        request.setValuationDate(valuationDate);
        request.setRequestReason(reason);
        request.setRequestedBy(requestedBy);
        request.setStatus(RevaluationStatus.PENDING);
        
        // Get original valuation
        EodValuationResult original = valuationRepo
            .findByValuationDateAndTradeId(valuationDate, tradeId);
        request.setOriginalValuationId(original.getId());
        request.setOriginalNpv(original.getNpv());
        
        revaluationRequestRepo.save(request);
        
        // Trigger revaluation job
        triggerRevaluationJob(request);
    }
    
    @Transactional
    public void reviewException(
        Long exceptionId,
        String reviewedBy,
        ExceptionStatus newStatus,
        String notes
    ) {
        ValuationException exception = exceptionRepo.findById(exceptionId)
            .orElseThrow(() -> new EntityNotFoundException("Exception not found"));
        
        exception.setStatus(newStatus);
        exception.setReviewedBy(reviewedBy);
        exception.setReviewedAt(LocalDateTime.now());
        exception.setResolutionNotes(notes);
        
        exceptionRepo.save(exception);
        
        // Add workflow step
        ExceptionWorkflowStep step = new ExceptionWorkflowStep();
        step.setExceptionId(exceptionId);
        step.setStepAction("REVIEWED");
        step.setPerformedBy(reviewedBy);
        step.setNotes(notes);
        workflowStepRepo.save(step);
    }
    
    public void compareWithExternalSource(
        LocalDate date,
        String externalSource
    ) {
        // Load external valuations from file or API
        List<ExternalValuationDto> externalVals = 
            loadExternalValuations(date, externalSource);
        
        for (ExternalValuationDto extVal : externalVals) {
            EodValuationResult ourVal = valuationRepo
                .findByValuationDateAndTradeId(date, extVal.getTradeId());
            
            if (ourVal != null) {
                BigDecimal npvDiff = ourVal.getNpv()
                    .subtract(extVal.getExternalNpv());
                
                ExternalValuation comparison = new ExternalValuation();
                comparison.setValuationDate(date);
                comparison.setTradeId(extVal.getTradeId());
                comparison.setExternalSource(externalSource);
                comparison.setExternalNpv(extVal.getExternalNpv());
                comparison.setOurNpv(ourVal.getNpv());
                comparison.setNpvDifference(npvDiff);
                
                // Check if within tolerance (e.g., 1% or $10k)
                BigDecimal tolerance = new BigDecimal("10000");
                comparison.setWithinTolerance(
                    npvDiff.abs().compareTo(tolerance) <= 0
                );
                
                externalValuationRepo.save(comparison);
                
                // Create exception if outside tolerance
                if (!comparison.getWithinTolerance()) {
                    createExternalMismatchException(comparison);
                }
            }
        }
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {
    
    @GetMapping("/exceptions/{date}")
    public ResponseEntity<List<ValuationException>> getExceptions(
        @PathVariable LocalDate date,
        @RequestParam(required = false) ExceptionStatus status,
        @RequestParam(required = false) ExceptionSeverity severity
    );
    
    @GetMapping("/exceptions/{exceptionId}")
    public ResponseEntity<ValuationException> getException(
        @PathVariable Long exceptionId
    );
    
    @PutMapping("/exceptions/{exceptionId}/review")
    public ResponseEntity<Void> reviewException(
        @PathVariable Long exceptionId,
        @RequestBody ExceptionReviewDto review
    );
    
    @PostMapping("/revaluation")
    public ResponseEntity<RevaluationRequest> requestRevaluation(
        @RequestBody RevaluationRequestDto request
    );
    
    @GetMapping("/summary/{date}")
    public ResponseEntity<DailyReconciliationSummary> getReconciliationSummary(
        @PathVariable LocalDate date
    );
    
    @GetMapping("/external-comparison/{date}")
    public ResponseEntity<List<ExternalValuation>> getExternalComparison(
        @PathVariable LocalDate date,
        @RequestParam String source
    );
    
    @PostMapping("/approve/{date}")
    public ResponseEntity<Void> approveReconciliation(
        @PathVariable LocalDate date,
        @RequestBody ApprovalDto approval
    );
}
```

## Test Scenarios
1. **NPV Change Detection**: Flag trade with 50% NPV change
2. **Large P&L Exception**: Detect $1M+ daily P&L move
3. **Missing Valuation**: Flag trade with no current valuation
4. **Negative Accrued**: Detect negative accrued interest anomaly
5. **External Comparison**: Compare with Bloomberg valuations
6. **Revaluation Request**: Submit and process revaluation
7. **Exception Review**: Review and approve exception
8. **Reconciliation Approval**: Approve daily reconciliation

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests for exception detection
- [ ] Tolerance rules configurable via API
- [ ] Exception workflow tested end-to-end
- [ ] Alert notifications working
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.5: Valuation Results Storage
- Story 16.6: Daily P&L Calculation

## Effort Estimate
**8 story points** (1.5 weeks)

## Notes
- Reconciliation runs after valuation and P&L calculation
- Configure tolerance rules per portfolio or trade type
- Implement email/Slack alerts for critical exceptions
- Support bulk exception approval for common issues
- Track exception resolution time (SLA monitoring)
- Consider machine learning for anomaly detection
- Maintain detailed audit trail for compliance
- Support uploading external valuations (CSV/Excel)
- Implement approval workflow for material adjustments
