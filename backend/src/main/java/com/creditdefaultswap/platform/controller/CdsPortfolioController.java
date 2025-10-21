package com.creditdefaultswap.platform.controller;

import com.creditdefaultswap.platform.dto.AttachTradesRequest;
import com.creditdefaultswap.platform.dto.PortfolioPricingResponse;
import com.creditdefaultswap.platform.model.BondPortfolioConstituent;
import com.creditdefaultswap.platform.model.BasketPortfolioConstituent;
import com.creditdefaultswap.platform.model.CdsPortfolio;
import com.creditdefaultswap.platform.model.CdsPortfolioConstituent;
import com.creditdefaultswap.platform.model.WeightType;
import com.creditdefaultswap.platform.service.CdsPortfolioService;
import com.creditdefaultswap.platform.service.PortfolioBondService;
import com.creditdefaultswap.platform.service.PortfolioBasketService;
import com.creditdefaultswap.platform.service.PortfolioPricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cds-portfolios")
public class CdsPortfolioController {
    
    private static final Logger logger = LoggerFactory.getLogger(CdsPortfolioController.class);
    
    /**
     * Sanitize input for logging to prevent CRLF injection (CWE-117)
     */
    private String sanitizeForLog(Object obj) {
        return obj == null ? "null" : obj.toString().replaceAll("[\r\n]", "_");
    }
    
    private final CdsPortfolioService portfolioService;
    private final PortfolioPricingService pricingService;
    private final PortfolioBondService bondService;
    private final PortfolioBasketService basketService;
    
    @Autowired
    public CdsPortfolioController(
            CdsPortfolioService portfolioService,
            PortfolioPricingService pricingService,
            PortfolioBondService bondService,
            PortfolioBasketService basketService) {
        this.portfolioService = portfolioService;
        this.pricingService = pricingService;
        this.bondService = bondService;
        this.basketService = basketService;
    }
    
    /**
     * POST /api/cds-portfolios - Create a new portfolio
     */
    @PostMapping
    public ResponseEntity<?> createPortfolio(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Portfolio name is required"));
            }
            
            CdsPortfolio portfolio = portfolioService.createPortfolio(name, description);
            return new ResponseEntity<>(portfolio, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            String correlationId = UUID.randomUUID().toString();
            logger.error("Error creating portfolio [{}]", correlationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create portfolio. Reference: " + correlationId));
        }
    }
    
    /**
     * GET /api/cds-portfolios - Get all portfolios
     */
    @GetMapping
    public ResponseEntity<List<CdsPortfolio>> getAllPortfolios() {
        try {
            List<CdsPortfolio> portfolios = portfolioService.getAllPortfolios();
            return ResponseEntity.ok(portfolios);
        } catch (Exception e) {
            logger.error("Error fetching portfolios", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/cds-portfolios/{id} - Get a specific portfolio
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPortfolio(@PathVariable Long id) {
        try {
            return portfolioService.getPortfolioById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            String correlationId = UUID.randomUUID().toString();
            logger.error("Error fetching portfolio {} [{}]", sanitizeForLog(id), correlationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch portfolio. Reference: " + correlationId));
        }
    }
    
    /**
     * PUT /api/cds-portfolios/{id} - Update a portfolio
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePortfolio(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Portfolio name is required"));
            }
            
            CdsPortfolio portfolio = portfolioService.updatePortfolio(id, name, description);
            return ResponseEntity.ok(portfolio);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update portfolio"));
        }
    }
    
    /**
     * DELETE /api/cds-portfolios/{id} - Delete a portfolio
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePortfolio(@PathVariable Long id) {
        try {
            portfolioService.deletePortfolio(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete portfolio"));
        }
    }
    
    /**
     * POST /api/cds-portfolios/{id}/constituents - Attach trades to portfolio
     */
    @PostMapping("/{id}/constituents")
    public ResponseEntity<?> attachTrades(
            @PathVariable Long id,
            @RequestBody AttachTradesRequest request) {
        try {
            List<CdsPortfolioConstituent> constituents = portfolioService.attachTrades(id, request);
            return new ResponseEntity<>(constituents, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error attaching trades to portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to attach trades"));
        }
    }
    
    /**
     * DELETE /api/cds-portfolios/{id}/constituents/{constituentId} - Detach a constituent
     */
    @DeleteMapping("/{id}/constituents/{constituentId}")
    public ResponseEntity<?> detachConstituent(
            @PathVariable Long id,
            @PathVariable Long constituentId) {
        try {
            portfolioService.detachConstituent(id, constituentId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error detaching constituent {} from portfolio {}", sanitizeForLog(constituentId), sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to detach constituent"));
        }
    }
    
    /**
     * GET /api/cds-portfolios/{id}/constituents - Get active constituents
     */
    @GetMapping("/{id}/constituents")
    public ResponseEntity<?> getConstituents(@PathVariable Long id) {
        try {
            List<CdsPortfolioConstituent> constituents = portfolioService.getActiveConstituents(id);
            return ResponseEntity.ok(constituents);
        } catch (Exception e) {
            logger.error("Error fetching constituents for portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/cds-portfolios/{id}/price - Price portfolio for a valuation date
     */
    @PostMapping("/{id}/price")
    public ResponseEntity<?> pricePortfolio(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate) {
        try {
            PortfolioPricingResponse response = pricingService.pricePortfolio(id, valuationDate);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error pricing portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to price portfolio: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/cds-portfolios/{id}/risk-summary - Get cached risk summary
     */
    @GetMapping("/{id}/risk-summary")
    public ResponseEntity<?> getRiskSummary(@PathVariable Long id) {
        try {
            return pricingService.getCachedRiskSummary(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching risk summary for portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/cds-portfolios/{id}/bonds - Attach a bond to portfolio
     */
    @PostMapping("/{id}/bonds")
    public ResponseEntity<?> attachBond(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Long bondId = Long.valueOf(request.get("bondId").toString());
            String weightTypeStr = request.getOrDefault("weightType", "NOTIONAL").toString();
            WeightType weightType = WeightType.valueOf(weightTypeStr);
            
            // Parse weight value, default to 1.0
            Object weightValueObj = request.get("weightValue");
            java.math.BigDecimal weightValue = weightValueObj != null 
                    ? new java.math.BigDecimal(weightValueObj.toString()) 
                    : java.math.BigDecimal.ONE;
            
            BondPortfolioConstituent constituent = bondService.attachBond(id, bondId, weightType, weightValue);
            return new ResponseEntity<>(constituent, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error attaching bond to portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to attach bond: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/cds-portfolios/{id}/bonds/{bondId} - Remove a bond from portfolio
     */
    @DeleteMapping("/{id}/bonds/{bondId}")
    public ResponseEntity<?> removeBond(
            @PathVariable Long id,
            @PathVariable Long bondId) {
        try {
            bondService.removeBond(id, bondId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error removing bond {} from portfolio {}", sanitizeForLog(bondId), sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove bond: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/cds-portfolios/{id}/bonds - Get all bonds in portfolio
     */
    @GetMapping("/{id}/bonds")
    public ResponseEntity<?> getPortfolioBonds(@PathVariable Long id) {
        try {
            List<BondPortfolioConstituent> bonds = bondService.getPortfolioBonds(id);
            return ResponseEntity.ok(bonds);
        } catch (Exception e) {
            logger.error("Error fetching bonds for portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/cds-portfolios/{id}/baskets - Attach a basket to portfolio
     */
    @PostMapping("/{id}/baskets")
    public ResponseEntity<?> attachBasket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Long basketId = Long.valueOf(request.get("basketId").toString());
            String weightTypeStr = request.getOrDefault("weightType", "NOTIONAL").toString();
            WeightType weightType = WeightType.valueOf(weightTypeStr);
            
            // Parse weight value, default to 1.0
            Object weightValueObj = request.get("weightValue");
            java.math.BigDecimal weightValue = weightValueObj != null 
                    ? new java.math.BigDecimal(weightValueObj.toString()) 
                    : java.math.BigDecimal.ONE;
            
            BasketPortfolioConstituent constituent = basketService.attachBasket(id, basketId, weightType, weightValue);
            return new ResponseEntity<>(constituent, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error attaching basket to portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to attach basket: " + e.getMessage()));
        }
    }
    
    /**
     * DELETE /api/cds-portfolios/{id}/baskets/{basketId} - Remove a basket from portfolio
     */
    @DeleteMapping("/{id}/baskets/{basketId}")
    public ResponseEntity<?> removeBasket(
            @PathVariable Long id,
            @PathVariable Long basketId) {
        try {
            basketService.removeBasket(id, basketId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error removing basket {} from portfolio {}", sanitizeForLog(basketId), sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove basket: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/cds-portfolios/{id}/baskets - Get all baskets in portfolio
     */
    @GetMapping("/{id}/baskets")
    public ResponseEntity<?> getPortfolioBaskets(@PathVariable Long id) {
        try {
            List<BasketPortfolioConstituent> baskets = basketService.getPortfolioBaskets(id);
            return ResponseEntity.ok(baskets);
        } catch (Exception e) {
            logger.error("Error fetching baskets for portfolio {}", sanitizeForLog(id), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
