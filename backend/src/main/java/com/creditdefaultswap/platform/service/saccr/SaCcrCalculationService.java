package com.creditdefaultswap.platform.service.saccr;

import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.repository.saccr.SaCcrCalculationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class SaCcrCalculationService {

    @Autowired
    private SaCcrCalculationRepository calculationRepository;

    public List<SaCcrCalculation> calculateAllExposures(LocalDate asOfDate, String jurisdiction) {
        log.info("Calculating all SA-CCR exposures for date: {} and jurisdiction: {}", asOfDate, jurisdiction);
        return calculationRepository.findAll();
    }

    public SaCcrCalculation calculateExposure(NettingSet nettingSet, LocalDate asOfDate, String jurisdiction) {
        log.info("Calculating SA-CCR exposure for netting set: {} as of date: {} and jurisdiction: {}", 
                 nettingSet.getId(), asOfDate, jurisdiction);
        
        // For now, return a mock calculation - full implementation would be more complex
        SaCcrCalculation calculation = new SaCcrCalculation();
        calculation.setNettingSet(nettingSet);
        calculation.setCalculationDate(asOfDate);
        calculation.setReplacementCost(BigDecimal.ZERO);
        calculation.setPotentialFutureExposure(BigDecimal.ZERO);
        calculation.setExposureAtDefault(BigDecimal.ZERO);
        calculation.setAlphaFactor(new BigDecimal("1.4"));
        calculation.setJurisdiction(jurisdiction);
        return calculation;
    }

    public List<SaCcrCalculation> getAllCalculations() {
        return calculationRepository.findAll();
    }
}