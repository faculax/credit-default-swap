package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.ScenarioRequest;
import com.creditdefaultswap.riskengine.service.TradeDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class OreInputBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(OreInputBuilder.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final OrePortfolioGenerator portfolioGenerator;
    private final OreMarketDataGenerator marketDataGenerator;
    private final OreTodaysMarketGenerator todaysMarketGenerator;
    private final OreCurveConfigGenerator curveConfigGenerator;
    private final TradeDataService tradeDataService;
    
    @Autowired
    public OreInputBuilder(OrePortfolioGenerator portfolioGenerator, 
                          OreMarketDataGenerator marketDataGenerator,
                          OreTodaysMarketGenerator todaysMarketGenerator,
                          OreCurveConfigGenerator curveConfigGenerator,
                          TradeDataService tradeDataService) {
        this.portfolioGenerator = portfolioGenerator;
        this.marketDataGenerator = marketDataGenerator;
        this.todaysMarketGenerator = todaysMarketGenerator;
        this.curveConfigGenerator = curveConfigGenerator;
        this.tradeDataService = tradeDataService;
    }
    
    /**
     * Builds ORE XML input for risk calculation with dynamic portfolio generation
     */
    public String buildRiskCalculationInput(ScenarioRequest request) {
        logger.info("=== BUILD RISK CALCULATION INPUT START ===");
        logger.info("Building ORE input for scenario: {}", request.getScenarioId());
        
        try {
            // Create unique working directories per request to avoid concurrency issues
            String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String workDirName = "ore-work-" + request.getScenarioId() + "-" + uniqueId;
            Path inputDir = Paths.get("/tmp", workDirName, "input");
            Path outputDir = Paths.get("/tmp", workDirName, "output");
            Files.createDirectories(inputDir);
            Files.createDirectories(outputDir);
            logger.info("Created unique working directories: input={}, output={}", inputDir, outputDir);
            
            // Collect all trade data
            // Get valuation date first to pass to trade data fetching
            LocalDate valuationDate = request.getValuationDate() != null ? 
                request.getValuationDate() : LocalDate.now();
            
            logger.info("🔍 DEBUG: Valuation date for ORE calculation: {} (from request: {})", 
                valuationDate, request.getValuationDate());
            
            Set<OrePortfolioGenerator.CDSTradeData> allTrades = new HashSet<>();
            for (Long tradeId : request.getTradeIds()) {
                // Pass valuation date to ensure effective date adjustment considers valuation date
                OrePortfolioGenerator.CDSTradeData tradeData = tradeDataService.fetchCDSTradeData(tradeId, valuationDate);
                allTrades.add(tradeData);
            }
            
            // Generate dynamic market data based on trades
            String marketData = marketDataGenerator.generateMarketData(allTrades, valuationDate);
            writeDynamicMarketData(marketData, inputDir);
            
            // Generate dynamic TodaysMarket configuration
            String todaysMarket = todaysMarketGenerator.generateTodaysMarket(allTrades);
            writeDynamicTodaysMarket(todaysMarket, inputDir);
            
            // Generate dynamic CurveConfig
            String curveConfig = curveConfigGenerator.generateCurveConfig(allTrades);
            writeDynamicCurveConfig(curveConfig, inputDir);
            
            // Copy only conventions file (static)
            copyConventionsFile(inputDir);
            
            // Generate dynamic portfolio for each trade
            for (OrePortfolioGenerator.CDSTradeData tradeData : allTrades) {
                String portfolioXml = portfolioGenerator.generatePortfolioXml(tradeData);
                writeDynamicPortfolio(portfolioXml, inputDir);
            }
            
            // Write dynamic ORE configuration that points to our working directory
            writeDynamicOreConfig(request, inputDir.getParent());
            
            return inputDir.getParent().toString(); // Return the working directory path
            
        } catch (Exception e) {
            logger.error("Failed to build ORE input for scenario: {}", request.getScenarioId(), e);
            throw new RuntimeException("Failed to build ORE input", e);
        }
    }
    
    /**
     * Copies static configuration files (conventions and pricingengine)
     */
    private void copyConventionsFile(Path inputDir) throws Exception {
        Path configDir = Paths.get("/app/ore/config");
        Files.copy(configDir.resolve("Conventions.xml"), inputDir.resolve("Conventions.xml"), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Copied Conventions.xml");
        
        Files.copy(configDir.resolve("pricingengine.xml"), inputDir.resolve("pricingengine.xml"), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Copied pricingengine.xml");
        
        // Create empty fixings file
        Files.writeString(inputDir.resolve("fixings.txt"), "# Empty fixings file\n");
        logger.info("Created empty fixings.txt");
    }
    
    /**
     * Writes dynamic market data to the writable ORE working directory
     */
    private void writeDynamicMarketData(String marketData, Path inputDir) {
        try {
            Path marketDataPath = inputDir.resolve("market.txt");
            Files.writeString(marketDataPath, marketData);
            logger.info("Written dynamic market data to ORE working directory: {}", marketDataPath);
        } catch (Exception e) {
            logger.error("Failed to write dynamic market data file", e);
            throw new RuntimeException("Failed to write market data file", e);
        }
    }
    
    /**
     * Writes dynamic TodaysMarket configuration
     */
    private void writeDynamicTodaysMarket(String todaysMarket, Path inputDir) {
        try {
            Path todaysMarketPath = inputDir.resolve("todaysmarket.xml");
            Files.writeString(todaysMarketPath, todaysMarket);
            logger.info("Written dynamic TodaysMarket to: {}", todaysMarketPath);
        } catch (Exception e) {
            logger.error("Failed to write TodaysMarket file", e);
            throw new RuntimeException("Failed to write TodaysMarket file", e);
        }
    }
    
    /**
     * Writes dynamic CurveConfig
     */
    private void writeDynamicCurveConfig(String curveConfig, Path inputDir) {
        try {
            Path curveConfigPath = inputDir.resolve("curveconfig.xml");
            Files.writeString(curveConfigPath, curveConfig);
            logger.info("Written dynamic CurveConfig to: {}", curveConfigPath);
        } catch (Exception e) {
            logger.error("Failed to write CurveConfig file", e);
            throw new RuntimeException("Failed to write CurveConfig file", e);
        }
    }
    
    /**
     * Writes dynamic ORE configuration file that points to working directory with credit analytics enabled
     */
    private void writeDynamicOreConfig(ScenarioRequest request, Path workDir) {
        try {
            LocalDate valuationDate = request.getValuationDate() != null ? 
                request.getValuationDate() : LocalDate.now();
                
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\"?>\n");
            xml.append("<ORE>\n");
            xml.append("  <Setup>\n");
            xml.append("    <Parameter name=\"asofDate\">").append(DATE_FORMAT.format(valuationDate)).append("</Parameter>\n");
            logger.info("🔍 DEBUG: Setting ORE asofDate to: {} (formatted: {})", 
                valuationDate, DATE_FORMAT.format(valuationDate));
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
            
            Path oreConfigPath = workDir.resolve("ore.xml");
            Files.writeString(oreConfigPath, xml.toString());
            logger.info("Written dynamic ORE config to: {}", oreConfigPath);
            
        } catch (Exception e) {
            logger.error("Failed to write dynamic ORE config", e);
            throw new RuntimeException("Failed to write ORE config", e);
        }
    }
    
    /**
     * Writes dynamic portfolio XML to the writable ORE working directory
     */
    private void writeDynamicPortfolio(String portfolioXml, Path inputDir) {
        try {
            // Write to the writable working directory that ORE can access
            Path portfolioPath = inputDir.resolve("portfolio.xml");
            Files.writeString(portfolioPath, portfolioXml);
            logger.info("Written dynamic CDS portfolio to ORE working directory: {}", portfolioPath);
        } catch (Exception e) {
            logger.error("Failed to write dynamic portfolio file", e);
            throw new RuntimeException("Failed to write portfolio file", e);
        }
    }
    
    private void appendSetup(StringBuilder xml, ScenarioRequest request) {
        LocalDate valuationDate = request.getValuationDate() != null ? 
            request.getValuationDate() : LocalDate.now();
        
        xml.append("  <Setup>\n");
        xml.append("    <Parameter name=\"asofDate\">").append(DATE_FORMAT.format(valuationDate)).append("</Parameter>\n");
        xml.append("    <Parameter name=\"inputPath\">Input</Parameter>\n");
        xml.append("    <Parameter name=\"outputPath\">Output</Parameter>\n");
        xml.append("    <Parameter name=\"logMask\">255</Parameter>\n");
        xml.append("    <Parameter name=\"logLevel\">2</Parameter>\n");
        xml.append("  </Setup>\n");
    }
    
    private void appendMarketData(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <Markets>\n");
        xml.append("    <Configuration>\n");
        xml.append("      <Market>\n");
        xml.append("        <YieldCurves>\n");
        
        // Add yield curves based on trade currencies
        for (Long tradeId : request.getTradeIds()) {
            // For now, add USD curve - in real implementation, get currency from trade
            xml.append("          <YieldCurve>\n");
            xml.append("            <CurveId>USD</CurveId>\n");
            xml.append("            <CurveDescription>USD yield curve</CurveDescription>\n");
            xml.append("            <Currency>USD</Currency>\n");
            xml.append("            <YieldCurveSegments>\n");
            xml.append("              <Simple>\n");
            xml.append("                <Type>Deposit</Type>\n");
            xml.append("                <Quotes>\n");
            xml.append("                  <Quote>MM/USD/0D/1D</Quote>\n");
            xml.append("                  <Quote>MM/USD/1D/1W</Quote>\n");
            xml.append("                  <Quote>MM/USD/1W/1M</Quote>\n");
            xml.append("                </Quotes>\n");
            xml.append("              </Simple>\n");
            xml.append("            </YieldCurveSegments>\n");
            xml.append("          </YieldCurve>\n");
        }
        
        xml.append("        </YieldCurves>\n");
        xml.append("        <DefaultCurves/>\n");
        xml.append("        <SwaptionVolatilities/>\n");
        xml.append("        <CapFloorVolatilities/>\n");
        xml.append("        <FxVolatilities/>\n");
        xml.append("        <EquityVolatilities/>\n");
        xml.append("      </Market>\n");
        xml.append("    </Configuration>\n");
        xml.append("  </Markets>\n");
    }
    
    private void appendScenarios(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <SensitivityAnalysis>\n");
        xml.append("    <PricingEngines>\n");
        xml.append("      <Product type=\"Swap\">\n");
        xml.append("        <Model>DiscountedCashflows</Model>\n");
        xml.append("        <ModelParameters/>\n");
        xml.append("        <Engine>DiscountingSwapEngine</Engine>\n");
        xml.append("        <EngineParameters/>\n");
        xml.append("      </Product>\n");
        xml.append("    </PricingEngines>\n");
        
        xml.append("    <SensitivityConfig>\n");
        xml.append("      <SensitivityScenarioData>\n");
        
        // Add interest rate scenarios
        xml.append("        <YieldCurves>\n");
        xml.append("          <YieldCurve>\n");
        xml.append("            <SensitivityParameter>YieldCurve/USD</SensitivityParameter>\n");
        xml.append("            <ShiftType>Absolute</ShiftType>\n");
        xml.append("            <ShiftSize>0.0001</ShiftSize>\n");
        xml.append("            <KeyType>Tenor</KeyType>\n");
        xml.append("            <KeyValues>1Y,2Y,3Y,5Y,7Y,10Y</KeyValues>\n");
        xml.append("          </YieldCurve>\n");
        xml.append("        </YieldCurves>\n");
        
        // Add scenarios based on request
        if (request.getScenarios() != null) {
            for (Map.Entry<String, Double> scenario : request.getScenarios().entrySet()) {
                xml.append("        <YieldCurveScenario>\n");
                xml.append("          <Key>").append(scenario.getKey()).append("</Key>\n");
                xml.append("          <ShiftSize>").append(scenario.getValue()).append("</ShiftSize>\n");
                xml.append("        </YieldCurveScenario>\n");
            }
        }
        
        xml.append("      </SensitivityScenarioData>\n");
        xml.append("    </SensitivityConfig>\n");
        xml.append("  </SensitivityAnalysis>\n");
    }
    
    private void appendAnalytics(StringBuilder xml, ScenarioRequest request) {
        xml.append("  <Analytics>\n");
        xml.append("    <Analytic type=\"npv\">true</Analytic>\n");
        xml.append("    <Analytic type=\"sensitivity\">true</Analytic>\n");
        xml.append("    <Analytic type=\"stress\">false</Analytic>\n");
        xml.append("    <Analytic type=\"parametricvar\">false</Analytic>\n");
        xml.append("    <Analytic type=\"simulation\">false</Analytic>\n");
        xml.append("  </Analytics>\n");
    }
    
    /**
     * Builds ORE XML input for a simple health check
     */
    public String buildHealthCheckInput() {
        return "<?xml version=\"1.0\"?>\n" +
               "<ORE>\n" +
               "  <Setup>\n" +
               "    <Parameter name=\"asofDate\">" + DATE_FORMAT.format(LocalDate.now()) + "</Parameter>\n" +
               "    <Parameter name=\"inputPath\">Input</Parameter>\n" +
               "    <Parameter name=\"outputPath\">Output</Parameter>\n" +
               "  </Setup>\n" +
               "  <Analytics>\n" +
               "    <Analytic type=\"npv\">false</Analytic>\n" +
               "  </Analytics>\n" +
               "</ORE>\n";
    }
}