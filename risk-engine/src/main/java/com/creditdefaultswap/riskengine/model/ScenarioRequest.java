package com.creditdefaultswap.riskengine.model;

import java.math.BigDecimal;
import java.util.List;

public class ScenarioRequest {
    private List<BigDecimal> parallelBpsShifts; // e.g. [10, -10]
    public List<BigDecimal> getParallelBpsShifts() { return parallelBpsShifts; }
    public void setParallelBpsShifts(List<BigDecimal> parallelBpsShifts) { this.parallelBpsShifts = parallelBpsShifts; }
}
