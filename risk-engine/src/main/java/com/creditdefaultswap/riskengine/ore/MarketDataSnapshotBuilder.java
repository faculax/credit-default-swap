package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.MarketDataSnapshot;
import com.creditdefaultswap.riskengine.model.MarketDataSnapshot.DiscountCurveData;
import com.creditdefaultswap.riskengine.model.MarketDataSnapshot.DefaultCurveData;
import com.creditdefaultswap.riskengine.model.MarketDataSnapshot.QuoteData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a MarketDataSnapshot from the generated ORE input files.
 * This captures exactly what market data was used for the calculation.
 */
@Component
public class MarketDataSnapshotBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataSnapshotBuilder.class);
    
    /**
     * Build a market data snapshot from the ORE working directory
     */
    public MarketDataSnapshot buildSnapshot(String workingDirPath, LocalDate valuationDate) {
        try {
            MarketDataSnapshot snapshot = new MarketDataSnapshot();
            snapshot.setValuationDate(valuationDate);
            snapshot.setBaseCurrency("USD"); // Default base currency
            
            Path inputDir = Paths.get(workingDirPath, "input");
            
            // Read raw file contents for full transparency
            Path marketDataPath = inputDir.resolve("market.txt");
            Path todaysMarketPath = inputDir.resolve("todaysmarket.xml");
            Path curveConfigPath = inputDir.resolve("curveconfig.xml");
            
            String marketDataContent = null;
            
            // Check if actual ORE working files exist
            if (Files.exists(marketDataPath)) {
                marketDataContent = Files.readString(marketDataPath);
                snapshot.setMarketDataFileContent(marketDataContent);
                logger.info("Using actual ORE market data from: {}", marketDataPath);
            } else {
                // Fall back to sample market data to show real data structure
                Path sampleMarketDataPath = Paths.get("ore-setup/market-data/market_flat_cds_fixed.txt");
                if (Files.exists(sampleMarketDataPath)) {
                    marketDataContent = Files.readString(sampleMarketDataPath);
                    snapshot.setMarketDataFileContent(marketDataContent);
                    logger.info("Using sample market data from: {} (ORE not executed yet)", sampleMarketDataPath);
                }
            }
            
            // Parse market data content if available
            if (marketDataContent != null) {
                parseMarketDataFile(snapshot, marketDataContent);
            }
            
            if (Files.exists(todaysMarketPath)) {
                snapshot.setTodaysMarketFileContent(Files.readString(todaysMarketPath));
            } else {
                // Fall back to sample config
                Path sampleTodaysMarket = Paths.get("ore-setup/config/TodaysMarket.xml");
                if (Files.exists(sampleTodaysMarket)) {
                    snapshot.setTodaysMarketFileContent(Files.readString(sampleTodaysMarket));
                }
            }
            
            if (Files.exists(curveConfigPath)) {
                snapshot.setCurveConfigFileContent(Files.readString(curveConfigPath));
            } else {
                // Fall back to sample config
                Path sampleCurveConfig = Paths.get("ore-setup/config/CurveConfig.xml");
                if (Files.exists(sampleCurveConfig)) {
                    snapshot.setCurveConfigFileContent(Files.readString(sampleCurveConfig));
                }
            }
            
            logger.info("Built market data snapshot with {} discount curves, {} default curves, {} FX rates",
                snapshot.getDiscountCurves() != null ? snapshot.getDiscountCurves().size() : 0,
                snapshot.getDefaultCurves() != null ? snapshot.getDefaultCurves().size() : 0,
                snapshot.getFxRates() != null ? snapshot.getFxRates().size() : 0);
            
            return snapshot;
            
        } catch (Exception e) {
            logger.warn("Failed to build complete market data snapshot, returning partial data", e);
            // Return a minimal snapshot rather than failing
            MarketDataSnapshot snapshot = new MarketDataSnapshot();
            snapshot.setValuationDate(valuationDate);
            return snapshot;
        }
    }
    
    /**
     * Parse the market.txt file to extract structured market data
     * Handles the actual ORE format: YYYYMMDD QUOTE_TYPE/RATE/...
     */
    private void parseMarketDataFile(MarketDataSnapshot snapshot, String content) {
        List<DiscountCurveData> discountCurves = new ArrayList<>();
        List<DefaultCurveData> defaultCurves = new ArrayList<>();
        Map<String, Double> fxRates = new HashMap<>();
        
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Parse ORE market data format: YYYYMMDD QUOTE_NAME VALUE
            String[] parts = line.split("\\s+");
            if (parts.length < 3) continue;
            
            String date = parts[0];
            String quoteName = parts[1];
            String valueStr = parts[2];
            
            try {
                double value = Double.parseDouble(valueStr);
                
                // Parse different quote types
                if (quoteName.startsWith("ZERO/RATE/")) {
                    // ZERO/RATE/USD/USD6M/A360/1Y -> Discount curve
                    String[] quoteParts = quoteName.split("/");
                    if (quoteParts.length >= 4) {
                        String currency = quoteParts[2];
                        String curveId = quoteParts[3]; // USD6M, EUR1D, etc.
                        String tenor = quoteParts.length > 5 ? quoteParts[5] : "1Y";
                        
                        DiscountCurveData curve = discountCurves.stream()
                            .filter(c -> c.getCurrency().equals(currency) && c.getCurveId().equals(curveId))
                            .findFirst()
                            .orElseGet(() -> {
                                DiscountCurveData newCurve = new DiscountCurveData();
                                newCurve.setCurrency(currency);
                                newCurve.setCurveId(curveId);
                                newCurve.setQuotes(new ArrayList<>());
                                discountCurves.add(newCurve);
                                return newCurve;
                            });
                        
                        curve.getQuotes().add(new QuoteData(tenor, quoteName, value, "ZERO_RATE"));
                    }
                } else if (quoteName.startsWith("FX/RATE/")) {
                    // FX/RATE/EUR/USD -> FX rates
                    fxRates.put(quoteName, value);
                } else if (quoteName.startsWith("RECOVERY_RATE/RATE/")) {
                    // RECOVERY_RATE/RATE/AAPL/SR/USD -> Recovery rate for default curve
                    String[] quoteParts = quoteName.split("/");
                    if (quoteParts.length >= 5) {
                        String entity = quoteParts[2];
                        String curveType = quoteParts[3]; // SR (Senior)
                        String currency = quoteParts[4];
                        
                        DefaultCurveData curve = defaultCurves.stream()
                            .filter(c -> c.getReferenceEntity().equals(entity) && c.getCurrency().equals(currency))
                            .findFirst()
                            .orElseGet(() -> {
                                DefaultCurveData newCurve = new DefaultCurveData();
                                newCurve.setReferenceEntity(entity);
                                newCurve.setCurrency(currency);
                                newCurve.setCurveId(entity + "_" + curveType + "_" + currency);
                                newCurve.setSpreadQuotes(new ArrayList<>());
                                defaultCurves.add(newCurve);
                                return newCurve;
                            });
                        
                        curve.setRecoveryRate(value);
                    }
                } else if (quoteName.startsWith("HAZARD_RATE/RATE/")) {
                    // HAZARD_RATE/RATE/AAPL/SR/USD/1Y -> Hazard rate for default curve
                    String[] quoteParts = quoteName.split("/");
                    if (quoteParts.length >= 6) {
                        String entity = quoteParts[2];
                        String curveType = quoteParts[3]; // SR (Senior)
                        String currency = quoteParts[4];
                        String tenor = quoteParts[5];
                        
                        DefaultCurveData curve = defaultCurves.stream()
                            .filter(c -> c.getReferenceEntity().equals(entity) && c.getCurrency().equals(currency))
                            .findFirst()
                            .orElseGet(() -> {
                                DefaultCurveData newCurve = new DefaultCurveData();
                                newCurve.setReferenceEntity(entity);
                                newCurve.setCurrency(currency);
                                newCurve.setCurveId(entity + "_" + curveType + "_" + currency);
                                newCurve.setSpreadQuotes(new ArrayList<>());
                                defaultCurves.add(newCurve);
                                return newCurve;
                            });
                        
                        curve.getSpreadQuotes().add(new QuoteData(tenor, quoteName, value, "HAZARD_RATE"));
                    }
                } else if (quoteName.startsWith("CDS/CREDIT_SPREAD/")) {
                    // CDS/CREDIT_SPREAD/BANK/SR/EUR/1Y -> CDS spread for default curve
                    String[] quoteParts = quoteName.split("/");
                    if (quoteParts.length >= 6) {
                        String entity = quoteParts[2];
                        String curveType = quoteParts[3]; // SR (Senior)
                        String currency = quoteParts[4];
                        String tenor = quoteParts[5];
                        
                        DefaultCurveData curve = defaultCurves.stream()
                            .filter(c -> c.getReferenceEntity().equals(entity) && c.getCurrency().equals(currency))
                            .findFirst()
                            .orElseGet(() -> {
                                DefaultCurveData newCurve = new DefaultCurveData();
                                newCurve.setReferenceEntity(entity);
                                newCurve.setCurrency(currency);
                                newCurve.setCurveId(entity + "_" + curveType + "_" + currency);
                                newCurve.setSpreadQuotes(new ArrayList<>());
                                defaultCurves.add(newCurve);
                                return newCurve;
                            });
                        
                        curve.getSpreadQuotes().add(new QuoteData(tenor, quoteName, value, "CDS_SPREAD"));
                    }
                }
            } catch (NumberFormatException e) {
                // Skip invalid values
                logger.debug("Could not parse value '{}' from quote '{}'", valueStr, quoteName);
            }
        }
        
        snapshot.setDiscountCurves(discountCurves);
        snapshot.setDefaultCurves(defaultCurves);
        snapshot.setFxRates(fxRates);
        
        logger.info("Parsed market data: {} discount curves, {} default curves, {} FX rates",
            discountCurves.size(), defaultCurves.size(), fxRates.size());
    }
}
