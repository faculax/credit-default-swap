package com.creditdefaultswap.riskengine.service;

import com.creditdefaultswap.riskengine.config.RiskEngineConfigProperties;
import com.creditdefaultswap.riskengine.model.*;
import com.creditdefaultswap.riskengine.ore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for running stress test scenarios on CDS trades
 */
@Service
public class StressTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(StressTestService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final RiskEngineConfigProperties config;
    private final OreProcessManager oreProcessManager;
    private final OrePortfolioGenerator portfolioGenerator;
    private final OreMarketDataGenerator marketDataGenerator;
    private final OreTodaysMarketGenerator todaysMarketGenerator;
    private final OreCurveConfigGenerator curveConfigGenerator;
    private final OreStressTestGenerator stressTestGenerator;
    private final OreOutputParser oreOutputParser;
    private final TradeDataService tradeDataService;
    
    @Autowired
    public StressTestService(
            RiskEngineConfigProperties config,
            OreProcessManager oreProcessManager,
            OrePortfolioGenerator portfolioGenerator,
            OreMarketDataGenerator marketDataGenerator,
            OreTodaysMarketGenerator todaysMarketGenerator,
            OreCurveConfigGenerator curveConfigGenerator,
            OreStressTestGenerator stressTestGenerator,
            OreOutputParser oreOutputParser,
            TradeDataService tradeDataService) {
        this.config = config;
        this.oreProcessManager = oreProcessManager;
        this.portfolioGenerator = portfolioGenerator;
        this.marketDataGenerator = marketDataGenerator;
        this.todaysMarketGenerator = todaysMarketGenerator;
        this.curveConfigGenerator = curveConfigGenerator;
        this.stressTestGenerator = stressTestGenerator;
        this.oreOutputParser = oreOutputParser;
        this.tradeDataService = tradeDataService;
    }
    
    /**
     * Run stress test analysis on a CDS trade
     */
    public CompletableFuture<StressImpactResult> runStressAnalysis(StressScenarioRequest request) {
        logger.info("Starting stress test analysis for trade {}", request.getTradeId());
        
        LocalDate valuationDate = request.getValuationDate() != null ? 
            request.getValuationDate() : LocalDate.now();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Fetch trade data
                OrePortfolioGenerator.CDSTradeData tradeData = 
                    tradeDataService.fetchCDSTradeData(request.getTradeId(), valuationDate);
                
                // 2. Run base case (no stress)
                logger.info("Running base case for trade {}", request.getTradeId());
                RiskMeasures baseCaseResult = runBaseCase(tradeData, valuationDate);
                
                // 3. Initialize result
                StressImpactResult result = new StressImpactResult();
                result.setTradeId(request.getTradeId());
                result.setBaseNpv(baseCaseResult.getNpv());
                result.setBaseJtd(baseCaseResult.getJtd());
                result.setCurrency(baseCaseResult.getCurrency());
                
                // 4. Run stress scenarios
                logger.info("Running stress scenarios");
                List<StressImpactResult.ScenarioResult> scenarios = runStressScenarios(
                    request, tradeData, valuationDate, baseCaseResult);
                
                result.setScenarios(scenarios);
                
                logger.info("Stress test analysis completed for trade {} with {} scenarios", 
                    request.getTradeId(), scenarios.size());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Stress test analysis failed for trade {}", request.getTradeId(), e);
                throw new RuntimeException("Stress test analysis failed", e);
            }
        });
    }
    
    /**
     * Runs the base case (current market conditions)
     */
    private RiskMeasures runBaseCase(OrePortfolioGenerator.CDSTradeData tradeData, LocalDate valuationDate) {
        try {
            // Create working directory
            String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String workDirName = "ore-stress-base-" + uniqueId;
            Path workDir = Paths.get("/tmp", workDirName);
            Path inputDir = workDir.resolve("input");
            Path outputDir = workDir.resolve("output");
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            
            // Generate ORE inputs
            generateOreInputs(tradeData, valuationDate, inputDir, workDir, null);
            
            // Execute ORE
            String oreOutput = oreProcessManager.executeCalculation(workDir.toString()).join();
            
            // Parse results
            RiskMeasures result = oreOutputParser.parseRiskMeasures(
                oreOutput, tradeData.getTradeId(), tradeData.getCurrency(), workDir.toString());
            
            logger.info("Base case: NPV={}, JTD={}", result.getNpv(), result.getJtd());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to run base case", e);
            throw new RuntimeException("Failed to run base case", e);
        }
    }
    
    /**
     * Runs all stress scenarios
     */
    private List<StressImpactResult.ScenarioResult> runStressScenarios(
            StressScenarioRequest request,
            OrePortfolioGenerator.CDSTradeData tradeData,
            LocalDate valuationDate,
            RiskMeasures baseCase) {
        
        List<StressImpactResult.ScenarioResult> results = new ArrayList<>();
        
        // Run spread stress scenarios
        if (request.getSpreadShifts() != null) {
            for (BigDecimal spreadShift : request.getSpreadShifts()) {
                String scenarioName = "Spread +" + spreadShift + "bp";
                StressImpactResult.ScenarioResult scenario = runSingleStressScenario(
                    tradeData, valuationDate, scenarioName, null, spreadShift, baseCase);
                results.add(scenario);
            }
        }
        
        // Run recovery rate stress scenarios
        if (request.getRecoveryRates() != null) {
            for (BigDecimal recoveryRate : request.getRecoveryRates()) {
                String scenarioName = "Recovery " + recoveryRate + "%";
                StressImpactResult.ScenarioResult scenario = runSingleStressScenario(
                    tradeData, valuationDate, scenarioName, recoveryRate, null, baseCase);
                results.add(scenario);
            }
        }
        
        // Run combined scenarios if requested (full matrix of recovery × spread)
        if (request.isCombined() && request.getRecoveryRates() != null && request.getSpreadShifts() != null) {
            for (BigDecimal recoveryRate : request.getRecoveryRates()) {
                for (BigDecimal spreadShift : request.getSpreadShifts()) {
                    String scenarioName = "Recovery " + recoveryRate + "% + Spread +" + spreadShift + "bp";
                    StressImpactResult.ScenarioResult scenario = runSingleStressScenario(
                        tradeData, valuationDate, scenarioName, recoveryRate, spreadShift, baseCase);
                    
                    // Flag as severe if it's the worst combination
                    BigDecimal worstRecovery = request.getRecoveryRates().stream()
                        .min(BigDecimal::compareTo).orElse(null);
                    BigDecimal worstSpread = request.getSpreadShifts().stream()
                        .max(BigDecimal::compareTo).orElse(null);
                    
                    if (recoveryRate.equals(worstRecovery) && spreadShift.equals(worstSpread)) {
                        scenario.setSevere(true);
                    }
                    
                    results.add(scenario);
                }
            }
        }
        
        return results;
    }
    
    /**
     * Runs a single stress scenario
     */
    private StressImpactResult.ScenarioResult runSingleStressScenario(
            OrePortfolioGenerator.CDSTradeData tradeData,
            LocalDate valuationDate,
            String scenarioName,
            BigDecimal recoveryRate,
            BigDecimal spreadShift,
            RiskMeasures baseCase) {
        
        logger.info("🎯 Running stress scenario: {}", scenarioName);
        
        try {
            // Create working directory
            String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String workDirName = "ore-stress-" + scenarioName.replaceAll("[^a-zA-Z0-9]", "_") + "-" + uniqueId;
            Path workDir = Paths.get("/tmp", workDirName);
            Path inputDir = workDir.resolve("input");
            Path outputDir = workDir.resolve("output");
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            
            logger.debug("Created working directory: {}", workDir);
            
            // Generate stressed ORE inputs
            Map<String, Object> stressParams = new HashMap<>();
            if (recoveryRate != null) {
                stressParams.put("recoveryRate", recoveryRate);
                logger.debug("Applying recovery rate stress: {}%", recoveryRate);
            }
            if (spreadShift != null) {
                stressParams.put("spreadShift", spreadShift);
                logger.debug("Applying spread shift stress: +{} bp", spreadShift);
            }
            
            generateOreInputs(tradeData, valuationDate, inputDir, workDir, stressParams);
            
            // Execute ORE
            logger.debug("Executing ORE calculation for scenario: {}", scenarioName);
            String oreOutput = oreProcessManager.executeCalculation(workDir.toString()).join();
            
            // Parse results
            logger.debug("Parsing ORE results from: {}", outputDir);
            RiskMeasures stressedResult = oreOutputParser.parseRiskMeasures(
                oreOutput, tradeData.getTradeId(), tradeData.getCurrency(), workDir.toString());
            
            // Calculate deltas
            StressImpactResult.ScenarioResult scenario = new StressImpactResult.ScenarioResult();
            scenario.setScenarioName(scenarioName);
            scenario.setNpv(stressedResult.getNpv());
            scenario.setJtd(stressedResult.getJtd());
            
            BigDecimal deltaNpv = stressedResult.getNpv().subtract(baseCase.getNpv());
            BigDecimal deltaJtd = stressedResult.getJtd() != null && baseCase.getJtd() != null ?
                stressedResult.getJtd().subtract(baseCase.getJtd()) : BigDecimal.ZERO;
            
            scenario.setDeltaNpv(deltaNpv);
            scenario.setDeltaJtd(deltaJtd);
            
            // Flag severe scenarios (NPV change > 100k or JTD change > 500k)
            boolean isSevere = deltaNpv.abs().compareTo(new BigDecimal("100000")) > 0 ||
                             deltaJtd.abs().compareTo(new BigDecimal("500000")) > 0;
            scenario.setSevere(isSevere);
            
            logger.info("Scenario '{}': ΔNPV={}, ΔJTD={}{}", 
                scenarioName, deltaNpv, deltaJtd,
                isSevere ? " ⚠️ SEVERE" : "");
            
            return scenario;
            
        } catch (Exception e) {
            logger.error("Failed to run stress scenario: {}", scenarioName, e);
            
            // Return error scenario
            StressImpactResult.ScenarioResult errorScenario = new StressImpactResult.ScenarioResult();
            errorScenario.setScenarioName(scenarioName + " (ERROR)");
            errorScenario.setNpv(BigDecimal.ZERO);
            errorScenario.setJtd(BigDecimal.ZERO);
            errorScenario.setDeltaNpv(BigDecimal.ZERO);
            errorScenario.setDeltaJtd(BigDecimal.ZERO);
            return errorScenario;
        }
    }
    
    /**
     * Generates all ORE input files
     */
    private void generateOreInputs(
            OrePortfolioGenerator.CDSTradeData tradeData,
            LocalDate valuationDate,
            Path inputDir,
            Path workDir,
            Map<String, Object> stressParams) throws IOException {
        
        logger.debug("Generating ORE input files in: {}", inputDir);
        
        // Apply stress to trade data if needed
        OrePortfolioGenerator.CDSTradeData stressedTradeData = applyStress(tradeData, stressParams);
        
        // Generate market data
        Set<OrePortfolioGenerator.CDSTradeData> trades = Set.of(stressedTradeData);
        String marketData = marketDataGenerator.generateMarketData(trades, valuationDate);
        Path marketDataPath = inputDir.resolve("market.txt");
        Files.writeString(marketDataPath, marketData);
        
        // Generate TodaysMarket
        String todaysMarket = todaysMarketGenerator.generateTodaysMarket(trades);
        Path todaysMarketPath = inputDir.resolve("todaysmarket.xml");
        Files.writeString(todaysMarketPath, todaysMarket);
        
        // Generate CurveConfig
        String curveConfig = curveConfigGenerator.generateCurveConfig(trades);
        Path curveConfigPath = inputDir.resolve("curveconfig.xml");
        Files.writeString(curveConfigPath, curveConfig);
        
        // Generate Portfolio
        String portfolio = portfolioGenerator.generatePortfolioXml(stressedTradeData);
        Path portfolioPath = inputDir.resolve("portfolio.xml");
        Files.writeString(portfolioPath, portfolio);
        
        // Copy conventions
        Path configDir = Paths.get("/app/ore/config");
        Path conventionsPath = inputDir.resolve("Conventions.xml");
        Files.copy(configDir.resolve("Conventions.xml"), 
                  conventionsPath, 
                  java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        Path pricingEnginePath = inputDir.resolve("pricingengine.xml");
        Files.copy(configDir.resolve("pricingengine.xml"), 
                  pricingEnginePath, 
                  java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        
        // Create empty fixings file
        Path fixingsPath = inputDir.resolve("fixings.txt");
        Files.writeString(fixingsPath, "# Empty fixings file\n");
        
        // Generate ORE config
        generateOreConfig(valuationDate, workDir);
        logger.debug("All ORE input files generated in {}", workDir);
    }
    
    /**
     * Applies stress parameters to trade data
     */
    private OrePortfolioGenerator.CDSTradeData applyStress(
            OrePortfolioGenerator.CDSTradeData tradeData, 
            Map<String, Object> stressParams) {
        
        if (stressParams == null || stressParams.isEmpty()) {
            return tradeData;
        }
        
        // Clone trade data using the existing constructor
        OrePortfolioGenerator.CDSTradeData stressed = new OrePortfolioGenerator.CDSTradeData(
            tradeData.getTradeId(),
            tradeData.getReferenceEntity(),
            tradeData.getNotionalAmount(),
            tradeData.getSpread(),
            tradeData.getMaturityDate(),
            tradeData.getEffectiveDate(),
            tradeData.getCurrency(),
            tradeData.getPremiumFrequency(),
            tradeData.getDayCountConvention(),
            tradeData.getBuySellProtection(),
            tradeData.getPaymentCalendar()
        );
        
        // Copy recovery rate if present
        if (tradeData.getRecoveryRate() != null) {
            stressed.setRecoveryRate(tradeData.getRecoveryRate());
        }
        
        // Copy first coupon date if present
        if (tradeData.getFirstCouponDate() != null) {
            stressed.setFirstCouponDate(tradeData.getFirstCouponDate());
        }
        
        // Apply recovery rate stress
        if (stressParams.containsKey("recoveryRate")) {
            BigDecimal newRecoveryRate = (BigDecimal) stressParams.get("recoveryRate");
            stressed.setRecoveryRate(newRecoveryRate);
            logger.debug("Applied recovery rate stress: {} -> {}", 
                tradeData.getRecoveryRate(), newRecoveryRate);
        }
        
        // Apply spread stress
        if (stressParams.containsKey("spreadShift")) {
            BigDecimal spreadShift = (BigDecimal) stressParams.get("spreadShift");
            BigDecimal newSpread = tradeData.getSpread().add(spreadShift);
            stressed.setSpread(newSpread);
            logger.debug("Applied spread stress: {} -> {} (+{} bp)", 
                tradeData.getSpread(), newSpread, spreadShift);
        }
        
        return stressed;
    }
    
    /**
     * Generates ORE configuration file
     */
    private void generateOreConfig(LocalDate valuationDate, Path workDir) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<ORE>\n");
        xml.append("  <Setup>\n");
        xml.append("    <Parameter name=\"asofDate\">").append(DATE_FORMAT.format(valuationDate)).append("</Parameter>\n");
        xml.append("    <Parameter name=\"inputPath\">").append(workDir.resolve("input")).append("</Parameter>\n");
        xml.append("    <Parameter name=\"outputPath\">").append(workDir.resolve("output")).append("</Parameter>\n");
        xml.append("    <Parameter name=\"logFile\">log.txt</Parameter>\n");
        xml.append("    <Parameter name=\"logMask\">255</Parameter>\n");
        xml.append("    <Parameter name=\"marketDataFile\">market.txt</Parameter>\n");
        xml.append("    <Parameter name=\"fixingDataFile\">fixings.txt</Parameter>\n");
        xml.append("    <Parameter name=\"implyTodaysFixings\">Y</Parameter>\n");
        xml.append("    <Parameter name=\"curveConfigFile\">curveconfig.xml</Parameter>\n");
        xml.append("    <Parameter name=\"conventionsFile\">Conventions.xml</Parameter>\n");
        xml.append("    <Parameter name=\"marketConfigFile\">todaysmarket.xml</Parameter>\n");
        xml.append("    <Parameter name=\"pricingEnginesFile\">pricingengine.xml</Parameter>\n");
        xml.append("    <Parameter name=\"portfolioFile\">portfolio.xml</Parameter>\n");
        xml.append("    <Parameter name=\"observationModel\">Disable</Parameter>\n");
        xml.append("  </Setup>\n");
        xml.append("  <Markets>\n");
        xml.append("    <Parameter name=\"lgmcalibration\">default</Parameter>\n");
        xml.append("    <Parameter name=\"fxcalibration\">default</Parameter>\n");
        xml.append("    <Parameter name=\"pricing\">default</Parameter>\n");
        xml.append("    <Parameter name=\"simulation\">default</Parameter>\n");
        xml.append("  </Markets>\n");
        xml.append("  <Analytics>\n");
        xml.append("    <Analytic type=\"npv\">\n");
        xml.append("      <Parameter name=\"active\">Y</Parameter>\n");
        xml.append("      <Parameter name=\"baseCurrency\">USD</Parameter>\n");
        xml.append("      <Parameter name=\"outputFileName\">npv.csv</Parameter>\n");
        xml.append("      <Parameter name=\"additionalResults\">Y</Parameter>\n");
        xml.append("      <Parameter name=\"additionalResultsReportPrecision\">12</Parameter>\n");
        xml.append("    </Analytic>\n");
        xml.append("    <Analytic type=\"cashflow\">\n");
        xml.append("      <Parameter name=\"active\">Y</Parameter>\n");
        xml.append("      <Parameter name=\"outputFileName\">flows.csv</Parameter>\n");
        xml.append("    </Analytic>\n");
        xml.append("  </Analytics>\n");
        xml.append("</ORE>\n");
        
        Files.writeString(workDir.resolve("ore.xml"), xml.toString());
    }
}
