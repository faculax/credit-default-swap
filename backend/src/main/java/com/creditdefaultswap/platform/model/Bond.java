package com.creditdefaultswap.platform.model;

import com.creditdefaultswap.platform.lineage.LineageEntityListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bond entity representing fixed-rate corporate/sovereign credit bonds
 * Epic 14: Credit Bonds Enablement
 */
@Entity
@Table(name = "bonds")
@EntityListeners(LineageEntityListener.class)
public class Bond {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Identification
    @Column(name = "isin", length = 12)
    private String isin;
    
    // Issuer & Credit Info
    @NotBlank(message = "Issuer is required")
    @Size(max = 40, message = "Issuer must not exceed 40 characters")
    @Column(name = "issuer", nullable = false, length = 40)
    private String issuer;
    
    @NotNull(message = "Seniority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "seniority", nullable = false, length = 20)
    private Seniority seniority;
    
    @Size(max = 30, message = "Sector must not exceed 30 characters")
    @Column(name = "sector", length = 30)
    private String sector;
    
    // Economics
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @NotNull(message = "Notional is required")
    @DecimalMin(value = "0.01", message = "Notional must be positive")
    @Column(name = "notional", nullable = false, precision = 18, scale = 2)
    private BigDecimal notional;
    
    @NotNull(message = "Coupon rate is required")
    @DecimalMin(value = "0.0", message = "Coupon rate must be >= 0")
    @Column(name = "coupon_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal couponRate;
    
    @NotNull(message = "Coupon frequency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_frequency", nullable = false, length = 20)
    private CouponFrequency couponFrequency;
    
    @NotNull(message = "Day count convention is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "day_count", nullable = false, length = 20)
    private DayCount dayCount;
    
    // Dates
    @NotNull(message = "Issue date is required")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    
    @NotNull(message = "Maturity date is required")
    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;
    
    @Min(value = 0, message = "Settlement days must be >= 0")
    @Column(name = "settlement_days")
    private Integer settlementDays;
    
    // Pricing Convention
    @DecimalMin(value = "0.01", message = "Face value must be positive")
    @Column(name = "face_value", precision = 18, scale = 2)
    private BigDecimal faceValue;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "price_convention", length = 10)
    private PriceConvention priceConvention;
    
    // Audit
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Bond() {
        this.createdAt = LocalDateTime.now();
        this.currency = "USD";
        this.couponFrequency = CouponFrequency.SEMI_ANNUAL;
        this.dayCount = DayCount.ACT_ACT;
        this.settlementDays = 2;
        this.faceValue = new BigDecimal("100.00");
        this.priceConvention = PriceConvention.CLEAN;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIsin() {
        return isin;
    }
    
    public void setIsin(String isin) {
        this.isin = isin;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public Seniority getSeniority() {
        return seniority;
    }
    
    public void setSeniority(Seniority seniority) {
        this.seniority = seniority;
    }
    
    public String getSector() {
        return sector;
    }
    
    public void setSector(String sector) {
        this.sector = sector;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getNotional() {
        return notional;
    }
    
    public void setNotional(BigDecimal notional) {
        this.notional = notional;
    }
    
    public BigDecimal getCouponRate() {
        return couponRate;
    }
    
    public void setCouponRate(BigDecimal couponRate) {
        this.couponRate = couponRate;
    }
    
    public CouponFrequency getCouponFrequency() {
        return couponFrequency;
    }
    
    public void setCouponFrequency(CouponFrequency couponFrequency) {
        this.couponFrequency = couponFrequency;
    }
    
    public DayCount getDayCount() {
        return dayCount;
    }
    
    public void setDayCount(DayCount dayCount) {
        this.dayCount = dayCount;
    }
    
    public LocalDate getIssueDate() {
        return issueDate;
    }
    
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }
    
    public Integer getSettlementDays() {
        return settlementDays;
    }
    
    public void setSettlementDays(Integer settlementDays) {
        this.settlementDays = settlementDays;
    }
    
    public BigDecimal getFaceValue() {
        return faceValue;
    }
    
    public void setFaceValue(BigDecimal faceValue) {
        this.faceValue = faceValue;
    }
    
    public PriceConvention getPriceConvention() {
        return priceConvention;
    }
    
    public void setPriceConvention(PriceConvention priceConvention) {
        this.priceConvention = priceConvention;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
