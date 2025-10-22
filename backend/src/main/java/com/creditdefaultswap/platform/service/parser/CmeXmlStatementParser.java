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
 * XML statement parser for CME margin statements
 */
@Component
public class CmeXmlStatementParser implements StatementParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseXmlContent(statement);
        } catch (Exception e) {
            throw new StatementParsingException("Failed to parse CME XML statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        if (content == null || content.trim().isEmpty()) {
            throw new StatementValidationException("Statement content is empty");
        }
        
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
            if (!"CCPMarginReport".equals(root.getLocalName())) {
                throw new StatementValidationException("Invalid XML root element. Expected 'CCPMarginReport', got: " + root.getLocalName());
            }
            
            // Validate required elements exist
            NodeList headerNodes = root.getElementsByTagName("ReportHeader");
            if (headerNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'ReportHeader' element");
            }
            
            NodeList memberAccountsNodes = root.getElementsByTagName("MemberAccounts");
            if (memberAccountsNodes.getLength() == 0) {
                throw new StatementValidationException("Missing required 'MemberAccounts' element");
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
                String accountId = accountElement.getAttribute("id");
                
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
            String dateStr = getElementText(positionElement, "PositionDate");
            String marginTypeStr = getElementText(positionElement, "MarginType");
            String portfolioCode = getElementText(positionElement, "Portfolio");
            String assetClass = getElementText(positionElement, "AssetClass");
            
            // Parse amount and currency from Amount element
            Element amountElement = (Element) positionElement.getElementsByTagName("Amount").item(0);
            if (amountElement == null) {
                throw new StatementParsingException("Missing Amount element");
            }
            String currency = amountElement.getAttribute("currency");
            String amountStr = amountElement.getTextContent().trim();
            
            // Parse date
            LocalDate effectiveDate = LocalDate.parse(dateStr, DATE_FORMAT);
            
            // Parse amount
            BigDecimal amount = new BigDecimal(amountStr);
            
            // Map CME margin type to our position type
            MarginPosition.PositionType positionType;
            switch (marginTypeStr.toUpperCase()) {
                case "VARIATION_MARGIN":
                    positionType = MarginPosition.PositionType.VARIATION_MARGIN;
                    break;
                case "INITIAL_MARGIN":
                    positionType = MarginPosition.PositionType.INITIAL_MARGIN;
                    break;
                case "EXCESS_COLLATERAL":
                    positionType = MarginPosition.PositionType.EXCESS_COLLATERAL;
                    break;
                default:
                    throw new StatementParsingException("Invalid margin type: " + marginTypeStr);
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
            position.setProductClass(assetClass.isEmpty() ? null : assetClass);
            
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
        
        String text = nodes.item(0).getTextContent();
        return text != null ? text.trim() : "";
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.XML;
    }
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "CME".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.XML;
    }
}