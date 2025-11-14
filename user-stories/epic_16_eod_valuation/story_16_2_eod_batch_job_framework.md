# Story 16.2: EOD Valuation Batch Job Framework

## Story
**As a** system operator  
**I want** an automated batch job framework for EOD valuations  
**So that** valuations run reliably every business day without manual intervention

## Acceptance Criteria
- [ ] Scheduled batch job runs automatically at configurable time (default: 6:00 PM EST)
- [ ] Job only runs on business days (respects holiday calendar)
- [ ] Job orchestrates the full EOD valuation workflow:
  - [ ] Market data snapshot capture
  - [ ] Trade data extraction (active trades only)
  - [ ] NPV calculation (via ORE)
  - [ ] Accrued interest calculation
  - [ ] Valuation storage
  - [ ] P&L calculation
  - [ ] Risk report generation
- [ ] Progress tracking and status updates at each stage
- [ ] Error handling and retry logic for failed steps
- [ ] Notifications on success/failure (email/Slack)
- [ ] Manual trigger capability for ad-hoc runs
- [ ] Dry-run mode for testing without persisting results
- [ ] Job execution history and audit trail
- [ ] Graceful shutdown capability

## Technical Details

### Database Schema
```sql
-- Valuation job executions
CREATE TABLE eod_valuation_jobs (
    id BIGSERIAL PRIMARY KEY,
    job_id VARCHAR(100) NOT NULL UNIQUE,
    valuation_date DATE NOT NULL,
    job_type VARCHAR(20) NOT NULL, -- SCHEDULED, MANUAL, RERUN
    status VARCHAR(20) NOT NULL, -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    total_trades INTEGER,
    trades_valued INTEGER DEFAULT 0,
    trades_failed INTEGER DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    error_message TEXT,
    triggered_by VARCHAR(100), -- system, user@email.com
    dry_run BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Job execution steps
CREATE TABLE eod_valuation_job_steps (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES eod_valuation_jobs(id) ON DELETE CASCADE,
    step_name VARCHAR(50) NOT NULL,
    step_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    records_processed INTEGER,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0
);

-- Job configuration
CREATE TABLE eod_valuation_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Create indexes
CREATE INDEX idx_valuation_jobs_date ON eod_valuation_jobs(valuation_date);
CREATE INDEX idx_valuation_jobs_status ON eod_valuation_jobs(status);
CREATE INDEX idx_valuation_job_steps_job ON eod_valuation_job_steps(job_id);

-- Insert default configuration
INSERT INTO eod_valuation_config (config_key, config_value, description) VALUES
('eod.schedule.time', '18:00', 'Daily run time (HH:MM in system timezone)'),
('eod.schedule.enabled', 'true', 'Enable/disable scheduled runs'),
('eod.notification.email', 'risk-team@example.com', 'Notification recipients'),
('eod.retry.max_attempts', '3', 'Maximum retry attempts per step'),
('eod.timeout.minutes', '120', 'Maximum job execution time'),
('eod.parallel.batch_size', '100', 'Number of trades to value in parallel');
```

### Java Implementation
```java
@Service
public class EodValuationJobService {
    
    @Autowired
    private EodValuationJobRepository jobRepository;
    
    @Autowired
    private MarketDataSnapshotService snapshotService;
    
    @Autowired
    private TradeRepository tradeRepository;
    
    @Autowired
    private OreValuationService oreValuationService;
    
    @Autowired
    private AccruedInterestService accruedInterestService;
    
    @Autowired
    private ValuationStorageService valuationStorageService;
    
    @Autowired
    private PLCalculationService plCalculationService;
    
    @Autowired
    private RiskReportingService riskReportingService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Transactional
    public EodValuationJob executeEodValuation(LocalDate valuationDate, boolean dryRun) {
        String jobId = generateJobId(valuationDate);
        
        EodValuationJob job = new EodValuationJob();
        job.setJobId(jobId);
        job.setValuationDate(valuationDate);
        job.setJobType(JobType.MANUAL);
        job.setStatus(JobStatus.PENDING);
        job.setDryRun(dryRun);
        job.setStartedAt(LocalDateTime.now());
        job = jobRepository.save(job);
        
        try {
            // Step 1: Market Data Snapshot
            executeStep(job, "MARKET_DATA_SNAPSHOT", () -> {
                return snapshotService.captureSnapshot(valuationDate);
            });
            
            // Step 2: Extract Active Trades
            List<CDSTrade> activeTrades = executeStep(job, "EXTRACT_TRADES", () -> {
                return tradeRepository.findActiveTradesForValuation(valuationDate);
            });
            job.setTotalTrades(activeTrades.size());
            
            // Step 3: Calculate NPVs (batched)
            Map<Long, BigDecimal> npvResults = executeStep(job, "CALCULATE_NPV", () -> {
                return oreValuationService.calculateNPVBatch(activeTrades, valuationDate);
            });
            
            // Step 4: Calculate Accrued Interest
            Map<Long, BigDecimal> accruedResults = executeStep(job, "CALCULATE_ACCRUED", () -> {
                return accruedInterestService.calculateAccruedBatch(activeTrades, valuationDate);
            });
            
            // Step 5: Store Valuations
            if (!dryRun) {
                executeStep(job, "STORE_VALUATIONS", () -> {
                    return valuationStorageService.storeValuations(
                        valuationDate, npvResults, accruedResults
                    );
                });
            }
            
            // Step 6: Calculate P&L
            executeStep(job, "CALCULATE_PL", () -> {
                return plCalculationService.calculateDailyPL(valuationDate);
            });
            
            // Step 7: Generate Risk Reports
            executeStep(job, "GENERATE_REPORTS", () -> {
                return riskReportingService.generateEodReports(valuationDate);
            });
            
            // Complete job
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setTradesValued(activeTrades.size());
            
            notificationService.sendSuccessNotification(job);
            
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            
            notificationService.sendFailureNotification(job, e);
            throw new EodValuationException("EOD valuation job failed", e);
        } finally {
            job.setDurationSeconds(calculateDuration(job));
            jobRepository.save(job);
        }
        
        return job;
    }
    
    private <T> T executeStep(EodValuationJob job, String stepName, 
                               Supplier<T> stepLogic) {
        EodValuationJobStep step = new EodValuationJobStep();
        step.setJob(job);
        step.setStepName(stepName);
        step.setStatus(StepStatus.RUNNING);
        step.setStartedAt(LocalDateTime.now());
        
        try {
            T result = stepLogic.get();
            step.setStatus(StepStatus.COMPLETED);
            return result;
        } catch (Exception e) {
            step.setStatus(StepStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            step.setCompletedAt(LocalDateTime.now());
            step.setDurationSeconds(calculateDuration(step));
            // Save step status
        }
    }
}

// Scheduled job trigger
@Component
public class EodValuationScheduler {
    
    @Autowired
    private EodValuationJobService jobService;
    
    @Autowired
    private BusinessDayCalendar businessDayCalendar;
    
    @Scheduled(cron = "${eod.schedule.cron:0 0 18 * * MON-FRI}")
    public void scheduledEodValuation() {
        LocalDate today = LocalDate.now();
        
        // Only run on business days
        if (!businessDayCalendar.isBusinessDay(today)) {
            log.info("Skipping EOD valuation - not a business day: {}", today);
            return;
        }
        
        log.info("Starting scheduled EOD valuation for date: {}", today);
        jobService.executeEodValuation(today, false);
    }
}
```

### REST API
```java
@RestController
@RequestMapping("/api/eod-valuation")
public class EodValuationController {
    
    @PostMapping("/run")
    public ResponseEntity<EodValuationJob> triggerValuation(
        @RequestParam LocalDate valuationDate,
        @RequestParam(defaultValue = "false") boolean dryRun
    );
    
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<EodValuationJob> getJobStatus(
        @PathVariable String jobId
    );
    
    @GetMapping("/jobs")
    public ResponseEntity<List<EodValuationJob>> getJobHistory(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate
    );
    
    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId);
    
    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<EodValuationJob> retryJob(@PathVariable String jobId);
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfiguration();
    
    @PutMapping("/config")
    public ResponseEntity<Void> updateConfiguration(
        @RequestBody Map<String, String> config
    );
}
```

## Test Scenarios
1. **Successful Run**: All steps complete successfully
2. **Partial Failure**: One step fails, job marked as failed
3. **Retry Logic**: Failed step retried up to max attempts
4. **Dry Run**: Job executes but doesn't persist results
5. **Manual Trigger**: Ad-hoc run via API
6. **Holiday Skip**: Job doesn't run on non-business day
7. **Concurrent Jobs**: Prevent multiple jobs for same date
8. **Timeout**: Job exceeds timeout, gracefully terminated
9. **Notification**: Email sent on success/failure

## Definition of Done
- [ ] Code implemented and reviewed
- [ ] Database migration scripts created
- [ ] Unit tests written (>80% coverage)
- [ ] Integration tests for job orchestration
- [ ] Scheduled task tested in dev environment
- [ ] Manual trigger API tested
- [ ] Notification system integrated
- [ ] Documentation updated
- [ ] Deployed to dev environment
- [ ] QA sign-off

## Dependencies
- Story 16.1: Market Data Snapshot Service
- Business day calendar service
- Email/notification service

## Effort Estimate
**8 story points** (1.5 weeks)

## Notes
- Consider using Spring Batch for more robust job orchestration
- Implement distributed locking to prevent concurrent runs
- Monitor job execution times and set up alerts for SLA breaches
- Consider using message queue for async step execution
- Implement checkpoint/restart capability for long-running jobs
