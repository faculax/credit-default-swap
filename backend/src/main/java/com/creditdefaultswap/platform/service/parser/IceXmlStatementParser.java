package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 * Parser for ICE margin statements in XML format
 * Handles ICE Clear specific XML structure
 */
@Component
public class IceXmlStatementParser implements StatementParser {
    
    private static final DateTimeFormatter ICE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "ICE".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.XML;
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
            throw new StatementParsingException("Failed to parse ICE XML statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing ICE XML statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes()));
            
            // Validate root element
            Element root = doc.getDocumentElement();
            if (!"ICEClearMarginStatement".equals(root.getNodeName())) {
                throw new StatementValidationException("Invalid XML root element. Expected 'ICEClearMarginStatement', got: " + root.getNodeName());
            }
            
            // Validate required elements exist
            NodeList headerNodes = root.getElementsByTagName("StatementHeader");
            if (headerNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'StatementHeader' element");
            }
            
            NodeList memberAccountsNodes = root.getElementsByTagName("MemberAccounts");
            if (memberAccountsNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'MemberAccounts' element");
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
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(statement.getRawContent().getBytes()));
            
            // Get all Account elements
            NodeList accountNodes = doc.getElementsByTagName("Account");
            
            for (int i = 0; i < accountNodes.getLength(); i++) {
                Element accountElement = (Element) accountNodes.item(i);
                String accountId = accountElement.getAttribute("accountId");
                
                // Get all MarginPosition elements within this account
                NodeList positionNodes = accountElement.getElementsByTagName("MarginPosition");
                
                for (int j = 0; j < positionNodes.getLength(); j++) {
                    Element positionElement = (Element) positionNodes.item(j);
                    MarginPosition position = parseMarginPositionElement(statement, accountId, positionElement);
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
    
    private MarginPosition parseMarginPositionElement(MarginStatement statement, String accountId, Element positionElement) throws StatementParsingException {
        try {
            String dateStr = getElementText(positionElement, "ReportDate");
            String marginClassStr = getElementText(positionElement, "MarginClass");
            String assetClass = getElementText(positionElement, "AssetClass");
            String product = getElementText(positionElement, "Product");
            String clearingService = getElementText(positionElement, "ClearingService");
            
            // Parse amount and currency from MarginAmount element
            Element amountElement = (Element) positionElement.getElementsByTagName("MarginAmount").item(0);
            if (amountElement == null) {
                throw new StatementParsingException("Missing MarginAmount element");
            }
            String currency = amountElement.getAttribute("currency");
            String amountStr = amountElement.getTextContent().trim();
            
            // Parse date using ISO format
            LocalDate effectiveDate = LocalDate.parse(dateStr, ICE_DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Map ICE margin classes to our position types
            MarginPosition.PositionType positionType;
            switch (marginClassStr.toUpperCase()) {
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid ICE margin class: " + marginClassStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountId);
            position.setPortfolioCode(clearingService.isEmpty() ? null : clearingService);
            position.setProductClass(assetClass.isEmpty() ? null : assetClass);
            position.setNettingSetId(product.isEmpty() ? null : product);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing margin position element: " + e.getMessage(), e);
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