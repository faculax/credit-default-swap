package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for EUREX margin statements in XML format
 * Handles EUREX-specific XML structure and European date formats
 */
@Component
public class EurexXmlStatementParser implements StatementParser {
    
    private static final DateTimeFormatter EUREX_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "EUREX".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.XML;
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.XML;
    }
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseXmlContent(statement);
        } catch (StatementValidationException e) {
            throw new StatementParsingException("Failed to parse EUREX XML statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing EUREX XML statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Security: Prevent XXE attacks (CWE-611)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes()));
            
            // Validate root element
            Element root = doc.getDocumentElement();
            if (!"EurexMarginReport".equals(root.getNodeName())) {
                throw new StatementValidationException("Invalid XML root element. Expected 'EurexMarginReport', got: " + root.getNodeName());
            }
            
            // Validate required elements exist
            NodeList metadataNodes = root.getElementsByTagName("ReportMetadata");
            if (metadataNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'ReportMetadata' element");
            }
            
            NodeList clearingAccountsNodes = root.getElementsByTagName("ClearingAccounts");
            if (clearingAccountsNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'ClearingAccounts' element");
            }
            
        } catch (StatementValidationException e) {
            throw e;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new StatementValidationException("Invalid XML format: " + e.getMessage(), e);
        }
    }
    
    private List<MarginPosition> parseXmlContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Security: Prevent XXE attacks (CWE-611)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(statement.getRawContent().getBytes()));
            
            // Get all Account elements
            NodeList accountNodes = doc.getElementsByTagName("Account");
            
            for (int i = 0; i < accountNodes.getLength(); i++) {
                Element accountElement = (Element) accountNodes.item(i);
                String accountId = accountElement.getAttribute("accountId");
                
                // Get all Position elements within this account
                NodeList positionNodes = accountElement.getElementsByTagName("Position");
                
                for (int j = 0; j < positionNodes.getLength(); j++) {
                    Element positionElement = (Element) positionNodes.item(j);
                    MarginPosition position = parsePositionElement(statement, accountId, positionElement);
                    if (position != null) {
                        positions.add(position);
                    }
                }
            }
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new StatementParsingException("Error parsing XML content: " + e.getMessage(), e);
        } catch (StatementParsingException e) {
            throw e;
        }
        
        return positions;
    }
    
    private MarginPosition parsePositionElement(MarginStatement statement, String accountId, Element positionElement) throws StatementParsingException {
        try {
            String dateStr = getElementText(positionElement, "StatementDate");
            String marginCategoryStr = getElementText(positionElement, "MarginCategory");
            String portfolioCode = getElementText(positionElement, "Portfolio");
            String productGroup = getElementText(positionElement, "ProductGroup");
            String nettingSet = getElementText(positionElement, "NettingSet");
            
            // Parse amount and currency from MarginAmount element
            Element amountElement = (Element) positionElement.getElementsByTagName("MarginAmount").item(0);
            if (amountElement == null) {
                throw new StatementParsingException("Missing MarginAmount element");
            }
            String currency = amountElement.getAttribute("currency");
            String amountStr = amountElement.getTextContent().trim();
            
            // Parse date using European format
            LocalDate effectiveDate = LocalDate.parse(dateStr, EUREX_DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Map EUREX margin categories to our position types
            MarginPosition.PositionType positionType;
            switch (marginCategoryStr.toUpperCase()) {
                case "IM":
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "VM":
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "EC":
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid EUREX margin category: " + marginCategoryStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountId);
            position.setPortfolioCode(portfolioCode.isEmpty() ? null : portfolioCode);
            position.setProductClass(productGroup.isEmpty() ? null : productGroup);
            position.setNettingSetId(nettingSet.isEmpty() ? null : nettingSet);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing position element: " + e.getMessage(), e);
        }
    }
    
    private String getElementText(Element parent, String tagName) throws StatementParsingException {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            throw new StatementParsingException("Missing required element: " + tagName);
        }
        
        Element element = (Element) nodes.item(0);
        String text = element.getTextContent().trim();
        
        if (text.isEmpty()) {
            throw new StatementParsingException("Empty value for required element: " + tagName);
        }
        
        return text;
    }
}