package com.creditdefaultswap.platform.service;

import com.creditdefaultswap.platform.model.Bond;
import com.creditdefaultswap.platform.model.BondPortfolioConstituent;
import com.creditdefaultswap.platform.model.CdsPortfolio;
import com.creditdefaultswap.platform.model.WeightType;
import com.creditdefaultswap.platform.repository.BondPortfolioConstituentRepository;
import com.creditdefaultswap.platform.repository.BondRepository;
import com.creditdefaultswap.platform.repository.CdsPortfolioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioBondService {
    
    @Autowired
    private BondPortfolioConstituentRepository bondConstituentRepository;
    
    @Autowired
    private CdsPortfolioRepository portfolioRepository;
    
    @Autowired
    private BondRepository bondRepository;
    
    /**
     * Attach a bond to a portfolio
     */
    @Transactional
    public BondPortfolioConstituent attachBond(Long portfolioId, Long bondId, WeightType weightType, BigDecimal weightValue) {
        // Validate portfolio exists
        CdsPortfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + portfolioId));
        
        // Validate bond exists
        Bond bond = bondRepository.findById(bondId)
                .orElseThrow(() -> new RuntimeException("Bond not found: " + bondId));
        
        // Check if bond already in portfolio
        if (bondConstituentRepository.findByPortfolioIdAndBondId(portfolioId, bondId).isPresent()) {
            throw new RuntimeException("Bond already exists in portfolio");
        }
        
        // Create constituent
        BondPortfolioConstituent constituent = new BondPortfolioConstituent(
                portfolio, 
                bond, 
                weightType, 
                weightValue
        );
        
        return bondConstituentRepository.save(constituent);
    }
    
    /**
     * Remove a bond from a portfolio
     */
    @Transactional
    public void removeBond(Long portfolioId, Long bondId) {
        BondPortfolioConstituent constituent = bondConstituentRepository
                .findByPortfolioIdAndBondId(portfolioId, bondId)
                .orElseThrow(() -> new RuntimeException("Bond not found in portfolio"));
        
        bondConstituentRepository.delete(constituent);
    }
    
    /**
     * Get all bonds in a portfolio
     */
    public List<BondPortfolioConstituent> getPortfolioBonds(Long portfolioId) {
        return bondConstituentRepository.findByPortfolioIdAndActiveTrue(portfolioId);
    }
    
    /**
     * Update bond weight in portfolio
     */
    @Transactional
    public BondPortfolioConstituent updateBondWeight(Long portfolioId, Long bondId, BigDecimal newWeight) {
        BondPortfolioConstituent constituent = bondConstituentRepository
                .findByPortfolioIdAndBondId(portfolioId, bondId)
                .orElseThrow(() -> new RuntimeException("Bond not found in portfolio"));
        
        constituent.setWeightValue(newWeight);
        return bondConstituentRepository.save(constituent);
    }
    
    /**
     * Get active bond count in portfolio
     */
    public long getBondCount(Long portfolioId) {
        return bondConstituentRepository.findByPortfolioIdAndActiveTrue(portfolioId).size();
    }
}
