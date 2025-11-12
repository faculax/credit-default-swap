package com.creditdefaultswap.platform.model;

import jakarta.persistence.*;
import jakarta.persistence.EntityListeners;
import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing trade amendments for economic terms.
 * Maintains full audit trail and version history.
 */
@Entity
@EntityListeners(LineageEntityListener.class)
@Table(name = "trade_amendments")
public class TradeAmendment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(name = "amendment_date", nullable = false)
    private LocalDate amendmentDate;

    @Column(name = "previous_version", nullable = false)
    private Integer previousVersion;

    @Column(name = "new_version", nullable = false)
    private Integer newVersion;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "amendment_reason", columnDefinition = "TEXT")
    private String amendmentReason;

    @Column(name = "pnl_impact_estimate", precision = 15, scale = 2)
    private BigDecimal pnlImpactEstimate;

    @Column(name = "amended_by", nullable = false, length = 50)
    private String amendedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public TradeAmendment() {}

    public TradeAmendment(Long tradeId, LocalDate amendmentDate, Integer previousVersion, 
                         Integer newVersion, String fieldName, String previousValue, 
                         String newValue, String amendedBy) {
        this.tradeId = tradeId;
        this.amendmentDate = amendmentDate;
        this.previousVersion = previousVersion;
        this.newVersion = newVersion;
        this.fieldName = fieldName;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.amendedBy = amendedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTradeId() { return tradeId; }
    public void setTradeId(Long tradeId) { this.tradeId = tradeId; }

    public LocalDate getAmendmentDate() { return amendmentDate; }
    public void setAmendmentDate(LocalDate amendmentDate) { this.amendmentDate = amendmentDate; }

    public Integer getPreviousVersion() { return previousVersion; }
    public void setPreviousVersion(Integer previousVersion) { this.previousVersion = previousVersion; }

    public Integer getNewVersion() { return newVersion; }
    public void setNewVersion(Integer newVersion) { this.newVersion = newVersion; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getAmendmentReason() { return amendmentReason; }
    public void setAmendmentReason(String amendmentReason) { this.amendmentReason = amendmentReason; }

    public BigDecimal getPnlImpactEstimate() { return pnlImpactEstimate; }
    public void setPnlImpactEstimate(BigDecimal pnlImpactEstimate) { this.pnlImpactEstimate = pnlImpactEstimate; }

    public String getAmendedBy() { return amendedBy; }
    public void setAmendedBy(String amendedBy) { this.amendedBy = amendedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}