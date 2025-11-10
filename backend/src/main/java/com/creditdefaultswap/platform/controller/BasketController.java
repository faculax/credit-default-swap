package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.*;
import com.creditdefaultswap.platform.service.BasketPricingService;
import com.creditdefaultswap.platform.service.BasketService;
import com.creditdefaultswap.platform.service.LineageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Basket operations
 * Epic 15: Basket & Multi-Name Credit Derivatives
 */
@RestController
@RequestMapping("/api/baskets")
public class BasketController {
    
    @Autowired
    private BasketService basketService;
    
    @Autowired
    private BasketPricingService pricingService;
    
    @Autowired
    private LineageService lineageService;
    
    /**
     * POST /api/baskets - Create a new basket
     */
    @PostMapping
    public ResponseEntity<?> createBasket(@RequestBody BasketRequest request) {
        try {
            BasketResponse response = basketService.createBasket(request);
            
            // Track lineage
            Map<String, Object> basketDetails = new HashMap<>();
            basketDetails.put("basketId", response.getId());
            basketDetails.put("constituentCount", request.getConstituents() != null ? request.getConstituents().size() : 0);
            basketDetails.put("notional", request.getNotional() != null ? request.getNotional() : 0);
            
            lineageService.trackBasketOperation("CREATE", response.getId(), "system", basketDetails);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Error creating basket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * GET /api/baskets - Get all baskets
     */
    @GetMapping
    public ResponseEntity<List<BasketResponse>> getAllBaskets() {
        try {
            List<BasketResponse> baskets = basketService.getAllBaskets();
            return ResponseEntity.ok(baskets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/baskets/{id} - Get basket by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBasketById(@PathVariable Long id) {
        try {
            Optional<BasketResponse> basketOpt = basketService.getBasketById(id);
            if (basketOpt.isPresent()) {
                return ResponseEntity.ok(basketOpt.get());
            } else {
                ErrorResponse error = new ErrorResponse("NOT_FOUND", "Basket not found: " + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Error fetching basket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * PUT /api/baskets/{id} - Update basket (limited fields)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBasket(@PathVariable Long id, @RequestBody BasketRequest request) {
        try {
            BasketResponse response = basketService.updateBasket(id, request);
            
            // Track lineage
            Map<String, Object> basketDetails = new HashMap<>();
            basketDetails.put("basketId", response.getId());
            basketDetails.put("constituentCount", response.getConstituents() != null ? response.getConstituents().size() : 0);
            
            lineageService.trackBasketOperation("UPDATE", response.getId(), "system", basketDetails);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Error updating basket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * POST /api/baskets/{id}/price - Price a basket
     */
    @PostMapping("/{id}/price")
    public ResponseEntity<?> priceBasket(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(required = false) Integer paths,
            @RequestParam(required = false) Long seed,
            @RequestParam(required = false, defaultValue = "true") Boolean includeSensitivities,
            @RequestParam(required = false, defaultValue = "false") Boolean includeEtlTimeline) {
        
        try {
            BasketPricingRequest request = new BasketPricingRequest();
            request.setValuationDate(valuationDate);
            request.setPaths(paths);
            request.setSeed(seed);
            request.setIncludeSensitivities(includeSensitivities);
            request.setIncludeEtlTimeline(includeEtlTimeline);
            
            BasketPricingResponse response = pricingService.priceBasket(id, request);
            
            // Track pricing lineage
            lineageService.trackPricingCalculation("BASKET", id, "MONTE_CARLO", "system");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse("PRICING_ERROR", "Error pricing basket: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * POST /api/baskets/price/batch - Price multiple baskets (stub for batch pricing)
     */
    @PostMapping("/price/batch")
    public ResponseEntity<?> priceBatch(@RequestBody Map<String, Object> request) {
        // Stub implementation for batch pricing
        ErrorResponse error = new ErrorResponse("NOT_IMPLEMENTED", 
            "Batch pricing will be implemented in Story 15.10");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
    }
}
