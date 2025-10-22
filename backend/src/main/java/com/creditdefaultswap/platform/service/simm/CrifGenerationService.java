package com.creditdefaultswap.platform.service.simm;

import com.creditdefaultswap.platform.model.CDSTrade;
import com.creditdefaultswap.platform.model.TradeStatus;
import com.creditdefaultswap.platform.model.simm.CrifSensitivity;
import com.creditdefaultswap.platform.model.simm.CrifUpload;
import com.creditdefaultswap.platform.repository.CDSTradeRepository;
import com.creditdefaultswap.platform.repository.CrifSensitivityRepository;
import com.creditdefaultswap.platform.repository.CrifUploadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Service to auto-generate CRIF sensitivities from CDS trades
 * Eliminates manual CSV upload by calculating sensitivities directly from trade data
 */
@Service
@Slf4j
public class CrifGenerationService {

    @Autowired
    private CDSTradeRepository tradeRepository;
    
    @Autowired
    private CrifSensitivityRepository sensitivityRepository;
    
    @Autowired
    private CrifUploadRepository uploadRepository;
    
    // Currency conversion rates (simplified - would use market data in production)
    private static final Map<String, BigDecimal> USD_RATES = Map.of(
        "USD", BigDecimal.ONE,
        "GBP", new BigDecimal("1.25"),
        "EUR", new BigDecimal("1.10"),
        "CHF", new BigDecimal("1.15"),
        "CAD", new BigDecimal("0.75")
    );
    
    /**
     * Generate CRIF sensitivities for all trades in a portfolio
     */
    @Transactional
    public CrifUpload generateFromPortfolio(String portfolioId, LocalDate valuationDate) {
        log.info("Generating CRIF sensitivities for portfolio: {} as of {}", portfolioId, valuationDate);
        
        // Get all active trades for this portfolio (netting set prefix)
        List<CDSTrade> trades = tradeRepository.findAll().stream()
            .filter(t -> t.getNettingSetId() != null)
            .filter(t -> t.getNettingSetId().startsWith(portfolioId))
            .filter(t -> t.getTradeStatus() == TradeStatus.ACTIVE)
            .toList();
        
        if (trades.isEmpty()) {
            throw new RuntimeException("No active trades found for portfolio: " + portfolioId);
        }
        
        log.info("Found {} active trades for portfolio {}", trades.size(), portfolioId);
        
        // Create upload record
        CrifUpload upload = new CrifUpload();
        upload.setUploadId("AUTO-" + portfolioId + "-" + valuationDate);
        upload.setFilename("auto-generated-" + portfolioId + "-" + valuationDate + ".csv");
        upload.setPortfolioId(portfolioId);
        upload.setValuationDate(valuationDate);
        upload.setCurrency("USD"); // Default to USD for aggregated reporting
        upload.setUploadTimestamp(LocalDateTime.now());
        upload.setProcessingStatus(CrifUpload.ProcessingStatus.COMPLETED);
        upload.setTotalRecords(0); // Will update after generating sensitivities
        upload.setValidRecords(0);
        upload.setErrorRecords(0);
        upload = uploadRepository.save(upload);
        
        // Generate sensitivities for each trade
        List<CrifSensitivity> allSensitivities = new ArrayList<>();
        for (CDSTrade trade : trades) {
            try {
                List<CrifSensitivity> tradeSensitivities = generateTradeSensitivities(trade, valuationDate, upload);
                allSensitivities.addAll(tradeSensitivities);
            } catch (Exception e) {
                log.error("Failed to generate sensitivities for trade {}: {}", trade.getId(), e.getMessage());
                upload.setErrorRecords(upload.getErrorRecords() + 1);
            }
        }
        
        // Save all sensitivities
        List<CrifSensitivity> savedSensitivities = sensitivityRepository.saveAll(allSensitivities);
        
        // Update upload record with counts
        upload.setTotalRecords(savedSensitivities.size());
        upload.setValidRecords(savedSensitivities.size());
        uploadRepository.save(upload);
        
        log.info("Generated {} CRIF sensitivities for portfolio {}", savedSensitivities.size(), portfolioId);
        
        return upload;
    }
    
    /**
     * Generate CRIF sensitivities for a single trade
     */
    private List<CrifSensitivity> generateTradeSensitivities(
            CDSTrade trade, LocalDate valuationDate, CrifUpload upload) {
        
        List<CrifSensitivity> sensitivities = new ArrayList<>();
        
        // 1. Credit Spread Sensitivity (Risk_CreditQ)
        CrifSensitivity creditSens = new CrifSensitivity();
        creditSens.setUpload(upload);
        creditSens.setTradeId(trade.getId().toString());
        creditSens.setPortfolioId(trade.getNettingSetId());
        creditSens.setProductClass("Credit");
        creditSens.setRiskType("Risk_CreditQ");
        creditSens.setRiskClass("CR_Q"); // Credit Qualifying
        creditSens.setLabel1(determineMaturityTenor(trade, valuationDate)); // Tenor
        creditSens.setLabel2(trade.getReferenceEntity()); // Reference Entity
        creditSens.setBucket(determineCreditBucket(trade));
        
        // Calculate CS01 (credit spread 01 basis point sensitivity)
        BigDecimal cs01 = calculateCS01(trade);
        creditSens.setAmountBaseCurrency(cs01);
        creditSens.setAmountUsd(convertToUsd(cs01, trade.getCurrency()));
        
        sensitivities.add(creditSens);
        
        // 2. Base Correlation (for diversified portfolios - optional)
        // Only add if this is part of a portfolio trade
        if (shouldIncludeBaseCorrelation(trade)) {
            CrifSensitivity baseCorrSens = new CrifSensitivity();
            baseCorrSens.setUpload(upload);
            baseCorrSens.setTradeId(trade.getId().toString());
            baseCorrSens.setPortfolioId(trade.getNettingSetId());
            baseCorrSens.setProductClass("Credit");
            baseCorrSens.setRiskType("Risk_BaseCorr");
            baseCorrSens.setRiskClass("CR_Q");
            baseCorrSens.setLabel1(creditSens.getLabel1()); // Same tenor
            baseCorrSens.setLabel2(trade.getReferenceEntity()); // Reference Entity
            baseCorrSens.setBucket(creditSens.getBucket());
            
            // Base correlation sensitivity (simplified - typically 10% of CS01)
            BigDecimal baseCorr = cs01.multiply(new BigDecimal("0.10"));
            baseCorrSens.setAmountBaseCurrency(baseCorr);
            baseCorrSens.setAmountUsd(convertToUsd(baseCorr, trade.getCurrency()));
            
            sensitivities.add(baseCorrSens);
        }
        
        return sensitivities;
    }
    
    /**
     * Calculate CS01 (sensitivity to 1bp spread change)
     * CS01 = Notional × Duration × 0.0001
     */
    private BigDecimal calculateCS01(CDSTrade trade) {
        BigDecimal notional = trade.getNotionalAmount();
        
        // Calculate approximate duration based on time to maturity
        LocalDate today = LocalDate.now();
        long daysToMaturity = ChronoUnit.DAYS.between(today, trade.getMaturityDate());
        BigDecimal yearsToMaturity = new BigDecimal(daysToMaturity).divide(new BigDecimal("365"), 4, RoundingMode.HALF_UP);
        
        // Simplified duration estimate: 0.7 × years to maturity (typical for CDS)
        BigDecimal duration = yearsToMaturity.multiply(new BigDecimal("0.7"));
        
        // CS01 = notional × duration × 0.0001 (1 basis point)
        BigDecimal cs01 = notional
            .multiply(duration)
            .multiply(new BigDecimal("0.0001"))
            .setScale(2, RoundingMode.HALF_UP);
        
        // Adjust for buy/sell protection
        if (trade.getBuySellProtection() == CDSTrade.ProtectionDirection.SELL) {
            cs01 = cs01.negate();
        }
        
        return cs01;
    }
    
    /**
     * Determine SIMM credit bucket based on spread and sector
     * Buckets 1-12: Investment Grade by sector
     * Buckets 13-15: High Yield
     * Bucket 16: Non-rated
     */
    private String determineCreditBucket(CDSTrade trade) {
        BigDecimal spread = trade.getSpread();
        
        // Classification based on spread:
        // < 500bps = Investment Grade
        // >= 500bps = High Yield
        boolean isInvestmentGrade = spread.compareTo(new BigDecimal("0.0500")) < 0;
        
        if (isInvestmentGrade) {
            return determineSectorBucket(trade);
        } else {
            // High Yield - bucket 13 (all sectors combined)
            return "13";
        }
    }
    
    /**
     * Map reference entity to ISDA SIMM sector bucket (1-12)
     * Based on GICS sector classification
     */
    private String determineSectorBucket(CDSTrade trade) {
        String entity = trade.getReferenceEntity().toUpperCase();
        
        // Sector mapping (simplified - would use reference data service in production)
        // Bucket 1: Sovereigns including central banks
        // Bucket 2: Financials (banks, insurance, etc.)
        // Bucket 3: Basic Materials
        // Bucket 4: Consumer Goods
        // Bucket 5: TMT (Technology, Media, Telecom)
        // Bucket 6: Energy, Utilities
        // Bucket 7: Industrials, Manufacturing
        // Bucket 8: Agriculture
        // Bucket 9: Precious Metals
        // Bucket 10: Other
        // Bucket 11: Indexes
        // Bucket 12: Securitizations
        
        // Technology companies
        if (entity.matches(".*(AAPL|MSFT|GOOGL|AMZN|META|NVDA|TSLA).*")) {
            return "5"; // TMT
        }
        
        // Financial institutions
        if (entity.matches(".*(JPM|GS|MS|BAC|C|WFC|BNP|DB|UBS).*")) {
            return "2"; // Financials
        }
        
        // Energy
        if (entity.matches(".*(XOM|CVX|BP|SHELL).*")) {
            return "6"; // Energy
        }
        
        // Consumer
        if (entity.matches(".*(WMT|TGT|HD|MCD|SBUX|NKE).*")) {
            return "4"; // Consumer
        }
        
        // Default to bucket 10 (Other)
        return "10";
    }
    
    /**
     * Determine maturity tenor bucket for SIMM
     * Buckets: 1y, 2y, 3y, 5y, 10y, 15y, 20y, 30y
     */
    private String determineMaturityTenor(CDSTrade trade, LocalDate valuationDate) {
        long daysToMaturity = ChronoUnit.DAYS.between(valuationDate, trade.getMaturityDate());
        double yearsToMaturity = daysToMaturity / 365.0;
        
        // Round to nearest standard tenor
        if (yearsToMaturity <= 1.5) return "1y";
        if (yearsToMaturity <= 2.5) return "2y";
        if (yearsToMaturity <= 4.0) return "3y";
        if (yearsToMaturity <= 7.5) return "5y";
        if (yearsToMaturity <= 12.5) return "10y";
        if (yearsToMaturity <= 17.5) return "15y";
        if (yearsToMaturity <= 25.0) return "20y";
        return "30y";
    }
    
    /**
     * Convert amount to USD using simplified FX rates
     */
    private BigDecimal convertToUsd(BigDecimal amount, String currency) {
        BigDecimal rate = USD_RATES.getOrDefault(currency.toUpperCase(), BigDecimal.ONE);
        return amount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    }
    
    /**
     * Determine if base correlation sensitivity should be included
     * Typically for index trades or large diversified portfolios
     */
    private boolean shouldIncludeBaseCorrelation(CDSTrade trade) {
        // Include for all trades (conservative approach)
        // In production, might check if reference entity is an index
        return true;
    }
}
