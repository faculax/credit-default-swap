package com.creditdefaultswap.riskengine.controller;

import com.creditdefaultswap.riskengine.model.RiskMeasures;
import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.service.RiskCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskCalculationService calcService;

    public RiskController(RiskCalculationService calcService) {
        this.calcService = calcService;
    }

    @GetMapping("/cds/{tradeId}")
    public ResponseEntity<RiskMeasures> getBase(@PathVariable Long tradeId){
        return ResponseEntity.ok(calcService.calculateBase(tradeId));
    }

    @PostMapping("/cds/{tradeId}/scenarios")
    public ResponseEntity<Map<String,Object>> runScenarios(@PathVariable Long tradeId,
                                                           @RequestBody ScenarioRequest request){
        RiskMeasures base = calcService.calculateBase(tradeId);
        List<Map<String,Object>> scenarios = new ArrayList<>();
        if(request.getParallelBpsShifts()!=null){
            for(BigDecimal shift : request.getParallelBpsShifts()){
                int bps = shift.intValue();
                RiskMeasures shifted = calcService.shiftParallel(base, bps);
                Map<String,Object> entry = new LinkedHashMap<>();
                entry.put("scenario","PARALLEL_"+bps+"BP");
                entry.put("measures", shifted);
                scenarios.add(entry);
            }
        }
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("base", base);
        response.put("scenarios", scenarios);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public Map<String,String> health(){
        return Map.of("status","UP","engineVersion","stub-0.1");
    }
}
