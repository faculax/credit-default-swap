package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Service
public class RiskCalculationService {
    private final Random random = new Random();

    public RiskMeasures calculateBase(Long tradeId){
        // Stub deterministic-ish values for now
        BigDecimal base = BigDecimal.valueOf(1000 + (tradeId % 37));
        return new RiskMeasures(tradeId)
                .withPvClean(base)
                .withPvDirty(base.add(BigDecimal.valueOf(5)))
                .withParSpread(BigDecimal.valueOf(120.5))
                .withCs01(BigDecimal.valueOf(75,2)) // 0.75
                .withDv01(BigDecimal.valueOf(55,2)) // 0.55
                .withJtd(base.negate().divide(BigDecimal.valueOf(10),2, RoundingMode.HALF_UP))
                .withRecovery01(BigDecimal.valueOf(32,2));
    }

    public RiskMeasures shiftParallel(RiskMeasures base, int bps){
        BigDecimal factor = BigDecimal.valueOf(bps).divide(BigDecimal.valueOf(10000),6,RoundingMode.HALF_UP);
        return new RiskMeasures(base.getTradeId())
                .withPvClean(base.getPvClean().subtract(base.getPvClean().multiply(factor)))
                .withPvDirty(base.getPvDirty().subtract(base.getPvDirty().multiply(factor)))
                .withParSpread(base.getParSpread().add(BigDecimal.valueOf(bps)))
                .withCs01(base.getCs01())
                .withDv01(base.getDv01())
                .withJtd(base.getJtd())
                .withRecovery01(base.getRecovery01());
    }
}
