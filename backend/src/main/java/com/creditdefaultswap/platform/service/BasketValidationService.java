package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.BasketConstituentRequest;
import com.creditdefaultswap.platform.dto.BasketRequest;
import com.creditdefaultswap.platform.model.BasketType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Validation service for basket operations
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@Service
public class BasketValidationService {
    
    private static final BigDecimal WEIGHT_EPSILON = new BigDecimal("0.000000001");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    
    /**
     * Validate basket request and return normalized weights
     */
    public Map<String, String> validateBasketRequest(BasketRequest request) {
        Map<String, String> errors = new HashMap<>();
        
        // Basic validations
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.put("name", "Basket name is required");
        }
        
        if (request.getType() == null) {
            errors.put("type", "Basket type is required");
        }
        
        if (request.getNotional() == null || request.getNotional().compareTo(ZERO) <= 0) {
            errors.put("notional", "Notional must be positive");
        }
        
        if (request.getMaturityDate() == null) {
            errors.put("maturityDate", "Maturity date is required");
        }
        
        if (request.getPremiumFrequency() == null || request.getPremiumFrequency().trim().isEmpty()) {
            errors.put("premiumFrequency", "Premium frequency is required");
        }
        
        if (request.getDayCount() == null || request.getDayCount().trim().isEmpty()) {
            errors.put("dayCount", "Day count convention is required");
        }
        
        // Constituents validation
        if (request.getConstituents() == null || request.getConstituents().isEmpty()) {
            errors.put("constituents", "At least one constituent is required");
        } else if (request.getConstituents().size() < 2) {
            errors.put("constituents", "Basket must have at least 2 constituents");
        } else {
            // Check for duplicate issuers
            Set<String> issuers = new HashSet<>();
            for (BasketConstituentRequest constituent : request.getConstituents()) {
                if (constituent.getIssuer() == null || constituent.getIssuer().trim().isEmpty()) {
                    errors.put("constituents", "All constituents must have an issuer");
                    break;
                }
                if (!issuers.add(constituent.getIssuer())) {
                    errors.put("constituents", "Duplicate issuer found: " + constituent.getIssuer());
                    break;
                }
                
                // Validate weight if provided
                if (constituent.getWeight() != null && constituent.getWeight().compareTo(ZERO) < 0) {
                    errors.put("constituents", "Weight cannot be negative for issuer: " + constituent.getIssuer());
                    break;
                }
                
                // Validate recovery override if provided
                if (constituent.getRecoveryOverride() != null) {
                    if (constituent.getRecoveryOverride().compareTo(ZERO) < 0 || 
                        constituent.getRecoveryOverride().compareTo(ONE) > 0) {
                        errors.put("constituents", "Recovery override must be between 0 and 1 for issuer: " + constituent.getIssuer());
                        break;
                    }
                }
            }
        }
        
        // Type-specific validations
        if (request.getType() != null) {
            validateTypeSpecificFields(request, errors);
        }
        
        return errors;
    }
    
    /**
     * Validate type-specific fields
     */
    private void validateTypeSpecificFields(BasketRequest request, Map<String, String> errors) {
        int constituentCount = request.getConstituents() != null ? request.getConstituents().size() : 0;
        
        switch (request.getType()) {
            case NTH_TO_DEFAULT:
                if (request.getNth() == null) {
                    errors.put("nth", "N-th parameter is required for NTH_TO_DEFAULT type");
                } else if (request.getNth() < 1) {
                    errors.put("nth", "N-th parameter must be at least 1");
                } else if (request.getNth() > constituentCount) {
                    errors.put("nth", "N-th parameter (" + request.getNth() + ") exceeds number of constituents (" + constituentCount + ")");
                }
                break;
                
            case TRANCHETTE:
                if (request.getAttachmentPoint() == null) {
                    errors.put("attachmentPoint", "Attachment point is required for TRANCHETTE type");
                }
                if (request.getDetachmentPoint() == null) {
                    errors.put("detachmentPoint", "Detachment point is required for TRANCHETTE type");
                }
                
                if (request.getAttachmentPoint() != null && request.getDetachmentPoint() != null) {
                    if (request.getAttachmentPoint().compareTo(ZERO) < 0) {
                        errors.put("attachmentPoint", "Attachment point must be >= 0");
                    }
                    if (request.getDetachmentPoint().compareTo(ONE) > 0) {
                        errors.put("detachmentPoint", "Detachment point must be <= 1");
                    }
                    if (request.getAttachmentPoint().compareTo(request.getDetachmentPoint()) >= 0) {
                        errors.put("attachmentPoint", "Attachment point must be less than detachment point");
                    }
                }
                break;
                
            case FIRST_TO_DEFAULT:
                // No additional validations for FTD
                break;
        }
    }
    
    /**
     * Normalize constituent weights
     * If any weight is provided, normalize all to sum to 1.0
     * If no weights provided, assign equal weights
     */
    public void normalizeWeights(List<BasketConstituentRequest> constituents) {
        if (constituents == null || constituents.isEmpty()) {
            return;
        }
        
        // Check if any weight is provided
        boolean hasWeights = constituents.stream().anyMatch(c -> c.getWeight() != null);
        
        if (!hasWeights) {
            // Assign equal weights
            BigDecimal equalWeight = ONE.divide(new BigDecimal(constituents.size()), 10, RoundingMode.HALF_UP);
            for (BasketConstituentRequest constituent : constituents) {
                constituent.setWeight(equalWeight);
            }
        } else {
            // Calculate sum of provided weights (treat null as 0)
            BigDecimal sum = constituents.stream()
                .map(c -> c.getWeight() != null ? c.getWeight() : ZERO)
                .reduce(ZERO, BigDecimal::add);
            
            if (sum.compareTo(ZERO) == 0) {
                // All weights are null or zero, use equal weights
                BigDecimal equalWeight = ONE.divide(new BigDecimal(constituents.size()), 10, RoundingMode.HALF_UP);
                for (BasketConstituentRequest constituent : constituents) {
                    constituent.setWeight(equalWeight);
                }
            } else {
                // Normalize to sum to 1.0
                for (BasketConstituentRequest constituent : constituents) {
                    if (constituent.getWeight() == null) {
                        constituent.setWeight(ZERO);
                    }
                    BigDecimal normalized = constituent.getWeight().divide(sum, 10, RoundingMode.HALF_UP);
                    constituent.setWeight(normalized);
                }
            }
        }
    }
    
    /**
     * Validate pricing request
     */
    public Map<String, String> validatePricingRequest(Long basketId, Integer paths) {
        Map<String, String> errors = new HashMap<>();
        
        if (basketId == null) {
            errors.put("basketId", "Basket ID is required");
        }
        
        if (paths != null && paths < 1000) {
            errors.put("paths", "Minimum 1000 paths required for stable pricing");
        }
        
        return errors;
    }
}
