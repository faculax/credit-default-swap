package com.creditdefaultswap.platform.service.saccr;

import com.creditdefaultswap.platform.model.saccr.SaCcrCalculation;
import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.model.saccr.SaCcrSupervisoryParameter;
import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.repository.saccr.SaCcrCalculationRepository;
import com.creditdefaultswap.platform.repository.saccr.NettingSetRepository;
import com.creditdefaultswap.platform.repository.saccr.SaCcrSupervisoryParameterRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class SaCcrCalculationService {

    @Autowired
    private SaCcrCalculationRepository calculationRepository;
    
    @Autowired
    private NettingSetRepository nettingSetRepository;
    
    @Autowired
    private CDSTradeRepository cdsTradeRepository;
    
    @Autowired
    private SaCcrJurisdictionService jurisdictionService;
    
    @Autowired
    private SaCcrSupervisoryParameterRepository supervisoryParameterRepository;

    /**
     * Calculate SA-CCR exposures for all netting sets
     */
    public List<SaCcrCalculation> calculateAllExposures(LocalDate asOfDate, String jurisdiction) {
        log.info("Calculating all SA-CCR exposures for date: {} and jurisdiction: {}", asOfDate, jurisdiction);
        
        List<NettingSet> allNettingSets = nettingSetRepository.findAll();
        List<SaCcrCalculation> calculations = new ArrayList<>();
        
        for (NettingSet nettingSet : allNettingSets) {
            try {
                // Always recalculate to ensure fresh data
                SaCcrCalculation calculation = calculateExposure(nettingSet, asOfDate, jurisdiction);
                
                // Check if calculation already exists for this netting set, date AND jurisdiction
                String calculationId = "SACCR-" + nettingSet.getNettingSetId() + "-" + asOfDate.toString() + "-" + jurisdiction;
                SaCcrCalculation existingCalculation = calculationRepository.findByCalculationId(calculationId);
                
                if (existingCalculation != null) {
                    log.info("Updating existing SA-CCR calculation for netting set {} on date {}", 
                             nettingSet.getNettingSetId(), asOfDate);
                    // Update existing record with new values
                    existingCalculation.setReplacementCost(calculation.getReplacementCost());
                    existingCalculation.setPotentialFutureExposure(calculation.getPotentialFutureExposure());
                    existingCalculation.setExposureAtDefault(calculation.getExposureAtDefault());
                    existingCalculation.setAlphaFactor(calculation.getAlphaFactor());
                    existingCalculation.setEffectiveNotional(calculation.getEffectiveNotional());
                    existingCalculation.setGrossMtm(calculation.getGrossMtm());
                    existingCalculation.setVmReceived(calculation.getVmReceived());
                    existingCalculation.setVmPosted(calculation.getVmPosted());
                    existingCalculation.setImReceived(calculation.getImReceived());
                    existingCalculation.setImPosted(calculation.getImPosted());
                    existingCalculation.setSupervisoryAddon(calculation.getSupervisoryAddon());
                    existingCalculation.setMultiplier(calculation.getMultiplier());
                    existingCalculation.setCalculationStatus(SaCcrCalculation.CalculationStatus.COMPLETED);
                    SaCcrCalculation savedCalculation = calculationRepository.save(existingCalculation);
                    calculations.add(savedCalculation);
                } else {
                    log.info("Creating new SA-CCR calculation for netting set {} on date {}", 
                             nettingSet.getNettingSetId(), asOfDate);
                    // Save new calculation to database
                    SaCcrCalculation savedCalculation = calculationRepository.save(calculation);
                    calculations.add(savedCalculation);
                }
            } catch (Exception e) {
                log.error("Failed to calculate exposure for netting set {}: {}", 
                         nettingSet.getNettingSetId(), e.getMessage());
            }
        }
        
        log.info("Successfully calculated and saved {} exposures for jurisdiction {}", calculations.size(), jurisdiction);
        return calculations;
    }

    /**
     * Calculate SA-CCR exposure for a specific netting set using Basel III methodology
     * EAD = α × (RC + PFE)
     * where α = 1.4 (alpha factor), RC = Replacement Cost, PFE = Potential Future Exposure
     */
    public SaCcrCalculation calculateExposure(NettingSet nettingSet, LocalDate asOfDate, String jurisdiction) {
        log.info("Calculating SA-CCR exposure for netting set: {} as of date: {} and jurisdiction: {}", 
                 nettingSet.getNettingSetId(), asOfDate, jurisdiction);
        
        try {
            // Get all active trades in the netting set
            List<CDSTrade> trades = cdsTradeRepository.findByNettingSetIdAndTradeStatus(
                nettingSet.getNettingSetId(), TradeStatus.ACTIVE);
            
            log.debug("Found {} active trades in netting set {}", trades.size(), nettingSet.getNettingSetId());
            
            // Calculate Replacement Cost (RC)
            BigDecimal replacementCost = calculateReplacementCost(trades, nettingSet);
            
            // Calculate Potential Future Exposure (PFE)
            BigDecimal potentialFutureExposure = calculatePotentialFutureExposure(trades, nettingSet, jurisdiction);
            
            // Calculate Exposure at Default (EAD) = α × (RC + PFE)
            BigDecimal alphaFactor = determineAlphaFactor(nettingSet, jurisdiction);
            BigDecimal exposureAtDefault = alphaFactor.multiply(
                replacementCost.add(potentialFutureExposure)
            ).setScale(2, RoundingMode.HALF_UP);
            
            // Create calculation record with jurisdiction in ID
            String calculationId = "SACCR-" + nettingSet.getNettingSetId() + "-" + asOfDate.toString() + "-" + jurisdiction;
            SaCcrCalculation calculation = new SaCcrCalculation(calculationId, nettingSet.getNettingSetId(), asOfDate, jurisdiction);
            calculation.setNettingSet(nettingSet);
            calculation.setReplacementCost(replacementCost);
            calculation.setPotentialFutureExposure(potentialFutureExposure);
            calculation.setExposureAtDefault(exposureAtDefault);
            calculation.setAlphaFactor(alphaFactor);
            calculation.setEffectiveNotional(potentialFutureExposure); // Store trade count info in effective notional for now
            calculation.setCalculationStatus(SaCcrCalculation.CalculationStatus.COMPLETED);
            
            // Set required fields that have NOT NULL constraints in database
            calculation.setGrossMtm(BigDecimal.ZERO);
            calculation.setVmReceived(BigDecimal.ZERO);
            calculation.setVmPosted(BigDecimal.ZERO);
            calculation.setImReceived(BigDecimal.ZERO);
            calculation.setImPosted(BigDecimal.ZERO);
            calculation.setSupervisoryAddon(BigDecimal.ZERO);
            calculation.setMultiplier(BigDecimal.ONE);
            
            log.info("SA-CCR calculation completed for netting set {}: RC={}, PFE={}, EAD={}", 
                     nettingSet.getNettingSetId(), replacementCost, potentialFutureExposure, exposureAtDefault);
            
            return calculation;
            
        } catch (Exception e) {
            log.error("Error calculating SA-CCR exposure for netting set {}: {}", 
                      nettingSet.getNettingSetId(), e.getMessage(), e);
            throw new RuntimeException("SA-CCR calculation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate Replacement Cost (RC) according to Basel III SA-CCR
     * RC = max(0, V - C + max(0, 0))
     * where V = current market value of derivative transactions, C = current market value of collateral
     */
    private BigDecimal calculateReplacementCost(List<CDSTrade> trades, NettingSet nettingSet) {
        log.debug("Calculating replacement cost for {} trades in netting set {}", 
                  trades.size(), nettingSet.getNettingSetId());
        
        BigDecimal netMarketValue = BigDecimal.ZERO;
        
        for (CDSTrade trade : trades) {
            if (trade.getMarkToMarketValue() != null) {
                netMarketValue = netMarketValue.add(trade.getMarkToMarketValue());
            }
        }
        
        // For now, assume no collateral posted (C = 0)
        // In a full implementation, we would fetch collateral amounts from margin statements
        BigDecimal collateralValue = BigDecimal.ZERO;
        
        // RC = max(0, V - C)
        BigDecimal replacementCost = netMarketValue.subtract(collateralValue).max(BigDecimal.ZERO);
        
        log.debug("Replacement cost calculation: net market value={}, collateral={}, RC={}", 
                  netMarketValue, collateralValue, replacementCost);
        
        return replacementCost.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate Potential Future Exposure (PFE) according to Basel III SA-CCR
     * PFE = multiplier × AddOn
     * where AddOn is based on supervisory parameters and notional amounts
     */
    private BigDecimal calculatePotentialFutureExposure(List<CDSTrade> trades, NettingSet nettingSet, String jurisdiction) {
        log.debug("Calculating potential future exposure for {} trades in netting set {}", 
                  trades.size(), nettingSet.getNettingSetId());
        
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Calculate effective notional for credit derivatives
        BigDecimal effectiveNotional = BigDecimal.ZERO;
        BigDecimal totalNotional = BigDecimal.ZERO;
        
        for (CDSTrade trade : trades) {
            BigDecimal notional = trade.getNotionalAmount();
            totalNotional = totalNotional.add(notional);
            
            // Apply supervisory factor based on trade characteristics
            BigDecimal supervisoryFactor = getSupervisoryFactor(trade, jurisdiction);
            BigDecimal adjustedNotional = notional.multiply(supervisoryFactor);
            
            effectiveNotional = effectiveNotional.add(adjustedNotional);
        }
        
        // Calculate multiplier (simplified version)
        BigDecimal multiplier = calculateMultiplier(nettingSet, trades);
        
        // Calculate Add-On
        BigDecimal addOn = effectiveNotional.multiply(multiplier);
        
        // PFE = multiplier × AddOn (simplified - in full implementation would aggregate across asset classes)
        BigDecimal pfe = addOn.setScale(2, RoundingMode.HALF_UP);
        
        log.debug("PFE calculation: effective notional={}, multiplier={}, PFE={}", 
                  effectiveNotional, multiplier, pfe);
        
        return pfe;
    }
    
    /**
     * Get supervisory factor for a CDS trade based on jurisdiction-specific parameters
     */
    private BigDecimal getSupervisoryFactor(CDSTrade trade, String jurisdiction) {
        try {
            // Determine if this is investment grade or high yield based on spread
            // This is a simplified heuristic - in practice would use credit ratings
            boolean isInvestmentGrade = trade.getSpread().compareTo(new BigDecimal("500")) <= 0; // 500 bps threshold
            
            String creditQuality = isInvestmentGrade ? "IG" : "HY";
            
            // Query database for jurisdiction-specific supervisory factor for Credit asset class
            LocalDate asOfDate = LocalDate.now();
            Optional<SaCcrSupervisoryParameter> paramOpt = supervisoryParameterRepository
                .findByJurisdictionAndAssetClassAndParameterTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    jurisdiction, "CREDIT", "SUPERVISORY_FACTOR", asOfDate)
                .stream()
                .filter(p -> creditQuality.equals(p.getCreditQuality()))
                .filter(p -> p.isEffective(asOfDate))
                .findFirst();
            
            if (paramOpt.isPresent()) {
                BigDecimal factor = paramOpt.get().getParameterValue();
                log.debug("Using jurisdiction-specific supervisory factor for {}-{}: {} = {}", 
                         jurisdiction, creditQuality, trade.getId(), factor);
                return factor;
            }
            
            // Fallback to standard Basel III factors if not found in database
            log.warn("No jurisdiction-specific supervisory factor found for {}-{}, using standard Basel III factor", 
                    jurisdiction, creditQuality);
            return isInvestmentGrade ? new BigDecimal("0.0050") : new BigDecimal("0.0130");
                
        } catch (Exception e) {
            log.warn("Error determining supervisory factor for trade {}, using default: {}", 
                     trade.getId(), e.getMessage());
            return new BigDecimal("0.0050"); // Default 0.5% for IG credit
        }
    }
    
    /**
     * Calculate multiplier according to Basel III SA-CCR formula
     * Multiplier = min(1, Floor + (1 - Floor) × exp(V / (2 × AddOn)))
     * where Floor = 0.05, V = net replacement cost
     */
    private BigDecimal calculateMultiplier(NettingSet nettingSet, List<CDSTrade> trades) {
        // Simplified multiplier calculation
        // In practice, this involves complex aggregation across asset classes
        
        if (!nettingSet.getCollateralAgreement()) {
            // No collateral agreement - multiplier = 1
            return BigDecimal.ONE;
        }
        
        // For collateralized netting sets, apply simplified multiplier
        // This is a placeholder - full implementation would be more complex
        return new BigDecimal("0.75"); // Simplified multiplier for collateralized exposures
    }
    
    /**
     * Determine alpha factor based on jurisdiction and netting set characteristics
     */
    private BigDecimal determineAlphaFactor(NettingSet nettingSet, String jurisdiction) {
        try {
            // Query database for jurisdiction-specific alpha factor
            LocalDate asOfDate = LocalDate.now();
            Optional<SaCcrSupervisoryParameter> paramOpt = supervisoryParameterRepository
                .findTopByJurisdictionAndParameterTypeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                    jurisdiction, "ALPHA_FACTOR", asOfDate);
            
            if (paramOpt.isPresent() && paramOpt.get().isEffective(asOfDate)) {
                BigDecimal alpha = paramOpt.get().getParameterValue();
                log.debug("Using jurisdiction-specific alpha factor for {}: {}", jurisdiction, alpha);
                return alpha;
            }
            
            // Fallback to standard Basel III alpha = 1.4
            log.warn("No jurisdiction-specific alpha factor found for {}, using standard Basel III alpha = 1.4", jurisdiction);
            return new BigDecimal("1.4");
            
        } catch (Exception e) {
            log.warn("Error determining alpha factor for jurisdiction {}, using default: {}", jurisdiction, e.getMessage());
            return new BigDecimal("1.4");
        }
    }

    public List<SaCcrCalculation> getAllCalculations() {
        return calculationRepository.findAll();
    }
}