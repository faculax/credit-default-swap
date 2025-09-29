package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.service.CDSTradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cds-trades")
public class CDSTradeController {
    
    private final CDSTradeService cdsTradeService;
    
    @Autowired
    public CDSTradeController(CDSTradeService cdsTradeService) {
        this.cdsTradeService = cdsTradeService;
    }
    
    /**
     * POST /api/cds-trades - Create a new CDS trade
     */
    @PostMapping
    public ResponseEntity<CDSTrade> createTrade(@RequestBody CDSTrade trade) {
        try {
            CDSTrade savedTrade = cdsTradeService.saveTrade(trade);
            return new ResponseEntity<>(savedTrade, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET /api/cds-trades - Get all CDS trades
     */
    @GetMapping
    public ResponseEntity<List<CDSTrade>> getAllTrades() {
        try {
            List<CDSTrade> trades = cdsTradeService.getAllTrades();
            return new ResponseEntity<>(trades, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET /api/cds-trades/{id} - Get a specific trade by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CDSTrade> getTradeById(@PathVariable Long id) {
        Optional<CDSTrade> trade = cdsTradeService.getTradeById(id);
        
        if (trade.isPresent()) {
            return new ResponseEntity<>(trade.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * GET /api/cds-trades/by-reference-entity/{referenceEntity} - Get trades by reference entity
     */
    @GetMapping("/by-reference-entity/{referenceEntity}")
    public ResponseEntity<List<CDSTrade>> getTradesByReferenceEntity(@PathVariable String referenceEntity) {
        try {
            List<CDSTrade> trades = cdsTradeService.getTradesByReferenceEntity(referenceEntity);
            return new ResponseEntity<>(trades, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET /api/cds-trades/by-counterparty/{counterparty} - Get trades by counterparty
     */
    @GetMapping("/by-counterparty/{counterparty}")
    public ResponseEntity<List<CDSTrade>> getTradesByCounterparty(@PathVariable String counterparty) {
        try {
            List<CDSTrade> trades = cdsTradeService.getTradesByCounterparty(counterparty);
            return new ResponseEntity<>(trades, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET /api/cds-trades/by-status/{status} - Get trades by status
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<CDSTrade>> getTradesByStatus(@PathVariable String status) {
        try {
            List<CDSTrade> trades = cdsTradeService.getTradesByStatus(status);
            return new ResponseEntity<>(trades, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * PUT /api/cds-trades/{id} - Update an existing trade
     */
    @PutMapping("/{id}")
    public ResponseEntity<CDSTrade> updateTrade(@PathVariable Long id, @RequestBody CDSTrade trade) {
        Optional<CDSTrade> existingTrade = cdsTradeService.getTradeById(id);
        
        if (existingTrade.isPresent()) {
            trade.setId(id); // Ensure the ID is set correctly
            CDSTrade updatedTrade = cdsTradeService.updateTrade(trade);
            return new ResponseEntity<>(updatedTrade, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * DELETE /api/cds-trades/{id} - Delete a trade by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteTrade(@PathVariable Long id) {
        try {
            Optional<CDSTrade> trade = cdsTradeService.getTradeById(id);
            if (trade.isPresent()) {
                cdsTradeService.deleteTrade(id);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * DELETE /api/cds-trades - Delete all trades (for testing/demo)
     */
    @DeleteMapping
    public ResponseEntity<HttpStatus> deleteAllTrades() {
        try {
            cdsTradeService.deleteAllTrades();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * GET /api/cds-trades/count - Get total number of trades
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTradeCount() {
        try {
            long count = cdsTradeService.getTradeCount();
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}