package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.dto.AttachTradesRequest;
import com.creditdefaultswap.platform.dto.ConstituentRequest;
import com.creditdefaultswap.platform.model.*;
import com.creditdefaultswap.platform.repository.CdsPortfolioConstituentRepository;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CdsPortfolioService {
    
    private static final Logger logger = LoggerFactory.getLogger(CdsPortfolioService.class);
    private static final BigDecimal PERCENT_TOLERANCE = new BigDecimal("0.05"); // 5% tolerance for percent weights
    
    private final CdsPortfolioRepository portfolioRepository;
    private final CdsPortfolioConstituentRepository constituentRepository;
    private final CDSTradeRepository tradeRepository;
    
    @Autowired
    public CdsPortfolioService(
            CdsPortfolioRepository portfolioRepository,
            CdsPortfolioConstituentRepository constituentRepository,
            CDSTradeRepository tradeRepository) {
        this.portfolioRepository = portfolioRepository;
        this.constituentRepository = constituentRepository;
        this.tradeRepository = tradeRepository;
    }
    
    @Transactional
    public CdsPortfolio createPortfolio(String name, String description) {
        logger.info("Creating portfolio: {}", name);
        
        // Check if portfolio with same name already exists
        if (portfolioRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Portfolio with name '" + name + "' already exists");
        }
        
        CdsPortfolio portfolio = new CdsPortfolio(name, description);
        return portfolioRepository.save(portfolio);
    }
    
    @Transactional(readOnly = true)
    public List<CdsPortfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<CdsPortfolio> getPortfolioById(Long id) {
        return portfolioRepository.findById(id);
    }
    
    @Transactional
    public CdsPortfolio updatePortfolio(Long id, String name, String description) {
        logger.info("Updating portfolio: {}", id);
        
        CdsPortfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + id));
        
        // Check name uniqueness if changed
        if (!portfolio.getName().equalsIgnoreCase(name)) {
            if (portfolioRepository.existsByNameIgnoreCase(name)) {
                throw new IllegalArgumentException("Portfolio with name '" + name + "' already exists");
            }
            portfolio.setName(name);
        }
        
        portfolio.setDescription(description);
        return portfolioRepository.save(portfolio);
    }
    
    @Transactional
    public void deletePortfolio(Long id) {
        logger.info("Deleting portfolio: {}", id);
        portfolioRepository.deleteById(id);
    }
    
    @Transactional
    public List<CdsPortfolioConstituent> attachTrades(Long portfolioId, AttachTradesRequest request) {
        logger.info("Attaching {} trades to portfolio: {}", request.getTrades().size(), portfolioId);
        
        CdsPortfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found: " + portfolioId));
        
        // Validate weight consistency if using PERCENT
        validateWeights(request.getTrades());
        
        List<CdsPortfolioConstituent> constituents = new ArrayList<>();
        
        for (ConstituentRequest req : request.getTrades()) {
            // Check if trade exists
            CDSTrade trade = tradeRepository.findById(req.getTradeId())
                    .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + req.getTradeId()));
            
            // Check if already attached
            Optional<CdsPortfolioConstituent> existing = constituentRepository
                    .findByPortfolioIdAndTradeId(portfolioId, req.getTradeId());
            
            if (existing.isPresent()) {
                // Reactivate and update if exists
                CdsPortfolioConstituent constituent = existing.get();
                constituent.setActive(true);
                constituent.setWeightType(req.getWeightType());
                constituent.setWeightValue(req.getWeightValue());
                constituents.add(constituentRepository.save(constituent));
            } else {
                // Create new
                CdsPortfolioConstituent constituent = new CdsPortfolioConstituent(
                        portfolio, trade, req.getWeightType(), req.getWeightValue());
                constituents.add(constituentRepository.save(constituent));
            }
        }
        
        return constituents;
    }
    
    @Transactional
    public void detachConstituent(Long portfolioId, Long constituentId) {
        logger.info("Detaching constituent {} from portfolio {}", constituentId, portfolioId);
        
        CdsPortfolioConstituent constituent = constituentRepository.findById(constituentId)
                .orElseThrow(() -> new IllegalArgumentException("Constituent not found: " + constituentId));
        
        if (!constituent.getPortfolio().getId().equals(portfolioId)) {
            throw new IllegalArgumentException("Constituent does not belong to portfolio: " + portfolioId);
        }
        
        // Soft delete
        constituent.setActive(false);
        constituentRepository.save(constituent);
    }
    
    @Transactional(readOnly = true)
    public List<CdsPortfolioConstituent> getActiveConstituents(Long portfolioId) {
        return constituentRepository.findActiveByPortfolioId(portfolioId);
    }
    
    private void validateWeights(List<ConstituentRequest> trades) {
        // Check if any trade uses PERCENT weight type
        boolean hasPercent = trades.stream()
                .anyMatch(t -> t.getWeightType() == WeightType.PERCENT);
        
        if (hasPercent) {
            BigDecimal sumPercent = trades.stream()
                    .filter(t -> t.getWeightType() == WeightType.PERCENT)
                    .map(ConstituentRequest::getWeightValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal diff = sumPercent.subtract(BigDecimal.ONE).abs();
            if (diff.compareTo(PERCENT_TOLERANCE) > 0) {
                logger.warn("Percent weights sum to {} (expected 1.0 ± {})", sumPercent, PERCENT_TOLERANCE);
                throw new IllegalArgumentException(
                        "Percent weights must sum to 1.0 (±" + PERCENT_TOLERANCE + "), got: " + sumPercent);
            }
        }
    }
}
