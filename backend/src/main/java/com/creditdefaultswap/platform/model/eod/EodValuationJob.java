package com.creditdefaultswap.platform.model.eod;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * EOD Valuation Job entity for batch processing orchestration
 */
@Entity
@Table(name = "eod_valuation_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EodValuationJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    private String jobId;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;
    
    // Execution timeline
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    // Configuration
    @Column(name = "dry_run")
    @Builder.Default
    private Boolean dryRun = false;
    
    @Column(name = "manual_trigger")
    @Builder.Default
    private Boolean manualTrigger = false;
    
    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;
    
    // Progress tracking
    @Column(name = "current_step")
    @Builder.Default
    private Integer currentStep = 0;
    
    @Column(name = "total_steps")
    @Builder.Default
    private Integer totalSteps = 7;
    
    @Column(name = "progress_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal progressPercentage = BigDecimal.ZERO;
    
    // Results summary
    @Column(name = "total_trades_processed")
    @Builder.Default
    private Integer totalTradesProcessed = 0;
    
    @Column(name = "successful_valuations")
    @Builder.Default
    private Integer successfulValuations = 0;
    
    @Column(name = "failed_valuations")
    @Builder.Default
    private Integer failedValuations = 0;
    
    // Error handling
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber ASC")
    @Builder.Default
    private List<EodValuationJobStep> steps = new ArrayList<>();
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Calculate duration if job is completed
        if (startTime != null && endTime != null) {
            durationSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
        
        // Calculate progress percentage
        if (totalSteps > 0) {
            progressPercentage = BigDecimal.valueOf(currentStep)
                .divide(BigDecimal.valueOf(totalSteps), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
    }
    
    public void addStep(EodValuationJobStep step) {
        steps.add(step);
        step.setJob(this);
    }
    
    public void startJob() {
        this.status = JobStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    public void completeJob() {
        this.status = JobStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.currentStep = this.totalSteps;
    }
    
    public void failJob(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    public void cancelJob() {
        this.status = JobStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }
    
    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
