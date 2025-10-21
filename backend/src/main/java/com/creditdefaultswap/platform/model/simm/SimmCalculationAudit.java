package com.creditdefaultswap.platform.model.simm;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * Entity representing SIMM calculation audit trail for step-by-step breakdown
 */
@Entity
@Table(name = "simm_calculation_audit")
public class SimmCalculationAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculation_id", nullable = false)
    @JsonIgnore
    private SimmCalculation calculation;
    
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;
    
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
    
    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;
    
    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;
    
    @Column(name = "calculation_details", columnDefinition = "TEXT")
    private String calculationDetails;
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public SimmCalculationAudit() {}
    
    public SimmCalculationAudit(String stepName, Integer stepOrder) {
        this.stepName = stepName;
        this.stepOrder = stepOrder;
    }
    
    public SimmCalculationAudit(String stepName, Integer stepOrder, 
                               String inputData, String outputData) {
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.inputData = inputData;
        this.outputData = outputData;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SimmCalculation getCalculation() { return calculation; }
    public void setCalculation(SimmCalculation calculation) { this.calculation = calculation; }
    
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    
    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }
    
    public String getOutputData() { return outputData; }
    public void setOutputData(String outputData) { this.outputData = outputData; }
    
    public String getCalculationDetails() { return calculationDetails; }
    public void setCalculationDetails(String calculationDetails) { 
        this.calculationDetails = calculationDetails; 
    }
    
    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { 
        this.processingTimeMs = processingTimeMs; 
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "SimmCalculationAudit{" +
                "id=" + id +
                ", stepName='" + stepName + '\'' +
                ", stepOrder=" + stepOrder +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}