package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.BondPricingResponse;
import com.creditdefaultswap.platform.dto.BondRequest;
import com.creditdefaultswap.platform.model.Bond;
import com.creditdefaultswap.platform.service.BondService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Bond operations
 * Epic 14: Credit Bonds Enablement (Stories 14.9, 14.10)
 */
@RestController
@RequestMapping("/api/bonds")
public class BondController {
    
    @Autowired
    private BondService bondService;
    
    /**
     * POST /api/bonds - Create a new bond
     */
    @PostMapping
    public ResponseEntity<?> createBond(@RequestBody BondRequest request) {
        try {
            Bond bond = bondService.createBond(request);
            return new ResponseEntity<>(bond, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating bond: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/bonds - Get all bonds
     */
    @GetMapping
    public ResponseEntity<List<Bond>> getAllBonds() {
        try {
            List<Bond> bonds = bondService.getAllBonds();
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/bonds/{id} - Get bond by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBondById(@PathVariable Long id) {
        try {
            return bondService.getBondById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching bond: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/bonds/{id} - Update bond
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBond(@PathVariable Long id, @RequestBody BondRequest request) {
        try {
            Bond bond = bondService.updateBond(id, request);
            return ResponseEntity.ok(bond);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating bond: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/bonds/{id}/price - Price a bond
     * Query params:
     * - valuationDate (optional, defaults to today)
     * - discountRate (optional, defaults to 0.05)
     * - hazardRate (optional, defaults to 0)
     */
    @PostMapping("/{id}/price")
    public ResponseEntity<?> priceBond(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(required = false, defaultValue = "0.05") Double discountRate,
            @RequestParam(required = false, defaultValue = "0.0") Double hazardRate) {
        
        try {
            LocalDate valDate = valuationDate != null ? valuationDate : LocalDate.now();
            BondPricingResponse response = bondService.priceBond(id, valDate, discountRate, hazardRate);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error pricing bond: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/bonds/issuer/{issuer} - Get bonds by issuer
     */
    @GetMapping("/issuer/{issuer}")
    public ResponseEntity<List<Bond>> getBondsByIssuer(@PathVariable String issuer) {
        try {
            List<Bond> bonds = bondService.getBondsByIssuer(issuer);
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/bonds/price/batch - Batch pricing (skeleton for Story 14.15)
     */
    @PostMapping("/price/batch")
    public ResponseEntity<?> priceBondsBatch(@RequestBody List<Long> bondIds) {
        // Placeholder for future implementation
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body("Batch pricing not yet implemented");
    }
}
