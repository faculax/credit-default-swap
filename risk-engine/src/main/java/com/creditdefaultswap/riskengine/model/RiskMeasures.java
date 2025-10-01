package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.time.Instant;

public class RiskMeasures {
    private Long tradeId;
    private BigDecimal pvClean;
    private BigDecimal pvDirty;
    private BigDecimal parSpread;
    private BigDecimal cs01;
    private BigDecimal dv01;
    private BigDecimal jtd;
    private BigDecimal recovery01;
    private Instant valuationTimestamp;

    public RiskMeasures(Long tradeId) {
        this.tradeId = tradeId;
        this.valuationTimestamp = Instant.now();
    }

    public Long getTradeId() { return tradeId; }
    public BigDecimal getPvClean() { return pvClean; }
    public BigDecimal getPvDirty() { return pvDirty; }
    public BigDecimal getParSpread() { return parSpread; }
    public BigDecimal getCs01() { return cs01; }
    public BigDecimal getDv01() { return dv01; }
    public BigDecimal getJtd() { return jtd; }
    public BigDecimal getRecovery01() { return recovery01; }
    public Instant getValuationTimestamp() { return valuationTimestamp; }

    public RiskMeasures withPvClean(BigDecimal v){ this.pvClean=v; return this; }
    public RiskMeasures withPvDirty(BigDecimal v){ this.pvDirty=v; return this; }
    public RiskMeasures withParSpread(BigDecimal v){ this.parSpread=v; return this; }
    public RiskMeasures withCs01(BigDecimal v){ this.cs01=v; return this; }
    public RiskMeasures withDv01(BigDecimal v){ this.dv01=v; return this; }
    public RiskMeasures withJtd(BigDecimal v){ this.jtd=v; return this; }
    public RiskMeasures withRecovery01(BigDecimal v){ this.recovery01=v; return this; }
}
