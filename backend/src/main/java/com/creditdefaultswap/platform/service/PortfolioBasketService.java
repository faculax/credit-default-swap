package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.BasketDefinition;
import com.creditdefaultswap.platform.model.BasketPortfolioConstituent;
import com.creditdefaultswap.platform.model.CdsPortfolio;
import com.creditdefaultswap.platform.model.WeightType;
import com.creditdefaultswap.platform.repository.BasketDefinitionRepository;
import com.creditdefaultswap.platform.repository.BasketPortfolioConstituentRepository;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioBasketService {
    
    @Autowired
    private BasketPortfolioConstituentRepository basketConstituentRepository;
    
    @Autowired
    private CdsPortfolioRepository portfolioRepository;
    
    @Autowired
    private BasketDefinitionRepository basketRepository;
    
    /**
     * Attach a basket to a portfolio
     */
    @Transactional
    public BasketPortfolioConstituent attachBasket(Long portfolioId, Long basketId, WeightType weightType, BigDecimal weightValue) {
        // Validate portfolio exists
        CdsPortfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));
        
        // Validate basket exists
        BasketDefinition basket = basketRepository.findById(basketId)
                .orElseThrow(() -> new RuntimeException("Basket not found: " + basketId));
        
        // Check if basket already in portfolio
        if (basketConstituentRepository.findByPortfolioIdAndBasketId(portfolioId, basketId).isPresent()) {
            throw new RuntimeException("Basket already exists in portfolio");
        }
        
        // Create constituent
        BasketPortfolioConstituent constituent = new BasketPortfolioConstituent(
                portfolio, 
                basket, 
                weightType, 
                weightValue
        );
        
        return basketConstituentRepository.save(constituent);
    }
    
    /**
     * Remove a basket from a portfolio
     */
    @Transactional
    public void removeBasket(Long portfolioId, Long basketId) {
        BasketPortfolioConstituent constituent = basketConstituentRepository
                .findByPortfolioIdAndBasketId(portfolioId, basketId)
                .orElseThrow(() -> new RuntimeException("Basket not found in portfolio"));
        
        basketConstituentRepository.delete(constituent);
    }
    
    /**
     * Get all baskets in a portfolio
     */
    public List<BasketPortfolioConstituent> getPortfolioBaskets(Long portfolioId) {
        return basketConstituentRepository.findByPortfolioIdAndActiveTrue(portfolioId);
    }
    
    /**
     * Update basket weight in portfolio
     */
    @Transactional
    public BasketPortfolioConstituent updateBasketWeight(Long portfolioId, Long basketId, BigDecimal newWeight) {
        BasketPortfolioConstituent constituent = basketConstituentRepository
                .findByPortfolioIdAndBasketId(portfolioId, basketId)
                .orElseThrow(() -> new RuntimeException("Basket not found in portfolio"));
        
        constituent.setWeightValue(newWeight);
        return basketConstituentRepository.save(constituent);
    }
    
    /**
     * Get active basket count in portfolio
     */
    public long getBasketCount(Long portfolioId) {
        return basketConstituentRepository.findByPortfolioIdAndActiveTrue(portfolioId).size();
    }
}
