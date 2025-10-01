package com.creditdefaultswap.riskengine.ore;

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
import java.util.HashMap;
import java.util.Map;

@Component
public class OreOutputParser {
    
    private static final Logger logger = LoggerFactory.getLogger(OreOutputParser.class);
    
    /**
     * Parses ORE XML output to extract risk measures
     */
    public RiskMeasures parseRiskMeasures(String oreXmlOutput, Long tradeId) {
        logger.debug("Parsing ORE output for trade {}, XML length: {}", tradeId, oreXmlOutput.length());
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(oreXmlOutput.getBytes()));
            
            RiskMeasures riskMeasures = new RiskMeasures();
            riskMeasures.setTradeId(tradeId);
            
            // Parse NPV
            parseNpv(document, riskMeasures);
            
            // Parse sensitivity measures
            parseSensitivities(document, riskMeasures);
            
            // Parse risk measures
            parseRiskMetrics(document, riskMeasures);
            
            logger.debug("Parsed risk measures for trade {}: NPV={}, DV01={}, Gamma={}", 
                tradeId, riskMeasures.getNpv(), riskMeasures.getDv01(), riskMeasures.getGamma());
            
            return riskMeasures;
            
        } catch (Exception e) {
            logger.error("Failed to parse ORE output for trade {}", tradeId, e);
            return createErrorRiskMeasures(tradeId, "Failed to parse ORE output: " + e.getMessage());
        }
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
        riskMeasures.setDv01(BigDecimal.ZERO);
        riskMeasures.setGamma(BigDecimal.ZERO);
        riskMeasures.setVar95(BigDecimal.ZERO);
        riskMeasures.setCurrency("USD");
        riskMeasures.setGreeks(new HashMap<>());
        
        logger.error("Created error risk measures for trade {}: {}", tradeId, errorMessage);
        return riskMeasures;
    }
    
    /**
     * Validates if ORE output contains valid risk calculation results
     */
    public boolean isValidOutput(String oreXmlOutput) {
        if (oreXmlOutput == null || oreXmlOutput.trim().isEmpty()) {
            return false;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(oreXmlOutput.getBytes()));
            
            // Check for essential elements
            NodeList npvNodes = document.getElementsByTagName("NPV");
            NodeList sensitivityNodes = document.getElementsByTagName("Sensitivity");
            
            return npvNodes.getLength() > 0 || sensitivityNodes.getLength() > 0;
            
        } catch (Exception e) {
            logger.debug("ORE output validation failed", e);
            return false;
        }
    }
    
    /**
     * Extracts error messages from ORE output if calculation failed
     */
    public String extractErrorMessage(String oreXmlOutput) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(oreXmlOutput.getBytes()));
            
            NodeList errorNodes = document.getElementsByTagName("Error");
            if (errorNodes.getLength() > 0) {
                return errorNodes.item(0).getTextContent();
            }
            
            NodeList warningNodes = document.getElementsByTagName("Warning");
            if (warningNodes.getLength() > 0) {
                return "Warning: " + warningNodes.item(0).getTextContent();
            }
            
        } catch (Exception e) {
            logger.debug("Failed to extract error message from ORE output", e);
        }
        
        return "Unknown ORE calculation error";
    }
}