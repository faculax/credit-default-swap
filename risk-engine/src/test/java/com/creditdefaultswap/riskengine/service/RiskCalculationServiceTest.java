package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RiskCalculationServiceTest {

    private final RiskCalculationService service = new RiskCalculationService();

    @Test
    void testBaseCalculationNotNull() {
        RiskMeasures measures = service.calculateBase(123L);
        assertNotNull(measures.getPvClean());
        assertNotNull(measures.getParSpread());
    }

    @Test
    void testParallelShiftAdjustsSpread() {
        RiskMeasures base = service.calculateBase(5L);
        RiskMeasures shifted = service.shiftParallel(base, 10);
        assertEquals(base.getParSpread().add(java.math.BigDecimal.valueOf(10)), shifted.getParSpread());
    }
}
