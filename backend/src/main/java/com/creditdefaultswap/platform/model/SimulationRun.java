package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulation_runs")
public class SimulationRun {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "run_id", nullable = false, unique = true, length = 50)
    private String runId;
    
    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationStatus status;
    
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;
    
    @Column(name = "paths", nullable = false)
    private Integer paths;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private String requestPayload;
    
    @Column(name = "seed_used")
    private Long seedUsed;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "runtime_ms")
    private Long runtimeMs;
    
    @Transient
    private volatile boolean cancelRequested = false;
    
    // Constructors
    public SimulationRun() {
        this.createdAt = LocalDateTime.now();
        this.status = SimulationStatus.QUEUED;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getRunId() {
        return runId;
    }
    
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    public Long getPortfolioId() {
        return portfolioId;
    }
    
    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }
    
    public SimulationStatus getStatus() {
        return status;
    }
    
    public void setStatus(SimulationStatus status) {
        this.status = status;
    }
    
    public LocalDate getValuationDate() {
        return valuationDate;
    }
    
    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
    
    public Integer getPaths() {
        return paths;
    }
    
    public void setPaths(Integer paths) {
        this.paths = paths;
    }
    
    public String getRequestPayload() {
        return requestPayload;
    }
    
    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }
    
    public Long getSeedUsed() {
        return seedUsed;
    }
    
    public void setSeedUsed(Long seedUsed) {
        this.seedUsed = seedUsed;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getRuntimeMs() {
        return runtimeMs;
    }
    
    public void setRuntimeMs(Long runtimeMs) {
        this.runtimeMs = runtimeMs;
    }
    
    public boolean isCancelRequested() {
        return cancelRequested;
    }
    
    public void setCancelRequested(boolean cancelRequested) {
        this.cancelRequested = cancelRequested;
    }
}
