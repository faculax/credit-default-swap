package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.BondPricingResponse;
import com.creditdefaultswap.platform.dto.BondRequest;
import com.creditdefaultswap.platform.model.Bond;
import com.creditdefaultswap.platform.repository.BondRepository;
import com.creditdefaultswap.platform.service.bond.BondPricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for Bond business logic and CRUD operations
 * Epic 14: Credit Bonds Enablement (Story 14.9)
 */
@Service
public class BondService {
    
    @Autowired
    private BondRepository bondRepository;
    
    @Autowired
    private BondValidator bondValidator;
    
    @Autowired
    private BondPricingService bondPricingService;
    
    /**
     * Create a new bond
     */
    @Transactional
    public Bond createBond(BondRequest request) {
        Bond bond = convertToEntity(request);
        
        // Validate
        List<String> errors = bondValidator.validate(bond);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Bond validation failed: " + String.join(", ", errors));
        }
        
        return bondRepository.save(bond);
    }
    
    /**
     * Get all bonds
     */
    public List<Bond> getAllBonds() {
        return bondRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Get bond by ID
     */
    public Optional<Bond> getBondById(Long id) {
        return bondRepository.findById(id);
    }
    
    /**
     * Update bond
     */
    @Transactional
    public Bond updateBond(Long id, BondRequest request) {
        Bond existingBond = bondRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bond not found: " + id));
        
        updateBondFromRequest(existingBond, request);
        
        // Validate
        List<String> errors = bondValidator.validate(existingBond);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Bond validation failed: " + String.join(", ", errors));
        }
        
        return bondRepository.save(existingBond);
    }
    
    /**
     * Price a bond
     * Story 14.10: Bond pricing endpoint
     */
    public BondPricingResponse priceBond(Long bondId, LocalDate valuationDate, Double discountRate, Double hazardRate) {
        Bond bond = bondRepository.findById(bondId)
            .orElseThrow(() -> new IllegalArgumentException("Bond not found: " + bondId));
        
        BondPricingResponse response = new BondPricingResponse();
        response.setBondId(bondId);
        response.setValuationDate(valuationDate);
        
        // Calculate pricing metrics
        double accrued = bondPricingService.calculateAccruedInterest(bond, valuationDate);
        double cleanPrice = bondPricingService.calculateCleanPrice(bond, valuationDate, discountRate);
        double dirtyPrice = bondPricingService.calculateDirtyPrice(bond, valuationDate, discountRate);
        double pv = bondPricingService.calculatePresentValue(bond, valuationDate, discountRate);
        
        response.setAccruedInterest(accrued);
        response.setCleanPrice(cleanPrice);
        response.setDirtyPrice(dirtyPrice);
        response.setPv(pv);
        
        // Calculate YTM and Z-spread (use dirty price as observed)
        double ytm = bondPricingService.calculateYieldToMaturity(bond, valuationDate, dirtyPrice, false);
        double zSpread = bondPricingService.calculateZSpread(bond, valuationDate, dirtyPrice, discountRate, false);
        
        response.setYieldToMaturity(ytm);
        response.setzSpread(zSpread);
        
        // Risky PV if hazard rate provided
        if (hazardRate != null && hazardRate > 0) {
            double riskyPv = bondPricingService.calculateRiskyPV(bond, valuationDate, discountRate, hazardRate);
            response.setPvRisky(riskyPv);
        }
        
        // Sensitivities
        BondPricingResponse.Sensitivities sensitivities = new BondPricingResponse.Sensitivities();
        sensitivities.setIrDv01(bondPricingService.calculateIRDV01(bond, valuationDate, discountRate));
        sensitivities.setModifiedDuration(bondPricingService.calculateModifiedDuration(bond, valuationDate, ytm));
        sensitivities.setJtd(bondPricingService.calculateJTD(bond, valuationDate, pv - bond.getNotional().doubleValue()));
        
        if (hazardRate != null && hazardRate > 0) {
            sensitivities.setSpreadDv01(bondPricingService.calculateSpreadDV01(bond, valuationDate, discountRate, hazardRate));
        }
        
        response.setSensitivities(sensitivities);
        
        // Input echo
        BondPricingResponse.Inputs inputs = new BondPricingResponse.Inputs();
        inputs.setCouponRate(bond.getCouponRate());
        inputs.setCouponFrequency(bond.getCouponFrequency().name());
        inputs.setDayCount(bond.getDayCount().name());
        
        response.setInputs(inputs);
        
        return response;
    }
    
    /**
     * Get bonds by issuer
     */
    public List<Bond> getBondsByIssuer(String issuer) {
        return bondRepository.findByIssuerOrderByCreatedAtDesc(issuer);
    }
    
    /**
     * Convert BondRequest to Bond entity
     */
    private Bond convertToEntity(BondRequest request) {
        Bond bond = new Bond();
        bond.setIsin(request.getIsin());
        bond.setIssuer(request.getIssuer());
        bond.setSeniority(request.getSeniority());
        bond.setSector(request.getSector());
        bond.setCurrency(request.getCurrency() != null ? request.getCurrency() : bond.getCurrency());
        bond.setNotional(request.getNotional());
        bond.setCouponRate(request.getCouponRate());
        bond.setCouponFrequency(request.getCouponFrequency() != null ? request.getCouponFrequency() : bond.getCouponFrequency());
        bond.setDayCount(request.getDayCount() != null ? request.getDayCount() : bond.getDayCount());
        bond.setIssueDate(request.getIssueDate());
        bond.setMaturityDate(request.getMaturityDate());
        bond.setSettlementDays(request.getSettlementDays() != null ? request.getSettlementDays() : bond.getSettlementDays());
        bond.setFaceValue(request.getFaceValue() != null ? request.getFaceValue() : bond.getFaceValue());
        bond.setPriceConvention(request.getPriceConvention() != null ? request.getPriceConvention() : bond.getPriceConvention());
        
        return bond;
    }
    
    /**
     * Update existing bond from request
     */
    private void updateBondFromRequest(Bond bond, BondRequest request) {
        if (request.getIsin() != null) bond.setIsin(request.getIsin());
        if (request.getIssuer() != null) bond.setIssuer(request.getIssuer());
        if (request.getSeniority() != null) bond.setSeniority(request.getSeniority());
        if (request.getSector() != null) bond.setSector(request.getSector());
        if (request.getCurrency() != null) bond.setCurrency(request.getCurrency());
        if (request.getNotional() != null) bond.setNotional(request.getNotional());
        if (request.getCouponRate() != null) bond.setCouponRate(request.getCouponRate());
        if (request.getCouponFrequency() != null) bond.setCouponFrequency(request.getCouponFrequency());
        if (request.getDayCount() != null) bond.setDayCount(request.getDayCount());
        if (request.getIssueDate() != null) bond.setIssueDate(request.getIssueDate());
        if (request.getMaturityDate() != null) bond.setMaturityDate(request.getMaturityDate());
        if (request.getSettlementDays() != null) bond.setSettlementDays(request.getSettlementDays());
        if (request.getFaceValue() != null) bond.setFaceValue(request.getFaceValue());
        if (request.getPriceConvention() != null) bond.setPriceConvention(request.getPriceConvention());
    }
}
