package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CDSTradeService {
    
    private final CDSTradeRepository cdsTradeRepository;
    
    @Autowired
    public CDSTradeService(CDSTradeRepository cdsTradeRepository) {
        this.cdsTradeRepository = cdsTradeRepository;
    }
    
    /**
     * Save a new CDS trade
     */
    public CDSTrade saveTrade(CDSTrade trade) {
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