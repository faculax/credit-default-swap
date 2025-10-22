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
 * XML statement parser for LCH margin statements
 */
@Component
public class LchXmlStatementParser implements StatementParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseXmlContent(statement);
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse LCH XML statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        if (content == null || content.trim().isEmpty()) {
            throw new StatementValidationException("Statement content is empty");
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Security: Prevent XXE attacks (CWE-611)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes()));
            
            // Validate root element
            Element root = doc.getDocumentElement();
            if (!"marginStatement".equals(root.getNodeName())) {
                throw new StatementValidationException("Invalid XML root element. Expected 'marginStatement', got: " + root.getNodeName());
            }
            
            // Validate required elements exist
            NodeList headerNodes = root.getElementsByTagName("header");
            if (headerNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'header' element");
            }
            
            NodeList positionsNodes = root.getElementsByTagName("positions");
            if (positionsNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'positions' element");
            }
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new StatementValidationException("Invalid XML format: " + e.getMessage(), e);
        } catch (StatementValidationException e) {
            throw e;
        }
    }
    
    private List<MarginPosition> parseXmlContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Security: Prevent XXE attacks (CWE-611)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(statement.getRawContent().getBytes()));
            
            // Get positions element
            NodeList positionsNodes = doc.getElementsByTagName("positions");
            if (positionsNodes.getLength() == 0) {
                throw new StatementParsingException("No positions element found");
            }
            
            Element positionsElement = (Element) positionsNodes.item(0);
            NodeList positionNodes = positionsElement.getElementsByTagName("position");
            
            for (int i = 0; i < positionNodes.getLength(); i++) {
                Element positionElement = (Element) positionNodes.item(i);
                MarginPosition position = parsePositionElement(statement, positionElement);
                if (position != null) {
                    positions.add(position);
                }
            }
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new StatementParsingException("Error parsing XML content: " + e.getMessage(), e);
        } catch (StatementParsingException e) {
            throw e;
        }
        
        return positions;
    }
    
    private MarginPosition parsePositionElement(MarginStatement statement, Element positionElement) throws StatementParsingException {
        try {
            String accountNumber = getElementText(positionElement, "account");
            String dateStr = getElementText(positionElement, "date");
            String positionTypeStr = getElementText(positionElement, "positionType");
            String amountStr = getElementText(positionElement, "amount");
            String currency = getElementText(positionElement, "currency");
            String portfolioCode = getElementText(positionElement, "portfolio");
            String productClass = getElementText(positionElement, "productClass");
            
            // Parse date
            LocalDate effectiveDate = LocalDate.parse(dateStr, DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Parse position type
            MarginPosition.PositionType positionType;
            switch (positionTypeStr.toUpperCase()) {
                case "VM":
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "IM":
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "EXCESS":
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid position type: " + positionTypeStr);
            }
            
            // Validate business rules
            if (positionType == MarginPosition.PositionType.INITIAL_MARGIN && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new StatementParsingException("Initial margin cannot be negative: " + amount);
            }
            
            if (!currency.matches("[A-Z]{3}")) {
                throw new StatementParsingException("Invalid currency code: " + currency);
            }
            
            MarginPosition position = new MarginPosition(statement, positionType, amount, currency, effectiveDate, accountNumber);
            position.setPortfolioCode(portfolioCode.isEmpty() ? null : portfolioCode);
            position.setProductClass(productClass.isEmpty() ? null : productClass);
            
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
        
        String text = nodes.item(0).getTextContent();
        return text != null ? text.trim() : "";
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.XML;
    }
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "LCH".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.XML;
    }
}