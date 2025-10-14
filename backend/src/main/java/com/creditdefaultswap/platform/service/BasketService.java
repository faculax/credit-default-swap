package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.*;
import com.creditdefaultswap.platform.model.BasketConstituent;
import com.creditdefaultswap.platform.model.BasketDefinition;
import com.creditdefaultswap.platform.repository.BasketConstituentRepository;
import com.creditdefaultswap.platform.repository.BasketDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for basket CRUD operations
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Service
public class BasketService {
    
    @Autowired
    private BasketDefinitionRepository basketRepository;
    
    @Autowired
    private BasketConstituentRepository constituentRepository;
    
    @Autowired
    private BasketValidationService validationService;
    
    /**
     * Create a new basket
     */
    @Transactional
    public BasketResponse createBasket(BasketRequest request) {
        // Validate request
        Map<String, String> errors = validationService.validateBasketRequest(request);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Validation failed: " + errors);
        }
        
        // Normalize weights
        validationService.normalizeWeights(request.getConstituents());
        
        // Create basket definition
        BasketDefinition basket = new BasketDefinition();
        basket.setName(request.getName());
        basket.setType(request.getType());
        basket.setNth(request.getNth());
        basket.setAttachmentPoint(request.getAttachmentPoint());
        basket.setDetachmentPoint(request.getDetachmentPoint());
        basket.setPremiumFrequency(request.getPremiumFrequency());
        basket.setDayCount(request.getDayCount());
        basket.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        basket.setNotional(request.getNotional());
        basket.setMaturityDate(request.getMaturityDate());
        
        // Add constituents
        int sequence = 0;
        for (BasketConstituentRequest constReq : request.getConstituents()) {
            BasketConstituent constituent = new BasketConstituent();
            constituent.setIssuer(constReq.getIssuer());
            constituent.setWeight(constReq.getWeight());
            constituent.setRecoveryOverride(constReq.getRecoveryOverride());
            constituent.setSeniority(constReq.getSeniority());
            constituent.setSector(constReq.getSector());
            constituent.setSequenceOrder(sequence++);
            basket.addConstituent(constituent);
        }
        
        // Save basket
        BasketDefinition saved = basketRepository.save(basket);
        
        return toResponse(saved);
    }
    
    /**
     * Get basket by ID
     */
    @Transactional(readOnly = true)
    public Optional<BasketResponse> getBasketById(Long id) {
        return basketRepository.findByIdWithConstituents(id)
            .map(this::toResponse);
    }
    
    /**
     * Get all baskets
     */
    @Transactional(readOnly = true)
    public List<BasketResponse> getAllBaskets() {
        return basketRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Update basket (limited fields)
     */
    @Transactional
    public BasketResponse updateBasket(Long id, BasketRequest request) {
        BasketDefinition basket = basketRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Basket not found: " + id));
        
        // Update mutable fields
        if (request.getName() != null) {
            basket.setName(request.getName());
        }
        if (request.getPremiumFrequency() != null) {
            basket.setPremiumFrequency(request.getPremiumFrequency());
        }
        if (request.getDayCount() != null) {
            basket.setDayCount(request.getDayCount());
        }
        
        BasketDefinition updated = basketRepository.save(basket);
        return toResponse(updated);
    }
    
    /**
     * Convert entity to response DTO
     */
    private BasketResponse toResponse(BasketDefinition basket) {
        BasketResponse response = new BasketResponse();
        response.setId(basket.getId());
        response.setName(basket.getName());
        response.setType(basket.getType());
        response.setNth(basket.getNth());
        response.setAttachmentPoint(basket.getAttachmentPoint());
        response.setDetachmentPoint(basket.getDetachmentPoint());
        response.setPremiumFrequency(basket.getPremiumFrequency());
        response.setDayCount(basket.getDayCount());
        response.setCurrency(basket.getCurrency());
        response.setNotional(basket.getNotional());
        response.setMaturityDate(basket.getMaturityDate());
        response.setCreatedAt(basket.getCreatedAt());
        response.setUpdatedAt(basket.getUpdatedAt());
        response.setConstituentCount(basket.getConstituentCount());
        
        // Convert constituents
        List<BasketConstituentResponse> constituentResponses = basket.getConstituents().stream()
            .map(this::toConstituentResponse)
            .collect(Collectors.toList());
        response.setConstituents(constituentResponses);
        
        return response;
    }
    
    /**
     * Convert constituent entity to response DTO
     */
    private BasketConstituentResponse toConstituentResponse(BasketConstituent constituent) {
        BasketConstituentResponse response = new BasketConstituentResponse();
        response.setId(constituent.getId());
        response.setIssuer(constituent.getIssuer());
        response.setWeight(constituent.getWeight());
        response.setNormalizedWeight(constituent.getWeight()); // Already normalized
        response.setRecoveryOverride(constituent.getRecoveryOverride());
        response.setSeniority(constituent.getSeniority());
        response.setSector(constituent.getSector());
        
        // Set hazard curve ID (pattern: ISSUER_SENIORITY_CURRENCY)
        String curveId = constituent.getIssuer();
        if (constituent.getSeniority() != null) {
            curveId += "_" + constituent.getSeniority();
        }
        if (constituent.getBasket() != null && constituent.getBasket().getCurrency() != null) {
            curveId += "_" + constituent.getBasket().getCurrency();
        }
        response.setHazardCurveId(curveId);
        
        // Set effective recovery (use override if present, else default 0.40)
        BigDecimal effectiveRecovery = constituent.getRecoveryOverride() != null ? 
            constituent.getRecoveryOverride() : new BigDecimal("0.40");
        response.setEffectiveRecovery(effectiveRecovery);
        
        return response;
    }
}
