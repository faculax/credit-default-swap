package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "simulation_contributors")
public class SimulationContributor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "run_id", nullable = false, length = 50)
    private String runId;
    
    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName;
    
    @Column(name = "marginal_el_pct", precision = 10, scale = 4)
    private BigDecimal marginalElPct;
    
    @Column(name = "beta", precision = 6, scale = 4)
    private BigDecimal beta;
    
    @Column(name = "standalone_el", precision = 18, scale = 4)
    private BigDecimal standaloneEl;
    
    @Column(name = "recovery_mean", precision = 6, scale = 4)
    private BigDecimal recoveryMean;
    
    @Column(name = "recovery_stdev", precision = 6, scale = 4)
    private BigDecimal recoveryStdev;
    
    // Constructors
    public SimulationContributor() {}
    
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
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public BigDecimal getMarginalElPct() {
        return marginalElPct;
    }
    
    public void setMarginalElPct(BigDecimal marginalElPct) {
        this.marginalElPct = marginalElPct;
    }
    
    public BigDecimal getBeta() {
        return beta;
    }
    
    public void setBeta(BigDecimal beta) {
        this.beta = beta;
    }
    
    public BigDecimal getStandaloneEl() {
        return standaloneEl;
    }
    
    public void setStandaloneEl(BigDecimal standaloneEl) {
        this.standaloneEl = standaloneEl;
    }
    
    public BigDecimal getRecoveryMean() {
        return recoveryMean;
    }
    
    public void setRecoveryMean(BigDecimal recoveryMean) {
        this.recoveryMean = recoveryMean;
    }
    
    public BigDecimal getRecoveryStdev() {
        return recoveryStdev;
    }
    
    public void setRecoveryStdev(BigDecimal recoveryStdev) {
        this.recoveryStdev = recoveryStdev;
    }
}
