package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.model.eod.ValuationException.*;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.eod.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for reconciling valuations and detecting exceptions
 * 
 * Checks:
 * - Large NPV changes (> threshold)
 * - Large P&L moves
 * - Missing valuations
 * - Negative accrued interest
 * - Other anomalies
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValuationReconciliationService {
    
    private final TradeValuationRepository valuationRepo;
    private final DailyPnlResultRepository pnlRepo;
    private final ValuationExceptionRepository exceptionRepo;
    private final ValuationToleranceRuleRepository toleranceRuleRepo;
    private final DailyReconciliationSummaryRepository summaryRepo;
    private final CDSTradeRepository tradeRepo;
    
    // Default thresholds if no rules configured
    private static final BigDecimal DEFAULT_NPV_CHANGE_THRESHOLD = new BigDecimal("100000"); // $100k
    private static final BigDecimal DEFAULT_NPV_CHANGE_PCT = new BigDecimal("50"); // 50%
    private static final BigDecimal DEFAULT_PNL_THRESHOLD = new BigDecimal("50000"); // $50k
    
    /**
     * Main reconciliation method - detect exceptions and create summary
     */
    @Transactional
    public DailyReconciliationSummary reconcileValuations(LocalDate date, String jobId) {
        log.info("Starting reconciliation for {}", date);
        
        // Get all valuations for the date
        List<TradeValuation> valuations = valuationRepo.findByValuationDate(date);
        
        if (valuations.isEmpty()) {
            log.warn("No valuations found for {}", date);
            return null;
        }
        
        // Get active tolerance rules
        List<ValuationToleranceRule> rules = toleranceRuleRepo.findByIsActiveTrue();
        
        List<ValuationException> exceptions = new ArrayList<>();
        
        // Check each valuation against rules
        for (TradeValuation valuation : valuations) {
            exceptions.addAll(checkValuation(valuation, date, rules));
        }
        
        // Check for missing valuations
        exceptions.addAll(checkMissingValuations(date));
        
        // Save all exceptions
        if (!exceptions.isEmpty()) {
            exceptionRepo.saveAll(exceptions);
            log.info("Created {} exceptions", exceptions.size());
        }
        
        // Create reconciliation summary
        DailyReconciliationSummary summary = createSummary(date, jobId, valuations.size(), exceptions);
        
        log.info("Reconciliation complete for {}: {} valuations, {} exceptions", 
            date, valuations.size(), exceptions.size());
        
        return summary;
    }
    
    /**
     * Check a single valuation for exceptions
     */
    private List<ValuationException> checkValuation(
        TradeValuation valuation,
        LocalDate date,
        List<ValuationToleranceRule> rules
    ) {
        List<ValuationException> exceptions = new ArrayList<>();
        
        // Check NPV change against previous day
        checkNpvChange(valuation, date, rules).ifPresent(exceptions::add);
        
        // Check P&L threshold
        checkPnlThreshold(valuation, date, rules).ifPresent(exceptions::add);
        
        // Check for negative accrued
        checkNegativeAccrued(valuation, date).ifPresent(exceptions::add);
        
        return exceptions;
    }
    
    /**
     * Check for large NPV changes
     */
    private Optional<ValuationException> checkNpvChange(
        TradeValuation current,
        LocalDate date,
        List<ValuationToleranceRule> rules
    ) {
        // Get previous valuation (T-1)
        LocalDate previousDate = date.minusDays(1);
        List<TradeValuation> previousVals = valuationRepo
            .findByTradeIdOrderByValuationDateDesc(current.getTradeId());
        
        if (previousVals.size() < 2) {
            return Optional.empty(); // No previous valuation to compare
        }
        
        TradeValuation previous = previousVals.get(1); // Second most recent
        
        BigDecimal npvChange = current.getNpv().subtract(previous.getNpv());
        BigDecimal percentageChange = BigDecimal.ZERO;
        
        if (previous.getNpv().abs().compareTo(BigDecimal.ZERO) > 0) {
            percentageChange = npvChange
                .divide(previous.getNpv().abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        
        // Find applicable rule
        ValuationToleranceRule applicableRule = rules.stream()
            .filter(r -> "NPV_CHANGE".equals(r.getRuleType()))
            .filter(r -> isRuleApplicable(r, current.getTrade()))
            .findFirst()
            .orElse(null);
        
        // Check thresholds
        BigDecimal absThreshold = applicableRule != null && applicableRule.getAbsoluteThreshold() != null
            ? applicableRule.getAbsoluteThreshold()
            : DEFAULT_NPV_CHANGE_THRESHOLD;
        
        BigDecimal pctThreshold = applicableRule != null && applicableRule.getPercentageThreshold() != null
            ? applicableRule.getPercentageThreshold()
            : DEFAULT_NPV_CHANGE_PCT;
        
        boolean breached = npvChange.abs().compareTo(absThreshold) > 0 ||
                          percentageChange.abs().compareTo(pctThreshold) > 0;
        
        if (breached) {
            ExceptionSeverity severity = applicableRule != null 
                ? applicableRule.getSeverity() 
                : ExceptionSeverity.WARNING;
            
            return Optional.of(ValuationException.builder()
                .exceptionDate(date)
                .tradeId(current.getTradeId())
                .exceptionType(ExceptionType.LARGE_NPV_CHANGE)
                .currentValue(current.getNpv())
                .previousValue(previous.getNpv())
                .valueChange(npvChange)
                .percentageChange(percentageChange)
                .thresholdValue(absThreshold)
                .rule(applicableRule)
                .severity(severity)
                .status(ExceptionStatus.OPEN)
                .valuationResultId(current.getId())
                .build());
        }
        
        return Optional.empty();
    }
    
    /**
     * Check for large P&L moves
     */
    private Optional<ValuationException> checkPnlThreshold(
        TradeValuation valuation,
        LocalDate date,
        List<ValuationToleranceRule> rules
    ) {
        // Get P&L result for this trade/date
        Optional<DailyPnlResult> pnlOpt = pnlRepo.findByPnlDateAndTradeId(date, valuation.getTradeId());
        
        if (pnlOpt.isEmpty()) {
            return Optional.empty();
        }
        
        DailyPnlResult pnl = pnlOpt.get();
        BigDecimal totalPnl = pnl.getTotalPnl();
        
        // Find applicable rule
        ValuationToleranceRule applicableRule = rules.stream()
            .filter(r -> "PNL_THRESHOLD".equals(r.getRuleType()))
            .filter(r -> isRuleApplicable(r, valuation.getTrade()))
            .findFirst()
            .orElse(null);
        
        BigDecimal threshold = applicableRule != null && applicableRule.getAbsoluteThreshold() != null
            ? applicableRule.getAbsoluteThreshold()
            : DEFAULT_PNL_THRESHOLD;
        
        if (totalPnl.abs().compareTo(threshold) > 0) {
            ExceptionSeverity severity = applicableRule != null 
                ? applicableRule.getSeverity() 
                : ExceptionSeverity.WARNING;
            
            return Optional.of(ValuationException.builder()
                .exceptionDate(date)
                .tradeId(valuation.getTradeId())
                .exceptionType(ExceptionType.LARGE_PNL)
                .currentValue(totalPnl)
                .thresholdValue(threshold)
                .percentageChange(pnl.getPnlPercentage())
                .rule(applicableRule)
                .severity(severity)
                .status(ExceptionStatus.OPEN)
                .valuationResultId(valuation.getId())
                .build());
        }
        
        return Optional.empty();
    }
    
    /**
     * Check for negative accrued interest (anomaly)
     * Note: TradeValuation doesn't store accrued separately, check P&L accrued instead
     */
    private Optional<ValuationException> checkNegativeAccrued(
        TradeValuation valuation,
        LocalDate date
    ) {
        // Get P&L result which contains accrued
        Optional<DailyPnlResult> pnlOpt = pnlRepo.findByPnlDateAndTradeId(date, valuation.getTradeId());
        
        if (pnlOpt.isPresent()) {
            DailyPnlResult pnl = pnlOpt.get();
            if (pnl.getCurrentAccrued() != null && 
                pnl.getCurrentAccrued().compareTo(BigDecimal.ZERO) < 0) {
                
                return Optional.of(ValuationException.builder()
                    .exceptionDate(date)
                    .tradeId(valuation.getTradeId())
                    .exceptionType(ExceptionType.NEGATIVE_ACCRUED)
                    .currentValue(pnl.getCurrentAccrued())
                    .severity(ExceptionSeverity.ERROR)
                    .status(ExceptionStatus.OPEN)
                    .valuationResultId(valuation.getId())
                    .build());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Check for missing valuations (trades that should have valuations but don't)
     */
    private List<ValuationException> checkMissingValuations(LocalDate date) {
        List<ValuationException> exceptions = new ArrayList<>();
        
        // Get all active trades
        // Simplified: just get all trades (would filter by status in production)
        List<CDSTrade> activeTrades = tradeRepo.findAll();
        
        // Get trades with valuations
        List<TradeValuation> valuations = valuationRepo.findByValuationDate(date);
        Set<Long> valuatedTradeIds = valuations.stream()
            .map(v -> v.getTradeId())
            .collect(Collectors.toSet());
        
        // Find missing
        for (CDSTrade trade : activeTrades) {
            if (!valuatedTradeIds.contains(trade.getId())) {
                exceptions.add(ValuationException.builder()
                    .exceptionDate(date)
                    .tradeId(trade.getId())
                    .exceptionType(ExceptionType.MISSING_VALUATION)
                    .severity(ExceptionSeverity.ERROR)
                    .status(ExceptionStatus.OPEN)
                    .build());
            }
        }
        
        if (!exceptions.isEmpty()) {
            log.warn("Found {} missing valuations for {}", exceptions.size(), date);
        }
        
        return exceptions;
    }
    
    /**
     * Check if a rule applies to a trade
     */
    private boolean isRuleApplicable(ValuationToleranceRule rule, CDSTrade trade) {
        if ("ALL".equals(rule.getAppliesTo())) {
            return true;
        }
        
        if ("PORTFOLIO".equals(rule.getAppliesTo()) && rule.getPortfolio() != null) {
            // Would need portfolio relationship in CDSTrade to check this
            return true; // Simplified for now
        }
        
        if ("TRADE_TYPE".equals(rule.getAppliesTo()) && rule.getTradeType() != null) {
            // Would check trade.getTradeType() == rule.getTradeType()
            return true; // Simplified for now
        }
        
        return false;
    }
    
    /**
     * Create reconciliation summary
     */
    private DailyReconciliationSummary createSummary(
        LocalDate date,
        String jobId,
        int totalValuations,
        List<ValuationException> exceptions
    ) {
        DailyReconciliationSummary summary = DailyReconciliationSummary.builder()
            .reconciliationDate(date)
            .jobId(jobId)
            .totalValuations(totalValuations)
            .totalExceptions(exceptions.size())
            .build();
        
        // Count by severity
        summary.setInfoCount((int) exceptions.stream()
            .filter(e -> e.getSeverity() == ExceptionSeverity.INFO).count());
        summary.setWarningCount((int) exceptions.stream()
            .filter(e -> e.getSeverity() == ExceptionSeverity.WARNING).count());
        summary.setErrorCount((int) exceptions.stream()
            .filter(e -> e.getSeverity() == ExceptionSeverity.ERROR).count());
        summary.setCriticalCount((int) exceptions.stream()
            .filter(e -> e.getSeverity() == ExceptionSeverity.CRITICAL).count());
        
        // Count by type
        summary.setLargeNpvChangeCount((int) exceptions.stream()
            .filter(e -> e.getExceptionType() == ExceptionType.LARGE_NPV_CHANGE).count());
        summary.setLargePnlCount((int) exceptions.stream()
            .filter(e -> e.getExceptionType() == ExceptionType.LARGE_PNL).count());
        summary.setMissingValuationCount((int) exceptions.stream()
            .filter(e -> e.getExceptionType() == ExceptionType.MISSING_VALUATION).count());
        summary.setNegativeAccruedCount((int) exceptions.stream()
            .filter(e -> e.getExceptionType() == ExceptionType.NEGATIVE_ACCRUED).count());
        
        // Count by status
        summary.setOpenExceptions((int) exceptions.stream()
            .filter(e -> e.getStatus() == ExceptionStatus.OPEN).count());
        summary.setUnderReviewExceptions(0);
        summary.setResolvedExceptions(0);
        
        // Set overall status
        if (summary.getCriticalCount() > 0) {
            summary.setReconciliationStatus("ISSUES");
        } else if (summary.getErrorCount() > 0) {
            summary.setReconciliationStatus("PENDING_REVIEW");
        } else {
            summary.setReconciliationStatus("IN_PROGRESS");
        }
        
        return summaryRepo.save(summary);
    }
    
    /**
     * Get exceptions for a date
     */
    public List<ValuationException> getExceptions(LocalDate date) {
        return exceptionRepo.findByExceptionDate(date);
    }
    
    /**
     * Get exceptions by status
     */
    public List<ValuationException> getExceptionsByStatus(ExceptionStatus status) {
        return exceptionRepo.findByStatusOrderByExceptionDateDescSeverityDesc(status);
    }
    
    /**
     * Get reconciliation summary
     */
    public Optional<DailyReconciliationSummary> getReconciliationSummary(LocalDate date) {
        return summaryRepo.findByReconciliationDate(date);
    }
    
    /**
     * Review an exception
     */
    @Transactional
    public void reviewException(Long exceptionId, String reviewedBy, ExceptionStatus newStatus, String notes) {
        ValuationException exception = exceptionRepo.findById(exceptionId)
            .orElseThrow(() -> new RuntimeException("Exception not found: " + exceptionId));
        
        exception.setStatus(newStatus);
        exception.setReviewedBy(reviewedBy);
        exception.setReviewedAt(java.time.OffsetDateTime.now());
        exception.setResolutionNotes(notes);
        
        exceptionRepo.save(exception);
        
        log.info("Exception {} reviewed by {}: status={}", exceptionId, reviewedBy, newStatus);
    }
    
    /**
     * Approve reconciliation for a date
     */
    @Transactional
    public void approveReconciliation(LocalDate date, String approvedBy) {
        DailyReconciliationSummary summary = summaryRepo.findByReconciliationDate(date)
            .orElseThrow(() -> new RuntimeException("Reconciliation summary not found for " + date));
        
        summary.setReconciliationStatus("APPROVED");
        summary.setApprovedBy(approvedBy);
        summary.setApprovedAt(java.time.OffsetDateTime.now());
        
        summaryRepo.save(summary);
        
        log.info("Reconciliation for {} approved by {}", date, approvedBy);
    }
}
