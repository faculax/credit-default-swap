package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CDSTradeService {
    
    private static final Logger logger = LoggerFactory.getLogger(CDSTradeService.class);
    
    private final CDSTradeRepository cdsTradeRepository;
    private final NettingSetAssignmentService nettingSetAssignmentService;
    
    @Autowired
    public CDSTradeService(CDSTradeRepository cdsTradeRepository,
                          NettingSetAssignmentService nettingSetAssignmentService) {
        this.cdsTradeRepository = cdsTradeRepository;
        this.nettingSetAssignmentService = nettingSetAssignmentService;
    }
    
    /**
     * Save a new CDS trade with automatic netting set assignment
     */
    public CDSTrade saveTrade(CDSTrade trade) {
        // Auto-assign netting set if not already specified
        if (trade.getNettingSetId() == null || trade.getNettingSetId().trim().isEmpty()) {
            String autoAssignedNettingSet = nettingSetAssignmentService.determineNettingSetId(
                trade.getCounterparty(),
                trade.getCcpName(),
                trade.getCurrency(),
                trade.getIsCleared()
            );
            
            if (autoAssignedNettingSet != null) {
                trade.setNettingSetId(autoAssignedNettingSet);
                logger.info("Auto-assigned trade to netting set: {} (Counterparty: {}, CCP: {}, Currency: {})", 
                    autoAssignedNettingSet, trade.getCounterparty(), trade.getCcpName(), trade.getCurrency());
            } else {
                logger.warn("Could not auto-assign netting set for trade with counterparty: {}, CCP: {}, currency: {}",
                    trade.getCounterparty(), trade.getCcpName(), trade.getCurrency());
            }
        } else {
            // Validate manually specified netting set exists
            if (!nettingSetAssignmentService.validateNettingSetExists(trade.getNettingSetId())) {
                logger.error("Manually specified netting set does not exist: {}", trade.getNettingSetId());
                throw new IllegalArgumentException("Netting set does not exist: " + trade.getNettingSetId());
            }
            logger.info("Using manually specified netting set: {}", trade.getNettingSetId());
        }
        
        return cdsTradeRepository.save(trade);
    }
    
    /**
     * Get all CDS trades ordered by creation date (newest first)
     */
    public List<CDSTrade> getAllTrades() {
        return cdsTradeRepository.findAllByOrderByCreatedAtDesc();
    }
    
    /**
     * Get a specific trade by ID
     */
    public Optional<CDSTrade> getTradeById(Long id) {
        return cdsTradeRepository.findById(id);
    }
    
    /**
     * Get trades by reference entity
     */
    public List<CDSTrade> getTradesByReferenceEntity(String referenceEntity) {
        return cdsTradeRepository.findByReferenceEntityOrderByCreatedAtDesc(referenceEntity);
    }
    
    /**
     * Get trades by counterparty
     */
    public List<CDSTrade> getTradesByCounterparty(String counterparty) {
        return cdsTradeRepository.findByCounterpartyOrderByCreatedAtDesc(counterparty);
    }
    
    /**
     * Get trades by status
     */
    public List<CDSTrade> getTradesByStatus(String status) {
        return cdsTradeRepository.findByTradeStatusOrderByCreatedAtDesc(status);
    }
    
    /**
     * Update an existing trade
     */
    public CDSTrade updateTrade(CDSTrade trade) {
        return cdsTradeRepository.save(trade);
    }
    
    /**
     * Delete a trade by ID
     */
    public void deleteTrade(Long id) {
        cdsTradeRepository.deleteById(id);
    }
    
    /**
     * Delete all trades (for testing/demo purposes)
     */
    public void deleteAllTrades() {
        cdsTradeRepository.deleteAll();
    }
    
    /**
     * Get trade count
     */
    public long getTradeCount() {
        return cdsTradeRepository.count();
    }
}