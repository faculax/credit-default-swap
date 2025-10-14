package com.creditdefaultswap.platform.service.parser;

import com.creditdefaultswap.platform.model.MarginStatement;
import com.creditdefaultswap.platform.model.MarginPosition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for LCH margin statements in JSON format
 */
@Component
public class LchJsonStatementParser implements StatementParser {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "LCH".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.JSON;
    }
    
    @Override
    public MarginStatement.StatementFormat getSupportedFormat() {
        return MarginStatement.StatementFormat.JSON;
    }
    
    @Override
    public List<MarginPosition> parseStatement(MarginStatement statement) throws StatementParsingException {
        try {
            validateFormat(statement.getRawContent());
            return parseJsonContent(statement);
        } catch (StatementValidationException e) {
            throw new StatementParsingException("Failed to parse LCH JSON statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing LCH JSON statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try {
            JsonNode root = objectMapper.readTree(content);
            
            // Validate root structure
            if (!root.has("marginStatement")) {
                throw new StatementValidationException("Missing required 'marginStatement' root element");
            }
            
            JsonNode marginStatement = root.get("marginStatement");
            
            // Validate required sections
            if (!marginStatement.has("header")) {
                throw new StatementValidationException("Missing required 'header' section");
            }
            
            if (!marginStatement.has("positions")) {
                throw new StatementValidationException("Missing required 'positions' section");
            }
            
            JsonNode positions = marginStatement.get("positions");
            if (!positions.isArray()) {
                throw new StatementValidationException("'positions' must be an array");
            }
            
        } catch (StatementValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new StatementValidationException("Invalid JSON format: " + e.getMessage(), e);
        }
    }
    
    private List<MarginPosition> parseJsonContent(MarginStatement statement) throws StatementParsingException {
        List<MarginPosition> positions = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(statement.getRawContent());
            JsonNode marginStatement = root.get("marginStatement");
            JsonNode positionsArray = marginStatement.get("positions");
            
            for (JsonNode positionNode : positionsArray) {
                MarginPosition position = parsePositionNode(statement, positionNode);
                if (position != null) {
                    positions.add(position);
                }
            }
            
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing JSON content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parsePositionNode(MarginStatement statement, JsonNode positionNode) throws StatementParsingException {
        try {
            String accountNumber = getRequiredTextValue(positionNode, "account");
            String dateStr = getRequiredTextValue(positionNode, "date");
            String positionTypeStr = getRequiredTextValue(positionNode, "positionType");
            BigDecimal amount = getRequiredDecimalValue(positionNode, "amount");
            String currency = getRequiredTextValue(positionNode, "currency");
            
            // Optional fields
            String portfolioCode = getOptionalTextValue(positionNode, "portfolio");
            String productClass = getOptionalTextValue(positionNode, "productClass");
            String nettingSetId = getOptionalTextValue(positionNode, "nettingSetId");
            
            // Parse date
            LocalDate effectiveDate = LocalDate.parse(dateStr, DATE_FORMAT);
            
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
            position.setPortfolioCode(portfolioCode);
            position.setProductClass(productClass);
            position.setNettingSetId(nettingSetId);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing position: " + e.getMessage(), e);
        }
    }
    
    private String getRequiredTextValue(JsonNode node, String fieldName) throws StatementParsingException {
        if (!node.has(fieldName)) {
            throw new StatementParsingException("Missing required field: " + fieldName);
        }
        
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode.isNull()) {
            throw new StatementParsingException("Required field cannot be null: " + fieldName);
        }
        
        return fieldNode.asText().trim();
    }
    
    private String getOptionalTextValue(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText().trim();
    }
    
    private BigDecimal getRequiredDecimalValue(JsonNode node, String fieldName) throws StatementParsingException {
        if (!node.has(fieldName)) {
            throw new StatementParsingException("Missing required field: " + fieldName);
        }
        
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode.isNull()) {
            throw new StatementParsingException("Required field cannot be null: " + fieldName);
        }
        
        try {
            return new BigDecimal(fieldNode.asText());
        } catch (NumberFormatException e) {
            throw new StatementParsingException("Invalid decimal value for field '" + fieldName + "': " + fieldNode.asText());
        }
    }
}