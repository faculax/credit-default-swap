package com.creditdefaultswap.platform.controller.eod;

import com.creditdefaultswap.platform.model.eod.EodValuationJob;
import com.creditdefaultswap.platform.model.eod.EodValuationJobStep;
import com.creditdefaultswap.platform.service.eod.EodValuationJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API for EOD valuation job management
 */
@RestController
@RequestMapping("/api/eod/valuation-jobs")
@RequiredArgsConstructor
@Slf4j
public class EodValuationJobController {
    
    private final EodValuationJobService jobService;
    
    /**
     * Manually trigger an EOD valuation job for a specific date
     * Accepts request body with valuationDate and dryRun flag
     */
    @PostMapping("/trigger")
    public ResponseEntity<?> triggerJob(@RequestBody TriggerJobRequest request) {
        try {
            log.info("Manual trigger requested for EOD job: date={}, dryRun={}",
                request.getValuationDate(), request.isDryRun());
            
            EodValuationJob job = jobService.executeEodJob(
                request.getValuationDate(), 
                "MANUAL", 
                request.isDryRun()
            );
            
            // Return response matching frontend expectation
            TriggerJobResponse response = new TriggerJobResponse(
                job.getJobId(),
                "EOD valuation job triggered successfully",
                "SUCCESS"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            log.error("Error triggering EOD job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new TriggerJobResponse(null, e.getMessage(), "ERROR"));
        }
    }
    
    // DTO classes for request/response
    public static class TriggerJobRequest {
        private LocalDate valuationDate;
        private boolean dryRun;
        
        public LocalDate getValuationDate() { return valuationDate; }
        public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }
        public boolean isDryRun() { return dryRun; }
        public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }
    }
    
    public static class TriggerJobResponse {
        private String jobId;
        private String message;
        private String status;
        
        public TriggerJobResponse(String jobId, String message, String status) {
            this.jobId = jobId;
            this.message = message;
            this.status = status;
        }
        
        public String getJobId() { return jobId; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
    }
    
    /**
     * Get the most recent EOD job
     * MUST come before /{jobId} to avoid matching "latest" as jobId
     */
    @GetMapping("/latest")
    public ResponseEntity<EodValuationJob> getLatestJob() {
        List<EodValuationJob> recentJobs = jobService.getRecentJobs(1);
        if (recentJobs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(recentJobs.get(0));
    }
    
    /**
     * Get recent EOD jobs
     * MUST come before /{jobId} to avoid matching "recent" as jobId
     */
    @GetMapping("/recent")
    public ResponseEntity<List<EodValuationJob>> getRecentJobs(
        @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(jobService.getRecentJobs(limit));
    }
    
    /**
     * Get job by job ID string
     * More specific than /{jobId} numeric matcher
     */
    @GetMapping("/by-job-id/{jobId}")
    public ResponseEntity<EodValuationJob> getJobByJobId(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(jobService.getJobByJobId(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get job for a specific valuation date
     */
    @GetMapping("/by-date/{date}")
    public ResponseEntity<EodValuationJob> getJobForDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            return ResponseEntity.ok(jobService.getJobForDate(date));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get job for a specific valuation date (alternative path for frontend)
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<EodValuationJob> getJobByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            return ResponseEntity.ok(jobService.getJobForDate(date));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get job by numeric ID
     * Regex constraint ensures only numeric IDs are matched here
     */
    @GetMapping("/{jobId:\\d+}")
    public ResponseEntity<EodValuationJob> getJob(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(jobService.getJob(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get jobs by status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<EodValuationJob>> getJobsByStatus(
        @PathVariable EodValuationJob.JobStatus status
    ) {
        return ResponseEntity.ok(jobService.getJobsByStatus(status));
    }
    
    /**
     * Get running jobs
     */
    @GetMapping("/running")
    public ResponseEntity<List<EodValuationJob>> getRunningJobs() {
        return ResponseEntity.ok(jobService.getJobsByStatus(EodValuationJob.JobStatus.RUNNING));
    }
    
    /**
     * Cancel a running job
     */
    @PostMapping("/{jobId:\\d+}/cancel")
    public ResponseEntity<Void> cancelJob(@PathVariable Long jobId) {
        try {
            jobService.cancelJob(jobId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("Error cancelling job: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get steps for a job
     */
    @GetMapping("/{jobId:\\d+}/steps")
    public ResponseEntity<List<EodValuationJobStep>> getJobSteps(@PathVariable Long jobId) {
        try {
            EodValuationJob job = jobService.getJob(jobId);
            return ResponseEntity.ok(job.getSteps());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
