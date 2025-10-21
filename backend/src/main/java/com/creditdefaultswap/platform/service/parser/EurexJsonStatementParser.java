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
 * Parser for EUREX margin statements in JSON format
 * Handles EUREX-specific JSON structure and European date formats
 */
@Component
public class EurexJsonStatementParser implements StatementParser {
    
    private static final DateTimeFormatter EUREX_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(String ccpName, MarginStatement.StatementFormat format) {
        return "EUREX".equalsIgnoreCase(ccpName) && format == MarginStatement.StatementFormat.JSON;
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
            throw new StatementParsingException("Failed to parse EUREX JSON statement: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new StatementParsingException("Unexpected error parsing EUREX JSON statement: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void validateFormat(String content) throws StatementValidationException {
        try {
            JsonNode root = objectMapper.readTree(content);
            
            // Validate root structure
            if (!root.has("eurexMarginReport")) {
                throw new StatementValidationException("Missing required 'eurexMarginReport' root element");
            }
            
            JsonNode eurexMarginReport = root.get("eurexMarginReport");
            
            // Validate required sections
            if (!eurexMarginReport.has("reportMetadata")) {
                throw new StatementValidationException("Missing required 'reportMetadata' section");
            }
            
            if (!eurexMarginReport.has("clearingAccounts")) {
                throw new StatementValidationException("Missing required 'clearingAccounts' section");
            }
            
            JsonNode clearingAccounts = eurexMarginReport.get("clearingAccounts");
            if (!clearingAccounts.isArray()) {
                throw new StatementValidationException("'clearingAccounts' must be an array");
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
            JsonNode eurexMarginReport = root.get("eurexMarginReport");
            JsonNode clearingAccountsArray = eurexMarginReport.get("clearingAccounts");
            
            for (JsonNode accountNode : clearingAccountsArray) {
                String accountId = getRequiredTextValue(accountNode, "accountId");
                
                if (accountNode.has("marginPositions")) {
                    JsonNode marginPositions = accountNode.get("marginPositions");
                    if (marginPositions.isArray()) {
                        for (JsonNode positionNode : marginPositions) {
                            MarginPosition position = parseMarginPositionNode(statement, accountId, positionNode);
                            if (position != null) {
                                positions.add(position);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            throw new StatementParsingException("Error parsing JSON content: " + e.getMessage(), e);
        }
        
        return positions;
    }
    
    private MarginPosition parseMarginPositionNode(MarginStatement statement, String accountId, JsonNode positionNode) throws StatementParsingException {
        try {
            String dateStr = getRequiredTextValue(positionNode, "statementDate");
            String marginCategoryStr = getRequiredTextValue(positionNode, "marginCategory");
            
            // Parse amount - could be nested in marginAmount object
            BigDecimal amount;
            String currency;
            if (positionNode.has("marginAmount") && positionNode.get("marginAmount").isObject()) {
                JsonNode amountNode = positionNode.get("marginAmount");
                amount = getRequiredDecimalValue(amountNode, "value");
                currency = getRequiredTextValue(amountNode, "currency");
            } else {
                throw new StatementParsingException("Missing or invalid 'marginAmount' object");
            }
            
            // Optional fields
            String portfolioCode = getOptionalTextValue(positionNode, "portfolio");
            String productGroup = getOptionalTextValue(positionNode, "productGroup");
            String nettingSet = getOptionalTextValue(positionNode, "nettingSet");
            
            // Parse date using European format
            LocalDate effectiveDate = LocalDate.parse(dateStr, EUREX_DATE_FORMAT);
            
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
            position.setPortfolioCode(portfolioCode);
            position.setProductClass(productGroup);
            position.setNettingSetId(nettingSet);
            
            return position;
            
        } catch (Exception e) {
            if (e instanceof StatementParsingException) {
                throw e;
            }
            throw new StatementParsingException("Error parsing margin position: " + e.getMessage(), e);
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