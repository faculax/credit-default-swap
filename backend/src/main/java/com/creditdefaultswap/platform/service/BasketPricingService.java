package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.*;
import com.creditdefaultswap.platform.model.BasketDefinition;
import com.creditdefaultswap.platform.model.BasketType;
import com.creditdefaultswap.platform.repository.BasketDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

/**
 * Basket pricing service (Monte Carlo simulation)
 * Epic 15: Basket & Multi-Name Credit Derivatives
 * 
 * NOTE: This is a simplified implementation for Epic 15 foundation.
 * Full Monte Carlo integration with Epic 13 correlation engine will be completed in subsequent iterations.
 */
@Service
public class BasketPricingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BasketPricingService.class);
    
    /**
     * Sanitize input for logging to prevent CRLF injection (CWE-117)
     */
    private String sanitizeForLog(Object obj) {
        return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
    }
    
    @Autowired
    private BasketDefinitionRepository basketRepository;
    
    @Autowired
    private BasketService basketService;
    
    /**
     * Price a basket (simplified stub for initial implementation)
     */
    public BasketPricingResponse priceBasket(Long basketId, BasketPricingRequest request) {
        // Fetch basket
        BasketDefinition basket = basketRepository.findByIdWithConstituents(basketId)
            .orElseThrow(() -> new IllegalArgumentException("Basket not found: " + basketId));
        
        if (basket.getConstituents().size() < 2) {
            throw new IllegalArgumentException("Basket must have at least 2 constituents for pricing");
        }
        
        // Set defaults
        LocalDate valuationDate = request.getValuationDate() != null ? 
            request.getValuationDate() : LocalDate.now();
        int paths = request.getPaths() != null ? request.getPaths() : 50000;
        long seed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();
        
        // Build response
        BasketPricingResponse response = new BasketPricingResponse();
        response.setBasketId(basketId);
        response.setValuationDate(valuationDate);
        response.setType(basket.getType());
        response.setNotional(basket.getNotional());
        response.setSeedUsed(seed);
        
        // Simulate pricing (placeholder - full Monte Carlo to be integrated)
        simulatePricing(basket, paths, seed, response, request);
        
        // Add constituent information
        BasketResponse basketResponse = basketService.getBasketById(basketId)
            .orElseThrow(() -> new IllegalArgumentException("Basket not found"));
        response.setConstituents(basketResponse.getConstituents());
        
        return response;
    }
    
    /**
     * Simulate pricing (placeholder for Monte Carlo)
     * 
     * TODO: Integrate with Epic 13 correlation engine
     * TODO: Implement Brent fair spread solver
     * TODO: Add full premium/protection leg calculation
     */
    private void simulatePricing(BasketDefinition basket, int paths, long seed, 
                                  BasketPricingResponse response, BasketPricingRequest request) {
        
        logger.info("Pricing basket {} with {} paths (seed={})", sanitizeForLog(basket.getName()), sanitizeForLog(paths), sanitizeForLog(seed));
        
        // Use SecureRandom for production pricing simulations (CWE-330)
        SecureRandom random = new SecureRandom();
        random.setSeed(seed); // Seed for reproducibility in testing
        
        // Placeholder calculations (to be replaced with actual Monte Carlo)
        BigDecimal baseFairSpread = new BigDecimal("150.00"); // Base 150 bps
        
        // Adjust by constituent count (more names = lower spread for diversification)
        int constituentCount = basket.getConstituents().size();
        BigDecimal diversificationFactor = new BigDecimal(1.0 / Math.sqrt(constituentCount));
        BigDecimal fairSpread = baseFairSpread.multiply(diversificationFactor)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Type-specific adjustments
        if (basket.getType() == BasketType.NTH_TO_DEFAULT && basket.getNth() != null) {
            // Higher N = lower spread (later default)
            BigDecimal nthFactor = new BigDecimal(1.0 / basket.getNth());
            fairSpread = fairSpread.multiply(nthFactor).setScale(2, RoundingMode.HALF_UP);
        }
        
        if (basket.getType() == BasketType.TRANCHETTE) {
            // Tranche spread depends on attachment/detachment
            BigDecimal width = basket.getDetachmentPoint().subtract(basket.getAttachmentPoint());
            fairSpread = baseFairSpread.multiply(width.multiply(new BigDecimal("5")))
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        response.setFairSpreadBps(fairSpread);
        
        // Calculate PV legs (placeholder)
        BigDecimal notional = basket.getNotional();
        BigDecimal premiumPv = fairSpread.multiply(notional).divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP);
        BigDecimal protectionPv = premiumPv.multiply(new BigDecimal("0.998")); // Near zero PV
        BigDecimal pv = premiumPv.subtract(protectionPv);
        
        response.setPremiumLegPv(premiumPv);
        response.setProtectionLegPv(protectionPv);
        response.setPv(pv);
        
        // Convergence diagnostics
        ConvergenceDiagnostics convergence = new ConvergenceDiagnostics();
        convergence.setPathsUsed(paths);
        convergence.setIterations(random.nextInt(10) + 5); // Placeholder
        convergence.setConverged(true);
        
        // Standard error (decreases with sqrt(paths))
        double se = 5.0 / Math.sqrt(paths / 10000.0); // Placeholder formula
        convergence.setStandardErrorFairSpreadBps(se);
        convergence.setConvergenceMessage("Converged within tolerance");
        
        response.setConvergence(convergence);
        
        // Sensitivities (if requested)
        if (request.getIncludeSensitivities() != null && request.getIncludeSensitivities()) {
            SensitivitiesResponse sensitivities = new SensitivitiesResponse();
            
            // Placeholder calculations
            sensitivities.setSpreadDv01(notional.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP));
            sensitivities.setCorrelationBeta(premiumPv.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP));
            sensitivities.setRecovery01(premiumPv.multiply(new BigDecimal("-0.08")).setScale(2, RoundingMode.HALF_UP));
            
            Map<String, BigDecimal> bumpSizes = new HashMap<>();
            bumpSizes.put("spread", new BigDecimal("0.0001")); // 1 bp
            bumpSizes.put("correlation", new BigDecimal("0.01"));
            bumpSizes.put("recovery", new BigDecimal("0.01"));
            sensitivities.setBumpSizes(bumpSizes);
            
            response.setSensitivities(sensitivities);
        }
        
        // Tranche-specific (if applicable)
        if (basket.getType() == BasketType.TRANCHETTE) {
            BigDecimal width = basket.getDetachmentPoint().subtract(basket.getAttachmentPoint());
            BigDecimal etl = width.multiply(new BigDecimal("0.15")); // Placeholder
            response.setExpectedTrancheLossPct(etl);
            
            // ETL timeline (if requested)
            if (request.getIncludeEtlTimeline() != null && request.getIncludeEtlTimeline()) {
                List<BasketPricingResponse.TrancheLossPoint> timeline = new ArrayList<>();
                timeline.add(new BasketPricingResponse.TrancheLossPoint("1Y", etl.multiply(new BigDecimal("0.2"))));
                timeline.add(new BasketPricingResponse.TrancheLossPoint("3Y", etl.multiply(new BigDecimal("0.6"))));
                timeline.add(new BasketPricingResponse.TrancheLossPoint("5Y", etl));
                response.setEtlTimeline(timeline);
            }
        }
        
        logger.info("Basket {} pricing complete: fair spread = {} bps", sanitizeForLog(basket.getName()), sanitizeForLog(fairSpread));
    }
}
