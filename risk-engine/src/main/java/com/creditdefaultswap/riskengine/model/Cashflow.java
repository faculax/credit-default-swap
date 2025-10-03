package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single cashflow from the CDS cashflow schedule.
 * Parsed from ORE's flows.csv output.
 */
public class Cashflow {
    private String tradeId;
    private String type;
    private Integer cashflowNo;
    private Integer legNo;
    private LocalDate payDate;
    private String flowType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal coupon;
    private BigDecimal accrual;
    private LocalDate accrualStartDate;
    private LocalDate accrualEndDate;
    private BigDecimal accruedAmount;
    private BigDecimal notional;
    private BigDecimal discountFactor;
    private BigDecimal presentValue;
    private BigDecimal fxRate;
    private BigDecimal presentValueBase;

    // Default constructor
    public Cashflow() {}

    // Getters and setters
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getCashflowNo() { return cashflowNo; }
    public void setCashflowNo(Integer cashflowNo) { this.cashflowNo = cashflowNo; }

    public Integer getLegNo() { return legNo; }
    public void setLegNo(Integer legNo) { this.legNo = legNo; }

    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }

    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getCoupon() { return coupon; }
    public void setCoupon(BigDecimal coupon) { this.coupon = coupon; }

    public BigDecimal getAccrual() { return accrual; }
    public void setAccrual(BigDecimal accrual) { this.accrual = accrual; }

    public LocalDate getAccrualStartDate() { return accrualStartDate; }
    public void setAccrualStartDate(LocalDate accrualStartDate) { this.accrualStartDate = accrualStartDate; }

    public LocalDate getAccrualEndDate() { return accrualEndDate; }
    public void setAccrualEndDate(LocalDate accrualEndDate) { this.accrualEndDate = accrualEndDate; }

    public BigDecimal getAccruedAmount() { return accruedAmount; }
    public void setAccruedAmount(BigDecimal accruedAmount) { this.accruedAmount = accruedAmount; }

    public BigDecimal getNotional() { return notional; }
    public void setNotional(BigDecimal notional) { this.notional = notional; }

    public BigDecimal getDiscountFactor() { return discountFactor; }
    public void setDiscountFactor(BigDecimal discountFactor) { this.discountFactor = discountFactor; }

    public BigDecimal getPresentValue() { return presentValue; }
    public void setPresentValue(BigDecimal presentValue) { this.presentValue = presentValue; }

    public BigDecimal getFxRate() { return fxRate; }
    public void setFxRate(BigDecimal fxRate) { this.fxRate = fxRate; }

    public BigDecimal getPresentValueBase() { return presentValueBase; }
    public void setPresentValueBase(BigDecimal presentValueBase) { this.presentValueBase = presentValueBase; }
}
