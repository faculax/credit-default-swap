package com.creditdefaultswap.platform.model.eod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Individual step within an EOD valuation job
 */
@Entity
@Table(name = "eod_valuation_job_steps",
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "step_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EodValuationJobStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private EodValuationJob job;
    
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;
    
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private StepStatus status = StepStatus.PENDING;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    @Column(name = "records_processed")
    @Builder.Default
    private Integer recordsProcessed = 0;
    
    @Column(name = "records_successful")
    @Builder.Default
    private Integer recordsSuccessful = 0;
    
    @Column(name = "records_failed")
    @Builder.Default
    private Integer recordsFailed = 0;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
        
        // Calculate duration if step is completed
        if (startTime != null && endTime != null) {
            durationSeconds = (int) java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
    
    public void startStep() {
        this.status = StepStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }
    
    public void completeStep() {
        this.status = StepStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
    }
    
    public void failStep(String errorMessage) {
        this.status = StepStatus.FAILED;
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
    
    public void skipStep() {
        this.status = StepStatus.SKIPPED;
        this.endTime = LocalDateTime.now();
    }
    
    public enum StepStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        SKIPPED
    }
}
