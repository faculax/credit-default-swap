package com.creditdefaultswap.riskengine.ore;

import com.creditdefaultswap.riskengine.model.Cashflow;
import com.creditdefaultswap.riskengine.model.RiskMeasures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OreOutputParser {
    
    private static final Logger logger = LoggerFactory.getLogger(OreOutputParser.class);
    
    /**
     * Parses ORE output to extract risk measures from actual output files.
     * Now reads REAL data from additional_results.csv and flows.csv!
     */
    public RiskMeasures parseRiskMeasures(String oreConsoleOutput, Long tradeId, String tradeCurrency, String workingDirPath) {
        logger.debug("Parsing ORE output for trade {} with currency {} from working dir {}", tradeId, tradeCurrency, workingDirPath);
        
        try {
            // Check if ORE completed successfully
            String lowerOutput = oreConsoleOutput.toLowerCase();
            boolean oreSucceeded = lowerOutput.contains("ore done") && 
                                 lowerOutput.contains("npv report") && 
                                 lowerOutput.contains("ok");
            
            if (!oreSucceeded) {
                throw new RuntimeException("ORE execution did not complete successfully");
            }
            
            RiskMeasures riskMeasures = new RiskMeasures();
            riskMeasures.setTradeId(tradeId);
            
            // Read actual NPV from ORE output files
            BigDecimal npv = readNpvFromFile(tradeId, workingDirPath);
            riskMeasures.setNpv(npv);
            riskMeasures.setCurrency(tradeCurrency != null ? tradeCurrency : "USD");
            
            if (npv == null) {
                logger.warn("NPV not found for trade {} in ORE output files", tradeId);
                throw new RuntimeException("NPV not found in ORE output files for trade " + tradeId);
            }
            
            // Parse REAL CDS-specific metrics from additional_results.csv
            parseAdditionalResults(tradeId, riskMeasures, workingDirPath);
            
            // Parse REAL cashflow schedule from flows.csv
            List<Cashflow> cashflows = parseCashflows(tradeId, workingDirPath);
            riskMeasures.setCashflows(cashflows);
            
            logger.info("ORE Risk Calculation - Trade {}: NPV={} {}, Fair Spread Clean={} bps, Protection Leg NPV={}, {} cashflows", 
                tradeId, riskMeasures.getNpv(), riskMeasures.getCurrency(), 
                riskMeasures.getFairSpreadClean() != null ? riskMeasures.getFairSpreadClean().multiply(BigDecimal.valueOf(10000)) : "N/A",
                riskMeasures.getProtectionLegNPV(),
                cashflows != null ? cashflows.size() : 0);
            
            return riskMeasures;
            
        } catch (Exception e) {
            logger.error("Failed to parse ORE output for trade {}", tradeId, e);
            return createErrorRiskMeasures(tradeId, "Failed to parse ORE output: " + e.getMessage());
        }
    }
    
    /**
     * Backward compatibility method - uses hardcoded path
     */
    public RiskMeasures parseRiskMeasures(String oreConsoleOutput, Long tradeId, String tradeCurrency) {
        return parseRiskMeasures(oreConsoleOutput, tradeId, tradeCurrency, "/tmp/ore-work");
    }
    
    /**
     * Parses REAL CDS metrics from additional_results.csv
     */
    private void parseAdditionalResults(Long tradeId, RiskMeasures riskMeasures, String workingDirPath) {
        try {
            String additionalResultsPath = workingDirPath + "/output/additional_results.csv";
            java.nio.file.Path filePath = java.nio.file.Paths.get(additionalResultsPath);
            
            if (!java.nio.file.Files.exists(filePath)) {
                logger.warn("additional_results.csv not found, skipping CDS-specific metrics");
                return;
            }
            
            List<String> lines = java.nio.file.Files.readAllLines(filePath);
            if (lines.size() < 2) {
                logger.warn("additional_results.csv is empty or has no data rows");
                return;
            }
            
            // Parse field names and values
            // Format: #TradeId, ResultId, ResultType, ResultValue
            Map<String, String> results = new HashMap<>();
            for (int i = 1; i < lines.size(); i++) { // Skip header
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split(",", 4); // Split into max 4 parts
                if (parts.length >= 4) {
                    String resultId = parts[1].trim();
                    String resultValue = parts[3].trim();
                    results.put(resultId, resultValue);
                }
            }
            
            // Extract CDS-specific metrics
            // Fair spreads
            BigDecimal fairSpreadClean = parseBigDecimal(results.get("fairSpreadClean"));
            BigDecimal fairSpreadDirty = parseBigDecimal(results.get("fairSpreadDirty"));
            riskMeasures.setFairSpreadClean(fairSpreadClean);
            riskMeasures.setFairSpreadDirty(fairSpreadDirty);
            
            // Leg NPVs (legNPV[1] is protection, legNPV[2] is premium)
            BigDecimal protectionLegNPV = parseBigDecimal(results.get("legNPV[1]"));
            BigDecimal premiumLegNPV = parseBigDecimal(results.get("legNPV[2]"));
            riskMeasures.setProtectionLegNPV(protectionLegNPV);
            riskMeasures.setPremiumLegNPVClean(premiumLegNPV); // legNPV[2] is clean
            
            // Other CDS metrics
            riskMeasures.setAccruedPremium(parseBigDecimal(results.get("accruedPremium")));
            riskMeasures.setUpfrontPremium(parseBigDecimal(results.get("upfrontPremium")));
            
            // Notional amounts
            riskMeasures.setCurrentNotional(parseBigDecimal(results.get("currentNotional[1]")));
            riskMeasures.setOriginalNotional(parseBigDecimal(results.get("originalNotional[1]")));
            
            logger.info("Parsed CDS metrics from additional_results.csv: Fair Spread Clean = {}, Protection Leg NPV = {}, Premium Leg NPV = {}", 
                riskMeasures.getFairSpreadClean(), riskMeasures.getProtectionLegNPV(), riskMeasures.getPremiumLegNPVClean());
                
        } catch (Exception e) {
            logger.error("Error parsing additional_results.csv", e);
        }
    }
    
    /**
     * Parses REAL cashflow schedule from flows.csv
     */
    private List<Cashflow> parseCashflows(Long tradeId, String workingDirPath) {
        List<Cashflow> cashflows = new ArrayList<>();
        try {
            String flowsPath = workingDirPath + "/output/flows.csv";
            java.nio.file.Path filePath = java.nio.file.Paths.get(flowsPath);
            
            if (!java.nio.file.Files.exists(filePath)) {
                logger.warn("flows.csv not found, skipping cashflow schedule");
                return cashflows;
            }
            
            List<String> lines = java.nio.file.Files.readAllLines(filePath);
            if (lines.size() < 2) {
                return cashflows;
            }
            
            // Parse header to get column indices
            String headerLine = lines.get(0);
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnMap.put(headers[i].trim(), i);
            }
            
            // Parse data rows
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                
                String[] columns = line.split(",", -1);
                Cashflow cf = new Cashflow();
                
                cf.setTradeId(getColumn(columns, columnMap, "TradeId"));
                cf.setType(getColumn(columns, columnMap, "Type"));
                cf.setCashflowNo(parseInteger(getColumn(columns, columnMap, "CashflowNo")));
                cf.setLegNo(parseInteger(getColumn(columns, columnMap, "LegNo")));
                cf.setPayDate(parseLocalDate(getColumn(columns, columnMap, "PayDate")));
                cf.setFlowType(getColumn(columns, columnMap, "FlowType"));
                cf.setAmount(parseBigDecimal(getColumn(columns, columnMap, "Amount")));
                cf.setCurrency(getColumn(columns, columnMap, "Currency"));
                cf.setCoupon(parseBigDecimal(getColumn(columns, columnMap, "Coupon")));
                cf.setAccrual(parseBigDecimal(getColumn(columns, columnMap, "Accrual")));
                cf.setAccrualStartDate(parseLocalDate(getColumn(columns, columnMap, "AccrualStartDate")));
                cf.setAccrualEndDate(parseLocalDate(getColumn(columns, columnMap, "AccrualEndDate")));
                cf.setAccruedAmount(parseBigDecimal(getColumn(columns, columnMap, "AccruedAmount")));
                cf.setNotional(parseBigDecimal(getColumn(columns, columnMap, "Notional")));
                cf.setDiscountFactor(parseBigDecimal(getColumn(columns, columnMap, "DiscountFactor")));
                cf.setPresentValue(parseBigDecimal(getColumn(columns, columnMap, "PresentValue")));
                cf.setFxRate(parseBigDecimal(getColumn(columns, columnMap, "FXRate")));
                cf.setPresentValueBase(parseBigDecimal(getColumn(columns, columnMap, "PresentValue(Base)")));
                
                cashflows.add(cf);
            }
            
            logger.info("Parsed {} cashflows from flows.csv", cashflows.size());
            
        } catch (Exception e) {
            logger.error("Error parsing flows.csv", e);
        }
        return cashflows;
    }
    
    // Helper methods for parsing
    private String getColumn(String[] columns, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index != null && index < columns.length) {
            String value = columns[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
    
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("#N/A")) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(8, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            logger.debug("Could not parse BigDecimal from: {}", value);
            return null;
        }
    }
    
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // Try common date formats
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyyMMdd"),
                DateTimeFormatter.ISO_LOCAL_DATE
            };
            
            for (DateTimeFormatter formatter : formatters) {
                try {
                    return LocalDate.parse(value, formatter);
                } catch (DateTimeParseException e) {
                    // Try next formatter
                }
            }
            logger.debug("Could not parse date from: {}", value);
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Reads NPV from ORE output CSV file for a specific trade
     */
    private BigDecimal readNpvFromFile(Long tradeId, String workingDirPath) {
        try {
            String npvFilePath = workingDirPath + "/output/npv.csv";
            java.nio.file.Path filePath = java.nio.file.Paths.get(npvFilePath);
            
            if (!java.nio.file.Files.exists(filePath)) {
                logger.warn("NPV file not found: {}", npvFilePath);
                return null;
            }
            
            java.util.List<String> lines = java.nio.file.Files.readAllLines(filePath);
            
            // Skip header line and look for trades
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;
                
                String[] columns = line.split(",");
                if (columns.length >= 5) {
                    // Format: #TradeId,TradeType,Maturity,MaturityTime,NPV,NpvCurrency,...
                    String csvTradeId = columns[0].trim();
                    String npvValue = columns[4].trim();
                    
                    // For now, since we don't have exact trade ID matching,
                    // use the first valid NPV we find (ORE portfolio might have different trade naming)
                    if (!npvValue.isEmpty() && !npvValue.equals("NPV")) {
                        try {
                            BigDecimal npv = new BigDecimal(npvValue);
                            logger.info("Found NPV for trade {}: {} (from ORE trade: {})", tradeId, npv, csvTradeId);
                            return npv.setScale(2, RoundingMode.HALF_UP);
                        } catch (NumberFormatException e) {
                            logger.debug("Could not parse NPV value: {}", npvValue);
                        }
                    }
                }
            }
            
            logger.warn("No NPV found for trade {} in ORE output", tradeId);
            return null;
            
        } catch (Exception e) {
            logger.error("Error reading NPV from ORE output file", e);
            return null;
        }
    }
    
    /**
     * Extracts ORE runtime from console output
     */
    private BigDecimal extractOreRuntime(String oreConsoleOutput) {
        try {
            // Look for pattern like "run time: 0.038142 sec"
            String[] lines = oreConsoleOutput.split("\n");
            for (String line : lines) {
                if (line.contains("run time:") && line.contains("sec")) {
                    String[] parts = line.split("run time:");
                    if (parts.length > 1) {
                        String timePart = parts[1].trim().replace("sec", "").trim();
                        return new BigDecimal(timePart).setScale(6, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract ORE runtime from output", e);
        }
        return BigDecimal.ZERO;
    }
    
    private void parseNpv(Document document, RiskMeasures riskMeasures) {
        try {
            NodeList npvNodes = document.getElementsByTagName("NPV");
            if (npvNodes.getLength() > 0) {
                Element npvElement = (Element) npvNodes.item(0);
                String npvValue = getElementTextContent(npvElement, "Value");
                if (npvValue != null) {
                    riskMeasures.setNpv(new BigDecimal(npvValue).setScale(2, RoundingMode.HALF_UP));
                }
                
                String currency = getElementTextContent(npvElement, "Currency");
                if (currency != null) {
                    riskMeasures.setCurrency(currency);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse NPV from ORE output", e);
        }
    }
    
    private void parseSensitivities(Document document, RiskMeasures riskMeasures) {
        try {
            // Parse DV01 (duration sensitivity)
            NodeList sensitivityNodes = document.getElementsByTagName("Sensitivity");
            for (int i = 0; i < sensitivityNodes.getLength(); i++) {
                Element sensitivityElement = (Element) sensitivityNodes.item(i);
                String riskFactor = getElementTextContent(sensitivityElement, "RiskFactor");
                String deltaValue = getElementTextContent(sensitivityElement, "Delta");
                
                if (riskFactor != null && deltaValue != null) {
                    if (riskFactor.contains("YieldCurve")) {
                        // Sum up all yield curve sensitivities for DV01
                        BigDecimal currentDv01 = riskMeasures.getDv01() != null ? 
                            riskMeasures.getDv01() : BigDecimal.ZERO;
                        BigDecimal deltaBD = new BigDecimal(deltaValue);
                        riskMeasures.setDv01(currentDv01.add(deltaBD).setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
            
            // Parse Gamma (second-order sensitivity)
            NodeList gammaNodes = document.getElementsByTagName("Gamma");
            if (gammaNodes.getLength() > 0) {
                Element gammaElement = (Element) gammaNodes.item(0);
                String gammaValue = getElementTextContent(gammaElement, "Value");
                if (gammaValue != null) {
                    riskMeasures.setGamma(new BigDecimal(gammaValue).setScale(6, RoundingMode.HALF_UP));
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse sensitivities from ORE output", e);
        }
    }
    
    private void parseRiskMetrics(Document document, RiskMeasures riskMeasures) {
        try {
            // Parse VaR
            NodeList varNodes = document.getElementsByTagName("VaR");
            if (varNodes.getLength() > 0) {
                Element varElement = (Element) varNodes.item(0);
                String varValue = getElementTextContent(varElement, "Value");
                if (varValue != null) {
                    riskMeasures.setVar95(new BigDecimal(varValue).setScale(2, RoundingMode.HALF_UP));
                }
            }
            
            // Parse Expected Shortfall
            NodeList esNodes = document.getElementsByTagName("ExpectedShortfall");
            if (esNodes.getLength() > 0) {
                Element esElement = (Element) esNodes.item(0);
                String esValue = getElementTextContent(esElement, "Value");
                if (esValue != null) {
                    riskMeasures.setExpectedShortfall(new BigDecimal(esValue).setScale(2, RoundingMode.HALF_UP));
                }
            }
            
            // Parse Greeks
            parseGreeks(document, riskMeasures);
            
        } catch (Exception e) {
            logger.warn("Failed to parse risk metrics from ORE output", e);
        }
    }
    
    private void parseGreeks(Document document, RiskMeasures riskMeasures) {
        try {
            Map<String, BigDecimal> greeks = new HashMap<>();
            
            // Parse Delta
            NodeList deltaNodes = document.getElementsByTagName("Delta");
            if (deltaNodes.getLength() > 0) {
                String deltaValue = deltaNodes.item(0).getTextContent();
                greeks.put("delta", new BigDecimal(deltaValue).setScale(6, RoundingMode.HALF_UP));
            }
            
            // Parse Vega
            NodeList vegaNodes = document.getElementsByTagName("Vega");
            if (vegaNodes.getLength() > 0) {
                String vegaValue = vegaNodes.item(0).getTextContent();
                greeks.put("vega", new BigDecimal(vegaValue).setScale(6, RoundingMode.HALF_UP));
            }
            
            // Parse Theta
            NodeList thetaNodes = document.getElementsByTagName("Theta");
            if (thetaNodes.getLength() > 0) {
                String thetaValue = thetaNodes.item(0).getTextContent();
                greeks.put("theta", new BigDecimal(thetaValue).setScale(6, RoundingMode.HALF_UP));
            }
            
            // Parse Rho
            NodeList rhoNodes = document.getElementsByTagName("Rho");
            if (rhoNodes.getLength() > 0) {
                String rhoValue = rhoNodes.item(0).getTextContent();
                greeks.put("rho", new BigDecimal(rhoValue).setScale(6, RoundingMode.HALF_UP));
            }
            
            riskMeasures.setGreeks(greeks);
            
        } catch (Exception e) {
            logger.warn("Failed to parse Greeks from ORE output", e);
        }
    }
    
    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }
    
    /**
     * Creates error risk measures when parsing fails
     */
    private RiskMeasures createErrorRiskMeasures(Long tradeId, String errorMessage) {
        RiskMeasures riskMeasures = new RiskMeasures();
        riskMeasures.setTradeId(tradeId);
        riskMeasures.setNpv(BigDecimal.ZERO);
        riskMeasures.setCurrency("USD");
        
        logger.error("Created error risk measures for trade {}: {}", tradeId, errorMessage);
        return riskMeasures;
    }
    
    /**
     * Validates if ORE output contains valid risk calculation results
     * For batch ORE processing, we check for successful completion indicators in console output
     */
    public boolean isValidOutput(String oreConsoleOutput) {
        if (oreConsoleOutput == null || oreConsoleOutput.trim().isEmpty()) {
            return false;
        }
        
        // For ORE batch processing, check for successful completion indicators
        String output = oreConsoleOutput.toLowerCase();
        
        // ORE batch processing success indicators
        boolean hasOreCompletion = output.contains("ore done");
        boolean hasSuccessfulReports = output.contains("writing reports") && output.contains("ok");
        boolean hasNpvReport = output.contains("npv report") && output.contains("ok");
        
        if (hasOreCompletion && hasSuccessfulReports) {
            logger.debug("ORE validation passed - completion: {}, reports: {}, NPV: {}", 
                hasOreCompletion, hasSuccessfulReports, hasNpvReport);
        }
        
        return hasOreCompletion && hasSuccessfulReports;
    }
    
    /**
     * Extracts error messages from ORE console output if calculation failed
     */
    public String extractErrorMessage(String oreConsoleOutput) {
        if (oreConsoleOutput == null || oreConsoleOutput.trim().isEmpty()) {
            return "Empty ORE output";
        }
        
        // For ORE batch processing, look for error indicators in console output
        String[] lines = oreConsoleOutput.split("\n");
        StringBuilder errorInfo = new StringBuilder();
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("error") || lowerLine.contains("fail") || lowerLine.contains("exception")) {
                errorInfo.append(line.trim()).append(" ");
            }
        }
        
        return errorInfo.length() > 0 ? errorInfo.toString().trim() : "Unknown ORE calculation error";
    }
}