package com.creditdefaultswap.platform.service.eod;

import com.creditdefaultswap.platform.model.eod.*;
import com.creditdefaultswap.platform.repository.eod.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for orchestrating EOD valuation batch jobs
 * 
 * The EOD valuation process consists of 8 steps:
 * 1. Capture Market Data Snapshot
 * 2. Load Active Trades
 * 3. Calculate NPV (via ORE)
 * 4. Calculate Accrued Interest
 * 5. Calculate P&L
 * 6. Aggregate Risk Metrics
 * 7. Reconcile and Validate
 * 8. Generate Accounting Events (optional)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EodValuationJobService {
    
    private final EodValuationJobRepository jobRepository;
    private final EodValuationJobStepRepository stepRepository;
    private final EodValuationConfigRepository configRepository;
    private final MarketDataSnapshotService marketDataSnapshotService;
    private final AccruedInterestService accruedInterestService;
    private final OreValuationService oreValuationService;
    private final ValuationStorageService valuationStorageService;
    private final DailyPnlService dailyPnlService;
    private final RiskAggregationService riskAggregationService;
    private final ValuationReconciliationService valuationReconciliationService;
    private final com.creditdefaultswap.platform.service.accounting.AccountingEventService accountingEventService;
    private final com.creditdefaultswap.platform.repository.CDSTradeRepository tradeRepository;
    private final EntityManager entityManager;
    
    // Step names
    private static final String STEP_CAPTURE_MARKET_DATA = "CAPTURE_MARKET_DATA";
    private static final String STEP_LOAD_ACTIVE_TRADES = "LOAD_ACTIVE_TRADES";
    private static final String STEP_CALCULATE_NPV = "CALCULATE_NPV";
    private static final String STEP_CALCULATE_ACCRUED = "CALCULATE_ACCRUED";
    private static final String STEP_CALCULATE_PNL = "CALCULATE_PNL";
    private static final String STEP_AGGREGATE_RISK = "AGGREGATE_RISK";
    private static final String STEP_RECONCILE = "RECONCILE";
    private static final String STEP_GENERATE_ACCOUNTING_EVENTS = "GENERATE_ACCOUNTING_EVENTS";
    
    /**
     * Scheduled EOD job - runs at 6pm on business days
     */
    @Scheduled(cron = "${eod.schedule.cron:0 0 18 * * MON-FRI}", zone = "${eod.schedule.timezone:America/New_York}")
    public void scheduledEodJob() {
        LocalDate valuationDate = LocalDate.now();
        
        log.info("Starting scheduled EOD valuation job for date: {}", valuationDate);
        
        try {
            executeEodJob(valuationDate, "SYSTEM", false);
        } catch (Exception e) {
            log.error("Scheduled EOD job failed for date: {}", valuationDate, e);
        }
    }
    
    /**
     * Execute EOD valuation job for a specific date
     */
    @Transactional
    public EodValuationJob executeEodJob(LocalDate valuationDate, String triggeredBy, boolean dryRun) {
        // Check if job already exists for this date
        if (jobRepository.existsByValuationDate(valuationDate)) {
            throw new IllegalStateException("EOD job already exists for date: " + valuationDate);
        }
        
        // Create job
        String jobId = generateJobId(valuationDate);
        EodValuationJob job = EodValuationJob.builder()
            .jobId(jobId)
            .valuationDate(valuationDate)
            .status(EodValuationJob.JobStatus.PENDING)
            .scheduledTime(LocalDateTime.now())
            .triggeredBy(triggeredBy)
            .dryRun(dryRun)
            .manualTrigger(!"SYSTEM".equals(triggeredBy))
            .build();
        
        // Create steps
        job.addStep(createStep(1, STEP_CAPTURE_MARKET_DATA));
        job.addStep(createStep(2, STEP_LOAD_ACTIVE_TRADES));
        job.addStep(createStep(3, STEP_CALCULATE_NPV));
        job.addStep(createStep(4, STEP_CALCULATE_ACCRUED));
        job.addStep(createStep(5, STEP_CALCULATE_PNL));
        job.addStep(createStep(6, STEP_AGGREGATE_RISK));
        job.addStep(createStep(7, STEP_RECONCILE));
        
        job = jobRepository.save(job);
        
        log.info("Created EOD valuation job: {} for date: {}", jobId, valuationDate);
        
        // Execute job asynchronously
        executeJobSteps(job.getId());
        
        return job;
    }
    
    /**
     * Execute all job steps sequentially
     */
    @Transactional
    public void executeJobSteps(Long jobId) {
        EodValuationJob job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        
        job.startJob();
        jobRepository.save(job);
        entityManager.flush();  // Force immediate persistence to avoid "delayed insert" errors
        
        try {
            // Execute each step in sequence (use defensive copy to avoid ConcurrentModificationException)
            List<EodValuationJobStep> steps = new ArrayList<>(job.getSteps());
            for (EodValuationJobStep step : steps) {
                executeStep(job, step);
                
                // Check if step failed
                if (step.getStatus() == EodValuationJobStep.StepStatus.FAILED) {
                    // Attempt retry if configured
                    if (step.getRetryCount() < job.getMaxRetries()) {
                        log.warn("Step {} failed, attempting retry {}/{}",
                            step.getStepName(), step.getRetryCount() + 1, job.getMaxRetries());
                        
                        step.setRetryCount(step.getRetryCount() + 1);
                        stepRepository.save(step);
                        
                        // Wait before retry
                        Thread.sleep(getRetryDelaySeconds() * 1000L);
                        
                        // Retry step
                        executeStep(job, step);
                    }
                    
                    // If still failed, fail entire job
                    if (step.getStatus() == EodValuationJobStep.StepStatus.FAILED) {
                        job.failJob("Step " + step.getStepName() + " failed: " + step.getErrorMessage());
                        jobRepository.save(job);
                        return;
                    }
                }
                
                // Update job progress
                job.setCurrentStep(step.getStepNumber());
                jobRepository.save(job);
            }
            
            // All steps completed successfully - update job-level metrics
            updateJobMetrics(job);
            job.completeJob();
            jobRepository.save(job);
            
            log.info("EOD valuation job completed successfully: {}", job.getJobId());
            
        } catch (Exception e) {
            log.error("EOD valuation job failed: {}", job.getJobId(), e);
            job.failJob("Job execution error: " + e.getMessage());
            jobRepository.save(job);
        }
    }
    
    /**
     * Execute a single step
     */
    private void executeStep(EodValuationJob job, EodValuationJobStep step) {
        log.info("Executing step {}: {} for job {}", 
            step.getStepNumber(), step.getStepName(), job.getJobId());
        
        step.startStep();
        stepRepository.save(step);
        entityManager.flush();  // Force immediate persistence to avoid "delayed insert" errors
        
        try {
            switch (step.getStepName()) {
                case STEP_CAPTURE_MARKET_DATA:
                    executeCaptureMarketData(job, step);
                    break;
                case STEP_LOAD_ACTIVE_TRADES:
                    executeLoadActiveTrades(job, step);
                    break;
                case STEP_CALCULATE_NPV:
                    executeCalculateNpv(job, step);
                    break;
                case STEP_CALCULATE_ACCRUED:
                    executeCalculateAccrued(job, step);
                    break;
                case STEP_CALCULATE_PNL:
                    executeCalculatePnl(job, step);
                    break;
                case STEP_AGGREGATE_RISK:
                    executeAggregateRisk(job, step);
                    break;
                case STEP_RECONCILE:
                    executeReconcile(job, step);
                    break;
                case STEP_GENERATE_ACCOUNTING_EVENTS:
                    executeGenerateAccountingEvents(job, step);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown step: " + step.getStepName());
            }
            
            step.completeStep();
            stepRepository.save(step);
            
            log.info("Step {} completed successfully in {} seconds",
                step.getStepName(), step.getDurationSeconds());
            
        } catch (Exception e) {
            log.error("Step {} failed: {}", step.getStepName(), e.getMessage(), e);
            step.failStep(e.getMessage());
            stepRepository.save(step);
            entityManager.flush();  // Force immediate persistence
        }
    }
    
    // Step 1: Capture Market Data
    private void executeCaptureMarketData(EodValuationJob job, EodValuationJobStep step) {
        log.info("Capturing market data snapshot for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would capture market data snapshot");
            step.setRecordsProcessed(0);
            return;
        }
        
        // Check if snapshot already exists
        if (marketDataSnapshotService.getSnapshotByDate(job.getValuationDate()).isPresent()) {
            log.info("Market data snapshot already exists for date: {}", job.getValuationDate());
            step.setRecordsSuccessful(1);
            return;
        }
        
        // TODO: Implement actual market data capture
        // This would typically call external data providers (Bloomberg, Markit, etc.)
        log.warn("Market data capture not yet implemented - placeholder step");
        step.setRecordsProcessed(0);
    }
    
    // Step 2: Load Active Trades
    private void executeLoadActiveTrades(EodValuationJob job, EodValuationJobStep step) {
        log.info("Loading active trades for valuation date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would load active trades");
            step.setRecordsProcessed(0);
            return;
        }
        
        // Query active trades from database
        // Active trades are those with status ACTIVE, trade date <= valuation date, and maturity > valuation date
        List<com.creditdefaultswap.platform.model.CDSTrade> activeTrades = 
            tradeRepository.findAll().stream()
                .filter(trade -> com.creditdefaultswap.platform.model.TradeStatus.ACTIVE.equals(trade.getTradeStatus()))
                .filter(trade -> !trade.getTradeDate().isAfter(job.getValuationDate()))
                .filter(trade -> trade.getMaturityDate().isAfter(job.getValuationDate()))
                .toList();
        
        log.info("Found {} active trades for valuation date: {}", activeTrades.size(), job.getValuationDate());
        
        step.setRecordsProcessed(activeTrades.size());
        step.setRecordsSuccessful(activeTrades.size());
    }
    
    // Step 3: Calculate NPV via ORE
    private void executeCalculateNpv(EodValuationJob job, EodValuationJobStep step) {
        log.info("Calculating NPV for trades on date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would calculate NPV");
            step.setRecordsProcessed(0);
            return;
        }
        
        // Get active trade IDs
        List<Long> tradeIds = getActiveTradeIds(job);
        
        if (tradeIds.isEmpty()) {
            log.warn("No active trades found for NPV calculation");
            step.setRecordsProcessed(0);
            return;
        }
        
        log.info("Calculating NPV for {} trades using ORE batch processing", tradeIds.size());
        
        // Calculate NPV using ORE valuation service in batch mode
        try {
            List<com.creditdefaultswap.platform.model.eod.TradeValuation> valuations = 
                oreValuationService.calculateNpvBatch(tradeIds, job.getValuationDate(), job.getJobId());
            
            int successCount = (int) valuations.stream()
                .filter(v -> v.getValuationStatus() == com.creditdefaultswap.platform.model.eod.TradeValuation.ValuationStatus.SUCCESS)
                .count();
            int failCount = valuations.size() - successCount;
            
            step.setRecordsProcessed(tradeIds.size());
            step.setRecordsSuccessful(successCount);
            step.setRecordsFailed(failCount);
            
            log.info("NPV calculation completed: {} successful, {} failed", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Batch NPV calculation failed: {}", e.getMessage(), e);
            throw new RuntimeException("NPV calculation failed", e);
        }
    }
    
    // Step 4: Calculate Accrued Interest
    private void executeCalculateAccrued(EodValuationJob job, EodValuationJobStep step) {
        log.info("Calculating accrued interest for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would calculate accrued interest for all active trades");
            step.setRecordsProcessed(0);
            return;
        }
        
        // Get trade IDs from previous step
        List<Long> tradeIds = getActiveTradeIds(job);
        
        if (tradeIds.isEmpty()) {
            log.warn("No active trades found for accrued interest calculation");
            step.setRecordsProcessed(0);
            return;
        }
        
        // Calculate accrued interest in batches
        int batchSize = getBatchSize();
        int totalProcessed = 0;
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < tradeIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, tradeIds.size());
            List<Long> batch = tradeIds.subList(i, endIndex);
            
            log.debug("Processing accrued batch {}/{}: {} trades",
                (i / batchSize) + 1,
                (tradeIds.size() + batchSize - 1) / batchSize,
                batch.size());
            
            var results = accruedInterestService.calculateAccruedBatch(
                batch,
                job.getValuationDate(),
                job.getJobId()
            );
            
            for (var result : results) {
                if (result.getCalculationStatus() == com.creditdefaultswap.platform.model.eod.TradeAccruedInterest.CalculationStatus.SUCCESS) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            
            totalProcessed += batch.size();
        }
        
        step.setRecordsProcessed(totalProcessed);
        log.info("Accrued interest calculation completed: {} successful, {} failed out of {} total",
            successCount, failCount, totalProcessed);
        
        if (failCount > 0) {
            double failureRate = (double) failCount / totalProcessed * 100;
            if (failureRate > 10.0) {
                throw new RuntimeException(String.format(
                    "Excessive accrued calculation failures: %.1f%% (%d/%d)",
                    failureRate, failCount, totalProcessed));
            }
        }
    }
    
    // Step 5: Calculate P&L
    private void executeCalculatePnl(EodValuationJob job, EodValuationJobStep step) {
        log.info("Calculating P&L for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would calculate P&L for all active trades");
            step.setRecordsProcessed(0);
            return;
        }
        
        try {
            List<com.creditdefaultswap.platform.model.eod.DailyPnlResult> results = 
                dailyPnlService.calculateDailyPnl(job.getValuationDate(), job.getJobId());
            
            step.setRecordsProcessed(results.size());
            step.setRecordsSuccessful(results.size());
            
            log.info("Calculated P&L for {} trades", results.size());
        } catch (Exception e) {
            log.error("Failed to calculate P&L: {}", e.getMessage(), e);
            step.setRecordsFailed(1);
            step.setErrorMessage("P&L calculation failed: " + e.getMessage());
        }
    }
    
    // Step 6: Aggregate Risk Metrics
    private void executeAggregateRisk(EodValuationJob job, EodValuationJobStep step) {
        log.info("Aggregating risk metrics for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would aggregate risk metrics");
            step.setRecordsProcessed(0);
            return;
        }
        
        try {
            // Aggregate all portfolios + firm-wide
            riskAggregationService.aggregateAllRisk(job.getValuationDate(), job.getJobId());
            
            // Calculate risk concentration
            riskAggregationService.calculateRiskConcentration(job.getValuationDate());
            
            // Check risk limits
            riskAggregationService.checkRiskLimits(job.getValuationDate());
            
            // Get firm risk summary to verify completion
            FirmRiskSummary firmRisk = riskAggregationService.getFirmRisk(job.getValuationDate()).orElse(null);
            
            int portfolioCount = firmRisk != null ? firmRisk.getTotalPortfolioCount() : 0;
            
            step.setRecordsProcessed(portfolioCount);
            step.setRecordsSuccessful(portfolioCount);
            
            log.info("Risk aggregation completed: {} portfolios, firm-wide, concentration, and limits checked",
                portfolioCount);
            
        } catch (Exception e) {
            log.error("Risk aggregation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Risk aggregation failed", e);
        }
    }
    
    // Step 7: Reconcile and Validate
    private void executeReconcile(EodValuationJob job, EodValuationJobStep step) {
        log.info("Reconciling valuations for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would reconcile valuations");
            step.setRecordsProcessed(0);
            return;
        }
        
        try {
            // Reconcile valuations and detect exceptions
            DailyReconciliationSummary summary = 
                valuationReconciliationService.reconcileValuations(job.getValuationDate(), job.getJobId());
            
            if (summary == null) {
                log.warn("No reconciliation summary created");
                step.setRecordsProcessed(0);
                return;
            }
            
            step.setRecordsProcessed(summary.getTotalValuations());
            step.setRecordsSuccessful(summary.getTotalValuations() - summary.getTotalExceptions());
            step.setRecordsFailed(summary.getTotalExceptions());
            
            log.info("Reconciliation completed: {} valuations, {} exceptions ({} critical, {} errors)",
                summary.getTotalValuations(),
                summary.getTotalExceptions(),
                summary.getCriticalCount(),
                summary.getErrorCount());
            
            // Fail the step if there are critical exceptions
            if (summary.getCriticalCount() > 0) {
                throw new RuntimeException("Reconciliation found " + summary.getCriticalCount() + 
                    " critical exceptions that must be resolved");
            }
            
        } catch (Exception e) {
            log.error("Reconciliation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Reconciliation failed", e);
        }
    }
    
    // Step 8: Generate Accounting Events (Optional)
    private void executeGenerateAccountingEvents(EodValuationJob job, EodValuationJobStep step) {
        log.info("Generating accounting events for date: {}", job.getValuationDate());
        
        if (job.getDryRun()) {
            log.info("[DRY RUN] Would generate accounting events");
            step.setRecordsProcessed(0);
            return;
        }
        
        try {
            List<com.creditdefaultswap.platform.model.accounting.AccountingEvent> events = 
                accountingEventService.generateAccountingEvents(job.getValuationDate(), job.getJobId());
            
            step.setRecordsProcessed(events.size());
            step.setRecordsSuccessful(events.size());
            
            log.info("Generated {} accounting events", events.size());
        } catch (Exception e) {
            log.error("Failed to generate accounting events: {}", e.getMessage(), e);
            step.setErrorMessage("Accounting event generation failed: " + e.getMessage());
            // Don't fail the step - accounting is optional
        }
    }
    
    /**
     * Get job by ID
     */
    public EodValuationJob getJob(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
    
    /**
     * Get job by job ID string
     */
    public EodValuationJob getJobByJobId(String jobId) {
        return jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }
    
    /**
     * Get job for a specific date
     */
    public EodValuationJob getJobForDate(LocalDate date) {
        return jobRepository.findByValuationDate(date)
            .orElseThrow(() -> new IllegalArgumentException("No job found for date: " + date));
    }
    
    /**
     * Get all jobs with a specific status
     */
    public List<EodValuationJob> getJobsByStatus(EodValuationJob.JobStatus status) {
        return jobRepository.findByStatus(status);
    }
    
    /**
     * Get recent EOD jobs, ordered by valuation date descending
     */
    public List<EodValuationJob> getRecentJobs(int limit) {
        return jobRepository.findAllByOrderByValuationDateDesc()
            .stream()
            .limit(limit)
            .toList();
    }
    
    /**
     * Cancel a running job
     */
    @Transactional
    public void cancelJob(Long jobId) {
        EodValuationJob job = getJob(jobId);
        
        if (job.getStatus() != EodValuationJob.JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot cancel job with status: " + job.getStatus());
        }
        
        job.cancelJob();
        jobRepository.save(job);
        
        log.info("Cancelled job: {}", job.getJobId());
    }
    
    // Helper methods
    
    private String generateJobId(LocalDate valuationDate) {
        String dateStr = valuationDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "EOD-" + dateStr + "-" + uuid;
    }
    
    private EodValuationJobStep createStep(int stepNumber, String stepName) {
        return EodValuationJobStep.builder()
            .stepNumber(stepNumber)
            .stepName(stepName)
            .status(EodValuationJobStep.StepStatus.PENDING)
            .build();
    }
    
    /**
     * Update job-level metrics by aggregating step metrics
     */
    private void updateJobMetrics(EodValuationJob job) {
        // Find the NPV calculation step (Step 3) to get trade counts
        job.getSteps().stream()
            .filter(step -> STEP_CALCULATE_NPV.equals(step.getStepName()))
            .findFirst()
            .ifPresent(npvStep -> {
                job.setTotalTradesProcessed(npvStep.getRecordsProcessed());
                job.setSuccessfulValuations(npvStep.getRecordsSuccessful());
                job.setFailedValuations(npvStep.getRecordsFailed());
            });
        
        log.debug("Updated job metrics - Total: {}, Success: {}, Failed: {}", 
            job.getTotalTradesProcessed(), 
            job.getSuccessfulValuations(), 
            job.getFailedValuations());
    }
    
    private int getRetryDelaySeconds() {
        return configRepository.findByConfigKey("eod.retry.delay.seconds")
            .map(config -> Integer.parseInt(config.getConfigValue()))
            .orElse(60);
    }
    
    private int getBatchSize() {
        return configRepository.findByConfigKey("eod.batch.size")
            .map(config -> Integer.parseInt(config.getConfigValue()))
            .orElse(100);
    }
    
    private List<Long> getActiveTradeIds(EodValuationJob job) {
        // Query active trades from database
        // Active trades are those with status ACTIVE, trade date <= valuation date, and maturity > valuation date
        List<Long> tradeIds = tradeRepository.findAll().stream()
            .filter(trade -> com.creditdefaultswap.platform.model.TradeStatus.ACTIVE.equals(trade.getTradeStatus()))
            .filter(trade -> !trade.getTradeDate().isAfter(job.getValuationDate()))
            .filter(trade -> trade.getMaturityDate().isAfter(job.getValuationDate()))
            .map(com.creditdefaultswap.platform.model.CDSTrade::getId)
            .toList();
        
        log.info("Found {} active trades for valuation date: {}", tradeIds.size(), job.getValuationDate());
        return tradeIds;
    }
}
