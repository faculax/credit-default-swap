package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.saccr.NettingSet;
import com.creditdefaultswap.platform.repository.saccr.NettingSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for automatically determining the appropriate netting set for a trade
 * based on counterparty, clearing status, and currency.
 */
@Service
public class NettingSetAssignmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(NettingSetAssignmentService.class);
    
    private final NettingSetRepository nettingSetRepository;
    
    @Autowired
    public NettingSetAssignmentService(NettingSetRepository nettingSetRepository) {
        this.nettingSetRepository = nettingSetRepository;
    }
    
    /**
     * Automatically determine the netting set for a trade based on:
     * - Whether it's cleared (CCP name present)
     * - Counterparty
     * - Currency (for cleared trades)
     * 
     * @param counterparty The counterparty for the trade
     * @param ccpName The CCP name (null if bilateral)
     * @param currency The trade currency
     * @param isCleared Whether the trade is cleared
     * @return The netting set ID, or null if none found
     */
    public String determineNettingSetId(String counterparty, String ccpName, String currency, Boolean isCleared) {
        
        // Determine if trade is cleared
        boolean cleared = (isCleared != null && isCleared) || (ccpName != null && !ccpName.trim().isEmpty());
        
        if (cleared) {
            // For cleared trades, determine by CCP + currency
            String nettingSetId = determineNettingSetForClearedTrade(ccpName, currency);
            if (nettingSetId != null) {
                logger.info("Auto-assigned cleared trade to netting set: {} (CCP: {}, Currency: {})", 
                    nettingSetId, ccpName, currency);
                return nettingSetId;
            }
        } else {
            // For bilateral trades, determine by counterparty
            String nettingSetId = determineNettingSetForBilateralTrade(counterparty);
            if (nettingSetId != null) {
                logger.info("Auto-assigned bilateral trade to netting set: {} (Counterparty: {})", 
                    nettingSetId, counterparty);
                return nettingSetId;
            }
        }
        
        logger.warn("Could not auto-assign netting set for counterparty: {}, CCP: {}, currency: {}, cleared: {}", 
            counterparty, ccpName, currency, cleared);
        return null;
    }
    
    /**
     * Determine netting set for bilateral trades based on counterparty.
     * Pattern: NS_<COUNTERPARTY>_001
     * 
     * @param counterparty The counterparty
     * @return The netting set ID, or null if not found
     */
    private String determineNettingSetForBilateralTrade(String counterparty) {
        if (counterparty == null || counterparty.trim().isEmpty()) {
            return null;
        }
        
        // Mapping of common counterparty values to netting set patterns
        String normalizedCounterparty = counterparty.trim().toUpperCase().replace(" ", "_");
        
        // Try direct lookup: NS_<COUNTERPARTY>_001
        String nettingSetId = "NS_" + normalizedCounterparty + "_001";
        if (nettingSetExists(nettingSetId)) {
            return nettingSetId;
        }
        
        // Try common abbreviations
        String abbreviatedId = tryCommonAbbreviations(normalizedCounterparty);
        if (abbreviatedId != null && nettingSetExists(abbreviatedId)) {
            return abbreviatedId;
        }
        
        // Try to find by querying the repository by counterparty (use first result)
        List<NettingSet> nettingSets = nettingSetRepository.findByCounterpartyIdOrderByCreatedAtDesc(normalizedCounterparty);
        if (!nettingSets.isEmpty()) {
            return nettingSets.get(0).getNettingSetId();
        }
        
        return null;
    }
    
    /**
     * Try common counterparty abbreviations
     */
    private String tryCommonAbbreviations(String counterparty) {
        // Map full names to abbreviations
        switch (counterparty) {
            case "JPMORGAN":
            case "JP_MORGAN":
            case "JPMORGAN_CHASE":
                return "NS_JPM_001";
            case "GOLDMAN":
            case "GOLDMAN_SACHS":
            case "GS":
                return "NS_GS_001";
            case "DEUTSCHE":
            case "DEUTSCHE_BANK":
            case "DB":
                return "NS_DB_001";
            case "CREDIT_SUISSE":
            case "CS":
                return "NS_CS_001";
            case "MORGAN":
            case "MORGAN_STANLEY":
            case "MS":
                return "NS_MS_001";
            case "BOA":
            case "BOFA":
            case "BANK_OF_AMERICA":
                return "NS_BOA_001";
            case "CITI":
            case "CITIBANK":
            case "CITIGROUP":
                return "NS_CITI_001";
            case "BARCLAYS":
            case "BARC":
                return "NS_BARCLAYS_001";
            case "HSBC":
                return "NS_HSBC_001";
            case "UBS":
                return "NS_UBS_001";
            default:
                return null;
        }
    }
    
    /**
     * Determine netting set for cleared trades based on CCP and currency.
     * Pattern: <CCP>-HOUSE-001-<CURRENCY>
     * 
     * @param ccpName The CCP name
     * @param currency The trade currency
     * @return The netting set ID, or null if not found
     */
    private String determineNettingSetForClearedTrade(String ccpName, String currency) {
        if (ccpName == null || ccpName.trim().isEmpty()) {
            return null;
        }
        if (currency == null || currency.trim().isEmpty()) {
            return null;
        }
        
        String normalizedCcp = ccpName.trim().toUpperCase();
        String normalizedCurrency = currency.trim().toUpperCase();
        
        // Map CCP names to standard prefixes
        String ccpPrefix = mapCcpToPrefix(normalizedCcp);
        if (ccpPrefix == null) {
            return null;
        }
        
        // Build netting set ID: <CCP>-HOUSE-001-<CURRENCY>
        String nettingSetId = ccpPrefix + "-HOUSE-001-" + normalizedCurrency;
        
        if (nettingSetExists(nettingSetId)) {
            return nettingSetId;
        }
        
        return null;
    }
    
    /**
     * Map CCP name to standard prefix
     */
    private String mapCcpToPrefix(String ccpName) {
        switch (ccpName) {
            case "LCH":
            case "LCH.CLEARNET":
            case "LCH_CLEARNET":
            case "LCHCLEARNET":
                return "LCH";
            case "CME":
            case "CME_CLEARING":
            case "CMECLEARING":
                return "CME";
            case "ICE":
            case "ICE_CLEAR":
            case "ICE_CLEAR_CREDIT":
            case "ICECLEAR":
                return "ICE";
            case "EUREX":
            case "EUREX_CLEARING":
            case "EUREXCLEARING":
                return "EUREX";
            default:
                return null;
        }
    }
    
    /**
     * Check if a netting set exists
     */
    private boolean nettingSetExists(String nettingSetId) {
        return nettingSetRepository.findByNettingSetId(nettingSetId).isPresent();
    }
    
    /**
     * Validate that a manually specified netting set exists
     * 
     * @param nettingSetId The netting set ID to validate
     * @return true if exists, false otherwise
     */
    public boolean validateNettingSetExists(String nettingSetId) {
        if (nettingSetId == null || nettingSetId.trim().isEmpty()) {
            return false;
        }
        return nettingSetExists(nettingSetId);
    }
}
